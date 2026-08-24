package com.yared.hymntracker;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import androidx.core.app.ActivityCompat;
import com.getcapacitor.BridgeActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(PlaybackNotificationPlugin.class);
        registerPlugin(AudioFileSaverPlugin.class);
        registerPlugin(ReminderPlugin.class);
        registerPlugin(IncomingAudioPlugin.class);
        super.onCreate(savedInstanceState);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1001);
        }
        if (Build.VERSION.SDK_INT >= 33) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1002);
            }
        }

        handleIncomingIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    /* Captures an audio file handed to the app via "Open with" (ACTION_VIEW)
       or "Share to" (ACTION_SEND) from a file manager or another app.
       Streams it straight to a cache file (constant memory use regardless
       of file size) rather than buffering the whole thing plus a base64
       copy in memory — the previous approach could use ~4x the file's
       size in RAM and crashed on larger audio files. JS picks the file
       back up via Capacitor.convertFileSrc(), which also streams rather
       than round-tripping bytes through the plugin bridge. */
    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        Uri uri = null;
        if (Intent.ACTION_VIEW.equals(action)) {
            uri = intent.getData();
        } else if (Intent.ACTION_SEND.equals(action)) {
            uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }
        if (uri == null) return;

        try {
            ContentResolver resolver = getContentResolver();
            String mimeType = resolver.getType(uri);
            if (mimeType == null) mimeType = "audio/mpeg";
            String filename = queryDisplayName(resolver, uri);

            File outDir = new File(getCacheDir(), "incoming");
            if (!outDir.exists()) outDir.mkdirs();
            // Clear anything left over from a previous share so the cache
            // folder doesn't grow forever.
            File[] old = outDir.listFiles();
            if (old != null) {
                for (File f : old) f.delete();
            }
            File outFile = new File(outDir, sanitizeFileName(filename));

            InputStream is = resolver.openInputStream(uri);
            if (is == null) return;
            try {
                OutputStream os = new FileOutputStream(outFile);
                try {
                    byte[] chunk = new byte[16384];
                    int len;
                    while ((len = is.read(chunk)) != -1) {
                        os.write(chunk, 0, len);
                    }
                } finally {
                    os.close();
                }
            } finally {
                is.close();
            }

            IncomingAudioPlugin.setPending(outFile.getAbsolutePath(), filename, mimeType);
        } catch (Exception e) {
            // Worst case the file just doesn't get picked up — no crash.
        }
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.isEmpty()) return "incoming_audio";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String queryDisplayName(ContentResolver resolver, Uri uri) {
        String result = "Untitled";
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = resolver.query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) {
                        String name = cursor.getString(idx);
                        if (name != null) result = name;
                    }
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        } else {
            String path = uri.getPath();
            if (path != null) {
                int cut = path.lastIndexOf('/');
                result = cut != -1 ? path.substring(cut + 1) : path;
            }
        }
        return result;
    }
}
