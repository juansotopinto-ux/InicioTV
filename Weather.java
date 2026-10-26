package cl.jc.inicio;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Clima actual de Iquique con Open-Meteo (gratis, sin cuenta ni clave).
 * Se consulta como máximo cada 30 minutos y se guarda para mostrarlo al instante.
 */
final class Weather {

    static final String CITY = "Iquique";
    private static final double LAT = -20.2133;
    private static final double LON = -70.1503;
    private static final long REFRESH_MS = 30 * 60 * 1000L;

    interface Callback {
        void onWeather(int tempC, String description);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Weather() {
    }

    /** Entrega primero el dato guardado y, si está viejo, consulta uno nuevo. */
    static void get(Context context, final Callback cb) {
        final SharedPreferences p = new Config(context).raw();
        long at = p.getLong("w_at", 0);
        if (at > 0) {
            cb.onWeather(p.getInt("w_temp", 0), describe(p.getInt("w_code", 0)));
        }
        if (System.currentTimeMillis() - at < REFRESH_MS) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=" + LAT
                            + "&longitude=" + LON + "&current=temperature_2m,weather_code&timezone=auto");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                    r.close();

                    JSONObject cur = new JSONObject(sb.toString()).getJSONObject("current");
                    final int temp = (int) Math.round(cur.getDouble("temperature_2m"));
                    final int code = cur.getInt("weather_code");
                    p.edit().putInt("w_temp", temp).putInt("w_code", code)
                            .putLong("w_at", System.currentTimeMillis()).apply();
                    MAIN.post(new Runnable() {
                        @Override
                        public void run() {
                            cb.onWeather(temp, describe(code));
                        }
                    });
                } catch (Exception ignored) {
                    // Sin internet: se mantiene el último dato o no se muestra nada
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }

    /** Códigos de clima WMO en español. */
    static String describe(int code) {
        if (code == 0) return "Despejado";
        if (code == 1) return "Mayormente despejado";
        if (code == 2) return "Parcialmente nublado";
        if (code == 3) return "Nublado";
        if (code == 45 || code == 48) return "Neblina";
        if (code >= 51 && code <= 57) return "Llovizna";
        if (code >= 61 && code <= 67) return "Lluvia";
        if (code >= 71 && code <= 77) return "Nieve";
        if (code >= 80 && code <= 82) return "Chubascos";
        if (code == 85 || code == 86) return "Chubascos de nieve";
        if (code >= 95) return "Tormenta";
        return "";
    }
}
