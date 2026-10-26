package cl.jc.inicio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Pantalla de inicio: reloj, clima, recientes y solo las apps que tú elijas. */
public class MainActivity extends Activity {

    private static final int COLUMNS = 4;
    private static final String KEY_ALL = "__all_apps__";
    private static final String KEY_STORE = "__store__";
    private static final String KEY_CUSTOMIZE = "__customize__";
    private static final String KEY_SETTINGS = "__settings__";
    private static final String RECENT_PREFIX = "r:";
    private static final Locale ES = new Locale("es", "CL");

    private AppRepository repo;
    private Config config;
    private Theme theme;

    private FrameLayout root;
    private ImageView wallpaper;
    private View scrim;
    private ScrollView scroll;
    private TextView clock, date, weatherTemp, weatherDesc;
    private LinearLayout weatherBox;
    private TextView recentsTitle, myAppsTitle, emptyText, hint;
    private LinearLayout recentsGrid, grid;
    private View firstTile;
    private String lastFocusedKey;

    private final BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateClock();
            refreshWeather();
        }
    };

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            IconLoader.clear();
            rebuild();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repo = new AppRepository(this);
        config = new Config(this);
        setContentView(buildLayout());

        IntentFilter pf = new IntentFilter();
        pf.addAction(Intent.ACTION_PACKAGE_ADDED);
        pf.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pf.addAction(Intent.ACTION_PACKAGE_CHANGED);
        pf.addDataScheme("package");
        registerReceiver(packageReceiver, pf);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(packageReceiver);
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter tf = new IntentFilter();
        tf.addAction(Intent.ACTION_TIME_TICK);
        tf.addAction(Intent.ACTION_TIME_CHANGED);
        tf.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(timeReceiver, tf);
        updateClock();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(timeReceiver);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyAppearance();
        rebuild();
    }

    /** Al apretar Home estando ya en Inicio, vuelve arriba. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        lastFocusedKey = null;
        scroll.smoothScrollTo(0, 0);
        if (firstTile != null) firstTile.requestFocus();
    }

    /** Atrás no hace nada en Inicio (es la pantalla base). */
    @Override
    public void onBackPressed() {
        // intencionalmente vacío
    }

    // --- construcción de la pantalla ---

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private View buildLayout() {
        root = new FrameLayout(this);

        wallpaper = new ImageView(this);
        wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        wallpaper.setVisibility(View.GONE);
        root.addView(wallpaper, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Oscurece la imagen de fondo para que el texto siempre se lea
        scrim = new View(this);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x8C000000, 0xB3000000, 0xE0000000}));
        scrim.setVisibility(View.GONE);
        root.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        int side = Ui.dp(this, Ui.SIDE_MARGIN_DP);
        content.setPadding(side, Ui.dp(this, Ui.TOP_MARGIN_DP), side, Ui.dp(this, 40));
        scroll.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Encabezado: reloj a la izquierda, clima a la derecha
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.BOTTOM);
        content.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        header.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        clock = Ui.text(this, "", 64, Theme.TEXT_PRIMARY, true);
        clock.setIncludeFontPadding(false);
        left.addView(clock);
        date = Ui.text(this, "", 18, Theme.TEXT_SECONDARY, false);
        LinearLayout.LayoutParams dlp = wrap();
        dlp.topMargin = Ui.dp(this, 6);
        left.addView(date, dlp);

        weatherBox = new LinearLayout(this);
        weatherBox.setOrientation(LinearLayout.VERTICAL);
        weatherBox.setGravity(Gravity.END);
        weatherBox.setVisibility(View.GONE);
        header.addView(weatherBox, wrap());
        weatherTemp = Ui.text(this, "", 44, Theme.TEXT_PRIMARY, true);
        weatherTemp.setIncludeFontPadding(false);
        weatherTemp.setGravity(Gravity.END);
        weatherBox.addView(weatherTemp, wrap());
        weatherDesc = Ui.text(this, "", 15, Theme.TEXT_SECONDARY, false);
        weatherDesc.setGravity(Gravity.END);
        LinearLayout.LayoutParams wlp = wrap();
        wlp.topMargin = Ui.dp(this, 4);
        weatherBox.addView(weatherDesc, wlp);

        recentsTitle = sectionTitle(getString(R.string.recents));
        content.addView(recentsTitle, sectionParams());
        recentsGrid = gridContainer();
        content.addView(recentsGrid);

        myAppsTitle = sectionTitle(getString(R.string.my_apps));
        content.addView(myAppsTitle, sectionParams());

        emptyText = Ui.text(this, getString(R.string.empty_home), 15, Theme.TEXT_SECONDARY, false);
        emptyText.setVisibility(View.GONE);
        LinearLayout.LayoutParams elp = wrap();
        elp.bottomMargin = Ui.dp(this, 18);
        content.addView(emptyText, elp);

        grid = gridContainer();
        content.addView(grid);

        hint = Ui.text(this, getString(R.string.home_hint), 12, Theme.TEXT_SECONDARY, false);
        hint.setAlpha(0.6f);
        content.addView(hint);

        return root;
    }

    private TextView sectionTitle(String s) {
        TextView t = Ui.text(this, s, 15, Theme.TEXT_SECONDARY, false);
        t.setAllCaps(true);
        t.setLetterSpacing(0.12f);
        return t;
    }

    private LinearLayout.LayoutParams sectionParams() {
        LinearLayout.LayoutParams lp = wrap();
        lp.topMargin = Ui.dp(this, 28);
        lp.bottomMargin = Ui.dp(this, 18);
        return lp;
    }

    private LinearLayout gridContainer() {
        LinearLayout g = new LinearLayout(this);
        g.setOrientation(LinearLayout.VERTICAL);
        g.setClipChildren(false);
        g.setClipToPadding(false);
        return g;
    }

    // --- apariencia: tema, fondo, clima ---

    private void applyAppearance() {
        theme = Theme.current(this);
        root.setBackground(theme.background());

        if (config.background() == Config.BG_IMAGE) {
            Wallpaper.load(this, new Wallpaper.Callback() {
                @Override
                public void onLoaded(Bitmap bitmap) {
                    boolean has = bitmap != null;
                    wallpaper.setImageBitmap(bitmap);
                    wallpaper.setVisibility(has ? View.VISIBLE : View.GONE);
                    scrim.setVisibility(has ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            wallpaper.setImageDrawable(null);
            wallpaper.setVisibility(View.GONE);
            scrim.setVisibility(View.GONE);
        }

        if (config.weather()) {
            refreshWeather();
        } else {
            weatherBox.setVisibility(View.GONE);
        }
    }

    private void refreshWeather() {
        if (!config.weather()) return;
        Weather.get(this, new Weather.Callback() {
            @Override
            public void onWeather(int tempC, String description) {
                if (!config.weather()) return;
                weatherTemp.setText(tempC + "°");
                weatherDesc.setText(description.isEmpty() ? Weather.CITY : description + "  ·  " + Weather.CITY);
                weatherBox.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateClock() {
        if (clock == null) return;
        Date now = new Date();
        String pattern = DateFormat.is24HourFormat(this) ? "HH:mm" : "h:mm";
        clock.setText(new SimpleDateFormat(pattern, ES).format(now));

        String d = new SimpleDateFormat("EEEE d 'de' MMMM", ES).format(now);
        d = Character.toUpperCase(d.charAt(0)) + d.substring(1);
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting = h < 6 ? "Buenas noches" : h < 12 ? "Buenos días" : h < 20 ? "Buenas tardes" : "Buenas noches";
        date.setText(d + "  ·  " + greeting);
    }

    // --- tarjetas ---

    private void rebuild() {
        if (theme == null) theme = Theme.current(this);
        List<AppEntry> all = repo.loadAllApps();
        int cardW = Ui.cardWidthDp(this, COLUMNS);
        View toFocus = null;
        firstTile = null;

        // Recientes
        List<View> recentTiles = new ArrayList<>();
        if (config.recents()) {
            for (AppEntry app : repo.recentApps(all)) {
                View v = appTile(app, cardW, RECENT_PREFIX + app.key(), true);
                if ((RECENT_PREFIX + app.key()).equals(lastFocusedKey)) toFocus = v;
                recentTiles.add(v);
            }
        }
        boolean showRecents = !recentTiles.isEmpty();
        recentsTitle.setVisibility(showRecents ? View.VISIBLE : View.GONE);
        recentsGrid.setVisibility(showRecents ? View.VISIBLE : View.GONE);
        Ui.fillGrid(recentsGrid, recentTiles, COLUMNS);
        if (showRecents) firstTile = recentTiles.get(0);

        // Mis apps
        List<AppEntry> home = repo.homeApps(all);
        List<View> tiles = new ArrayList<>();
        for (AppEntry app : home) {
            View v = appTile(app, cardW, app.key(), false);
            if (app.key().equals(lastFocusedKey)) toFocus = v;
            tiles.add(v);
        }

        // Accesos fijos
        View allApps = fixedTile(cardW, getString(R.string.all_apps), R.drawable.ic_all_apps, KEY_ALL,
                new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(MainActivity.this, AllAppsActivity.class));
                    }
                });
        tiles.add(allApps);

        final Intent store = Ui.playStoreIntent(this);
        if (store != null) {
            tiles.add(fixedTile(cardW, getString(R.string.get_apps), R.drawable.ic_store, KEY_STORE,
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                startActivity(store);
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(MainActivity.this, "No se pudo abrir Google Play", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }));
        }

        tiles.add(fixedTile(cardW, getString(R.string.customize), R.drawable.ic_customize, KEY_CUSTOMIZE,
                new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(MainActivity.this, CustomizeActivity.class));
                    }
                }));

        tiles.add(fixedTile(cardW, getString(R.string.settings), R.drawable.ic_settings, KEY_SETTINGS,
                new Runnable() {
                    @Override
                    public void run() {
                        Ui.openSettings(MainActivity.this);
                    }
                }));

        for (View v : tiles) {
            if (lastFocusedKey != null && lastFocusedKey.equals(v.getTag())) toFocus = v;
        }

        emptyText.setVisibility(home.isEmpty() ? View.VISIBLE : View.GONE);
        Ui.fillGrid(grid, tiles, COLUMNS);
        if (firstTile == null) firstTile = tiles.get(0);

        final View target = toFocus != null ? toFocus : firstTile;
        target.post(new Runnable() {
            @Override
            public void run() {
                target.requestFocus();
            }
        });
    }

    private View appTile(final AppEntry app, int cardW, String focusKey, final boolean isRecent) {
        final Ui.Tile tile = Ui.createTile(this, theme, cardW, app.label);
        tile.root.setTag(focusKey);
        IconLoader.load(this, app, new IconLoader.Callback() {
            @Override
            public void onLoaded(Drawable d, boolean isBanner) {
                if (isBanner) tile.showBanner(d);
                else tile.showIcon(d);
            }
        });
        tile.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ui.launch(MainActivity.this, app);
            }
        });
        tile.root.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (isRecent) showRecentMenu(app);
                else showAppMenu(app);
                return true;
            }
        });
        trackFocus(tile.root, focusKey);
        return tile.root;
    }

    private View fixedTile(int cardW, String label, int iconRes, String key, final Runnable action) {
        Ui.Tile tile = Ui.createTile(this, theme, cardW, label);
        tile.root.setTag(key);
        tile.showIcon(getDrawable(iconRes));
        tile.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
            }
        });
        trackFocus(tile.root, key);
        return tile.root;
    }

    private void trackFocus(View v, final String key) {
        final View.OnFocusChangeListener original = v.getOnFocusChangeListener();
        v.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (original != null) original.onFocusChange(view, hasFocus);
                if (hasFocus) lastFocusedKey = key;
            }
        });
    }

    // --- menús ---

    private AlertDialog.Builder dialog(String title) {
        return new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert).setTitle(title);
    }

    private void showAppMenu(final AppEntry app) {
        final String[] options = {
                "Mover a la izquierda",
                "Mover a la derecha",
                "Mover al principio",
                "Quitar de Inicio",
                "Información de la app"
        };
        dialog(app.label).setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                String key = app.key();
                switch (which) {
                    case 0: repo.move(key, -1); break;
                    case 1: repo.move(key, +1); break;
                    case 2: repo.moveToFirst(key); break;
                    case 3: repo.removeFromHome(key); lastFocusedKey = null; break;
                    case 4: Ui.openAppInfo(MainActivity.this, app); return;
                }
                rebuild();
            }
        }).show();
    }

    private void showRecentMenu(final AppEntry app) {
        final boolean onHome = repo.isOnHome(app.key());
        final String[] options = {
                "Quitar de recientes",
                onHome ? "Quitar de Inicio" : "Agregar a Inicio",
                "Información de la app"
        };
        dialog(app.label).setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                switch (which) {
                    case 0: repo.removeRecent(app.key()); lastFocusedKey = null; break;
                    case 1:
                        if (onHome) repo.removeFromHome(app.key());
                        else repo.addToHome(app.key());
                        break;
                    case 2: Ui.openAppInfo(MainActivity.this, app); return;
                }
                rebuild();
            }
        }).show();
    }
}
