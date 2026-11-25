package com.example.smartgymtracker;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.Calendar;

// This month as a grid (Monday to Sunday). Each day is colored by how hard the workout was
// (more volume = brighter). Today has an outline. Days pop in one by one.
public class MonthCalendarView extends View {

    private static final String[] DAY_NAMES = {"M", "T", "W", "T", "F", "S", "S"};

    private final long[] days;      // volume per day, index 0 = day 1
    private final int firstColumn;  // column of day 1 (0 = Monday)
    private final int today;
    private final int rows;
    private final Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint todayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int accent;
    private final int empty;
    private float progress = 0f;

    public MonthCalendarView(Context context, long[] days) {
        super(context);
        this.days = days;
        Calendar c = Calendar.getInstance();
        today = c.get(Calendar.DAY_OF_MONTH);
        c.set(Calendar.DAY_OF_MONTH, 1);
        firstColumn = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        rows = (firstColumn + days.length + 6) / 7;

        accent = WorkoutUi.color(context, R.color.gym_accent);
        empty = WorkoutUi.color(context, R.color.gym_surface_high);
        todayPaint.setStyle(Paint.Style.STROKE);
        todayPaint.setStrokeWidth(WorkoutUi.dp(context, 2));
        todayPaint.setColor(WorkoutUi.color(context, R.color.gym_text));
        numberPaint.setTextSize(StatsSection.sp(context, 10));
        numberPaint.setTextAlign(Paint.Align.CENTER);
        headerPaint.setTextSize(StatsSection.sp(context, 10));
        headerPaint.setTextAlign(Paint.Align.CENTER);
        headerPaint.setFakeBoldText(true);
        headerPaint.setColor(WorkoutUi.color(context, R.color.gym_text_muted));
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        // Square cells: height follows the width
        int width = MeasureSpec.getSize(widthSpec);
        float cell = width / 7f;
        int height = (int) (cell * rows + StatsSection.sp(getContext(), 20));
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ValueAnimator pop = ValueAnimator.ofFloat(0f, 1f);
        pop.setDuration(900);
        pop.setInterpolator(new LinearInterpolator());
        pop.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        pop.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float header = StatsSection.sp(getContext(), 20);
        float cell = getWidth() / 7f;
        float gap = cell * 0.12f;

        for (int col = 0; col < 7; col++) {
            canvas.drawText(DAY_NAMES[col], cell * col + cell / 2, header * 0.65f, headerPaint);
        }

        long max = 1;
        for (long day : days) {
            max = Math.max(max, day);
        }

        for (int d = 0; d < days.length; d++) {
            int position = firstColumn + d;
            float x = cell * (position % 7);
            float y = header + cell * (position / 7);

            // Pop in: each day a little after the previous one
            float t = Math.max(0f, Math.min(1f, progress * 2f - d * 0.03f));
            if (t <= 0) {
                continue;
            }
            float shrink = (cell / 2 - gap) * (1 - t);
            rect.set(x + gap + shrink, y + gap + shrink, x + cell - gap - shrink, y + cell - gap - shrink);
            float radius = cell * 0.22f;

            int level = 0;
            if (days[d] > 0) {
                level = 1 + (int) Math.min(3, 4 * days[d] / (max + 1));   // 1 to 4
            }
            if (level == 0) {
                cellPaint.setColor(empty);
            } else {
                cellPaint.setColor(accent);
                cellPaint.setAlpha(new int[]{0, 90, 150, 210, 255}[level]);
            }
            canvas.drawRoundRect(rect, radius, radius, cellPaint);
            if (d + 1 == today) {
                canvas.drawRoundRect(rect, radius, radius, todayPaint);
            }

            numberPaint.setColor(WorkoutUi.color(getContext(),
                    level >= 3 ? R.color.gym_on_accent : (level > 0 ? R.color.gym_text : R.color.gym_text_muted)));
            numberPaint.setAlpha((int) (255 * t));
            canvas.drawText(String.valueOf(d + 1), x + cell / 2, y + cell / 2 + numberPaint.getTextSize() / 3, numberPaint);
        }
    }
}
