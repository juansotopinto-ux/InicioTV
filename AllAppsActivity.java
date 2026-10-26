package cl.jc.inicio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Todas las apps instaladas. Desde aquí se agregan o quitan de Inicio. */
public class AllAppsActivity extends Activity {

    private static final int COLUMNS = 5;

    private AppRepository repo;
    private Theme theme;
    private ScrollView scroll;
    private LinearLayout grid;
    private String lastFocusedKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repo = new AppRepository(this);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        int side = Ui.dp(this, Ui.SIDE_MARGIN_DP);
        content.setPadding(side, Ui.dp(this, Ui.TOP_MARGIN_DP), side, Ui.dp(this, 40));
        scroll.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(Ui.text(this, getString(R.string.all_apps), 34, Theme.TEXT_PRIMARY, true));

        TextView hint = Ui.text(this, getString(R.string.all_apps_hint), 14, Theme.TEXT_SECONDARY, false);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = Ui.dp(this, 6);
        hlp.bottomMargin = Ui.dp(this, 28);
        content.addView(hint, hlp);

        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        content.addView(grid);

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        rebuild();
    }

    private void rebuild() {
        theme = Theme.current(this);
        scroll.setBackground(theme.background());
        List<AppEntry> all = repo.loadAllApps();
        int cardW = Ui.cardWidthDp(this, COLUMNS);
        List<View> tiles = new ArrayList<>();
        View toFocus = null;

        for (final AppEntry app : all) {
            final Ui.Tile tile = Ui.createTile(this, theme, cardW, app.label);
            if (repo.isOnHome(app.key())) {
                tile.badge.setText("★ En Inicio");
                tile.badge.setVisibility(View.VISIBLE);
            } else {
                tile.badge.setText(" ");
                tile.badge.setVisibility(View.INVISIBLE); // mantiene la altura pareja
            }
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
                    Ui.launch(AllAppsActivity.this, app);
                }
            });
            tile.root.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showMenu(app);
                    return true;
                }
            });
            final View.OnFocusChangeListener base = tile.root.getOnFocusChangeListener();
            tile.root.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    base.onFocusChange(v, hasFocus);
                    if (hasFocus) lastFocusedKey = app.key();
                }
            });
            if (app.key().equals(lastFocusedKey)) toFocus = tile.root;
            tiles.add(tile.root);
        }

        Ui.fillGrid(grid, tiles, COLUMNS);
        if (tiles.isEmpty()) return;
        final View target = toFocus != null ? toFocus : tiles.get(0);
        target.post(new Runnable() {
            @Override
            public void run() {
                target.requestFocus();
            }
        });
    }

    private void showMenu(final AppEntry app) {
        final boolean onHome = repo.isOnHome(app.key());
        String[] options = {
                onHome ? "Quitar de Inicio" : "Agregar a Inicio",
                "Información de la app"
        };
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(app.label)
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            if (onHome) repo.removeFromHome(app.key());
                            else repo.addToHome(app.key());
                            rebuild();
                        } else {
                            Ui.openAppInfo(AllAppsActivity.this, app);
                        }
                    }
                })
                .show();
    }
}
