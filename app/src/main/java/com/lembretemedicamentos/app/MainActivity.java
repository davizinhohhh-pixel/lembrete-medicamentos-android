
package com.lembretemedicamentos.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> arquivoCallback;
    private static final int FOTO_REQUEST = 100;

    private static final String SITE =
        "https://davizinhohhh-pixel.github.io/lembrete-medicamentos/";

    private static final String SITE_IDOSO =
        SITE + "?tela=idoso";

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
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request) {

                Uri uri = request.getUrl();

                if ("https".equals(uri.getScheme())
                    && "davizinhohhh-pixel.github.io"
                        .equals(uri.getHost())
                    && uri.getPath() != null
                    && uri.getPath().startsWith(
                        "/lembrete-medicamentos/")) {
                    return false;
                }

                try {
                    startActivity(new Intent(
                        Intent.ACTION_VIEW, uri));
                } catch (Exception e) {
                    return true;
                }
                return true;
            }

            @Override
            public void onPageFinished(
                    WebView view, String url) {

                super.onPageFinished(view, url);

                if (url != null && url.startsWith(SITE)) {
                    view.evaluateJavascript(
                        "(function() {" +
                        "if (localStorage.getItem('perfilRemedio') !== 'idoso') {" +
                        "localStorage.setItem('perfilRemedio', 'idoso');" +
                        "location.reload();" +
                        "}" +
                        "})();",
                        null
                    );
                }
            }
        });

        setContentView(webView);
        webView.loadUrl(SITE_IDOSO);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{
                    Manifest.permission.POST_NOTIFICATIONS
                }, 101);
        }
    }

    private PendingIntent criarAlarme(
            String id, String remedio) {

        Intent intent = new Intent(
            this, AlarmeReceiver.class);

        intent.setAction(
            "com.lembretemedicamentos.ALARME");

        intent.setData(Uri.parse(
            "meuremedio://alarme/" + Uri.encode(id)));

        intent.putExtra("remedio", remedio);

        return PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT |
            PendingIntent.FLAG_IMMUTABLE
        );
    }

    public class PonteAndroid {

        @JavascriptInterface
        public boolean agendarAlarme(
                String id, String remedio, long horario) {

            if (id == null || id.isEmpty()
                    || horario <= System.currentTimeMillis()) {
                return false;
            }

            AlarmManager manager =
                (AlarmManager) getSystemService(
                    Context.ALARM_SERVICE);

            if (manager == null) return false;

            try {
                if (Build.VERSION.SDK_INT >= 31
                        && !manager.canScheduleExactAlarms()) {
                    return false;
                }

                PendingIntent pendente =
                    criarAlarme(id, remedio);

                if (Build.VERSION.SDK_INT >= 23) {
                    manager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        horario,
                        pendente
                    );
                } else {
                    manager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        horario,
                        pendente
                    );
                }

                return true;

            } catch (SecurityException e) {
                return false;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public boolean cancelarAlarme(String id) {

            if (id == null || id.isEmpty()) {
                return false;
            }

            try {
                AlarmManager manager =
                    (AlarmManager) getSystemService(
                        Context.ALARM_SERVICE);

                if (manager == null) return false;

                PendingIntent pendente =
                    criarAlarme(id, "");

                manager.cancel(pendente);
                pendente.cancel();

                return true;

            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public void solicitarPermissaoAlarme() {

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
                }
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
