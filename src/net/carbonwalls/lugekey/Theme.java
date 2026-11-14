package net.carbonwalls.lugekey;

import android.graphics.Color;

/** Curated FUTO-style palettes. Each palette fully describes the keyboard look. */
public final class Theme {

    public static final class Palette {
        public final String name;
        public final boolean dark;
        public final int bg;        // behind the keys
        public final int key;       // normal key fill
        public final int keyFunc;   // function key fill (shift/backspace/toolbar)
        public final int keyBorder; // hairline border
        public final int pressed;   // pressed fill
        public final int text;      // primary label
        public final int textFunc;  // function label
        public final int hint;      // small symbol-hint text
        public final int accent;    // tint (shift-on, popover, selected)

        Palette(String name, boolean dark, int bg, int key, int keyFunc, int keyBorder,
                int pressed, int text, int textFunc, int hint, int accent) {
            this.name = name; this.dark = dark;
            this.bg = bg; this.key = key; this.keyFunc = keyFunc; this.keyBorder = keyBorder;
            this.pressed = pressed; this.text = text; this.textFunc = textFunc;
            this.hint = hint; this.accent = accent;
        }

        public int funcPressed() {
            return blend(keyFunc, accent, 0.55f);
        }
    }

    public static final Palette[] PALETTES = {
            new Palette("Light", false,
                    0xFFF0F1F5, 0xFFFFFFFF, 0xFFE4E6ED, 0xFFD6D9E2, 0xFFDDE0EA,
                    0xFF1B1C22, 0xFF5A5D68, 0xFF9AA0AD, 0xFF6C63FF),
            new Palette("Ayu", false,
                    0xFFF7F3EC, 0xFFFFFFFF, 0xFFECE6DA, 0xFFE0D9C9, 0xFFE9E2D3,
                    0xFF3A342A, 0xFF7A715F, 0xFFB3A98F, 0xFFFF8A4C),
            new Palette("Ocean", false,
                    0xFFEAF2FA, 0xFFFFFFFF, 0xFFD9E6F2, 0xFFCDDCEB, 0xFFDFEAF6,
                    0xFF20303F, 0xFF4E6B82, 0xFF8AA4BA, 0xFF1F8FFF),
            new Palette("Coal", true,
                    0xFF17181C, 0xFF232429, 0xFF1D1E23, 0xFF2E3037, 0xFF34363E,
                    0xFFE9EAEE, 0xFF9AA0A8, 0xFF62666E, 0xFF8A7BFF),
            new Palette("Navy", true,
                    0xFF111925, 0xFF1C2835, 0xFF172230, 0xFF2A3A4C, 0xFF31455A,
                    0xFFE9EFF5, 0xFF9FB2C5, 0xFF5F7488, 0xFF4C9AFF),
            new Palette("Rust", true,
                    0xFF1B1510, 0xFF2B2218, 0xFF241D15, 0xFF3A2F22, 0xFF463827,
                    0xFFF0E5DA, 0xFFBDA892, 0xFF7E6D59, 0xFFFF8A4C),
            new Palette("Midnight", true,
                    0xFF0D0F13, 0xFF191C22, 0xFF14171C, 0xFF262B34, 0xFF2D3340,
                    0xFFDDE3EA, 0xFF8A94A3, 0xFF525A66, 0xFF35E0A1),
            new Palette("Rose", true,
                    0xFF1C1520, 0xFF2B2030, 0xFF241A28, 0xFF3B2C42, 0xFF493351,
                    0xFFF2E4F5, 0xFFC09FC9, 0xFF8A6A92, 0xFFFF5F8F),
    };

    private Theme() {}

    /** Load current palette from settings (clamped index). */
    public static Palette current(Prefs prefs) {
        int i = prefs.theme();
        if (i < 0 || i >= PALETTES.length) i = 0;
        return PALETTES[i];
    }

    public static int blend(int a, int b, float t) {
        int ar = Color.red(a), ag = Color.green(a), ab = Color.blue(a);
        int br = Color.red(b), bg = Color.green(b), bb = Color.blue(b);
        return Color.rgb((int) (ar + (br - ar) * t), (int) (ag + (bg - ag) * t), (int) (ab + (bb - ab) * t));
    }
}
