package net.carbonwalls.lugekey;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** One logical key. Everything about a key is editable (CleverKeys-style). */
public final class KeyDef {

    public static final int CONTENT     = 0;   // commits `output` (letter, digit, word, emoji, macro)
    public static final int SPACE       = 1;
    public static final int BACKSPACE   = 2;
    public static final int ENTER       = 3;
    public static final int SHIFT       = 4;
    public static final int SYMBOLS     = 5;   // toggle letters <-> symbols page
    public static final int SWITCH_IME  = 6;
    public static final int TAB         = 7;
    public static final int DELETE_WORD = 8;
    public static final int UNDO        = 9;
    public static final int CURSOR_LEFT = 10;
    public static final int CURSOR_RIGHT= 11;
    public static final int HOME        = 12;
    public static final int END         = 13;
    public static final int CLIPBOARD   = 14;  // opens clipboard panel
    public static final int EMOJI       = 15;  // toggle emoji page
    public static final int SETTINGS    = 16;  // opens the app settings

    public static final String DIR_UP    = "up";
    public static final String DIR_DOWN  = "down";
    public static final String DIR_LEFT  = "left";
    public static final String DIR_RIGHT = "right";
    public static final String DIR_NONE  = "none";

    public int id = -1;
    public int kind = CONTENT;
    public String label = "";
    public String output = "";
    public List<String> subkeys = new ArrayList<>(); // up to 8 items (chars, emoji, or macros)
    public float width = 1f;
    public boolean enabled = true;
    public String popupDir = DIR_UP; // swipe direction that opens the subkey popover

    public KeyDef() {}

    public KeyDef(int kind, String label, String output, List<String> subkeys, float width) {
        this.kind = kind;
        this.label = label;
        this.output = output;
        if (subkeys != null) this.subkeys = subkeys;
        this.width = width;
    }

    public boolean hasSubkeys() {
        if (subkeys == null) return false;
        for (String s : subkeys) if (s != null && !s.isEmpty()) return true;
        return false;
    }

    public String firstSubkey() {
        if (subkeys == null) return null;
        for (String s : subkeys) if (s != null && !s.isEmpty()) return s;
        return null;
    }

    public boolean isSpecial() {
        return kind != CONTENT && kind != SPACE && kind != TAB;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("kind", kind);
            o.put("label", label);
            o.put("output", output);
            JSONArray arr = new JSONArray();
            if (subkeys != null) for (String s : subkeys) arr.put(s == null ? "" : s);
            o.put("subkeys", arr);
            o.put("width", (double) width);
            o.put("enabled", enabled);
            o.put("popupDir", popupDir);
        } catch (Exception ignored) {}
        return o;
    }

    public static KeyDef fromJson(JSONObject o) {
        KeyDef k = new KeyDef();
        k.id = o.optInt("id", -1);
        k.kind = o.optInt("kind", CONTENT);
        k.label = o.optString("label", "");
        k.output = o.optString("output", "");
        k.subkeys = new ArrayList<>();
        JSONArray arr = o.optJSONArray("subkeys");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) k.subkeys.add(arr.optString(i, ""));
        }
        k.width = (float) o.optDouble("width", 1.0);
        k.enabled = o.optBoolean("enabled", true);
        k.popupDir = o.optString("popupDir", DIR_UP);
        if (k.id < 0) k.id = k.label.hashCode();
        return k;
    }
}
