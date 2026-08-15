package com.yared.hymntracker;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Holds audio data captured by MainActivity when the app is opened via
 * "Open with" from a file manager, or as a share target from another app.
 * JS polls getPendingAudio() shortly after launch/resume to pick it up.
 */
@CapacitorPlugin(name = "IncomingAudio")
public class IncomingAudioPlugin extends Plugin {
    private static String pendingBase64;
    private static String pendingFilename;
    private static String pendingMimeType;

    public static void setPending(String base64, String filename, String mimeType) {
        pendingBase64 = base64;
        pendingFilename = filename;
        pendingMimeType = mimeType;
    }

    @PluginMethod
    public void getPendingAudio(PluginCall call) {
        JSObject ret = new JSObject();
        if (pendingBase64 != null) {
            ret.put("data", pendingBase64);
            ret.put("filename", pendingFilename != null ? pendingFilename : "Untitled.mp3");
            ret.put("mimeType", pendingMimeType != null ? pendingMimeType : "audio/mpeg");
            pendingBase64 = null;
            pendingFilename = null;
            pendingMimeType = null;
        }
        call.resolve(ret);
    }
}
