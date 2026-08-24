package com.yared.hymntracker;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Holds a reference to audio captured by MainActivity when the app is
 * opened via "Open with" from a file manager, or as a share target from
 * another app. JS polls getPendingAudio() shortly after launch/resume to
 * pick it up, then loads the actual file via Capacitor.convertFileSrc()
 * on the returned path — keeping the raw audio bytes off the plugin
 * bridge entirely, which is what made this safe for large files.
 */
@CapacitorPlugin(name = "IncomingAudio")
public class IncomingAudioPlugin extends Plugin {
    private static String pendingPath;
    private static String pendingFilename;
    private static String pendingMimeType;

    public static void setPending(String path, String filename, String mimeType) {
        pendingPath = path;
        pendingFilename = filename;
        pendingMimeType = mimeType;
    }

    @PluginMethod
    public void getPendingAudio(PluginCall call) {
        JSObject ret = new JSObject();
        if (pendingPath != null) {
            ret.put("path", pendingPath);
            ret.put("filename", pendingFilename != null ? pendingFilename : "Untitled.mp3");
            ret.put("mimeType", pendingMimeType != null ? pendingMimeType : "audio/mpeg");
            pendingPath = null;
            pendingFilename = null;
            pendingMimeType = null;
        }
        call.resolve(ret);
    }
}
