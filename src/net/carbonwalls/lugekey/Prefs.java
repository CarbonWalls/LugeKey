package net.carbonwalls.lugekey;

import android.content.Context;
import android.content.SharedPreferences;

/** App settings + user-defined keyboard layouts, all in one shared prefs file. */
public final class Prefs {

    private static final String FILE = "lugekey";

    public static final String KEY_LAYOUT_ALPHA   = "layout_alpha";
    public static final String KEY_LAYOUT_SYMBOLS = "layout_symbols";
    public static final String KEY_DARK    = "dark";
    public static final String KEY_ACCENT  = "accent";
    public static final String KEY_HEIGHT  = "height";    // dp
    public static final String KEY_FONT    = "font";      // percent 80..140
    public static final String KEY_SOUND   = "sound";
    public static final String KEY_HAPTIC  = "haptic";
    public static final String KEY_POPUP_DIR = "popup_dir";
    public static final String KEY_LONGPRESS = "longpress";
    public static final String KEY_AUTOCAP = "autocap";
    public static final String KEY_NUMBER_ROW = "number_row";

    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public SharedPreferences raw() { return sp; }

    public boolean dark() { return sp.getBoolean(KEY_DARK, false); }
    public void setDark(boolean v) { sp.edit().putBoolean(KEY_DARK, v).apply(); }

    public int accent() { return sp.getInt(KEY_ACCENT, 0); }
    public void setAccent(int v) { sp.edit().putInt(KEY_ACCENT, v).apply(); }

    public int heightDp() { return sp.getInt(KEY_HEIGHT, 48); }
    public void setHeightDp(int v) { sp.edit().putInt(KEY_HEIGHT, v).apply(); }

    public int fontPercent() { return sp.getInt(KEY_FONT, 100); }
    public void setFontPercent(int v) { sp.edit().putInt(KEY_FONT, v).apply(); }

    public boolean sound() { return sp.getBoolean(KEY_SOUND, true); }
    public void setSound(boolean v) { sp.edit().putBoolean(KEY_SOUND, v).apply(); }

    public boolean haptic() { return sp.getBoolean(KEY_HAPTIC, true); }
    public void setHaptic(boolean v) { sp.edit().putBoolean(KEY_HAPTIC, v).apply(); }

    public String popupDir() { return sp.getString(KEY_POPUP_DIR, KeyDef.DIR_UP); }
    public void setPopupDir(String v) { sp.edit().putString(KEY_POPUP_DIR, v).apply(); }

    public boolean longPressPopup() { return sp.getBoolean(KEY_LONGPRESS, false); }
    public void setLongPressPopup(boolean v) { sp.edit().putBoolean(KEY_LONGPRESS, v).apply(); }

    public boolean autoCap() { return sp.getBoolean(KEY_AUTOCAP, true); }
    public void setAutoCap(boolean v) { sp.edit().putBoolean(KEY_AUTOCAP, v).apply(); }

    public boolean numberRow() { return sp.getBoolean(KEY_NUMBER_ROW, false); }
    public void setNumberRow(boolean v) { sp.edit().putBoolean(KEY_NUMBER_ROW, v).apply(); }

    public void buildLayoutIfMissing() {
        if (!sp.contains(KEY_LAYOUT_ALPHA)) {
            sp.edit().putString(KEY_LAYOUT_ALPHA, LayoutStore.serialize(LayoutStore.defaultAlpha()))
                    .putString(KEY_LAYOUT_SYMBOLS, LayoutStore.serialize(LayoutStore.defaultSymbols())).apply();
        }
    }
}
