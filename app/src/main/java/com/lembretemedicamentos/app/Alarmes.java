package com.lembretemedicamentos.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class Alarmes {
    static final String DISPARAR = "com.lembretemedicamentos.ALARME_DIARIO";
    static final String ANTIGO = "com.lembretemedicamentos.ALARME";
    private static final int FLAGS = PendingIntent.FLAG_IMMUTABLE;

    private Alarmes() {}

    private static SharedPreferences preferencias(Context context) {
        return context.getSharedPreferences("alarmes_diarios", Context.MODE_PRIVATE);
    }

    static synchronized boolean salvar(Context context, String dados) {
        try {
            JSONArray horarios = new JSONArray(dados);
            for (int i = 0; i < horarios.length(); i++) {
                JSONObject item = horarios.getJSONObject(i);
                if (!item.getString("hora").matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]")) {
                    return false;
                }
                item.getString("nome");
                item.getString("tomado");
            }
            SharedPreferences prefs = preferencias(context);
            if (!dados.equals(prefs.getString("horarios", null))
                    && !prefs.edit().putString("horarios", dados).commit()) {
                return false;
            }
            return !permitido(context) || reagendar(context);
        } catch (JSONException e) {
            return false;
        }
    }

    static boolean permitido(Context context) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        return manager != null && (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms());
    }

    static synchronized boolean reagendar(Context context) {
        return programar(context, System.currentTimeMillis());
    }

    private static boolean programar(Context context, long depoisDe) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null) return false;

        long proximo = Long.MAX_VALUE;
        JSONArray horarios = lerHorarios(context);
        for (int i = 0; i < horarios.length(); i++) {
            proximo = Math.min(proximo, proximaOcorrencia(horarios.optJSONObject(i), depoisDe));
        }

        Intent intent = new Intent(context, AlarmeReceiver.class).setAction(DISPARAR);
        if (proximo == Long.MAX_VALUE) {
            PendingIntent anterior = PendingIntent.getBroadcast(context, 0, intent,
                FLAGS | PendingIntent.FLAG_NO_CREATE);
            if (anterior != null) {
                manager.cancel(anterior);
                anterior.cancel();
            }
            return true;
        }

        PendingIntent abrir = PendingIntent.getActivity(context, 0,
            new Intent(context, MainActivity.class), FLAGS | PendingIntent.FLAG_UPDATE_CURRENT);
        // AlarmClock é visível ao usuário e permite que o Android acorde durante o modo ocioso.
        try {
            if (!permitido(context)) return false;
            intent.putExtra("horario", proximo);
            PendingIntent disparo = PendingIntent.getBroadcast(context, 0, intent,
                FLAGS | PendingIntent.FLAG_UPDATE_CURRENT);
            manager.setAlarmClock(new AlarmManager.AlarmClockInfo(proximo, abrir), disparo);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    static synchronized List<String> disparar(Context context, long horario) {
        List<String> nomes = new ArrayList<>();
        if (horario <= 0 || horario > System.currentTimeMillis()) return nomes;

        JSONArray horarios = lerHorarios(context);
        for (int i = 0; i < horarios.length(); i++) {
            JSONObject item = horarios.optJSONObject(i);
            if (proximaOcorrencia(item, horario - 1) == horario) {
                nomes.add(item.optString("nome"));
            }
        }
        programar(context, Math.max(horario, System.currentTimeMillis()));
        return nomes;
    }

    static boolean cancelarAntigo(Context context, String id) {
        Intent intent = new Intent(context, AlarmeReceiver.class).setAction(ANTIGO)
            .setData(Uri.parse("meuremedio://alarme/" + Uri.encode(id)));
        PendingIntent anterior = PendingIntent.getBroadcast(context, 0, intent,
            FLAGS | PendingIntent.FLAG_NO_CREATE);
        if (anterior != null) anterior.cancel();
        return true;
    }

    private static JSONArray lerHorarios(Context context) {
        try {
            return new JSONArray(preferencias(context).getString("horarios", "[]"));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private static long proximaOcorrencia(JSONObject item, long depoisDe) {
        String hora = item.optString("hora");
        Calendar data = Calendar.getInstance();
        data.setTimeInMillis(depoisDe);
        data.set(Calendar.HOUR_OF_DAY, 12);
        data.set(Calendar.MINUTE, 0);
        data.set(Calendar.SECOND, 0);
        data.set(Calendar.MILLISECOND, 0);
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

        // A confirmação pula apenas o dia registrado, mesmo após mudança de fuso.
        for (int i = 0; i < 3; i++, data.add(Calendar.DAY_OF_MONTH, 1)) {
            if (item.optString("tomado").equals(formato.format(new Date(data.getTimeInMillis())))) {
                continue;
            }
            Calendar ocorrencia = (Calendar) data.clone();
            ocorrencia.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hora.substring(0, 2)));
            ocorrencia.set(Calendar.MINUTE, Integer.parseInt(hora.substring(3)));
            long horario = ocorrencia.getTimeInMillis();
            if (horario > depoisDe) return horario;
        }
        return Long.MAX_VALUE;
    }
}
