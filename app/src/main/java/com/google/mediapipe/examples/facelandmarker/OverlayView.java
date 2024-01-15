package com.google.mediapipe.examples.facelandmarker;

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
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import androidx.core.content.ContextCompat;

import com.google.mediapipe.tasks.components.containers.Category;
import com.google.mediapipe.tasks.components.containers.Connection;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;


import java.util.List;

public class OverlayView extends View {

    private static final float LANDMARK_STROKE_WIDTH = 8F;
    private static final String TAG = "Face Landmarker Overlay";
    private FaceLandmarkerResult results;
    private Paint linePaint;
    private Paint pointPaint;

    private float scaleFactor = 1f;
    private int imageWidth = 1;
    private int imageHeight = 1;

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        initPaints();
    }


    public void clear() {
        results = null;
        linePaint.reset();
        pointPaint.reset();
        invalidate();
        initPaints();
    }

    private void initPaints() {
        linePaint = new Paint();
        int primaryColor = 0x007F8B;
        linePaint.setColor(ContextCompat.getColor(getContext(),primaryColor));
        linePaint.setStrokeWidth(LANDMARK_STROKE_WIDTH);
        linePaint.setStyle(Paint.Style.STROKE);

        pointPaint = new Paint();
        pointPaint.setColor(Color.YELLOW);
        pointPaint.setStrokeWidth(LANDMARK_STROKE_WIDTH);
        pointPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (results == null || results.faceLandmarks().isEmpty()) {
            clear();
            return;
        }

        if (results != null) {
            FaceLandmarkerResult faceLandmarkerResult = results;

            if (faceLandmarkerResult.faceBlendshapes().isPresent()) {
                for (List<Category> blendshape : faceLandmarkerResult.faceBlendshapes().get()) {
                    for (Category point : blendshape) {
                        Log.e(TAG, point.displayName() + " " + point.score());
                    }
                }
            }

            for (List<NormalizedLandmark> landmark : faceLandmarkerResult.faceLandmarks()) {
                for (NormalizedLandmark normalizedLandmark : landmark) {
                    canvas.drawPoint(
                            normalizedLandmark.x() * imageWidth * scaleFactor,
                            normalizedLandmark.y() * imageHeight * scaleFactor,
                            pointPaint
                    );
                }
            }

            for (Connection connector : FaceLandmarker.FACE_LANDMARKS_CONNECTORS) {
                canvas.drawLine(
                        faceLandmarkerResult.faceLandmarks().get(0).get(connector.start()).x() * imageWidth * scaleFactor,
                        faceLandmarkerResult.faceLandmarks().get(0).get(connector.start()).y() * imageHeight * scaleFactor,
                        faceLandmarkerResult.faceLandmarks().get(0).get(connector.end()).x() * imageWidth * scaleFactor,
                        faceLandmarkerResult.faceLandmarks().get(0).get(connector.end()).y() * imageHeight * scaleFactor,
                        linePaint
                );
            }
        }
    }

    public void setResults(
            FaceLandmarkerResult faceLandmarkResults,
            int imageHeight,
            int imageWidth,
            RunningMode runningMode) {
        results = faceLandmarkResults;

        this.imageHeight = imageHeight;
        this.imageWidth = imageWidth;

        switch (runningMode) {
            case IMAGE:
            case VIDEO:
                scaleFactor = Math.min(getWidth() * 1f / imageWidth, getHeight() * 1f / imageHeight);
                break;
            case LIVE_STREAM:
                scaleFactor = Math.max(getWidth() * 1f / imageWidth, getHeight() * 1f / imageHeight);
                break;
        }
        invalidate();
    }

}
