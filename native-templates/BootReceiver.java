package com.yared.hymntracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences(PlaybackService.PREFS_NAME, Context.MODE_PRIVATE);
            boolean enabled = prefs.getBoolean("reminderEnabled", false);
            if (enabled) {
                int hour = prefs.getInt("reminderHour", 9);
                int minute = prefs.getInt("reminderMinute", 0);
                ReminderPlugin.scheduleAlarm(context, hour, minute);
            }
        }
    }
}
