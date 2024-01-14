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
package com.google.mediapipe.examples.facelandmarker.fragment;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import static androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.examples.facelandmarker.FaceLandmarkerHelper;
import com.google.mediapipe.examples.facelandmarker.MainViewModel;
import com.google.mediapipe.examples.facelandmarker.R;
import com.google.mediapipe.examples.facelandmarker.databinding.FragmentCameraBinding;
import com.google.mediapipe.tasks.vision.core.RunningMode;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author LiHao Liao
 * @version 1.0
 * @Package_Name com.motioninput.eyegaze
 * @date 2024/01/13 12:50
 * @Description
 * @since 1.0
 */
public class CameraFragment extends Fragment implements FaceLandmarkerHelper.LandmarkerListener{
    private static final String TAG = "Face Landmarker";

    private FragmentCameraBinding fragmentCameraBinding = null;
    private FaceLandmarkerHelper faceLandmarkerHelper;
    private MainViewModel viewModel;
    private FaceBlendshapesResultAdapter faceBlendshapesResultAdapter;

    private Preview preview = null;
    private ImageAnalysis imageAnalyzer = null;
    private Camera camera = null;
    private ProcessCameraProvider cameraProvider = null;
    private int cameraFacing = CameraSelector.LENS_FACING_FRONT;
    private ExecutorService backgroundExecutor;

    public CameraFragment() {
        // Initialize lazy properties here or in another appropriate place
        faceBlendshapesResultAdapter = new FaceBlendshapesResultAdapter();
        // ViewModel initialization (use ViewModelProviders)
        viewModel = new MainViewModel();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check if all permissions are still present
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                    .navigate(R.id.action_camera_to_permissions);
        }

        // Restart or initialize FaceLandmarkerHelper when the app returns to the foreground
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (faceLandmarkerHelper.isClose()) {
                    faceLandmarkerHelper.setupFaceLandmarker();
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (faceLandmarkerHelper != null) {
            viewModel.setMaxFaces(faceLandmarkerHelper.getMaxNumFaces());
            viewModel.setMinFaceDetectionConfidence(faceLandmarkerHelper.getMinFaceDetectionConfidence());
            viewModel.setMinFaceTrackingConfidence(faceLandmarkerHelper.getMinFaceTrackingConfidence());
            viewModel.setMinFacePresenceConfidence(faceLandmarkerHelper.getMinFacePresenceConfidence());
            viewModel.setDelegate(faceLandmarkerHelper.getCurrentDelegate());

            // Close the FaceLandmarkerHelper and release resources
            backgroundExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    faceLandmarkerHelper.clearFaceLandmarker();
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        // Release the binding
        fragmentCameraBinding = null;
        super.onDestroyView();

        // Shut down the background executor
        backgroundExecutor.shutdown();
        try {
            if (!backgroundExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                backgroundExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            backgroundExecutor.shutdownNow();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false);
        return fragmentCameraBinding.getRoot();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up RecyclerView with its layout manager and adapter
        fragmentCameraBinding.recyclerviewResults.setLayoutManager(new LinearLayoutManager(getContext()));
        fragmentCameraBinding.recyclerviewResults.setAdapter(faceBlendshapesResultAdapter);

        // Initialize the background executor
        backgroundExecutor = Executors.newSingleThreadExecutor();

        // Set up the camera after the layout has been properly laid out
        fragmentCameraBinding.viewFinder.post(new Runnable() {
            @Override
            public void run() {
                setUpCamera();
            }
        });

        // Initialize the FaceLandmarkerHelper in the background
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                faceLandmarkerHelper = new FaceLandmarkerHelper(
                        viewModel.getCurrentMinFaceDetectionConfidence(),
                        viewModel.getCurrentMinFaceTrackingConfidence(),
                        viewModel.getCurrentMinFacePresenceConfidence(),
                        viewModel.getCurrentMaxFaces(),
                        viewModel.getCurrentDelegate(),
                        RunningMode.LIVE_STREAM,
                        getContext(),
                        faceLandmarkerHelper.getFaceLandmarkerHelperListener() // Assuming 'this' refers to an implementation of FaceLandmarkerHelperListener
                );
            }
        });

        // Initialize UI control listeners
        initBottomSheetControls();
    }

    private void initBottomSheetControls() {
        // Initialize bottom sheet settings
        fragmentCameraBinding.bottomSheetLayout.maxFacesValue.setText(String.valueOf(faceLandmarkerHelper.getMaxNumFaces()));
        fragmentCameraBinding.bottomSheetLayout.detectionThresholdValue.setText(String.format(Locale.US, "%.2f", faceLandmarkerHelper.getMinFaceDetectionConfidence()));
        fragmentCameraBinding.bottomSheetLayout.trackingThresholdValue.setText(String.format(Locale.US, "%.2f", faceLandmarkerHelper.getMinFaceTrackingConfidence()));
        fragmentCameraBinding.bottomSheetLayout.presenceThresholdValue.setText(String.format(Locale.US, "%.2f", faceLandmarkerHelper.getMinFacePresenceConfidence()));

        // Set click listeners for each control button
        fragmentCameraBinding.bottomSheetLayout.detectionThresholdMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Decrease detection threshold and update UI
                // Similar implementation as in Kotlin
            }
        });

        // ... Additional click listeners for other controls ...

        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Handle item selection
                // Similar implementation as in Kotlin
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle no selection
            }
        });
    }

    // Implement updateControlsUi method as needed
    private void updateControlsUi() {
        fragmentCameraBinding.bottomSheetLayout.maxFacesValue.setText(String.valueOf(faceLandmarkerHelper.getMaxNumFaces()));
        fragmentCameraBinding.bottomSheetLayout.detectionThresholdValue.setText(String.format(Locale.US, "%.2f", faceLandmarkerHelper.getMinFaceDetectionConfidence()));
        fragmentCameraBinding.bottomSheetLayout.trackingThresholdValue.setText(String.format(Locale.US, "%.2f", faceLandmarkerHelper.getMinFaceTrackingConfidence()));
        fragmentCameraBinding.bottomSheetLayout.presenceThresholdValue.setText(String.format(Locale.US, "%.2f", faceLandmarkerHelper.getMinFacePresenceConfidence()));

        // Clearing and setting up the FaceLandmarkerHelper in the background
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                faceLandmarkerHelper.clearFaceLandmarker();
                faceLandmarkerHelper.setupFaceLandmarker();
            }
        });

        // Clear the overlay
        fragmentCameraBinding.overlay.clear();
    }

    private void setUpCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    // CameraProvider
                    cameraProvider = cameraProviderFuture.get();

                    // Build and bind the camera use cases
                    bindCameraUseCases();
                } catch (ExecutionException | InterruptedException e) {
                    // Handle exceptions
                    Log.e(TAG, "Error setting up camera provider", e);
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void bindCameraUseCases() {
        // CameraProvider
        ProcessCameraProvider cameraProvider = this.cameraProvider;
        if (cameraProvider == null) {
            throw new IllegalStateException("Camera initialization failed.");
        }

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cameraFacing)
                .build();

        // Preview
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.getDisplay().getRotation())
                .build();

        // ImageAnalysis
        ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.getDisplay().getRotation())
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();

        imageAnalyzer.setAnalyzer(backgroundExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                detectFace(image);
            }
        });

        // Unbind use cases before rebinding
        cameraProvider.unbindAll();

        try {
            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
            );

            // Attach surface provider
            preview.setSurfaceProvider(fragmentCameraBinding.viewFinder.getSurfaceProvider());
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void detectFace(ImageProxy imageProxy) {
        faceLandmarkerHelper.detectLiveStream(
                imageProxy,
                cameraFacing == CameraSelector.LENS_FACING_FRONT
        );
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (imageAnalyzer != null) {
            imageAnalyzer.setTargetRotation(fragmentCameraBinding.viewFinder.getDisplay().getRotation());
        }
    }
    @Override
    public void onResults(FaceLandmarkerHelper.ResultBundle resultBundle) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (fragmentCameraBinding != null) {
                        if (fragmentCameraBinding.recyclerviewResults.getScrollState() != SCROLL_STATE_DRAGGING) {
                            faceBlendshapesResultAdapter.updateResults(resultBundle.getResult());
                            faceBlendshapesResultAdapter.notifyDataSetChanged();
                        }

                        fragmentCameraBinding.bottomSheetLayout.inferenceTimeVal.setText(
                                String.format(Locale.getDefault(), "%d ms", resultBundle.getInferenceTime())
                        );

                        // Update OverlayView with the results
                        fragmentCameraBinding.overlay.setResults(
                                resultBundle.getResult(),
                                resultBundle.getInputImageHeight(),
                                resultBundle.getInputImageWidth(),
                                RunningMode.LIVE_STREAM
                        );
                        // Redraw the OverlayView
                        fragmentCameraBinding.overlay.invalidate();
                    }
                }
            });
        }
    }
    public void onEmpty() {
        fragmentCameraBinding.overlay.clear();
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    faceBlendshapesResultAdapter.updateResults(null);
                    faceBlendshapesResultAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    public void onError(String error, int errorCode) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    faceBlendshapesResultAdapter.updateResults(null);
                    faceBlendshapesResultAdapter.notifyDataSetChanged();

                    if (errorCode == FaceLandmarkerHelper.GPU_ERROR) {
                        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(
                                FaceLandmarkerHelper.DELEGATE_CPU, false
                        );
                    }
                }
            });
        }
    }







    // ... Additional methods and lifecycle implementations ...
}
