package cl.jc.inicio;

import android.content.ComponentName;

/** Una app que se puede abrir desde el launcher. */
public final class AppEntry {
    public final ComponentName component;
    public final String label;
    /** true si la app está hecha para TV (aparece en LEANBACK_LAUNCHER). */
    public final boolean isTvApp;

    AppEntry(ComponentName component, String label, boolean isTvApp) {
        this.component = component;
        this.label = label;
        this.isTvApp = isTvApp;
    }

    public String key() {
        return component.flattenToString();
    }
}
