package cl.jc.inicio;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lee las apps instaladas y guarda qué apps van en Inicio y en qué orden.
 * Todo queda en SharedPreferences, sin servicios en segundo plano.
 */
public final class AppRepository {

    private static final String PREFS = "inicio";
    private static final String KEY_HOME = "home_order";
    private static final String KEY_INITIALIZED = "initialized";

    private final Context context;
    private final PackageManager pm;
    private final SharedPreferences prefs;

    public AppRepository(Context context) {
        this.context = context.getApplicationContext();
        this.pm = this.context.getPackageManager();
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Todas las apps que se pueden abrir, una por paquete, ordenadas alfabéticamente. */
    public List<AppEntry> loadAllApps() {
        Map<String, AppEntry> byPackage = new LinkedHashMap<>();
        // Primero las apps de TV: si una app tiene versión TV, se usa esa.
        collect(byPackage, Intent.CATEGORY_LEANBACK_LAUNCHER, true);
        collect(byPackage, Intent.CATEGORY_LAUNCHER, false);

        List<AppEntry> list = new ArrayList<>(byPackage.values());
        final Collator collator = Collator.getInstance(new Locale("es", "CL"));
        collator.setStrength(Collator.PRIMARY);
        Collections.sort(list, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry a, AppEntry b) {
                return collator.compare(a.label, b.label);
            }
        });
        return list;
    }

    private void collect(Map<String, AppEntry> out, String category, boolean tv) {
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(category);
        List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);
        String self = context.getPackageName();
        for (ResolveInfo ri : infos) {
            ActivityInfo ai = ri.activityInfo;
            if (ai == null || self.equals(ai.packageName) || out.containsKey(ai.packageName)) {
                continue;
            }
            CharSequence label = ri.loadLabel(pm);
            String text = label == null ? ai.packageName : label.toString().trim();
            out.put(ai.packageName, new AppEntry(new ComponentName(ai.packageName, ai.name), text, tv));
        }
    }

    /** Apps de Inicio en el orden elegido. Solo incluye apps que siguen instaladas. */
    public List<AppEntry> homeApps(List<AppEntry> all) {
        Map<String, AppEntry> byKey = new LinkedHashMap<>();
        for (AppEntry e : all) {
            byKey.put(e.key(), e);
        }
        List<String> keys = homeKeys(all);
        List<AppEntry> result = new ArrayList<>();
        List<String> cleaned = new ArrayList<>();
        for (String k : keys) {
            AppEntry e = byKey.get(k);
            if (e != null) {
                result.add(e);
                cleaned.add(k);
            }
        }
        if (cleaned.size() != keys.size()) {
            saveHomeKeys(cleaned); // limpia apps desinstaladas
        }
        return result;
    }

    public boolean isOnHome(String key) {
        return readHomeKeys().contains(key);
    }

    public void addToHome(String key) {
        List<String> keys = readHomeKeys();
        if (!keys.contains(key)) {
            keys.add(key);
            saveHomeKeys(keys);
        }
    }

    public void removeFromHome(String key) {
        List<String> keys = readHomeKeys();
        if (keys.remove(key)) {
            saveHomeKeys(keys);
        }
    }

    /** Mueve una app dentro de Inicio. delta = -1 izquierda, +1 derecha. */
    public void move(String key, int delta) {
        List<String> keys = readHomeKeys();
        int from = keys.indexOf(key);
        if (from < 0) return;
        int to = Math.max(0, Math.min(keys.size() - 1, from + delta));
        if (to == from) return;
        keys.remove(from);
        keys.add(to, key);
        saveHomeKeys(keys);
    }

    public void moveToFirst(String key) {
        List<String> keys = readHomeKeys();
        if (keys.remove(key)) {
            keys.add(0, key);
            saveHomeKeys(keys);
        }
    }

    // --- recientes ---

    private static final String KEY_RECENT = "recent";
    static final int MAX_RECENT = 4;

    /** Guarda la app recién abierta al principio de la lista de recientes. */
    public void recordLaunch(String key) {
        List<String> keys = readList(KEY_RECENT);
        keys.remove(key);
        keys.add(0, key);
        while (keys.size() > MAX_RECENT) keys.remove(keys.size() - 1);
        prefs.edit().putString(KEY_RECENT, TextUtils.join("\n", keys)).apply();
    }

    public void removeRecent(String key) {
        List<String> keys = readList(KEY_RECENT);
        if (keys.remove(key)) {
            prefs.edit().putString(KEY_RECENT, TextUtils.join("\n", keys)).apply();
        }
    }

    public List<AppEntry> recentApps(List<AppEntry> all) {
        Map<String, AppEntry> byKey = new LinkedHashMap<>();
        for (AppEntry e : all) byKey.put(e.key(), e);
        List<AppEntry> result = new ArrayList<>();
        for (String k : readList(KEY_RECENT)) {
            AppEntry e = byKey.get(k);
            if (e != null) result.add(e);
        }
        return result;
    }

    private List<String> readList(String prefKey) {
        String raw = prefs.getString(prefKey, "");
        List<String> keys = new ArrayList<>();
        if (!TextUtils.isEmpty(raw)) keys.addAll(Arrays.asList(raw.split("\n")));
        return keys;
    }

    // --- almacenamiento ---

    private List<String> homeKeys(List<AppEntry> all) {
        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            // Primera vez: Inicio parte solo con las apps hechas para TV.
            List<String> initial = new ArrayList<>();
            for (AppEntry e : all) {
                if (e.isTvApp) initial.add(e.key());
            }
            prefs.edit().putBoolean(KEY_INITIALIZED, true).apply();
            saveHomeKeys(initial);
            return initial;
        }
        return readHomeKeys();
    }

    private List<String> readHomeKeys() {
        String raw = prefs.getString(KEY_HOME, "");
        List<String> keys = new ArrayList<>();
        if (!TextUtils.isEmpty(raw)) {
            keys.addAll(Arrays.asList(raw.split("\n")));
        }
        return keys;
    }

    private void saveHomeKeys(List<String> keys) {
        prefs.edit().putString(KEY_HOME, TextUtils.join("\n", keys)).apply();
    }
}
