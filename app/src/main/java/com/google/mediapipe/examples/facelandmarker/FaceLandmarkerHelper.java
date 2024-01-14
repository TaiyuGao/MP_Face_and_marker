/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mediapipe.examples.facelandmarker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ImageProxy;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;

import java.util.List;

public class FaceLandmarkerHelper {
    //Default settings
    public static Companion Companion = new Companion();


    final int DelegateCpu = Companion.getDelegateCpu();
    final int DelegateGpu =Companion.getDelegateGpu();
    Float minFaceDetectionConfidence = Companion.getDefaultFaceDetectionConfidence();
    Float minFaceTrackingConfidence = Companion.getDefaultFaceTrackingConfidence();
    Float minFacePresenceConfidence = Companion.getDefaultFacePresenceConfidence();
    int maxNumFaces = Companion.getDefaultNumFaces();
    int currentDelegate = Companion.getDelegateCpu();
    int errorCode = Companion.getOtherError();
    int gpuError = Companion.getGpuError();
    String mpFaceLandmarkerTask =Companion.getMpFaceLandmarkerTask();
    String tag = Companion.getTag();
    RunningMode runningMode = RunningMode.IMAGE;
    Context context;
    // this listener is only used when running in RunningMode.LIVE_STREAM
    LandmarkerListener faceLandmarkerHelperListener = null;


    public FaceLandmarkerHelper(
            Float minFaceDetectionConfidence,
            Float minFaceTrackingConfidence,
            Float minFacePresenceConfidence,
            int maxNumFaces,
            int currentDelegate,
            RunningMode runningMode,
            Context context,
            LandmarkerListener faceLandmarkerHelperListener
    ){
        this.minFaceDetectionConfidence = minFaceDetectionConfidence;
        this.minFaceTrackingConfidence = minFaceTrackingConfidence;
        this.minFacePresenceConfidence = minFacePresenceConfidence;
        this.maxNumFaces = maxNumFaces;
        this.currentDelegate = currentDelegate;
        this.runningMode = runningMode;
        this.context = context;
        this.faceLandmarkerHelperListener = faceLandmarkerHelperListener;


        setupFaceLandmarker();
    }

    // For this example this needs to be a var so it can be reset on changes.
    // If the Face Landmarker will not change, a lazy val would be preferable.
    private FaceLandmarker faceLandmarker;


    public void clearFaceLandmarker() {
        if (faceLandmarker != null) {
            faceLandmarker.close();
        }
        faceLandmarker = null;
    }

    // Return running status of FaceLandmarkerHelper
    public boolean isClose(){
        return faceLandmarker == null;
    }

    // Initialize the Face landmarker using current settings on the
    // thread that is using it. CPU can be used with Landmarker
    // that are created on the main thread and used on a background thread, but
    // the GPU delegate needs to be used on the thread that initialized the
    // Landmarker
    public void setupFaceLandmarker(){
        // Set general face landmarker options
        BaseOptions.Builder baseOptionBuilder = BaseOptions.builder();

        // Use the specified hardware for running the model. Default to CPU
        // Use the specified hardware for running the model. Default to CPU
        if (currentDelegate == DelegateCpu) {
            baseOptionBuilder.setDelegate(Delegate.CPU);
        } else if (currentDelegate == DelegateGpu) {
            baseOptionBuilder.setDelegate(Delegate.GPU);
        }


        baseOptionBuilder.setModelAssetPath(mpFaceLandmarkerTask);

        // Check if runningMode is consistent with faceLandmarkerHelperListener
        if (runningMode == RunningMode.LIVE_STREAM) {
            if (faceLandmarkerHelperListener == null) {
                throw new IllegalStateException(
                        "faceLandmarkerHelperListener must be set when runningMode is LIVE_STREAM."
                );
            }
        } else {
            // no-op
        }

        try {
            BaseOptions baseOptions = baseOptionBuilder.build();
            // Create an option builder with base options and specific
            // options only use for Face Landmarker.
            FaceLandmarker.FaceLandmarkerOptions.Builder optionsBuilder =
                    FaceLandmarker.FaceLandmarkerOptions.builder()
                            .setBaseOptions(baseOptions)
                            .setMinFaceDetectionConfidence(minFaceDetectionConfidence)
                            .setMinTrackingConfidence(minFaceTrackingConfidence)
                            .setMinFacePresenceConfidence(minFacePresenceConfidence)
                            .setNumFaces(maxNumFaces)
                            .setOutputFaceBlendshapes(true)
                            .setRunningMode(runningMode);

            // The ResultListener and ErrorListener only use for LIVE_STREAM mode.
            if (runningMode == RunningMode.LIVE_STREAM) {
                optionsBuilder
                        .setResultListener(this::returnLivestreamResult)
                        .setErrorListener(this::returnLivestreamError);
            }

            FaceLandmarker.FaceLandmarkerOptions options = optionsBuilder.build();
            faceLandmarker =
                    FaceLandmarker.createFromOptions(context, options);
        } catch (IllegalStateException e) {
            if (faceLandmarkerHelperListener != null) {
                faceLandmarkerHelperListener.onError(
                        "Face Landmarker failed to initialize. See error logs for details",
                        errorCode
                );
            }
            Log.e(tag, "MediaPipe failed to load the task with error: " + e.getMessage());
        }catch (RuntimeException e) {
            // This occurs if the model being used does not support GPU
            if (faceLandmarkerHelperListener != null){
                faceLandmarkerHelperListener.onError(
                        "Face Landmarker failed to initialize. See error logs for details",
                        gpuError
                );
            }
            Log.e(
                    tag,
                    "Face Landmarker failed to load model with error: " + e.getMessage()
            );
        }
    }

    public void detectLiveStream(ImageProxy imageProxy, boolean isFrontCamera) {
        if (runningMode != RunningMode.LIVE_STREAM) {
            throw new IllegalArgumentException(
                    "Attempting to call detectLiveStream while not using RunningMode.LIVE_STREAM"
            );
        }

        long frameTime = SystemClock.uptimeMillis();

        // Copy out RGB bits from the frame to a bitmap buffer
        Bitmap bitmapBuffer = Bitmap.createBitmap(
                imageProxy.getWidth(),
                imageProxy.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        try {
            bitmapBuffer.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());
        } finally {
            imageProxy.close();
        }

        Matrix matrix = new Matrix();
        // Rotate the frame received from the camera to be in the same direction as it'll be shown
        matrix.postRotate((float) imageProxy.getImageInfo().getRotationDegrees());

        // Flip image if user uses front camera
        if (isFrontCamera) {
            matrix.postScale(
                    -1f,
                    1f,
                    (float) imageProxy.getWidth(),
                    (float) imageProxy.getHeight()
            );
        }

        Bitmap rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.getWidth(), bitmapBuffer.getHeight(),
                matrix, true
        );

        // Convert the input Bitmap object to an MPImage object to run inference
        MPImage mpImage = new BitmapImageBuilder(rotatedBitmap).build();

        detectAsync(mpImage, frameTime);
    }
    @VisibleForTesting
    public void detectAsync(MPImage mpImage, long frameTime) {
        if (faceLandmarker != null) {
            faceLandmarker.detectAsync(mpImage, frameTime);
            // As we're using running mode LIVE_STREAM, the landmark result will
            // be returned in returnLivestreamResult function
        }
    }
    private void returnLivestreamResult(FaceLandmarkerResult result, MPImage input) {
        if (result.faceLandmarks().size() > 0) {
            long finishTimeMs = SystemClock.uptimeMillis();
            long inferenceTime = finishTimeMs - result.timestampMs();

            if (faceLandmarkerHelperListener != null) {
                faceLandmarkerHelperListener.onResults(
                        new ResultBundle(
                                result,
                                inferenceTime,
                                input.getHeight(),
                                input.getWidth()
                        )
                );
            }
        } else {
            if (faceLandmarkerHelperListener != null) {
                faceLandmarkerHelperListener.onEmpty();
            }
        }
    }

    private void returnLivestreamError(RuntimeException error) {
        if (faceLandmarkerHelperListener != null) {
            faceLandmarkerHelperListener.onError(
                    error.getMessage() != null ? error.getMessage() : "An unknown error has occurred"
            );
        }
    }





    public static final class Companion {
        private static final String TAG =  "FaceLandmarkerHelper";
        private static final String MP_FACE_LANDMARKER_TASK = "face_landmarker.task";

        private static final int DELEGATE_CPU = 0;
        private static final int DELEGATE_GPU = 1;
        private static final Float DEFAULT_FACE_DETECTION_CONFIDENCE = 0.5F;
        private static final Float DEFAULT_FACE_TRACKING_CONFIDENCE = 0.5F;
        private static final Float DEFAULT_FACE_PRESENCE_CONFIDENCE = 0.5F;
        private static final int DEFAULT_NUM_FACES = 1;
        private static final int OTHER_ERROR = 0;
        private static final int GPU_ERROR = 1;

        public final String getTag(){
            return TAG;
        }

        public final String getMpFaceLandmarkerTask(){
            return MP_FACE_LANDMARKER_TASK;
        }

        public final int getDelegateCpu(){
            return DELEGATE_CPU;
        }

        public final int getDelegateGpu(){
            return DELEGATE_GPU;
        }

        public final int getDefaultNumFaces(){
            return DEFAULT_NUM_FACES;
        }

        public final int getOtherError(){
            return OTHER_ERROR;
        }

        public final int getGpuError(){
            return GPU_ERROR;
        }

        public final Float getDefaultFaceDetectionConfidence(){
            return DEFAULT_FACE_DETECTION_CONFIDENCE;
        }

        public final Float getDefaultFaceTrackingConfidence(){
            return DEFAULT_FACE_TRACKING_CONFIDENCE;
        }

        public final Float getDefaultFacePresenceConfidence(){
            return DEFAULT_FACE_PRESENCE_CONFIDENCE;
        }
    }




    public static class ResultBundle {
        private final FaceLandmarkerResult result;
        private final long inferenceTime;
        private final int inputImageHeight;
        private final int inputImageWidth;

        public ResultBundle(FaceLandmarkerResult result, long inferenceTime, int inputImageHeight, int inputImageWidth) {
            this.result = result;
            this.inferenceTime = inferenceTime;
            this.inputImageHeight = inputImageHeight;
            this.inputImageWidth = inputImageWidth;
        }

        public FaceLandmarkerResult getResult() {
            return result;
        }

        public long getInferenceTime() {
            return inferenceTime;
        }

        public int getInputImageHeight() {
            return inputImageHeight;
        }

        public int getInputImageWidth() {
            return inputImageWidth;
        }
    }




    public class VideoResultBundle {
        private final List<FaceLandmarkerResult> results;
        private final long inferenceTime;
        private final int inputImageHeight;
        private final int inputImageWidth;

        public VideoResultBundle(List<FaceLandmarkerResult> results, long inferenceTime, int inputImageHeight, int inputImageWidth) {
            this.results = results;
            this.inferenceTime = inferenceTime;
            this.inputImageHeight = inputImageHeight;
            this.inputImageWidth = inputImageWidth;
        }

        public List<FaceLandmarkerResult> getResults() {
            return results;
        }

        public long getInferenceTime() {
            return inferenceTime;
        }

        public int getInputImageHeight() {
            return inputImageHeight;
        }

        public int getInputImageWidth() {
            return inputImageWidth;
        }

        @Override
        public String toString() {
            return "VideoResultBundle{" +
                    "results=" + results +
                    ", inferenceTime=" + inferenceTime +
                    ", inputImageHeight=" + inputImageHeight +
                    ", inputImageWidth=" + inputImageWidth +
                    '}';
        }
    }



    public interface LandmarkerListener {
        void onError(String error, int errorCode);

        default void onError(String error) {
            onError(error, FaceLandmarkerHelper.OTHER_ERROR);
        }

        void onResults(ResultBundle resultBundle);
        void onEmpty();
    }


}

