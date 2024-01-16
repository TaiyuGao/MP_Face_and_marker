package com.google.mediapipe.examples.facelandmarker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.google.mediapipe.tasks.components.containers.Connection;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;

import java.util.List;
import java.util.Set;


public class OverlayView extends View {

    private FaceLandmarkerResult results;
    private Paint linePaint = new Paint();
    private Paint pointPaint = new Paint();

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
        linePaint.setColor(ContextCompat.getColor(getContext(), R.color.mp_color_primary));
        linePaint.setStrokeWidth(LANDMARK_STROKE_WIDTH);
        linePaint.setStyle(Paint.Style.STROKE);

        pointPaint.setColor(Color.YELLOW);
        pointPaint.setStrokeWidth(LANDMARK_STROKE_WIDTH);
        pointPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (results == null || results.faceLandmarks().isEmpty()) {
            clear();
            return;
        }

        for (List<NormalizedLandmark> landmark : results.faceLandmarks()) {
            for (NormalizedLandmark normalizedLandmark : landmark) {
                canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        pointPaint);
            }
        }

        for (Connection connector : FaceLandmarker.FACE_LANDMARKS_CONNECTORS) {
            canvas.drawLine(
                    results.faceLandmarks().get(0).get(connector.start()).x() * imageWidth * scaleFactor,
                    results.faceLandmarks().get(0).get(connector.start()).y() * imageHeight * scaleFactor,
                    results.faceLandmarks().get(0).get(connector.end()).x() * imageWidth * scaleFactor,
                    results.faceLandmarks().get(0).get(connector.end()).y() * imageHeight * scaleFactor,
                    linePaint);
        }
    }

    public void setResults(FaceLandmarkerResult faceLandmarkerResults, int imageHeight, int imageWidth, RunningMode runningMode) {
        results = faceLandmarkerResults;
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

    private static final float LANDMARK_STROKE_WIDTH = 8F;
    private static final String TAG = "Face Landmarker Overlay";
}

