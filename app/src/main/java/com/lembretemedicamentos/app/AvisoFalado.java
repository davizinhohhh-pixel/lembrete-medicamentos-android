package com.lembretemedicamentos.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class AvisoFalado extends Service {
    private static final String CANAL = "voz_em_andamento";
    private static final int NOTIFICACAO = 1002;
    private static final long INTERVALO_MS = 10_000;
    private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());
    private static volatile AvisoFalado atual;

    private final Map<String, Bundle> pendentes = new LinkedHashMap<>();
    private final Runnable repetir = this::falar;
    private TextToSpeech sintetizador;
    private MediaPlayer gravacao;
    private PowerManager.WakeLock vigilia;
    private boolean vozLocal;
    private int proximaDose;

    static boolean ativo() {
        return atual != null;
    }

    static void confirmar(String id, String nome) {
        PRINCIPAL.post(() -> {
            AvisoFalado servico = atual;
            if (servico == null) return;
            boolean removido = servico.pendentes.remove(id) != null;
            if (!removido) removido = servico.pendentes.remove("nome:" + nome) != null;
            if (!removido) return;
            if (servico.pendentes.isEmpty()) servico.encerrar();
            else servico.reiniciar();
        });
    }

    private static AudioAttributes audio() {
        return new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sintetizador = new TextToSpeech(this, resultado -> PRINCIPAL.post(() -> {
            if (resultado != TextToSpeech.SUCCESS || sintetizador == null) return;
            if (sintetizador.getVoices() == null) return;
            for (Voice voz : sintetizador.getVoices()) {
                Locale idioma = voz.getLocale();
                if ("pt".equals(idioma.getLanguage()) && "BR".equals(idioma.getCountry())
                        && !voz.isNetworkConnectionRequired()
                        && (voz.getFeatures() == null || !voz.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED))
                        && sintetizador.setVoice(voz) == TextToSpeech.SUCCESS) {
                    sintetizador.setAudioAttributes(audio());
                    vozLocal = true;
                    return;
                }
            }
        }));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ArrayList<Bundle> doses = intent == null ? null : intent.getParcelableArrayListExtra("doses");
        if (doses == null || doses.isEmpty()) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        startForeground(NOTIFICACAO, notificacao());
        for (Bundle dose : doses) {
            String id = dose.getString("id", "");
            pendentes.put(id.isEmpty() ? "nome:" + dose.getString("nome", "") : id, dose);
        }
        atual = this;
        if (vigilia == null) {
            PowerManager energia = getSystemService(PowerManager.class);
            if (energia != null) {
                vigilia = energia.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName() + ":avisoFalado");
                vigilia.acquire();
            }
        }
        PRINCIPAL.removeCallbacks(repetir);
        PRINCIPAL.postDelayed(repetir, 6_000);
        return START_NOT_STICKY;
    }

    private Notification notificacao() {
        Notification.Builder aviso;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel canal = new NotificationChannel(CANAL, "Lembrete em andamento",
                NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(canal);
            aviso = new Notification.Builder(this, CANAL);
        } else {
            aviso = new Notification.Builder(this).setPriority(Notification.PRIORITY_LOW);
        }
        PendingIntent abrir = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return aviso.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Lembrete de medicamento")
            .setContentText("Abra o app e toque em Já tomei.")
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(abrir)
            .setOngoing(true)
            .build();
    }

    private void falar() {
        if (pendentes.isEmpty()) return;
        if (AlarmeReceiver.notificacoesPermitidas(this)) {
            ArrayList<Bundle> doses = new ArrayList<>(pendentes.values());
            proximaDose %= doses.size();
            Bundle dose = doses.get(proximaDose++);
            String idoso = dose.getString("idoso", "").trim();
            String fala = (idoso.isEmpty() ? "" : idoso + ", ")
                + "hora de tomar " + dose.getString("nome", "seu medicamento") + ".";
            if (!vozLocal || sintetizador.speak(fala, TextToSpeech.QUEUE_FLUSH, null, "dose")
                    != TextToSpeech.SUCCESS) {
                if (gravacao == null) {
                    gravacao = MediaPlayer.create(this, R.raw.hora_do_remedio, audio(), 0);
                }
                if (gravacao != null) gravacao.start();
            }
        }
        PRINCIPAL.postDelayed(repetir, INTERVALO_MS);
    }

    private void reiniciar() {
        PRINCIPAL.removeCallbacks(repetir);
        pararAudio();
        PRINCIPAL.postDelayed(repetir, 1_000);
    }

    private void pararAudio() {
        if (sintetizador != null) sintetizador.stop();
        if (gravacao != null) {
            gravacao.release();
            gravacao = null;
        }
    }

    private void encerrar() {
        PRINCIPAL.removeCallbacks(repetir);
        pararAudio();
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (atual == this) atual = null;
        PRINCIPAL.removeCallbacks(repetir);
        pararAudio();
        if (sintetizador != null) {
            sintetizador.shutdown();
            sintetizador = null;
        }
        if (vigilia != null && vigilia.isHeld()) vigilia.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
