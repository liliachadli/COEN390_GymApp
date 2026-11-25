package com.example.smartgymtracker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import java.util.Random;

// Small animations used on the workout screens
public class Effects {

    // Quick "pop" when a number changes (rep counter)
    public static void bump(View view) {
        view.animate().cancel();
        view.setScaleX(1.35f);
        view.setScaleY(1.35f);
        view.animate().scaleX(1f).scaleY(1f).setDuration(350)
                .setInterpolator(new OvershootInterpolator(3f)).start();
    }

    // Small vibration, like a click
    public static void tick(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    // New row slides in from the right
    public static void slideIn(View view) {
        view.setAlpha(0f);
        view.setTranslationX(WorkoutUi.dp(view.getContext(), 40));
        view.animate().alpha(1f).translationX(0).setDuration(300)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    // Cards appear one after the other, coming up from below
    public static void cascade(ViewGroup list) {
        float lift = WorkoutUi.dp(list.getContext(), 24);
        for (int i = 0; i < list.getChildCount(); i++) {
            View child = list.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(lift);
            child.animate().alpha(1f).translationY(0).setStartDelay(60L * Math.min(i, 8))
                    .setDuration(380).setInterpolator(new DecelerateInterpolator(2f)).start();
        }
    }

    // Soft breathing (used for "MOVING" and the Finish button). Returns the animator to stop it.
    public static ObjectAnimator breathe(View view, float scale) {
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, scale),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, scale));
        animator.setDuration(700);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.start();
        return animator;
    }

    public static void stop(ObjectAnimator animator, View view) {
        if (animator != null) {
            animator.cancel();
        }
        view.setScaleX(1f);
        view.setScaleY(1f);
    }

    // One heart beat: grows fast, comes back slower
    public static void heartBeat(View heart) {
        heart.animate().cancel();
        heart.animate().scaleX(1.4f).scaleY(1.4f).setDuration(110)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> heart.animate().scaleX(1f).scaleY(1f).setDuration(260)
                        .setInterpolator(new AccelerateInterpolator()).start())
                .start();
    }

    // Colored paper falling over the screen (end of a workout)
    public static void confetti(Activity activity) {
        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) {
            return;
        }
        ConfettiView view = new ConfettiView(activity);
        root.addView(view, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        view.start(() -> root.removeView(view));
    }

    private static class ConfettiView extends View {

        private static final int COUNT = 90;
        private final float[] x = new float[COUNT];
        private final float[] speedX = new float[COUNT];
        private final float[] speedY = new float[COUNT];
        private final float[] spin = new float[COUNT];
        private final int[] colors = new int[COUNT];
        private final float size;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float progress = 0f;

        ConfettiView(Context context) {
            super(context);
            setClickable(false);
            size = WorkoutUi.dp(context, 7);
            int[] palette = {R.color.gym_accent, R.color.gym_coral, R.color.gym_cyan,
                    R.color.gym_amber, R.color.gym_violet, R.color.gym_pink};
            Random random = new Random();
            for (int i = 0; i < COUNT; i++) {
                x[i] = random.nextFloat();
                speedX[i] = (random.nextFloat() - 0.5f) * 0.4f;
                speedY[i] = 0.6f + random.nextFloat() * 0.9f;
                spin[i] = random.nextFloat() * 720f;
                colors[i] = WorkoutUi.color(context, palette[i % palette.length]);
            }
        }

        void start(Runnable whenDone) {
            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(2600);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(a -> {
                progress = (float) a.getAnimatedValue();
                invalidate();
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    whenDone.run();
                }
            });
            animator.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            for (int i = 0; i < COUNT; i++) {
                float px = (x[i] + speedX[i] * progress) * w;
                float py = -size * 2 + speedY[i] * progress * h * 1.2f;
                paint.setColor(colors[i]);
                paint.setAlpha((int) (255 * Math.min(1f, (1f - progress) * 3f)));
                canvas.save();
                canvas.rotate(spin[i] * progress, px, py);
                canvas.drawRect(px, py, px + size, py + size * 0.5f, paint);
                canvas.restore();
            }
        }
    }
}
