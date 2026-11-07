package com.example.smartgymtracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

// Simple line graph of the heart rate (BPM over time)
public class HeartRateGraphView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private List<Integer> values = new ArrayList<>();

    public HeartRateGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(WorkoutUi.dp(context, 2));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(WorkoutUi.color(context, R.color.gym_coral));
    }

    public void setValues(List<Integer> newValues) {
        values = new ArrayList<>(newValues);
        invalidate();
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
        float margin = paint.getStrokeWidth();
        float width = getWidth() - 2 * margin;
        float height = getHeight() - 2 * margin;

        path.reset();
        for (int i = 0; i < values.size(); i++) {
            float x = margin + width * i / (values.size() - 1);
            float y = margin + height - height * (values.get(i) - min) / range;
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        canvas.drawPath(path, paint);
    }
}
