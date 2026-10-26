package cl.jc.inicio;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;

/** Temas de color disponibles. */
final class Theme {

    final String name;
    final int bgTop, bgBottom, card, cardFocused, accent;

    static final int TEXT_PRIMARY = 0xFFF2F4F7;
    static final int TEXT_SECONDARY = 0xFF9AA3B2;

    private Theme(String name, int bgTop, int bgBottom, int card, int cardFocused, int accent) {
        this.name = name;
        this.bgTop = bgTop;
        this.bgBottom = bgBottom;
        this.card = card;
        this.cardFocused = cardFocused;
        this.accent = accent;
    }

    static final Theme[] ALL = {
            new Theme("Medianoche", 0xFF0B0E13, 0xFF161D29, 0xFF1E2531, 0xFF2A3445, 0xFF7FB8FF),
            new Theme("Océano",     0xFF03141C, 0xFF0A2E3D, 0xFF0F3747, 0xFF19506A, 0xFF5ED6D0),
            new Theme("Desierto",   0xFF170F0A, 0xFF2E1F14, 0xFF392A1D, 0xFF4E3826, 0xFFF2B66D),
            new Theme("Atardecer",  0xFF170C1A, 0xFF371A2D, 0xFF412236, 0xFF5A2E4B, 0xFFFF8A80),
            new Theme("Grafito",    0xFF0E0E0F, 0xFF1C1C1E, 0xFF29292C, 0xFF3A3A3E, 0xFFE6E6E6),
    };

    static Theme current(Context c) {
        int i = new Config(c).themeIndex();
        return ALL[Math.max(0, Math.min(ALL.length - 1, i))];
    }

    Drawable background() {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{bgTop, bgBottom});
    }

    /** Fondo de tarjeta: normal y con borde del color de acento al enfocar. */
    Drawable cardBackground(Context c) {
        float r = Ui.dp(c, 14);
        GradientDrawable focused = new GradientDrawable();
        focused.setCornerRadius(r);
        focused.setColor(cardFocused);
        focused.setStroke(Ui.dp(c, 3), accent);

        GradientDrawable normal = new GradientDrawable();
        normal.setCornerRadius(r);
        normal.setColor(card);

        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, focused);
        s.addState(new int[]{}, normal);
        return s;
    }
}
