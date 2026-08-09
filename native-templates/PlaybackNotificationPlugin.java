package com.yared.hymntracker;

import android.content.Intent;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "PlaybackNotification")
public class PlaybackNotificationPlugin extends Plugin implements PlaybackService.ControlListener {

    @Override
    public void load() {
        PlaybackService.setControlListener(this);
    }

    @PluginMethod
    public void update(PluginCall call) {
        String title = call.getString("title", "Yared Hymn Tracker");
        boolean playing = Boolean.TRUE.equals(call.getBoolean("playing", false));
        boolean modeActive = Boolean.TRUE.equals(call.getBoolean("modeActive", false));
        String statusText = call.getString("statusText", "");
        Intent intent = new Intent(getContext(), PlaybackService.class);
        intent.setAction(PlaybackService.ACTION_UPDATE);
        intent.putExtra(PlaybackService.EXTRA_TITLE, title);
        intent.putExtra(PlaybackService.EXTRA_PLAYING, playing);
        intent.putExtra(PlaybackService.EXTRA_MODE_ACTIVE, modeActive);
        intent.putExtra(PlaybackService.EXTRA_STATUS_TEXT, statusText);
        ContextCompat.startForegroundService(getContext(), intent);
        call.resolve();
    }

    @PluginMethod
    public void stop(PluginCall call) {
        Intent intent = new Intent(getContext(), PlaybackService.class);
        intent.setAction(PlaybackService.ACTION_STOP);
        getContext().startService(intent);
        call.resolve();
    }

    @Override
    public void onControl(String action) {
        JSObject data = new JSObject();
        if (PlaybackService.ACTION_PLAY.equals(action)) {
            notifyListeners("play", data);
        } else if (PlaybackService.ACTION_PAUSE.equals(action)) {
            notifyListeners("pause", data);
        } else if (PlaybackService.ACTION_NEXT.equals(action)) {
            notifyListeners("next", data);
        } else if (PlaybackService.ACTION_PREV.equals(action)) {
            notifyListeners("previous", data);
        } else if (PlaybackService.ACTION_MODE_NEXT.equals(action)) {
            notifyListeners("modenext", data);
        } else if (PlaybackService.ACTION_MODE_PREV.equals(action)) {
            notifyListeners("modeprev", data);
        }
    }
}
