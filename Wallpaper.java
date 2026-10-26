package cl.jc.inicio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import java.io.File;

/**
 * Imagen de fondo propia. Se busca "fondo.jpg" (o .png / .jpeg) en:
 *  1) la carpeta de la app:  /sdcard/Android/data/cl.jc.inicio/files/
 *  2) la carpeta Descargas:  /sdcard/Download/   (requiere permiso de almacenamiento)
 */
final class Wallpaper {

    interface Callback {
        void onLoaded(Bitmap bitmap); // null si no hay imagen
    }

    private static final String[] NAMES = {"fondo.jpg", "fondo.jpeg", "fondo.png"};
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static Bitmap cached;
    private static String cachedSignature;

    private Wallpaper() {
    }

    static File appFolder(Context c) {
        return c.getExternalFilesDir(null); // también crea la carpeta
    }

    static File find(Context c) {
        File dir = appFolder(c);
        if (dir != null) {
            for (String n : NAMES) {
                File f = new File(dir, n);
                if (f.isFile()) return f;
            }
        }
        if (canReadDownloads(c)) {
            File dl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            for (String n : NAMES) {
                File f = new File(dl, n);
                if (f.isFile()) return f;
            }
        }
        return null;
    }

    static boolean canReadDownloads(Context c) {
        return Build.VERSION.SDK_INT < 23
                || c.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    static void load(final Context context, final Callback cb) {
        final File f = find(context);
        if (f == null) {
            cb.onLoaded(null);
            return;
        }
        final String sig = f.getAbsolutePath() + ":" + f.lastModified() + ":" + f.length();
        if (sig.equals(cachedSignature) && cached != null) {
            cb.onLoaded(cached);
            return;
        }
        final int targetW = context.getResources().getDisplayMetrics().widthPixels;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = null;
                try {
                    BitmapFactory.Options bounds = new BitmapFactory.Options();
                    bounds.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(f.getAbsolutePath(), bounds);
                    int sample = 1;
                    while (bounds.outWidth / (sample * 2) >= targetW) sample *= 2;

                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inSampleSize = sample;
                    opts.inPreferredConfig = Bitmap.Config.RGB_565; // ahorra la mitad de memoria
                    bmp = BitmapFactory.decodeFile(f.getAbsolutePath(), opts);
                } catch (OutOfMemoryError | RuntimeException ignored) {
                }
                final Bitmap result = bmp;
                if (result != null) {
                    cached = result;
                    cachedSignature = sig;
                }
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        cb.onLoaded(result);
                    }
                });
            }
        }).start();
    }
}
