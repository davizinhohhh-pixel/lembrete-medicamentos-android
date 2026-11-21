
package com.lembretemedicamentos.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

public class AlarmeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String remedio = intent.getStringExtra("remedio");

        if (remedio == null || remedio.isEmpty()) {
            remedio = "seu medicamento";
        }

        String canal = "alarme_medicamentos";

        NotificationManager manager =
            (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);

        if (manager == null) {
            return;
        }

        Uri som = RingtoneManager.getDefaultUri(
            RingtoneManager.TYPE_ALARM);

        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel =
                new NotificationChannel(
                    canal,
                    "Alarmes de medicamentos",
                    NotificationManager.IMPORTANCE_HIGH);

            channel.enableVibration(true);

            AudioAttributes audio =
                new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();

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
            aviso = new Notification.Builder(context, canal);
        } else {
            aviso = new Notification.Builder(context);
            aviso.setPriority(Notification.PRIORITY_HIGH);
            aviso.setSound(som);
            aviso.setVibrate(
                new long[]{0, 1000, 500, 1000});
        }

        aviso.setSmallIcon(
                android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Hora do remédio!")
            .setContentText("Tomar " + remedio)
            .setAutoCancel(true)
            .setContentIntent(pending);

        manager.notify(1001, aviso.build());
    }
}
