package com.example.stopwatchapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CircularTimerView extends View {

    private Paint paint;
    private RectF rectF;
    private float sweepAngle = 0; // Angle for the arc representing time
    private long currentTimeMillis = 0;

    public CircularTimerView(Context context) {
        super(context);
        init();
    }

    public CircularTimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularTimerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true); // Smooth edges
        paint.setStyle(Paint.Style.STROKE); // Only draw the outline
        paint.setStrokeWidth(10f); // Thickness of the circle lines
        paint.setColor(Color.parseColor("#2196F3")); // Blue color for the progress

        rectF = new RectF();
    }

    /**
     * Updates the time for the circular visualizer.
     * @param timeMillis The current time in milliseconds from the stopwatch.
     */
    public void updateTime(long timeMillis) {
        this.currentTimeMillis = timeMillis;
        // One full circle (360 degrees) is 60 seconds (60,000 milliseconds)
        // Calculate the sweep angle based on current time
        // (currentTimeMillis % 60000) ensures it wraps around every minute
        sweepAngle = (float) ((currentTimeMillis % 60000) / 60000.0 * 360.0);
        invalidate(); // Request a redraw of the view
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int radius = Math.min(width, height) / 2 - (int) paint.getStrokeWidth(); // Adjust for stroke width

        // Set the bounds of the oval for the arc
        rectF.set(width / 2 - radius, height / 2 - radius,
                width / 2 + radius, height / 2 + radius);

        // Draw the background circle (full circle)
        paint.setColor(Color.parseColor("#BBDEFB")); // Lighter blue for background
        canvas.drawCircle(width / 2, height / 2, radius, paint);

        // Draw the progress arc
        paint.setColor(Color.parseColor("#2196F3")); // Blue for progress
        canvas.drawArc(rectF, -90, sweepAngle, false, paint); // Start from top (-90 degrees)

        // Draw minute/second markers (60 lines for 60 seconds/minutes)
        paint.setColor(Color.parseColor("#424242")); // Darker color for markers
        paint.setStrokeWidth(2f); // Thinner lines for markers

        for (int i = 0; i < 60; i++) {
            // Calculate angle for each marker (6 degrees per second/marker)
            float angle = (float) Math.toRadians(i * 6 - 90); // -90 to start from top

            // Calculate start and end points of the line
            float startX = (float) (width / 2 + radius * Math.cos(angle));
            float startY = (float) (height / 2 + radius * Math.sin(angle));
            float endX = (float) (width / 2 + (radius - 15) * Math.cos(angle)); // Shorter line
            float endY = (float) (height / 2 + (radius - 15) * Math.sin(angle));

            // Draw longer lines for every 5 seconds/minutes
            if (i % 5 == 0) {
                endX = (float) (width / 2 + (radius - 25) * Math.cos(angle));
                endY = (float) (height / 2 + (radius - 25) * Math.sin(angle));
            }
            canvas.drawLine(startX, startY, endX, endY, paint);
        }
    }
}