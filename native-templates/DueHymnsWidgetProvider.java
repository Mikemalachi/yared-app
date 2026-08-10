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
import org.json.JSONArray;
import org.json.JSONException;

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
        int dueCount = prefs.getInt("dueCount", 0);
        String titlesJson = prefs.getString("dueTitles", "[]");

        String[] dueTitles;
        try {
            JSONArray arr = new JSONArray(titlesJson);
            dueTitles = new String[arr.length()];
            for (int i = 0; i < arr.length(); i++) {
                dueTitles[i] = arr.optString(i);
            }
        } catch (JSONException e) {
            dueTitles = new String[0];
        }

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_due_hymns);

            views.setTextViewText(R.id.widget_title, title);
            views.setTextViewText(R.id.widget_due_summary, dueCount > 0 ? (dueCount + " hymn(s) due") : "Nothing due");
            views.setTextViewText(R.id.widget_play_pause, playing ? "\u275A\u275A" : "\u25B6");

            int[] dueIds = new int[]{ R.id.widget_due_1, R.id.widget_due_2, R.id.widget_due_3 };
            for (int i = 0; i < dueIds.length; i++) {
                if (i < dueTitles.length && dueTitles[i] != null && !dueTitles[i].isEmpty()) {
                    views.setTextViewText(dueIds[i], "\u2022 " + dueTitles[i]);
                    views.setViewVisibility(dueIds[i], View.VISIBLE);
                } else {
                    views.setViewVisibility(dueIds[i], View.GONE);
                }
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
