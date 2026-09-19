package com.yared.hymntracker;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Downloads an audio link natively, straight to a cache file. The web page
 * can't fetch most audio links itself (Google Drive, Telegram, etc. don't
 * send CORS headers, so the browser blocks the read even though the <audio>
 * tag can still play them). Native code isn't subject to CORS. The bytes
 * never cross the plugin bridge: JS reads the file back through
 * Capacitor.convertFileSrc(), which streams, so large files are fine.
 */
@CapacitorPlugin(name = "UrlDownloader")
public class UrlDownloaderPlugin extends Plugin {
    private static final long MAX_BYTES = 300L * 1024 * 1024;

    @PluginMethod
    public void download(PluginCall call) {
        final String startUrl = call.getString("url");
        if (startUrl == null || startUrl.isEmpty()) { call.reject("No url"); return; }
        new Thread(() -> {
            HttpURLConnection conn = null;
            File out = null;
            try {
                String current = startUrl;
                int redirects = 0;
                while (true) {
                    URL u = new URL(current);
                    conn = (HttpURLConnection) u.openConnection();
                    conn.setInstanceFollowRedirects(false); // follow manually so https->other-host redirects work
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(60000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) YaredHymnTracker");
                    int code = conn.getResponseCode();
                    if (code >= 300 && code < 400) {
                        String loc = conn.getHeaderField("Location");
                        conn.disconnect();
                        if (loc == null || ++redirects > 10) throw new IOException("Too many redirects");
                        current = new URL(u, loc).toString();
                        continue;
                    }
                    if (code >= 400) throw new IOException("The site answered HTTP " + code);
                    break;
                }
                String type = conn.getContentType();
                out = new File(getContext().getCacheDir(), "dl-" + System.currentTimeMillis() + ".bin");
                long total = 0;
                try (InputStream in = new BufferedInputStream(conn.getInputStream());
                     OutputStream os = new FileOutputStream(out)) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        os.write(buf, 0, n);
                        total += n;
                        if (total > MAX_BYTES) throw new IOException("File is too large");
                    }
                }
                JSObject ret = new JSObject();
                ret.put("path", out.getAbsolutePath());
                ret.put("mimeType", type == null ? "" : type);
                ret.put("size", total);
                call.resolve(ret);
            } catch (Exception e) {
                if (out != null) out.delete();
                call.reject(e.getMessage() == null ? "Download failed" : e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    /** Deletes a file this plugin put in the cache folder once JS has read it. */
    @PluginMethod
    public void remove(PluginCall call) {
        String p = call.getString("path");
        if (p != null && p.startsWith(getContext().getCacheDir().getAbsolutePath())) {
            new File(p).delete();
        }
        call.resolve();
    }
}
