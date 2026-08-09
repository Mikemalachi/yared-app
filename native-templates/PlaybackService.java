package com.yared.hymntracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class PlaybackService extends Service {
    public static final String CHANNEL_ID = "playback_channel";
    public static final int NOTIF_ID = 4201;

    public static final String ACTION_PLAY = "com.yared.hymntracker.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.yared.hymntracker.ACTION_PAUSE";
    public static final String ACTION_NEXT = "com.yared.hymntracker.ACTION_NEXT";
    public static final String ACTION_PREV = "com.yared.hymntracker.ACTION_PREV";
    public static final String ACTION_STOP = "com.yared.hymntracker.ACTION_STOP";
    public static final String ACTION_UPDATE = "com.yared.hymntracker.ACTION_UPDATE";

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_PLAYING = "playing";

    public interface ControlListener {
        void onControl(String action);
    }
    private static ControlListener listener;
    public static void setControlListener(ControlListener l) {
        listener = l;
    }

    private String currentTitle = "Yared Hymn Tracker";
    private boolean isPlaying = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (ACTION_UPDATE.equals(action)) {
                if (intent.hasExtra(EXTRA_TITLE)) {
                    currentTitle = intent.getStringExtra(EXTRA_TITLE);
                }
                if (intent.hasExtra(EXTRA_PLAYING)) {
                    isPlaying = intent.getBooleanExtra(EXTRA_PLAYING, false);
                }
                startForeground(NOTIF_ID, buildNotification());
            } else if (ACTION_STOP.equals(action)) {
                stopForeground(true);
                stopSelf();
            } else {
                if (listener != null) {
                    listener.onControl(action);
                }
            }
        }
        return START_NOT_STICKY;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Playback", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Hymn playback controls");
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private PendingIntent actionIntent(String action) {
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getService(this, action.hashCode(), intent, flags);
    }

    private Notification buildNotification() {
        int playPauseIcon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        String playPauseAction = isPlaying ? ACTION_PAUSE : ACTION_PLAY;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(currentTitle)
            .setContentText("Yared Hymn Tracker")
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Previous", actionIntent(ACTION_PREV))
            .addAction(playPauseIcon, isPlaying ? "Pause" : "Play", actionIntent(playPauseAction))
            .addAction(android.R.drawable.ic_media_next, "Next", actionIntent(ACTION_NEXT))
            .setPriority(NotificationCompat.PRIORITY_LOW);

        return builder.build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
