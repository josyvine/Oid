package com.oid.crash.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * NEW UI COMPONENT: Draws the real-time activity pulse graph.
 * This is the 6th file required for the Pulse Graph feature.
 */
public class ActivityPulseView extends View {

    private final Paint paint = new Paint();
    private final List<Integer> pulseHistory = new ArrayList<>();
    private final int maxPoints = 40; // Number of pulses visible on screen

    public ActivityPulseView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(5f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);

        // Initialize graph with empty data (0 = no signal)
        for (int i = 0; i < maxPoints; i++) {
            pulseHistory.add(0);
        }
    }

    /**
     * Receives pulse data from HomeFragment.
     * @param status 1 = Active (Green), 2 = Background (Gray), 3 = Crash (Red)
     */
    public void postPulse(int status) {
        if (pulseHistory.size() >= maxPoints) {
            pulseHistory.remove(0);
        }
        pulseHistory.add(status);
        invalidate(); // Redraw the view immediately
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float spacing = width / (maxPoints - 1);
        float midY = height / 2;

        for (int i = 1; i < pulseHistory.size(); i++) {
            int status = pulseHistory.get(i);
            float startX = (i - 1) * spacing;
            float endX = i * spacing;

            // Set Color based on Pulse Status
            if (status == 1) {
                paint.setColor(Color.GREEN); // Target app is in foreground
            } else if (status == 2) {
                paint.setColor(Color.DKGRAY); // Target app is in background
            } else if (status == 3) {
                paint.setColor(Color.RED); // JAVA.LANG CRASH DETECTED
                paint.setStrokeWidth(8f); // Make crash line thicker
            } else {
                paint.setColor(Color.TRANSPARENT);
                paint.setStrokeWidth(5f);
            }

            // Draw the pulse line (Jagged "Heartbeat" style)
            if (status != 0) {
                float peak = (status == 3) ? midY * 0.8f : midY * 0.4f;
                // Upward stroke
                canvas.drawLine(startX, midY, startX + (spacing / 2), midY - peak, paint);
                // Downward stroke
                canvas.drawLine(startX + (spacing / 2), midY - peak, endX, midY, paint);
            } else {
                // Flat line if no data
                paint.setColor(Color.GRAY);
                canvas.drawLine(startX, midY, endX, midY, paint);
            }
            
            // Reset stroke width for next points
            paint.setStrokeWidth(5f);
        }
    }
}