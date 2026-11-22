
package com.lembretemedicamentos.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import java.util.ArrayList;

public class AlarmeReceiver extends BroadcastReceiver {

    private static final String CANAL = "lembrete_medicamentos_voz";

    static boolean notificacoesPermitidas(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || (Build.VERSION.SDK_INT >= 24 && !manager.areNotificationsEnabled())) return false;
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = manager.getNotificationChannel(CANAL);
            return channel == null || (channel.getImportance() != NotificationManager.IMPORTANCE_NONE
                && channel.getSound() != null);
        }
        return true;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String texto;
        ArrayList<Bundle> doses = new ArrayList<>();
        long horario = intent.getLongExtra("horario", 0);
        if (Alarmes.DISPARAR.equals(intent.getAction())) {
            doses = Alarmes.disparar(context, horario);
            if (doses.isEmpty()) return;
            ArrayList<String> nomes = new ArrayList<>();
            for (Bundle dose : doses) nomes.add(dose.getString("nome"));
            texto = TextUtils.join(", ", nomes);
        } else if (Alarmes.ANTIGO.equals(intent.getAction())) {
            texto = intent.getStringExtra("remedio");
            if (texto == null || texto.isEmpty()) texto = "seu medicamento";
        } else {
            Alarmes.reagendar(context);
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)) {
            return;
        }

        Uri som = Uri.parse("android.resource://" + context.getPackageName()
            + "/raw/hora_do_remedio");
        AudioAttributes audio = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build();

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CANAL,
                "Lembretes de medicamentos com voz", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            channel.setSound(som, audio);
            manager.createNotificationChannel(channel);
        }

        Intent abrir = new Intent(
            context, MainActivity.class);

        PendingIntent pending = PendingIntent.getActivity(
            context,
            0,
            abrir,
            PendingIntent.FLAG_UPDATE_CURRENT |
            PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder aviso;

        if (Build.VERSION.SDK_INT >= 26) {
            aviso = new Notification.Builder(context, CANAL);
        } else {
            aviso = new Notification.Builder(context);
            aviso.setPriority(Notification.PRIORITY_HIGH);
            aviso.setSound(som, audio);
            aviso.setVibrate(
                new long[]{0, 1000, 500, 1000});
        }

        aviso.setSmallIcon(
                android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Hora do remédio!")
            .setContentText("Tomar " + texto)
            .setStyle(new Notification.BigTextStyle().bigText("Tomar " + texto))
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pending);

        manager.notify(horario == 0 ? null : Long.toString(horario), 1001, aviso.build());
        if (!doses.isEmpty() && notificacoesPermitidas(context)) {
            Intent voz = new Intent(context, AvisoFalado.class);
            voz.putParcelableArrayListExtra("doses", doses);
            try {
                if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(voz);
                else context.startService(voz);
            } catch (IllegalStateException | SecurityException e) {
                // A notificação sonora acima permanece como aviso.
            }
        }
    }
}
