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

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.google.mediapipe.examples.facelandmarker.databinding.FaceBlendshapesResultBinding;
import com.google.mediapipe.tasks.components.containers.Category;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class FaceBlendshapesResultAdapter extends RecyclerView.Adapter<FaceBlendshapesResultAdapter.ViewHolder> {
    private static final String NO_VALUE = "--";

    private List<Category> categories = new ArrayList<>(Collections.nCopies(52, null));

    public void updateResults(FaceLandmarkerResult faceLandmarkerResult) {
        categories = new ArrayList<>(Collections.nCopies(52, null));
        if (faceLandmarkerResult != null) {
            Optional<List<List<Category>>> optionalFaceBlendShapes = faceLandmarkerResult.faceBlendshapes(); // Update this based on the actual method name

            // Unwrap the Optional and get the inner list
            List<List<Category>> faceBlendShapes = optionalFaceBlendShapes.orElse(Collections.emptyList());
            if (!faceBlendShapes.isEmpty()) {
                List<Category> sortedCategories = faceBlendShapes.get(0);
                sortedCategories.sort((category1, category2) -> Float.compare(category2.score(), category1.score()));
                int min = Math.min(sortedCategories.size(), categories.size());
                for (int i = 0; i < min; i++) {
                    categories.set(i, sortedCategories.get(i));
                }
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        FaceBlendshapesResultBinding binding = FaceBlendshapesResultBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.bind(category != null ? category.categoryName() : null, category != null ? category.score() : null);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final FaceBlendshapesResultBinding binding;

        public ViewHolder(FaceBlendshapesResultBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(String label, Float score) {
            binding.tvLabel.setText(label != null ? label : NO_VALUE);
            binding.tvScore.setText(score != null ? String.format("%.2f", score) : NO_VALUE);
        }
    }
}

