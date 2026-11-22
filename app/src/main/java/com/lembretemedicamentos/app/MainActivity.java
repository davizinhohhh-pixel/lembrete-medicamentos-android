
package com.lembretemedicamentos.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;

import java.io.IOException;
import java.util.Collections;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> arquivoCallback;
    private static final int FOTO_REQUEST = 100;

    private static final String SITE =
        "https://davizinhohhh-pixel.github.io/lembrete-medicamentos/";

    private static final String PAGINA_APP =
        SITE + "?android=app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        WebSettings config = webView.getSettings();
        config.setJavaScriptEnabled(true);
        config.setDomStorageEnabled(true);
        config.setAllowFileAccess(false);
        config.setAllowContentAccess(true);

        webView.addJavascriptInterface(
            new PonteAndroid(), "AndroidAlarme");

        webView.setWebChromeClient(new WebChromeClient() {
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
                    startActivityForResult(
                        params.createIntent(), FOTO_REQUEST);
                    return true;
                } catch (Exception e) {
                    arquivoCallback = null;
                    callback.onReceiveValue(null);
                    return true;
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame() || !PAGINA_APP.equals(request.getUrl().toString())) {
                    return null;
                }
                try {
                    return new WebResourceResponse("text/html", "UTF-8", 200, "OK",
                        Collections.singletonMap("Content-Security-Policy",
                            "frame-src 'none'; object-src 'none'; base-uri 'none'; "
                            + "script-src 'unsafe-inline' https://www.gstatic.com/firebasejs/10.12.5/"),
                        getAssets().open("index.html"));
                } catch (IOException e) {
                    return new WebResourceResponse("text/plain", "UTF-8", 500,
                        "Asset unavailable", Collections.emptyMap(), null);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view, WebResourceRequest request) {
                return !request.isForMainFrame() || abrirLink(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return abrirLink(Uri.parse(url));
            }
        });

        setContentView(webView);
        webView.loadUrl(PAGINA_APP);
    }

    private boolean abrirLink(Uri uri) {
        if (PAGINA_APP.equals(uri.toString())) return false;
        if ("https".equals(uri.getScheme())) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (android.content.ActivityNotFoundException ignored) {
                // O aparelho não tem um aplicativo para abrir o link.
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && webView != null) {
            webView.evaluateJavascript("window.atualizarStatusAlarmes?.()", null);
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && !Alarmes.permitido(this)) {
                new PonteAndroid().solicitarPermissaoAlarme();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Alarmes.reagendar(this);
        if (webView != null) {
            webView.evaluateJavascript("window.atualizarStatusAlarmes?.()", null);
        }
    }

    public class PonteAndroid {

        @JavascriptInterface
        public boolean versaoTeste() {
            return getPackageName().endsWith(".teste");
        }

        @JavascriptInterface
        public boolean salvarHorarios(String horarios) {
            return Alarmes.salvar(MainActivity.this, horarios);
        }

        @JavascriptInterface
        public boolean limparAlarmeAntigo(String id) {
            return Alarmes.cancelarAntigo(MainActivity.this, id);
        }

        @JavascriptInterface
        public boolean alarmesExatosPermitidos() {
            return Alarmes.permitido(MainActivity.this);
        }

        @JavascriptInterface
        public boolean notificacoesPermitidas() {
            return AlarmeReceiver.notificacoesPermitidas(MainActivity.this);
        }

        @JavascriptInterface
        public void solicitarPermissaoAlarme() {
            if (Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                    && !getPreferences(MODE_PRIVATE).getBoolean("notificacoesSolicitadas", false)) {
                runOnUiThread(() -> {
                    getPreferences(MODE_PRIVATE).edit()
                        .putBoolean("notificacoesSolicitadas", true).apply();
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
                });
                return;
            }
            if (Build.VERSION.SDK_INT >= 31) {
                AlarmManager manager =
                    (AlarmManager) getSystemService(
                        Context.ALARM_SERVICE);

                if (manager != null
                        && !manager.canScheduleExactAlarms()) {

                    runOnUiThread(() -> {
                        try {
                            Intent intent = new Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);

                            intent.setData(Uri.parse(
                                "package:" + getPackageName()));

                            startActivity(intent);
                        } catch (Exception e) {
                            // Permissão indisponível.
                        }
                    });
                    return;
                }
            }
            if (!notificacoesPermitidas()) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName()));
                    if (Build.VERSION.SDK_INT >= 26) {
                        intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    }
                    startActivity(intent);
                });
            }
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {

        super.onActivityResult(
            requestCode, resultCode, data);

        if (requestCode == FOTO_REQUEST
                && arquivoCallback != null) {

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
