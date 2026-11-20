
package com.lembretemedicamentos.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> arquivoCallback;
    private static final int FOTO_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        WebSettings config = webView.getSettings();
        config.setJavaScriptEnabled(true);
        config.setDomStorageEnabled(true);

        webView.setWebChromeClient(
            new WebChromeClient() {
                @Override
                public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                    if (arquivoCallback != null) {
                        arquivoCallback.onReceiveValue(null);
                    }

                    arquivoCallback = callback;

                    try {
                        Intent intent =
                            params.createIntent();

                        startActivityForResult(
                            intent, FOTO_REQUEST
                        );
                        return true;

                    } catch (Exception e) {
                        arquivoCallback = null;
                        return false;
                    }
                }
            }
        );

        webView.setWebViewClient(
            new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(
                    WebView view, String url) {

                    if (url.startsWith("https://") ||
                        url.startsWith("http://")) {
                        return false;
                    }

                    try {
                        Intent intent = new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                        );
                        startActivity(intent);
                    } catch (Exception e) {
                        return true;
                    }
                    return true;
                }
            }
        );

        setContentView(webView);

        webView.loadUrl(
            "https://davizinhohhh-pixel.github.io/lembrete-medicamentos/"
        );
    }

    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data) {

        super.onActivityResult(
            requestCode, resultCode, data
        );

        if (requestCode == FOTO_REQUEST &&
            arquivoCallback != null) {

            Uri[] resultado =
                WebChromeClient.FileChooserParams
                    .parseResult(resultCode, data);

            arquivoCallback.onReceiveValue(resultado);
            arquivoCallback = null;
        }
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
