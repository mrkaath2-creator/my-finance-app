package com.mrkaath2.myfinance;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
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

import com.googlecode.tesseract.android.TessBaseAPI;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 1001;
    private WebView webView;
    private WebViewAssetLoader assetLoader;
    private ValueCallback<Uri[]> filePathCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);

        webView.addJavascriptInterface(new FinanceNative(), "FinanceNative");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;
                Intent intent;
                try {
                    intent = params.createIntent();
                    startActivityForResult(intent, PICK_FILE);
                } catch (Exception e) {
                    filePathCallback = null;
                    callback.onReceiveValue(null);
                    return false;
                }
                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            @SuppressWarnings("deprecation")
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return assetLoader.shouldInterceptRequest(Uri.parse(url));
            }
        });

        webView.loadUrl("https://appassets.androidplatform.net/assets/www/index.html");
    }

    private class FinanceNative {
        @JavascriptInterface
        public void recognizeImage(String dataUrl) {
            new Thread(() -> {
                String error = null;
                try {
                    File baseDir = new File(getFilesDir(), "tesseract");
                    File tessdata = new File(baseDir, "tessdata");
                    if (!tessdata.exists() && !tessdata.mkdirs()) {
                        throw new IOException("Не удалось создать каталог OCR");
                    }
                    File trained = new File(tessdata, "rus.traineddata");
                    if (!trained.exists()) {
                        copyAsset("tessdata/rus.traineddata", trained);
                    }

                    int comma = dataUrl.indexOf(',');
                    if (comma < 0) throw new IOException("Некорректный формат изображения");
                    byte[] bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bitmap == null) throw new IOException("Не удалось прочитать изображение");

                    TessBaseAPI tess = new TessBaseAPI();
                    boolean ok = tess.init(baseDir.getAbsolutePath() + File.separator, "rus");
                    if (!ok) {
                        tess.recycle();
                        throw new IOException("Не удалось запустить русский OCR");
                    }
                    tess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
                    tess.setImage(bitmap);
                    String text = tess.getUTF8Text();
                    tess.recycle();
                    bitmap.recycle();

                    final String safe = JSONObject.quote(text == null ? "" : text);
                    webView.post(() -> webView.evaluateJavascript("window.onNativeOcrResult(" + safe + ")", null));
                    return;
                } catch (Throwable t) {
                    error = t.getMessage() == null ? "ошибка OCR" : t.getMessage();
                }
                final String safeErr = JSONObject.quote(error);
                webView.post(() -> webView.evaluateJavascript("window.onNativeOcrError(" + safeErr + ")", null));
            }).start();
        }
    }

    private void copyAsset(String assetName, File target) throws IOException {
        try (InputStream in = getAssets().open(assetName);
             FileOutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_FILE || filePathCallback == null) return;
        Uri[] results = null;
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) results = new Uri[]{uri};
        }
        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
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
