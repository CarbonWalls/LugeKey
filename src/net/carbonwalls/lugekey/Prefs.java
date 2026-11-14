package net.carbonwalls.lugekey;

import android.content.Context;
import android.content.SharedPreferences;

/** App settings + user-defined keyboard layouts, all in one shared prefs file. */
public final class Prefs {

    private static final String FILE = "lugekey";

    public static final String KEY_LAYOUT_ALPHA   = "layout_alpha";
    public static final String KEY_LAYOUT_SYMBOLS = "layout_symbols";
    public static final String KEY_CLIP_HISTORY   = "clip_history";
    public static final String KEY_THEME          = "theme";
    public static final String KEY_HEIGHT         = "height";     // dp
    public static final String KEY_FONT           = "font";       // percent 80..140
    public static final String KEY_SOUND          = "sound";
    public static final String KEY_HAPTIC         = "haptic";
    public static final String KEY_POPUP_DIR      = "popup_dir";
    public static final String KEY_LONGPRESS      = "longpress";
    public static final String KEY_AUTOCAP        = "autocap";
    public static final String KEY_NUMBER_ROW     = "number_row";
    public static final String KEY_ARROW_ROW      = "arrow_row";
    public static final String KEY_TOOLBAR        = "toolbar";
    public static final String KEY_HINTS          = "hints";
    public static final String KEY_POPUP_ON_PRESS = "popup_on_press";
    public static final String KEY_SAVE_TYPED     = "save_typed";

    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public SharedPreferences raw() { return sp; }

    public int theme() { return sp.getInt(KEY_THEME, 3); }
    public void setTheme(int v) { sp.edit().putInt(KEY_THEME, v).apply(); }

    public int heightDp() { return sp.getInt(KEY_HEIGHT, 50); }
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

    public boolean numberRow() { return sp.getBoolean(KEY_NUMBER_ROW, true); }
    public void setNumberRow(boolean v) { sp.edit().putBoolean(KEY_NUMBER_ROW, v).apply(); }

    public boolean arrowRow() { return sp.getBoolean(KEY_ARROW_ROW, false); }
    public void setArrowRow(boolean v) { sp.edit().putBoolean(KEY_ARROW_ROW, v).apply(); }

    public boolean toolbar() { return sp.getBoolean(KEY_TOOLBAR, true); }
    public void setToolbar(boolean v) { sp.edit().putBoolean(KEY_TOOLBAR, v).apply(); }

    public boolean hints() { return sp.getBoolean(KEY_HINTS, true); }
    public void setHints(boolean v) { sp.edit().putBoolean(KEY_HINTS, v).apply(); }

    public boolean popupOnPress() { return sp.getBoolean(KEY_POPUP_ON_PRESS, true); }
    public void setPopupOnPress(boolean v) { sp.edit().putBoolean(KEY_POPUP_ON_PRESS, v).apply(); }

    public boolean saveTyped() { return sp.getBoolean(KEY_SAVE_TYPED, true); }
    public void setSaveTyped(boolean v) { sp.edit().putBoolean(KEY_SAVE_TYPED, v).apply(); }

    public void buildLayoutIfMissing() {
        if (!sp.contains(KEY_LAYOUT_ALPHA)) {
            sp.edit()
              .putString(KEY_LAYOUT_ALPHA, LayoutStore.serialize(LayoutStore.defaultAlpha()))
              .putString(KEY_LAYOUT_SYMBOLS, LayoutStore.serialize(LayoutStore.defaultSymbols()))
              .apply();
        }
    }
}
