package cl.jc.inicio;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Personalizar: tema de color, fondo, clima y fila de recientes. Los cambios se ven al tiro. */
public class CustomizeActivity extends Activity {

    private static final int REQ_STORAGE = 7;

    private Config config;
    private Theme theme;
    private ScrollView scroll;
    private LinearLayout content;
    private int focusIndex = 0;
    private final List<View> rows = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        config = new Config(this);
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        int side = Ui.dp(this, 120);
        content.setPadding(side, Ui.dp(this, Ui.TOP_MARGIN_DP), side, Ui.dp(this, 40));
        scroll.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        build(); // vuelve a revisar si ya existe la imagen de fondo
    }

    private void build() {
        theme = Theme.current(this);
        scroll.setBackground(theme.background());
        content.removeAllViews();
        rows.clear();

        content.addView(Ui.text(this, getString(R.string.customize), 34, Theme.TEXT_PRIMARY, true));
        TextView sub = Ui.text(this, "Usa ◀ ▶ o presiona OK para cambiar cada opción", 14, Theme.TEXT_SECONDARY, false);
        LinearLayout.LayoutParams slp = lp();
        slp.topMargin = Ui.dp(this, 6);
        slp.bottomMargin = Ui.dp(this, 24);
        content.addView(sub, slp);

        addOption("Tema de color", theme.name, new Step() {
            @Override
            public void go(int dir) {
                int n = Theme.ALL.length;
                config.setThemeIndex(((config.themeIndex() + dir) % n + n) % n);
            }
        });
        addSwatches();

        final boolean image = config.background() == Config.BG_IMAGE;
        addOption("Fondo", image ? "Imagen propia" : "Degradado del tema", new Step() {
            @Override
            public void go(int dir) {
                boolean nowImage = config.background() != Config.BG_IMAGE;
                config.setBackground(nowImage ? Config.BG_IMAGE : Config.BG_GRADIENT);
                if (nowImage) askStoragePermission();
            }
        });
        if (image) addWallpaperHelp();

        addOption("Clima en " + Weather.CITY, config.weather() ? "Mostrar" : "Ocultar", new Step() {
            @Override
            public void go(int dir) {
                config.setWeather(!config.weather());
            }
        });

        addOption("Fila de recientes", config.recents() ? "Mostrar" : "Ocultar", new Step() {
            @Override
            public void go(int dir) {
                config.setRecents(!config.recents());
            }
        });

        int i = Math.max(0, Math.min(rows.size() - 1, focusIndex));
        final View target = rows.get(i);
        target.post(new Runnable() {
            @Override
            public void run() {
                target.requestFocus();
            }
        });
    }

    private interface Step {
        void go(int dir);
    }

    private LinearLayout.LayoutParams lp() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void addOption(String label, String value, final Step step) {
        final int index = rows.size();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setFocusable(true);
        row.setClickable(true);
        row.setBackground(theme.cardBackground(this));
        int ph = Ui.dp(this, 24), pv = Ui.dp(this, 16);
        row.setPadding(ph, pv, ph, pv);

        row.addView(Ui.text(this, label, 18, Theme.TEXT_PRIMARY, false),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView val = Ui.text(this, "◀   " + value + "   ▶", 18, theme.accent, false);
        row.addView(val);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                apply(step, +1, index);
            }
        });
        row.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent e) {
                if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) { apply(step, -1, index); return true; }
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) { apply(step, +1, index); return true; }
                return false;
            }
        });

        LinearLayout.LayoutParams p = lp();
        p.bottomMargin = Ui.dp(this, 12);
        content.addView(row, p);
        rows.add(row);
    }

    private void apply(Step step, int dir, int index) {
        step.go(dir);
        focusIndex = index;
        build();
    }

    /** Muestra los colores de acento de todos los temas, marcando el actual. */
    private void addSwatches() {
        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER_VERTICAL);
        int current = config.themeIndex();
        for (int i = 0; i < Theme.ALL.length; i++) {
            Theme t = Theme.ALL[i];
            View dot = new View(this);
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
            g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            g.setColor(t.accent);
            if (i == current) g.setStroke(Ui.dp(this, 3), Theme.TEXT_PRIMARY);
            dot.setBackground(g);
            int size = Ui.dp(this, i == current ? 26 : 18);
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(size, size);
            dp.rightMargin = Ui.dp(this, 14);
            strip.addView(dot, dp);
        }
        LinearLayout.LayoutParams p = lp();
        p.leftMargin = Ui.dp(this, 24);
        p.bottomMargin = Ui.dp(this, 20);
        content.addView(strip, p);
    }

    private void addWallpaperHelp() {
        File found = Wallpaper.find(this);
        String status = found != null
                ? "✓ Imagen encontrada: " + found.getName()
                : "Aún no hay imagen. Mientras tanto se usa el degradado del tema.";
        TextView st = Ui.text(this, status, 15, found != null ? theme.accent : Theme.TEXT_SECONDARY, false);

        File folder = Wallpaper.appFolder(this);
        String help = "Guarda una foto llamada fondo.jpg en la carpeta Descargas (Download) del Stick, "
                + "por ejemplo enviándola desde tu celular con la app «Send Files to TV». "
                + "También puedes copiarla con ADB:\n"
                + "adb push fondo.jpg " + (folder != null ? folder.getAbsolutePath() : "/sdcard/Download") + "/fondo.jpg\n"
                + "Recomendado: imagen horizontal de 1920×1080.";
        TextView h = Ui.text(this, help, 13, Theme.TEXT_SECONDARY, false);
        h.setLineSpacing(0, 1.25f);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 24);
        box.setPadding(pad, 0, pad, Ui.dp(this, 20));
        box.addView(st);
        LinearLayout.LayoutParams hp = lp();
        hp.topMargin = Ui.dp(this, 6);
        box.addView(h, hp);
        content.addView(box, lp());
    }

    private void askStoragePermission() {
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT <= 32 && !Wallpaper.canReadDownloads(this)) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        build();
    }
}
