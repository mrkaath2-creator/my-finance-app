package com.mrkaath2.myfinance;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.webkit.WebViewAssetLoader;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 1001;
    private static final String KEY_ALIAS = "MoiFinansyGeminiKey";
    private static final String PREFS = "secure_gemini";
    private WebView webView;
    private WebViewAssetLoader loader;
    private ValueCallback<android.net.Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);

        webView.addJavascriptInterface(new SecureStore(), "SecureStore");
        webView.addJavascriptInterface(new GeminiNative(), "GeminiNative");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<android.net.Uri[]> cb, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                try {
                    startActivityForResult(params.createIntent(), PICK_FILE);
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    cb.onReceiveValue(null);
                    return false;
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }

            @Override
            @SuppressWarnings("deprecation")
            public WebResourceResponse shouldInterceptRequest(WebView v, String url) {
                return loader.shouldInterceptRequest(android.net.Uri.parse(url));
            }
        });

        webView.loadUrl("https://appassets.androidplatform.net/assets/www/index.html");
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (ks.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) ks.getEntry(KEY_ALIAS, null)).getSecretKey();
        }
        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        kg.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
         .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
         .build());
        return kg.generateKey();
    }

    private String encrypt(String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + "." +
               Base64.encodeToString(encrypted, Base64.NO_WRAP);
    }

    private String decrypt(String packed) throws Exception {
        if (packed == null || packed.isEmpty()) return "";
        String[] parts = packed.split("\\.", 2);
        if (parts.length != 2) return "";
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP))
        );
        return new String(
                cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)),
                StandardCharsets.UTF_8
        );
    }

    private class SecureStore {
        @JavascriptInterface
        public String getSecret() {
            try {
                return decrypt(getSharedPreferences(PREFS, MODE_PRIVATE).getString("key", ""));
            } catch (Exception e) {
                return "";
            }
        }

        @JavascriptInterface
        public boolean setSecret(String value) {
            try {
                getSharedPreferences(PREFS, MODE_PRIVATE)
                        .edit()
                        .putString("key", encrypt(value))
                        .apply();
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public boolean clearSecret() {
            return getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .remove("key")
                    .commit();
        }
    }

    private class GeminiNative {
        @JavascriptInterface
        public void request(String payload) {
            new Thread(() -> doRequest(payload)).start();
        }

        private void doRequest(String payload) {
            String requestId = "unknown";
            try {
                JSONObject input = new JSONObject(payload);
                requestId = input.optString("id", "unknown");

                String prompt = input.optString("prompt", "");
                String imageDataUrl = input.optString("image", "");
                String apiKey = new SecureStore().getSecret();

                if (apiKey.isEmpty()) {
                    throw new Exception("Gemini API key не задан");
                }

                URL url = new URL(
                        "https://generativelanguage.googleapis.com/v1beta/models/" +
                        "gemini-2.5-flash:generateContent"
                );

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(90000);
                connection.setDoOutput(true);
                connection.setRequestProperty("x-goog-api-key", apiKey);
                connection.setRequestProperty("Content-Type", "application/json");

                JSONObject body = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject user = new JSONObject();
                user.put("role", "user");

                JSONArray parts = new JSONArray();
                parts.put(new JSONObject().put("text", prompt));

                if (!imageDataUrl.isEmpty()) {
                    String mimeType = "image/jpeg";
                    int comma = imageDataUrl.indexOf(',');
                    String base64 = comma >= 0 ? imageDataUrl.substring(comma + 1) : imageDataUrl;
                    if (imageDataUrl.startsWith("data:")) {
                        int semi = imageDataUrl.indexOf(';');
                        if (semi > 5) mimeType = imageDataUrl.substring(5, semi);
                    }

                    JSONObject inlineData = new JSONObject();
                    inlineData.put("mime_type", mimeType);
                    inlineData.put("data", base64);

                    parts.put(new JSONObject().put("inline_data", inlineData));
                }

                user.put("parts", parts);
                contents.put(user);
                body.put("contents", contents);

                byte[] requestBytes = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(requestBytes);
                }

                int responseCode = connection.getResponseCode();
                InputStream inputStream = responseCode >= 200 && responseCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while (inputStream != null && (read = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }

                String raw = new String(output.toByteArray(), StandardCharsets.UTF_8);

                if (responseCode < 200 || responseCode >= 300) {
                    String errorMessage = raw;
                    try {
                        JSONObject error = new JSONObject(raw).optJSONObject("error");
                        if (error != null) {
                            errorMessage = error.optString("message", raw);
                        }
                    } catch (Exception ignored) {
                    }
                    throw new Exception("Gemini: HTTP " + responseCode + " — " + errorMessage);
                }

                JSONObject response = new JSONObject(raw);
                JSONArray candidates = response.optJSONArray("candidates");
                String text = "";

                if (candidates != null && candidates.length() > 0) {
                    JSONObject candidate = candidates.optJSONObject(0);
                    JSONObject content = candidate == null ? null : candidate.optJSONObject("content");
                    JSONArray responseParts = content == null ? null : content.optJSONArray("parts");

                    if (responseParts != null) {
                        StringBuilder result = new StringBuilder();
                        for (int i = 0; i < responseParts.length(); i++) {
                            JSONObject part = responseParts.optJSONObject(i);
                            if (part != null) {
                                String piece = part.optString("text", "");
                                if (!piece.isEmpty()) result.append(piece);
                            }
                        }
                        text = result.toString();
                    }
                }

                final String finalId = requestId;
                final String finalText = text;
                webView.post(() -> webView.evaluateJavascript(
                        "window.onGeminiResult(" +
                        JSONObject.quote(finalId) + "," +
                        JSONObject.quote(finalText) + ")",
                        null
                ));

            } catch (Exception e) {
                final String finalId = requestId;
                final String error = e.getMessage() == null ? "Ошибка Gemini" : e.getMessage();
                webView.post(() -> webView.evaluateJavascript(
                        "window.onGeminiError(" +
                        JSONObject.quote(finalId) + "," +
                        JSONObject.quote(error) + ")",
                        null
                ));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_FILE || fileCallback == null) return;

        android.net.Uri[] result = null;
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            result = new android.net.Uri[]{data.getData()};
        }

        fileCallback.onReceiveValue(result);
        fileCallback = null;
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}