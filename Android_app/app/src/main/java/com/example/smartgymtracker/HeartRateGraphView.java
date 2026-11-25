package com.example.smartgymtracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

// Line graph of the heart rate (BPM over time): smooth line, colored fill under it
// and a glowing dot on the last value
public class HeartRateGraphView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Path fillPath = new Path();
    private final int color;
    private List<Integer> values = new ArrayList<>();

    public HeartRateGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        color = WorkoutUi.color(context, R.color.gym_coral);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(WorkoutUi.dp(context, 2));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(color);
        fillPaint.setStyle(Paint.Style.FILL);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(color);
    }

    public void setValues(List<Integer> newValues) {
        values = new ArrayList<>(newValues);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Fill fades from the line color to transparent at the bottom
        int top = (color & 0x00FFFFFF) | 0x55000000;
        fillPaint.setShader(new LinearGradient(0, 0, 0, h, top, color & 0x00FFFFFF, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values.size() < 2) {
            return;
        }

        // Lowest value at the bottom, highest at the top
        int min = values.get(0);
        int max = values.get(0);
        for (int value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        float range = Math.max(10, max - min);
        float margin = WorkoutUi.dp(getContext(), 6);
        float width = getWidth() - 2 * margin;
        float height = getHeight() - 2 * margin;

        path.reset();
        float lastX = 0;
        float lastY = 0;
        for (int i = 0; i < values.size(); i++) {
            float x = margin + width * i / (values.size() - 1);
            float y = margin + height - height * (values.get(i) - min) / range;
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                // Curve through the middle of each segment: smooth line
                path.quadTo(lastX, lastY, (lastX + x) / 2, (lastY + y) / 2);
            }
            lastX = x;
            lastY = y;
        }
        path.lineTo(lastX, lastY);

        fillPath.set(path);
        fillPath.lineTo(lastX, getHeight());
        fillPath.lineTo(margin, getHeight());
        fillPath.close();
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(path, paint);

        // Last value: dot with a halo that grows and fades, again and again
        float phase = (SystemClock.uptimeMillis() % 1200) / 1200f;
        float dot = WorkoutUi.dp(getContext(), 4);
        dotPaint.setAlpha((int) (110 * (1 - phase)));
        canvas.drawCircle(lastX, lastY, dot + dot * 2.5f * phase, dotPaint);
        dotPaint.setAlpha(255);
        canvas.drawCircle(lastX, lastY, dot, dotPaint);
        postInvalidateOnAnimation();
    }
}
