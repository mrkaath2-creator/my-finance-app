package com.mrkaath2.myfinance;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends Activity {
    private static final int PICK_FILE=1001;
    private static final String KEY_ALIAS="MoiFinansyApiKey";
    private static final String PREFS="secure_ai";
    private WebView webView;
    private WebViewAssetLoader loader;
    private ValueCallback<android.net.Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        webView=new WebView(this);
        setContentView(webView);
        loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);
        webView.addJavascriptInterface(new SecureStore(),"SecureStore");
        webView.addJavascriptInterface(new OpenAINative(),"OpenAINative");
        webView.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onShowFileChooser(WebView v,ValueCallback<android.net.Uri[]> cb,FileChooserParams params){
                if(fileCallback!=null)fileCallback.onReceiveValue(null);
                fileCallback=cb;
                try{startActivityForResult(params.createIntent(),PICK_FILE);return true;}catch(Exception e){fileCallback=null;cb.onReceiveValue(null);return false;}
            }
        });
        webView.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){return loader.shouldInterceptRequest(r.getUrl());}
            @Override @SuppressWarnings("deprecation") public WebResourceResponse shouldInterceptRequest(WebView v,String url){return loader.shouldInterceptRequest(android.net.Uri.parse(url));}
        });
        webView.loadUrl("https://appassets.androidplatform.net/assets/www/index.html");
    }

    private SecretKey getOrCreateKey() throws Exception{
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(ks.containsAlias(KEY_ALIAS))return ((KeyStore.SecretKeyEntry)ks.getEntry(KEY_ALIAS,null)).getSecretKey();
        KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        kg.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return kg.generateKey();
    }
    private String encrypt(String value) throws Exception{
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,getOrCreateKey());
        byte[] ct=c.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+"."+Base64.encodeToString(ct,Base64.NO_WRAP);
    }
    private String decrypt(String packed) throws Exception{
        String[] p=packed.split("\\.",2);if(p.length!=2)return "";
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,getOrCreateKey(),new GCMParameterSpec(128,Base64.decode(p[0],Base64.NO_WRAP)));
        return new String(c.doFinal(Base64.decode(p[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);
    }

    private class SecureStore {
        @JavascriptInterface public String getSecret(){
            try{return decrypt(getSharedPreferences(PREFS,MODE_PRIVATE).getString("key",""));}catch(Exception e){return "";}
        }
        @JavascriptInterface public boolean setSecret(String v){
            try{getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString("key",encrypt(v)).apply();return true;}catch(Exception e){return false;}
        }
        @JavascriptInterface public boolean clearSecret(){return getSharedPreferences(PREFS,MODE_PRIVATE).edit().remove("key").commit();}
    }

    private class OpenAINative {
        @JavascriptInterface public void request(String payload){
            new Thread(() -> doRequest(payload)).start();
        }
        private void doRequest(String payload){
            String id="unknown";
            try{
                org.json.JSONObject input=new org.json.JSONObject(payload);
                id=input.optString("id","unknown");
                String prompt=input.optString("prompt","");
                String image=input.optString("image","");
                String key=new SecureStore().getSecret();
                if(key.isEmpty())throw new Exception("Сначала подключи OpenAI API key");
                java.net.URL url=new java.net.URL("https://api.openai.com/v1/responses");
                java.net.HttpURLConnection c=(java.net.HttpURLConnection)url.openConnection();
                c.setRequestMethod("POST");c.setConnectTimeout(20000);c.setReadTimeout(90000);c.setDoOutput(true);
                c.setRequestProperty("Authorization","Bearer "+key);c.setRequestProperty("Content-Type","application/json");

                org.json.JSONObject body=new org.json.JSONObject();
                body.put("model","gpt-4.1-mini");
                body.put("store",false);
                org.json.JSONArray inputs=new org.json.JSONArray();
                org.json.JSONObject msg=new org.json.JSONObject();msg.put("role","user");
                org.json.JSONArray content=new org.json.JSONArray();
                content.put(new org.json.JSONObject().put("type","input_text").put("text",prompt));
                if(!image.isEmpty())content.put(new org.json.JSONObject().put("type","input_image").put("image_url",image));
                msg.put("content",content);inputs.put(msg);body.put("input",inputs);
                byte[] data=body.toString().getBytes(StandardCharsets.UTF_8);
                try(java.io.OutputStream os=c.getOutputStream()){os.write(data);}
                int code=c.getResponseCode();
                java.io.InputStream stream=code>=200&&code<300?c.getInputStream():c.getErrorStream();
                ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
                while(stream!=null&&(n=stream.read(buf))!=-1)out.write(buf,0,n);
                String raw=new String(out.toByteArray(),StandardCharsets.UTF_8);
                if(code<200||code>=300){
                    String err=raw;try{err=new org.json.JSONObject(raw).optJSONObject("error").optString("message",raw);}catch(Exception ignored){}
                    throw new Exception(err);
                }
                org.json.JSONObject resp=new org.json.JSONObject(raw);
                String text=resp.optString("output_text","");
                if(text.isEmpty()){
                    org.json.JSONArray os=resp.optJSONArray("output");StringBuilder sb=new StringBuilder();
                    if(os!=null)for(int i=0;i<os.length();i++){org.json.JSONObject o=os.optJSONObject(i);org.json.JSONArray cc=o==null?null:o.optJSONArray("content");if(cc!=null)for(int j=0;j<cc.length();j++){org.json.JSONObject part=cc.optJSONObject(j);if(part!=null&&!part.optString("text","").isEmpty())sb.append(part.optString("text"));}}
                    text=sb.toString();
                }
                final String result=text;
                webView.post(() -> webView.evaluateJavascript("window.onOpenAIResult("+org.json.JSONObject.quote(id)+","+org.json.JSONObject.quote(result)+")",null));
            }catch(Exception e){
                final String err=e.getMessage()==null?"Ошибка AI":e.getMessage();
                webView.post(() -> webView.evaluateJavascript("window.onOpenAIError("+org.json.JSONObject.quote(id)+","+org.json.JSONObject.quote(err)+")",null));
            }
        }
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,@Nullable Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=PICK_FILE||fileCallback==null)return;
        android.net.Uri[] result=null;
        if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null)result=new android.net.Uri[]{data.getData()};
        fileCallback.onReceiveValue(result);fileCallback=null;
    }

    @Override public void onBackPressed(){if(webView!=null&&webView.canGoBack())webView.goBack();else super.onBackPressed();}
}