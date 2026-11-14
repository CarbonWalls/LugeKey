package net.carbonwalls.lugekey;

import org.json.JSONObject;

/** One logical key in a layout row. Everything about a key is editable. */
public final class KeyDef {

    public static final int KIND_CHAR      = 0;  // commits its output text
    public static final int KIND_SPACE     = 1;
    public static final int KIND_BACKSPACE = 2;
    public static final int KIND_ENTER     = 3;
    public static final int KIND_SHIFT     = 4;
    public static final int KIND_SYMBOLS   = 5;  // toggles alpha<->symbols layer
    public static final int KIND_SWITCH    = 6;  // switch to next IME
    public static final int KIND_TAB       = 7;

    public static final String DIR_UP    = "up";
    public static final String DIR_DOWN  = "down";
    public static final String DIR_LEFT  = "left";
    public static final String DIR_RIGHT = "right";
    public static final String DIR_NONE  = "none";

    public int id = -1;
    public int kind = KIND_CHAR;
    public String label = "";
    public String output = "";      // text committed on a plain tap
    public String popup = "";       // sequence of extra characters shown in the swipe popup
    public float width = 1f;        // relative width within its row
    public boolean popupEnabled = true;
    public String popupDir = DIR_UP; // swipe direction that opens the popup

    public KeyDef() {}

    public KeyDef(int kind, String label, String output, String popup, float width) {
        this.kind = kind;
        this.label = label;
        this.output = output;
        this.popup = popup;
        this.width = width;
    }

    public boolean isSpecial() {
        return kind != KIND_CHAR && kind != KIND_SPACE && kind != KIND_TAB;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("kind", kind);
            o.put("label", label);
            o.put("output", output);
            o.put("popup", popup);
            o.put("width", (double) width);
            o.put("popupEnabled", popupEnabled);
            o.put("popupDir", popupDir);
        } catch (Exception ignored) {}
        return o;
    }

    public static KeyDef fromJson(JSONObject o) {
        KeyDef k = new KeyDef();
        k.id = o.optInt("id", -1);
        k.kind = o.optInt("kind", KIND_CHAR);
        k.label = o.optString("label", "");
        k.output = o.optString("output", "");
        k.popup = o.optString("popup", "");
        k.width = (float) o.optDouble("width", 1.0);
        k.popupEnabled = o.optBoolean("popupEnabled", true);
        k.popupDir = o.optString("popupDir", DIR_UP);
        if (k.id < 0) k.id = k.label.hashCode();
        return k;
    }
}
