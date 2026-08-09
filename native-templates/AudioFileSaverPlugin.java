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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

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

        String relativeDir = ROOT_FOLDER + (subPath == null || subPath.isEmpty() ? "" : "/" + subPath);

        try {
            byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Audio.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Audio.Media.MIME_TYPE, mimeType);
                values.put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/" + relativeDir);

                Uri uri = getContext().getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    call.reject("Could not create the file entry");
                    return;
                }
                try (OutputStream os = getContext().getContentResolver().openOutputStream(uri)) {
                    if (os != null) {
                        os.write(bytes);
                    }
                }
            } else {
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    call.reject("Storage permission not granted");
                    return;
                }
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), relativeDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File outFile = new File(dir, filename);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(bytes);
                }
            }

            JSObject ret = new JSObject();
            ret.put("success", true);
            ret.put("path", relativeDir + "/" + filename);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Save failed: " + e.getMessage());
        }
    }
}
