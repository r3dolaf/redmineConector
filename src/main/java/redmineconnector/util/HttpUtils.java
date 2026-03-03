package redmineconnector.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class HttpUtils {
    private static final int TIMEOUT = 30000; // 30 seconds for slow connections

    static {
        trustAllCertificates();
    }

    private static void trustAllCertificates() {
        try {
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] {
                    new javax.net.ssl.X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
            };
            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            redmineconnector.util.LoggerUtil.logError("HttpUtils",
                    "Failed to configure SSL trust manager", e);
        }
    }

    public static String get(String urlStr) throws Exception {
        return request(urlStr, "GET", null, null, null);
    }

    public static String get(String urlStr, String apiKey) throws Exception {
        return request(urlStr, "GET", apiKey, null, null);
    }

    public static String post(String urlStr, String apiKey, String json, boolean isAi) throws Exception {
        return request(urlStr, "POST", apiKey, json, "application/json");
    }

    public static String put(String urlStr, String apiKey, String json) throws Exception {
        return request(urlStr, "PUT", apiKey, json, "application/json");
    }

    public static String delete(String urlStr, String apiKey) throws Exception {
        return request(urlStr, "DELETE", apiKey, null, null);
    }

    public static String postBinary(String urlStr, byte[] data, String contentType, String apiKey) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", contentType != null ? contentType : "application/octet-stream");
        conn.setRequestProperty("Accept", "application/json");
        if (apiKey != null && !apiKey.isEmpty()) {
            conn.setRequestProperty("X-Redmine-API-Key", apiKey);
        }
        conn.setRequestProperty("Content-Length", String.valueOf(data.length));
        try (OutputStream os = conn.getOutputStream()) {
            os.write(data);
        }
        return readResponse(conn);
    }

    public static byte[] downloadBytes(String urlStr, String apiKey) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);
        if (apiKey != null && !apiKey.isEmpty()) {
            conn.setRequestProperty("X-Redmine-API-Key", apiKey);
        }
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        conn.setRequestProperty("Accept", "*/*");
        conn.setInstanceFollowRedirects(true);
        int status = conn.getResponseCode();

        // Handle potential redirects manually to preserve headers if automatic follow
        // fails to keep them
        if (status >= 300 && status < 400) {
            String newUrl = conn.getHeaderField("Location");
            if (newUrl != null)
                return downloadBytes(newUrl, apiKey);
        }

        if (status >= 400) {
            throw new IOException("Server returned HTTP " + status + " for URL: " + urlStr);
        }
        try (InputStream is = conn.getInputStream()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[4096];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        }
    }

    private static String request(String urlStr, String method, String apiKey, String body, String contentType)
            throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);
        if (apiKey != null && !apiKey.isEmpty())
            conn.setRequestProperty("X-Redmine-API-Key", apiKey);
        if (contentType != null)
            conn.setRequestProperty("Content-Type", contentType);
        if (body != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }
        return readResponse(conn);
    }

    private static String readResponse(HttpURLConnection conn) throws IOException {
        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null)
            throw new IOException("HTTP Error " + status + " (No response body)");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String body = br.lines().collect(Collectors.joining("\n"));
            if (status >= 400) {
                throw new IOException("HTTP Error " + status + " for URL: " + conn.getURL() + " - Detalle: " + body);
            }
            return body;
        }
    }
}
