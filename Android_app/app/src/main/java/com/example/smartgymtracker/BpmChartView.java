package com.example.smartgymtracker;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.List;

// Heart rate of the last workouts: average BPM (solid line with a fill) and max BPM (dashed).
// The lines are drawn from left to right when the chart appears.
public class BpmChartView extends View {

    private final List<int[]> points;   // {avg, max}, oldest first
    private final Paint avgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint maxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int avgColor;
    private final int maxColor;
    private float progress = 0f;

    public BpmChartView(Context context, List<int[]> points) {
        super(context);
        this.points = points;
        avgColor = WorkoutUi.color(context, R.color.gym_coral);
        maxColor = WorkoutUi.color(context, R.color.gym_amber);
        float stroke = WorkoutUi.dp(context, 2);

        avgPaint.setStyle(Paint.Style.STROKE);
        avgPaint.setStrokeWidth(stroke * 1.3f);
        avgPaint.setStrokeCap(Paint.Cap.ROUND);
        avgPaint.setColor(avgColor);

        maxPaint.setStyle(Paint.Style.STROKE);
        maxPaint.setStrokeWidth(stroke);
        maxPaint.setStrokeCap(Paint.Cap.ROUND);
        maxPaint.setColor(maxColor);
        maxPaint.setPathEffect(new DashPathEffect(new float[]{stroke * 3, stroke * 2.5f}, 0));

        gridPaint.setColor(WorkoutUi.color(context, R.color.gym_stroke));
        gridPaint.setStrokeWidth(1);
        textPaint.setColor(WorkoutUi.color(context, R.color.gym_text_muted));
        textPaint.setTextSize(StatsSection.sp(context, 9));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ValueAnimator draw = ValueAnimator.ofFloat(0f, 1f);
        draw.setDuration(1100);
        draw.setInterpolator(new DecelerateInterpolator());
        draw.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        draw.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fillPaint.setShader(new LinearGradient(0, 0, 0, h,
                (avgColor & 0x00FFFFFF) | 0x44000000, avgColor & 0x00FFFFFF, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (int[] p : points) {
            min = Math.min(min, p[0]);
            max = Math.max(max, Math.max(p[0], p[1]));
        }
        min = Math.max(0, (min - 10) / 10 * 10);
        max = (max + 10) / 10 * 10;
        float range = Math.max(10, max - min);

        float left = StatsSection.sp(getContext(), 24);
        float top = WorkoutUi.dp(getContext(), 8);
        float width = getWidth() - left - WorkoutUi.dp(getContext(), 8);
        float height = getHeight() - top - WorkoutUi.dp(getContext(), 8);

        // 3 grid lines with their BPM value
        for (int i = 0; i <= 2; i++) {
            float y = top + height * i / 2f;
            canvas.drawLine(left, y, getWidth(), y, gridPaint);
            canvas.drawText(String.valueOf(Math.round(max - range * i / 2f)), 0, y + textPaint.getTextSize() / 3, textPaint);
        }

        Path avgPath = new Path();
        Path maxPath = new Path();
        int n = points.size();
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            float x = n == 1 ? left + width / 2 : left + width * i / (n - 1);
            float yAvg = top + height - height * (points.get(i)[0] - min) / range;
            float yMax = top + height - height * (points.get(i)[1] - min) / range;
            xs[i] = x;
            ys[i] = yAvg;
            if (i == 0) {
                avgPath.moveTo(x, yAvg);
                maxPath.moveTo(x, yMax);
            } else {
                float midX = (xs[i - 1] + x) / 2;
                avgPath.cubicTo(midX, ys[i - 1], midX, yAvg, x, yAvg);
                float prevMax = top + height - height * (points.get(i - 1)[1] - min) / range;
                maxPath.cubicTo(midX, prevMax, midX, yMax, x, yMax);
            }
        }

        // Reveal from left to right
        canvas.save();
        canvas.clipRect(0, 0, left + (getWidth() - left) * progress, getHeight());

        Path fill = new Path(avgPath);
        fill.lineTo(xs[n - 1], top + height);
        fill.lineTo(xs[0], top + height);
        fill.close();
        canvas.drawPath(fill, fillPaint);
        canvas.drawPath(maxPath, maxPaint);
        canvas.drawPath(avgPath, avgPaint);

        float dot = WorkoutUi.dp(getContext(), 3);
        for (int i = 0; i < n; i++) {
            dotPaint.setColor(WorkoutUi.color(getContext(), R.color.gym_surface));
            canvas.drawCircle(xs[i], ys[i], dot * 1.6f, dotPaint);
            dotPaint.setColor(avgColor);
            canvas.drawCircle(xs[i], ys[i], dot, dotPaint);
        }
        canvas.restore();
    }
}
