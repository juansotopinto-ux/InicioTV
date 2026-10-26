package cl.jc.inicio;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Piezas visuales compartidas: tarjetas, grilla y acciones comunes. */
final class Ui {

    static final int SIDE_MARGIN_DP = 48;   // margen seguro de TV (overscan)
    static final int TOP_MARGIN_DP = 32;
    static final int GAP_DP = 24;

    private Ui() {
    }

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** Ancho de cada tarjeta para que entren `cols` columnas en pantalla. */
    static int cardWidthDp(Context c, int cols) {
        int screen = c.getResources().getConfiguration().screenWidthDp;
        if (screen <= 0) screen = 960;
        return (screen - 2 * SIDE_MARGIN_DP - (cols - 1) * GAP_DP) / cols;
    }

    /** Una tarjeta enfocable con imagen, nombre y una etiqueta opcional. */
    static final class Tile {
        final LinearLayout root;
        final ImageView image;
        final TextView title;
        final TextView badge;
        final int cardHeightDp;

        Tile(LinearLayout root, ImageView image, TextView title, TextView badge, int cardHeightDp) {
            this.root = root;
            this.image = image;
            this.title = title;
            this.badge = badge;
            this.cardHeightDp = cardHeightDp;
        }

        /** Banner de TV ocupando toda la tarjeta. */
        void showBanner(Drawable d) {
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setPadding(0, 0, 0, 0);
            image.setImageDrawable(d);
        }

        /** Ícono pequeño centrado (apps sin banner o accesos propios). */
        void showIcon(Drawable d) {
            Context c = image.getContext();
            int p = dp(c, cardHeightDp * 0.24f);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setPadding(p, p, p, p);
            image.setImageDrawable(d);
        }
    }

    static Tile createTile(final Context c, Theme theme, int cardWidthDp, String label) {
        final int cardHeightDp = Math.round(cardWidthDp * 9f / 16f);

        final LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setFocusable(true);
        root.setFocusableInTouchMode(false);
        root.setClickable(true);
        root.setLongClickable(true);
        root.setClipChildren(false);
        root.setClipToPadding(false);

        FrameLayout card = new FrameLayout(c);
        card.setBackground(theme.cardBackground(c));
        card.setDuplicateParentStateEnabled(true);
        int pad = dp(c, 3);
        card.setPadding(pad, pad, pad, pad);
        root.addView(card, new LinearLayout.LayoutParams(dp(c, cardWidthDp), dp(c, cardHeightDp)));

        ImageView image = new ImageView(c);
        final int radius = dp(c, 11);
        image.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        image.setClipToOutline(true);
        card.addView(image, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final TextView title = new TextView(c);
        title.setText(label);
        title.setTextSize(13);
        title.setTextColor(Theme.TEXT_SECONDARY);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                dp(c, cardWidthDp), ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(c, 10);
        root.addView(title, tlp);

        TextView badge = new TextView(c);
        badge.setTextSize(11);
        badge.setTextColor(theme.accent);
        badge.setGravity(Gravity.CENTER_HORIZONTAL);
        badge.setVisibility(View.GONE);
        root.addView(badge, new LinearLayout.LayoutParams(
                dp(c, cardWidthDp), ViewGroup.LayoutParams.WRAP_CONTENT));

        root.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                float s = hasFocus ? 1.10f : 1f;
                v.animate().scaleX(s).scaleY(s).setDuration(140).start();
                v.setTranslationZ(hasFocus ? dp(c, 8) : 0);
                title.setTextColor(hasFocus ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);
                title.setTypeface(null, hasFocus ? Typeface.BOLD : Typeface.NORMAL);
            }
        });

        return new Tile(root, image, title, badge, cardHeightDp);
    }

    /** Reparte las tarjetas en filas de `cols` columnas. */
    static void fillGrid(LinearLayout container, List<View> tiles, int cols) {
        Context c = container.getContext();
        container.removeAllViews();
        LinearLayout row = null;
        for (int i = 0; i < tiles.size(); i++) {
            if (i % cols == 0) {
                row = new LinearLayout(c);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setClipChildren(false);
                row.setClipToPadding(false);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rlp.bottomMargin = dp(c, GAP_DP);
                container.addView(row, rlp);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i % cols != 0) lp.leftMargin = dp(c, GAP_DP);
            row.addView(tiles.get(i), lp);
        }
    }

    static void launch(Activity a, AppEntry app) {
        new AppRepository(a).recordLaunch(app.key());
        Intent i = new Intent(Intent.ACTION_MAIN)
                .addCategory(app.isTvApp ? Intent.CATEGORY_LEANBACK_LAUNCHER : Intent.CATEGORY_LAUNCHER)
                .setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            a.startActivity(i);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(a, "No se pudo abrir " + app.label, Toast.LENGTH_SHORT).show();
        }
    }

    static void openAppInfo(Activity a, AppEntry app) {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + app.component.getPackageName()));
        try {
            a.startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(a, "Este equipo no permite abrir la información de la app", Toast.LENGTH_SHORT).show();
        }
    }

    /** Intent para abrir Google Play en la TV, o null si no está instalado. */
    static Intent playStoreIntent(Context c) {
        Intent i = c.getPackageManager().getLeanbackLaunchIntentForPackage("com.android.vending");
        if (i == null) i = c.getPackageManager().getLaunchIntentForPackage("com.android.vending");
        return i;
    }

    static void openSettings(Activity a) {
        try {
            a.startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(a, "No se encontraron los Ajustes", Toast.LENGTH_SHORT).show();
        }
    }

    static TextView text(Context c, String s, float sp, int argb, boolean light) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(argb);
        if (light) t.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        return t;
    }
}
