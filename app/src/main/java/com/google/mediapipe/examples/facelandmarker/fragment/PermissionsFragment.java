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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import androidx.lifecycle.LifecycleObserver;
import androidx.navigation.Navigation;

import com.google.mediapipe.examples.facelandmarker.R;

//import androidx.lifecycle.lifecycleScope;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

public class PermissionsFragment extends Fragment {

    //new
    private final LifecycleOwner lifecycleOwner;
    public PermissionsFragment(LifecycleOwner lifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner;
        lifecycleOwner.getLifecycle().addObserver((LifecycleObserver) this);
    }

    private static final String[] PERMISSIONS_REQUIRED = {Manifest.permission.CAMERA};

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            Toast.makeText(
                                    requireContext(),
                                    "Permission request granted",
                                    Toast.LENGTH_LONG
                            ).show();
                            navigateToCamera();
                        } else {
                            Toast.makeText(
                                    requireContext(),
                                    "Permission request denied",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
        )) {
            navigateToCamera();
        } else {
            requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
            );
        }
    }

    private void navigateToCamera() {
        if (lifecycleOwner instanceof Fragment) {
            Fragment fragment = (Fragment) lifecycleOwner;
            Navigation.findNavController(
                    fragment.requireActivity(),
                    R.id.fragment_container
            ).navigate(
                    R.id.action_permissions_to_camera
            );
        }
    }

    /**
     * Convenience method used to check if all permissions required by this app are granted
     */
    public static boolean hasPermissions(Context context) {
        for (String permission : PERMISSIONS_REQUIRED) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}

