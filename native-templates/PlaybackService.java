package com.yared.hymntracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

public class PlaybackService extends Service {
    public static final String CHANNEL_ID = "playback_channel";
    public static final int NOTIF_ID = 4201;
    public static final String PREFS_NAME = "yared_prefs";

    public static final String ACTION_PLAY = "com.yared.hymntracker.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.yared.hymntracker.ACTION_PAUSE";
    public static final String ACTION_NEXT = "com.yared.hymntracker.ACTION_NEXT";
    public static final String ACTION_PREV = "com.yared.hymntracker.ACTION_PREV";
    public static final String ACTION_MODE_NEXT = "com.yared.hymntracker.ACTION_MODE_NEXT";
    public static final String ACTION_MODE_PREV = "com.yared.hymntracker.ACTION_MODE_PREV";
    public static final String ACTION_STOP = "com.yared.hymntracker.ACTION_STOP";
    public static final String ACTION_UPDATE = "com.yared.hymntracker.ACTION_UPDATE";

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_PLAYING = "playing";
    public static final String EXTRA_MODE_ACTIVE = "modeActive";
    public static final String EXTRA_STATUS_TEXT = "statusText";

    public interface ControlListener {
        void onControl(String action);
    }
    private static ControlListener listener;
    public static void setControlListener(ControlListener l) {
        listener = l;
    }

    private String currentTitle = "Yared Hymn Tracker";
    private String statusText = "";
    private boolean isPlaying = false;
    private boolean modeActive = false;

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
                if (intent.hasExtra(EXTRA_MODE_ACTIVE)) {
                    modeActive = intent.getBooleanExtra(EXTRA_MODE_ACTIVE, false);
                }
                if (intent.hasExtra(EXTRA_STATUS_TEXT)) {
                    statusText = intent.getStringExtra(EXTRA_STATUS_TEXT);
                }
                startForeground(NOTIF_ID, buildNotification());
                persistStateForWidget();
                DueHymnsWidgetProvider.refreshAll(this);
            } else if (ACTION_STOP.equals(action)) {
                stopForeground(true);
                stopSelf();
            } else {
                // Control action (play/pause/next/prev/mode next/prev). If no
                // app instance is alive to handle it (listener never got
                // registered — e.g. a widget tap after the app was fully
                // killed), Android still requires any freshly-started service
                // to promote to foreground; satisfy that, then stop cleanly
                // rather than leaving a dangling service or crashing.
                if (listener != null) {
                    listener.onControl(action);
                } else {
                    startForeground(NOTIF_ID, buildNotification());
                    stopForeground(true);
                    stopSelf();
                }
            }
        }
        return START_NOT_STICKY;
    }

    private void persistStateForWidget() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putString("nowPlayingTitle", currentTitle)
            .putBoolean("nowPlayingIsPlaying", isPlaying)
            .apply();
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

    private PendingIntent contentIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(this, 0, intent, flags);
    }

    private Notification buildNotification() {
        int playPauseIcon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        String playPauseAction = isPlaying ? ACTION_PAUSE : ACTION_PLAY;
        String displayText = (statusText == null || statusText.isEmpty()) ? "Yared Hymn Tracker" : statusText;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(currentTitle)
            .setContentText(displayText)
            .setContentIntent(contentIntent())
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setPriority(NotificationCompat.PRIORITY_LOW);

        if (modeActive) {
            // Five actions: outer pair skips the whole hymn, inner pair steps
            // through whatever practice mode (AB repeat / Teacher / Continuous)
            // is currently active — mirrors the in-app Next/Previous for that mode.
            // MediaStyle keeps all 5 available (swipe to expand); compact view
            // shows the 3 most relevant while actively practicing a segment.
            builder
                .addAction(android.R.drawable.ic_media_previous, "Previous hymn", actionIntent(ACTION_PREV))
                .addAction(android.R.drawable.ic_media_rew, "Previous segment", actionIntent(ACTION_MODE_PREV))
                .addAction(playPauseIcon, isPlaying ? "Pause" : "Play", actionIntent(playPauseAction))
                .addAction(android.R.drawable.ic_media_ff, "Next segment", actionIntent(ACTION_MODE_NEXT))
                .addAction(android.R.drawable.ic_media_next, "Next hymn", actionIntent(ACTION_NEXT))
                .setStyle(new MediaStyle().setShowActionsInCompactView(1, 2, 3));
        } else {
            builder
                .addAction(android.R.drawable.ic_media_previous, "Previous", actionIntent(ACTION_PREV))
                .addAction(playPauseIcon, isPlaying ? "Pause" : "Play", actionIntent(playPauseAction))
                .addAction(android.R.drawable.ic_media_next, "Next", actionIntent(ACTION_NEXT))
                .setStyle(new MediaStyle().setShowActionsInCompactView(0, 1, 2));
        }

        return builder.build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
