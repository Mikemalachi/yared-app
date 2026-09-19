package com.yared.hymntracker;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import androidx.core.app.ActivityCompat;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.PermissionState;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Saves audio files into Music/Yared music/<month>/<celebration>[/<subfolder>]/<title>.<ext>
 * On Android 10+ this uses the MediaStore API (scoped storage — no special
 * permission needed, since the app owns whatever it inserts). On Android 9
 * and below it needs WRITE_EXTERNAL_STORAGE, requested at runtime.
 */
@CapacitorPlugin(
    name = "AudioFileSaver",
    permissions = {
        @Permission(strings = { Manifest.permission.WRITE_EXTERNAL_STORAGE }, alias = "storage")
    }
)
public class AudioFileSaverPlugin extends Plugin {

    private static final String ROOT_FOLDER = "Yared music";

    @PluginMethod
    public void requestAccess(PluginCall call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            JSObject ret = new JSObject();
            ret.put("granted", true);
            call.resolve(ret);
            return;
        }
        if (getPermissionState("storage") != PermissionState.GRANTED) {
            requestPermissionForAlias("storage", call, "accessCallback");
        } else {
            JSObject ret = new JSObject();
            ret.put("granted", true);
            call.resolve(ret);
        }
    }

    @PermissionCallback
    private void accessCallback(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("granted", getPermissionState("storage") == PermissionState.GRANTED);
        call.resolve(ret);
    }

    /** Parts of a file being sent from JS in small base64 chunks (keeps big files off the bridge in one piece). */
    private final Map<String, File> pending = new HashMap<>();

    @PluginMethod
    public void save(PluginCall call) {
        String base64Data = call.getString("data");
        String filename = call.getString("filename");
        String subPath = call.getString("subPath", "");
        String mimeType = call.getString("mimeType", "audio/mpeg");
        if (base64Data == null || filename == null) {
            call.reject("Missing data or filename");
            return;
        }
        try {
            byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
            String path = writeToMusic(new ByteArrayInputStream(bytes), filename, subPath, mimeType);
            JSObject ret = new JSObject();
            ret.put("success", true);
            ret.put("path", path);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Save failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void beginChunked(PluginCall call) {
        try {
            File f = File.createTempFile("save-", ".part", getContext().getCacheDir());
            synchronized (pending) { pending.put(f.getName(), f); }
            JSObject ret = new JSObject();
            ret.put("token", f.getName());
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Could not start saving: " + e.getMessage());
        }
    }

    @PluginMethod
    public void appendChunk(PluginCall call) {
        String token = call.getString("token");
        String data = call.getString("data");
        File f;
        synchronized (pending) { f = pending.get(token); }
        if (f == null || data == null) { call.reject("Unknown save"); return; }
        try (FileOutputStream fos = new FileOutputStream(f, true)) {
            fos.write(Base64.decode(data, Base64.DEFAULT));
            call.resolve();
        } catch (Exception e) {
            call.reject("Write failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void finishChunked(PluginCall call) {
        String token = call.getString("token");
        String filename = call.getString("filename");
        String subPath = call.getString("subPath", "");
        String mimeType = call.getString("mimeType", "audio/mpeg");
        File f;
        synchronized (pending) { f = pending.remove(token); }
        if (f == null || filename == null) { call.reject("Unknown save"); return; }
        try (InputStream in = new FileInputStream(f)) {
            String path = writeToMusic(in, filename, subPath, mimeType);
            JSObject ret = new JSObject();
            ret.put("success", true);
            ret.put("path", path);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Save failed: " + e.getMessage());
        } finally {
            f.delete();
        }
    }

    /** Writes into Music/Yared music/...; if Android refuses the file as music, falls back to Download/Yared music/... */
    private String writeToMusic(InputStream in, String filename, String subPath, String mimeType) throws Exception {
        String relativeDir = ROOT_FOLDER + (subPath == null || subPath.isEmpty() ? "" : "/" + subPath);
        String mime = mimeType == null ? "audio/mpeg" : mimeType.split(";")[0].trim();
        if (!mime.startsWith("audio/")) mime = "audio/mpeg";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Uri uri = null;
            String where = "Music/" + relativeDir;
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Audio.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Audio.Media.MIME_TYPE, mime);
                values.put(MediaStore.Audio.Media.RELATIVE_PATH, where);
                uri = getContext().getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
            } catch (Exception ignored) { uri = null; }
            if (uri == null) {
                where = "Download/" + relativeDir;
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                values.put(MediaStore.Downloads.MIME_TYPE, mime);
                values.put(MediaStore.Downloads.RELATIVE_PATH, where);
                uri = getContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            }
            if (uri == null) throw new Exception("Could not create the file entry");
            try (OutputStream os = getContext().getContentResolver().openOutputStream(uri)) {
                if (os == null) throw new Exception("Could not open the file for writing");
                copy(in, os);
            }
            return where + "/" + filename;
        }

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            throw new Exception("Storage permission not granted");
        }
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), relativeDir);
        if (!dir.exists()) dir.mkdirs();
        File outFile = new File(dir, filename);
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            copy(in, fos);
        }
        return "Music/" + relativeDir + "/" + filename;
    }

    private static void copy(InputStream in, OutputStream out) throws Exception {
        byte[] buf = new byte[65536];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
    }
}
