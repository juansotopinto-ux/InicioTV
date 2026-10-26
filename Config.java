package cl.jc.inicio;

import android.content.Context;
import android.content.SharedPreferences;

/** Preferencias de personalización. */
final class Config {

    static final int BG_GRADIENT = 0;
    static final int BG_IMAGE = 1;

    private static final String PREFS = "config";
    private final SharedPreferences p;

    Config(Context c) {
        p = c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    int themeIndex() { return p.getInt("theme", 0); }
    void setThemeIndex(int i) { p.edit().putInt("theme", i).apply(); }

    int background() { return p.getInt("bg", BG_GRADIENT); }
    void setBackground(int b) { p.edit().putInt("bg", b).apply(); }

    boolean weather() { return p.getBoolean("weather", true); }
    void setWeather(boolean on) { p.edit().putBoolean("weather", on).apply(); }

    boolean recents() { return p.getBoolean("recents", true); }
    void setRecents(boolean on) { p.edit().putBoolean("recents", on).apply(); }

    // Caché del clima para mostrarlo al instante
    SharedPreferences raw() { return p; }
}
