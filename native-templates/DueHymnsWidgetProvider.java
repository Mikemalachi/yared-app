package com.yared.hymntracker;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

public class DueHymnsWidgetProvider extends AppWidgetProvider {

    /* Called from PlaybackService (on every state update) and from
       ReminderPlugin (whenever JS pushes new due-hymn data), so the widget
       refreshes immediately rather than waiting for its own update cycle. */
    public static void refreshAll(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, DueHymnsWidgetProvider.class));
        if (ids.length > 0) {
            new DueHymnsWidgetProvider().onUpdate(context, mgr, ids);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SharedPreferences prefs = context.getSharedPreferences(PlaybackService.PREFS_NAME, Context.MODE_PRIVATE);
        String title = prefs.getString("nowPlayingTitle", "Yared Hymn Tracker");
        boolean playing = prefs.getBoolean("nowPlayingIsPlaying", false);
        boolean modeActive = prefs.getBoolean("nowPlayingModeActive", false);
        String statusText = prefs.getString("nowPlayingStatusText", "");
        int dueCount = prefs.getInt("dueCount", 0);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_due_hymns);

            views.setTextViewText(R.id.widget_title, title);
            // Only one subtitle line shows at a time — status takes priority
            // when there's something actively playing/reviewing, otherwise
            // the due count — keeps this fitting a compact 4x1 widget.
            if (statusText != null && !statusText.isEmpty()) {
                views.setTextViewText(R.id.widget_status, statusText);
                views.setViewVisibility(R.id.widget_status, View.VISIBLE);
                views.setViewVisibility(R.id.widget_due_summary, View.GONE);
            } else {
                views.setViewVisibility(R.id.widget_status, View.GONE);
                views.setTextViewText(R.id.widget_due_summary, dueCount > 0 ? (dueCount + " hymn(s) due") : "Nothing due");
                views.setViewVisibility(R.id.widget_due_summary, View.VISIBLE);
            }
            views.setTextViewText(R.id.widget_play_pause, playing ? "\u275A\u275A" : "\u25B6");

            // Mode-aware controls: mirrors the notification — when a practice
            // mode (AB Repeat / Teacher / Continuous) is active, show a second
            // prev/next pair for stepping through the current segment/review.
            if (modeActive) {
                views.setViewVisibility(R.id.widget_mode_prev, View.VISIBLE);
                views.setViewVisibility(R.id.widget_mode_next, View.VISIBLE);
                views.setOnClickPendingIntent(R.id.widget_mode_prev, actionIntent(context, PlaybackService.ACTION_MODE_PREV));
                views.setOnClickPendingIntent(R.id.widget_mode_next, actionIntent(context, PlaybackService.ACTION_MODE_NEXT));
            } else {
                views.setViewVisibility(R.id.widget_mode_prev, View.GONE);
                views.setViewVisibility(R.id.widget_mode_next, View.GONE);
            }

            views.setOnClickPendingIntent(R.id.widget_prev, actionIntent(context, PlaybackService.ACTION_PREV));
            views.setOnClickPendingIntent(R.id.widget_play_pause,
                actionIntent(context, playing ? PlaybackService.ACTION_PAUSE : PlaybackService.ACTION_PLAY));
            views.setOnClickPendingIntent(R.id.widget_next, actionIntent(context, PlaybackService.ACTION_NEXT));

            PendingIntent openPending = openAppIntent(context);
            views.setOnClickPendingIntent(R.id.widget_title, openPending);
            views.setOnClickPendingIntent(R.id.widget_due_summary, openPending);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private PendingIntent actionIntent(Context context, String action) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getService(context, action.hashCode(), intent, flags);
    }

    private PendingIntent openAppIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, 100, intent, flags);
    }
}
