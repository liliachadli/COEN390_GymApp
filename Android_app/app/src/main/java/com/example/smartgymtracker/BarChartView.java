package com.example.smartgymtracker;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

// Bar chart (volume per week). The last bar (this week) is full color, the others lighter.
// Bars grow from the bottom when the chart appears.
public class BarChartView extends View {

    private final long[] values;
    private final String[] labels;
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int color;
    private float progress = 0f;

    public BarChartView(Context context, long[] values, String[] labels, int colorRes) {
        super(context);
        this.values = values;
        this.labels = labels;
        color = WorkoutUi.color(context, colorRes);
        trackPaint.setColor(WorkoutUi.color(context, R.color.gym_surface_high));
        labelPaint.setColor(WorkoutUi.color(context, R.color.gym_text_secondary));
        labelPaint.setTextSize(StatsSection.sp(context, 9));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setColor(WorkoutUi.color(context, R.color.gym_text));
        valuePaint.setTextSize(StatsSection.sp(context, 10));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setFakeBoldText(true);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ValueAnimator grow = ValueAnimator.ofFloat(0f, 1f);
        grow.setDuration(900);
        grow.setInterpolator(new DecelerateInterpolator(2f));
        grow.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        grow.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long max = 1;
        for (long value : values) {
            max = Math.max(max, value);
        }
        float labelSpace = StatsSection.sp(getContext(), 18);
        float valueSpace = StatsSection.sp(getContext(), 16);
        float chartHeight = getHeight() - labelSpace - valueSpace;
        float slot = (float) getWidth() / values.length;
        float barWidth = slot * 0.56f;
        float radius = barWidth / 3;

        for (int i = 0; i < values.length; i++) {
            float center = slot * i + slot / 2;
            float left = center - barWidth / 2;
            float bottom = valueSpace + chartHeight;

            // Light track behind every bar
            rect.set(left, valueSpace, left + barWidth, bottom);
            canvas.drawRoundRect(rect, radius, radius, trackPaint);

            // Each bar starts growing a little after the previous one
            float barProgress = Math.max(0f, Math.min(1f, progress * 1.6f - i * 0.08f));
            float height = chartHeight * values[i] / max * barProgress;
            if (height > 0) {
                boolean last = i == values.length - 1;
                barPaint.setColor(color);
                barPaint.setAlpha(last ? 255 : 120);
                rect.set(left, bottom - Math.max(height, radius * 2), left + barWidth, bottom);
                canvas.drawRoundRect(rect, radius, radius, barPaint);
                if (barProgress >= 1f && (last || values[i] == max)) {
                    canvas.drawText(StatsSection.compact(values[i]), center, bottom - height - StatsSection.sp(getContext(), 4), valuePaint);
                }
            }
            canvas.drawText(labels[i], center, getHeight() - StatsSection.sp(getContext(), 4), labelPaint);
        }
    }
}
