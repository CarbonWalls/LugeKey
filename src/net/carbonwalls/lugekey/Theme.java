package net.carbonwalls.lugekey;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;

/** Resolves the current colour palette from settings. Futo-style flat minimal. */
public final class Theme {

    public static final int[] ACCENTS = {
            0xFF3D7BFF, 0xFF0A84FF, 0xFF34C759, 0xFFAF52DE,
            0xFFFF9F0A, 0xFFFF3B30, 0xFF30B0C7, 0xFF8E8E93
    };

    public final boolean dark;
    public final int accent;
    public final int bg;
    public final int key;
    public final int keyFunc;
    public final int keyPressed;
    public final int text;
    public final int textFunc;
    public final int keyBorder;
    public final int scrim;

    private Theme(boolean dark, int accent,
                  int bg, int key, int keyFunc, int keyPressed,
                  int text, int textFunc, int keyBorder, int scrim) {
        this.dark = dark;
        this.accent = accent;
        this.bg = bg;
        this.key = key;
        this.keyFunc = keyFunc;
        this.keyPressed = keyPressed;
        this.text = text;
        this.textFunc = textFunc;
        this.keyBorder = keyBorder;
        this.scrim = scrim;
    }

    public static Theme load(Context c, Prefs prefs) {
        boolean dark = prefs.dark()
                || (c.getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                   == android.content.res.Configuration.UI_MODE_NIGHT_YES
                || darkSystem(c);
        int accent = ACCENTS[Math.max(0, Math.min(ACCENTS.length - 1, prefs.accent()))];
        if (dark) {
            return new Theme(true, accent,
                    0xFF121317,       // bg
                    0xFF26272E,       // key
                    0xFF1E1F26,       // function key
                    0xFF3A3B44,       // pressed
                    0xFFF2F3F7,       // text
                    0xFFC7C9D1,       // function text
                    0xFF3A3B44,       // border
                    0xB3000000);      // scrim
        } else {
            return new Theme(false, accent,
                    0xFFEDEEF2,       // bg
                    0xFFFFFFFF,       // key
                    0xFFE1E3EA,       // function key
                    0xFFD9DCE6,       // pressed
                    0xFF191A1F,       // text
                    0xFF4A4C55,       // function text
                    0xFFD9DCE6,       // border
                    0x66000000);      // scrim
        }
    }

    public Typeface bold() { return Typeface.DEFAULT_BOLD; }

    private static boolean darkSystem(Context c) {
        try {
            int mode = c.getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            return mode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        } catch (Exception e) {
            return false;
        }
    }

    public static int blend(int a, int b, float t) {
        int ar = Color.red(a), ag = Color.green(a), ab = Color.blue(a);
        int br = Color.red(b), bg = Color.green(b), bb = Color.blue(b);
        return Color.rgb((int) (ar + (br - ar) * t), (int) (ag + (bg - ag) * t), (int) (ab + (bb - ab) * t));
    }
}
