package com.example.smartgymtracker;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

// Keeps the app alive during a workout, even when the phone is locked or the user
// is on another app. Android shows a notification while it runs ("Workout in progress").
// The Bluetooth connection and WorkoutSession keep working because the app is not killed.
public class WorkoutService extends Service {

    private static final String CHANNEL_ID = "workout";
    private static final int NOTIFICATION_ID = 1;
    private static final long UPDATE_MS = 2000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String lastText = "";
    private int saveCounter = 0;

    // Call when a workout starts (does nothing if it is already running)
    public static void start(Context context) {
        // Android 14+ only allows this service once Bluetooth permission is given
        if (Build.VERSION.SDK_INT >= 34
                && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            ContextCompat.startForegroundService(context, new Intent(context, WorkoutService.class));
        } catch (Exception e) {
            // Android can refuse in rare cases (app already in the background): the workout still works on screen
            Log.e("WorkoutService", "Could not start the workout service", e);
        }
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, WorkoutService.class));
    }

    // The notification channel must exist before the first notification (called in GymApp)
    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Workout in progress",
                    NotificationManager.IMPORTANCE_LOW);   // low = no sound
            channel.setDescription("Shown while a workout is running");
            context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = buildNotification();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            // e.g. Bluetooth permission not given yet on Android 14+
            Log.e("WorkoutService", "Could not run in the foreground", e);
            stopSelf();
            return START_NOT_STICKY;
        }
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(updater, UPDATE_MS);
        return START_NOT_STICKY;
    }

    // Every 2 s: stop when the workout is over, otherwise refresh the notification text
    private final Runnable updater = new Runnable() {
        @Override
        public void run() {
            if (!WorkoutSession.active) {
                stopSelf();
                return;
            }
            saveCounter++;
            if (saveCounter % 5 == 0) {
                WorkoutStore.save();   // every 10 s, in case Android kills the app
            }
            String text = statusText();
            if (!text.equals(lastText) && canNotify()) {
                getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, buildNotification());
            }
            handler.postDelayed(this, UPDATE_MS);
        }
    };

    private Notification buildNotification() {
        lastText = statusText();

        // Tapping the notification brings the app back where the user left it
        Intent open = new Intent(this, LoginActivity.class);
        open.setAction(Intent.ACTION_MAIN);
        open.addCategory(Intent.CATEGORY_LAUNCHER);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_dumbbell)
                .setContentTitle(WorkoutSession.name + " in progress")
                .setContentText(lastText)
                .setWhen(WorkoutSession.startTime)
                .setUsesChronometer(true)      // the notification shows the workout time
                .setShowWhen(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setContentIntent(pending)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .build();
    }

    private String statusText() {
        int done = 0;
        for (ExerciseEntry exercise : WorkoutSession.exercises) {
            done += exercise.getDoneCount();
        }
        String text = done + (done == 1 ? " set done" : " sets done");
        if (WorkoutSession.restStartElapsed > 0) {
            text += " · resting";
        }
        return text;
    }

    private boolean canNotify() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
