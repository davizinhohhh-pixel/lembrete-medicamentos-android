package com.lembretemedicamentos.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class AlarmeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String remedio = intent.getStringExtra("remedio");
        if (remedio == null || remedio.isEmpty()) {
            remedio = "Seu medicamento";
        }

        NotificationManager manager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String canal = "alarme_medicamentos";

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                canal,
                "Alarmes de medicamentos",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }

        Intent abrir = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(
            context, 0, abrir,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder aviso =
            new NotificationCompat.Builder(context, canal)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Hora do remédio")
                .setContentText("Tomar: " + remedio)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        manager.notify((int) System.currentTimeMillis(), aviso.build());
    }
}
