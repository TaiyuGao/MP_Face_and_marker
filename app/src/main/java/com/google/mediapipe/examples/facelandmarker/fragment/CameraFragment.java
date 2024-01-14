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
import androidx.camera.core.Preview;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Camera;
import androidx.camera.core.AspectRatio;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
//import androidx.fragment.app.activityViewModels;
import androidx.navigation.Navigation;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING;
//import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE;
//import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_SETTLING;
import androidx.viewpager2.widget.ViewPager2.ScrollState;
//import com.google.mediapipe.examples.facelandmarker.FaceLandmarkerHelper;
import com.google.mediapipe.examples.facelandmarker.MainViewModel;
import com.google.mediapipe.examples.facelandmarker.R;
import com.google.mediapipe.examples.facelandmarker.databinding.FragmentCameraBinding;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

