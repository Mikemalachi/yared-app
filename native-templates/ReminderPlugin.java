package com.yared.hymntracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import com.getcapacitor.JSArray;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.Plugin;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.Calendar;

@CapacitorPlugin(name = "Reminder")
public class ReminderPlugin extends Plugin {

    @PluginMethod
    public void schedule(PluginCall call) {
        int hour = call.getInt("hour", 9);
        int minute = call.getInt("minute", 0);
        Context context = getContext();

        SharedPreferences prefs = context.getSharedPreferences(PlaybackService.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putBoolean("reminderEnabled", true)
            .putInt("reminderHour", hour)
            .putInt("reminderMinute", minute)
            .apply();

        scheduleAlarm(context, hour, minute);
        call.resolve();
    }

    @PluginMethod
    public void cancel(PluginCall call) {
        Context context = getContext();
        SharedPreferences prefs = context.getSharedPreferences(PlaybackService.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean("reminderEnabled", false).apply();

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(reminderPendingIntent(context));
        }
        call.resolve();
    }

    @PluginMethod
    public void updateDueInfo(PluginCall call) {
        int count = call.getInt("count", 0);
        JSArray titlesArr = call.getArray("titles");
        Context context = getContext();
        SharedPreferences prefs = context.getSharedPreferences(PlaybackService.PREFS_NAME, Context.MODE_PRIVATE);
        String titlesJson = titlesArr != null ? titlesArr.toString() : "[]";
        prefs.edit()
            .putInt("dueCount", count)
            .putString("dueTitles", titlesJson)
            .apply();
        DueHymnsWidgetProvider.refreshAll(context);
        call.resolve();
    }

    /* Uses inexact repeating rather than exact alarms — no special
       "schedule exact alarms" permission needed, at the cost of Android
       sometimes firing within a window rather than the precise minute.
       Reasonable trade-off for a daily reminder. */
    static void scheduleAlarm(Context context, int hour, int minute) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.getTimeInMillis(),
            AlarmManager.INTERVAL_DAY,
            reminderPendingIntent(context)
        );
    }

    static PendingIntent reminderPendingIntent(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, 5001, intent, flags);
    }
}
