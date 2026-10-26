package cl.jc.inicio;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Carga el banner (o el ícono) de cada app en segundo plano para que
 * la pantalla aparezca al instante incluso en un equipo de 1 GB de RAM.
 */
final class IconLoader {

    interface Callback {
        void onLoaded(Drawable drawable, boolean isBanner);
    }

    private static final class Cached {
        final Drawable.ConstantState state;
        final boolean isBanner;

        Cached(Drawable.ConstantState state, boolean isBanner) {
            this.state = state;
            this.isBanner = isBanner;
        }
    }

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<String, Cached> CACHE = new HashMap<>();

    private IconLoader() {
    }

    static void load(Context context, final AppEntry app, final Callback callback) {
        Cached hit;
        synchronized (CACHE) {
            hit = CACHE.get(app.key());
        }
        if (hit != null) {
            callback.onLoaded(hit.state.newDrawable(), hit.isBanner);
            return;
        }
        final PackageManager pm = context.getApplicationContext().getPackageManager();
        EXEC.execute(new Runnable() {
            @Override
            public void run() {
                Drawable d = null;
                boolean banner = false;
                try {
                    d = pm.getActivityBanner(app.component);
                } catch (PackageManager.NameNotFoundException ignored) {
                }
                if (d == null) {
                    try {
                        d = pm.getApplicationBanner(app.component.getPackageName());
                    } catch (PackageManager.NameNotFoundException ignored) {
                    }
                }
                if (d != null) {
                    banner = true;
                } else {
                    try {
                        d = pm.getActivityIcon(app.component);
                    } catch (PackageManager.NameNotFoundException e) {
                        d = pm.getDefaultActivityIcon();
                    }
                }
                final Drawable result = d;
                final boolean isBanner = banner;
                Drawable.ConstantState cs = result.getConstantState();
                if (cs != null) {
                    synchronized (CACHE) {
                        CACHE.put(app.key(), new Cached(cs, isBanner));
                    }
                }
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onLoaded(result, isBanner);
                    }
                });
            }
        });
    }

    /** Se llama cuando se instalan o actualizan apps. */
    static void clear() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }
}
