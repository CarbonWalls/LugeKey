package net.carbonwalls.lugekey;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Serialises/deserialises keyboard layouts as JSON in prefs, plus the default layouts. */
public final class LayoutStore {

    private LayoutStore() {}

    /* ---------------- defaults ---------------- */

    private static KeyDef ch(String label, String popup) {
        KeyDef k = new KeyDef(KeyDef.KIND_CHAR, label, label, popup == null ? "" : popup, 1f);
        k.id = label.hashCode();
        return k;
    }

    private static KeyDef special(int kind, String label, String output, float width) {
        KeyDef k = new KeyDef(kind, label, output, "", width);
        k.id = label.hashCode() * 31 + kind;
        return k;
    }

    public static List<List<KeyDef>> defaultAlpha() {
        List<List<KeyDef>> rows = new ArrayList<>();

        rows.add(list(
                ch("q", "ǫ"),
                ch("w", "w"),
                ch("e", "éèêë€"),
                ch("r", "ŕř"),
                ch("t", "ťþ"),
                ch("y", "ýÿ"),
                ch("u", "úùûüµ"),
                ch("i", "íìîï"),
                ch("o", "óòôöõø"),
                ch("p", "þ¶")));

        rows.add(list(
                ch("a", "áàâäãåæ@"),
                ch("s", "šß§$"),
                ch("d", "ðđ"),
                ch("f", "ƒ"),
                ch("g", "ğ"),
                ch("h", "ĥ"),
                ch("j", "ĵ"),
                ch("k", "ķ"),
                ch("l", "ł£")));

        KeyDef shift = special(KeyDef.KIND_SHIFT, "shift", "", 1.5f);
        shift.popupDir = KeyDef.DIR_NONE;
        keyNoPopup(shift);
        KeyDef bs = special(KeyDef.KIND_BACKSPACE, "\u232B", "", 1.5f);
        bs.popupDir = KeyDef.DIR_NONE;
        keyNoPopup(bs);
        rows.add(list(
                shift,
                ch("z", "žź"),
                ch("x", "×"),
                ch("c", "ç¢"),
                ch("v", "v"),
                ch("b", "ß"),
                ch("n", "ñ"),
                ch("m", "µ"),
                bs));

        KeyDef sym = special(KeyDef.KIND_SYMBOLS, "?123", "?123", 1.4f);
        sym.popupDir = KeyDef.DIR_NONE;
        keyNoPopup(sym);
        KeyDef enter = special(KeyDef.KIND_ENTER, "\u23CE", "", 1.4f);
        enter.popupDir = KeyDef.DIR_NONE;
        keyNoPopup(enter);
        KeyDef comma = ch(",", "");
        comma.width = 1f;
        KeyDef period = ch(".", "");
        period.width = 1f;
        KeyDef space = special(KeyDef.KIND_SPACE, "space", " ", 5.2f);
        space.popupDir = KeyDef.DIR_NONE;
        keyNoPopup(space);
        rows.add(list(sym, comma, space, period, enter));

        return rows;
    }

    public static List<List<KeyDef>> defaultSymbols() {
        List<List<KeyDef>> rows = new ArrayList<>();

        rows.add(list(
                ch("1", ""), ch("2", ""), ch("3", ""), ch("4", ""), ch("5", ""),
                ch("6", ""), ch("7", ""), ch("8", ""), ch("9", ""), ch("0", "")));

        rows.add(list(
                ch("@", "«»"),
                ch("#", ""),
                ch("$", "€£¥¢"),
                ch("_", ""),
                ch("-", "–—"),
                ch("+", ""),
                ch("(", "["),
                ch(")", "]"),
                ch("/", "\\"),
                ch("*", "")));

        KeyDef shift = special(KeyDef.KIND_SHIFT, "shift", "", 1.5f);
        shift.popupDir = KeyDef.DIR_NONE;
        keyNoPopup(shift);
        KeyDef bs = special(KeyDef.KIND_BACKSPACE, "\u232B", "", 1.5f);
        bs.popupDir = KeyDef.DIR_NONE;
        keyNoPopup(bs);
        rows.add(list(
                shift,
                ch(":", ""), ch(";", ""), ch(",", ""), ch(".", ""),
                ch("?", ""), ch("!", ""), ch("'", ""), ch("\"", ""),
                bs));

        KeyDef abc = special(KeyDef.KIND_SYMBOLS, "ABC", "ABC", 1.4f);
        abc.popupDir = KeyDef.DIR_NONE;
        keyNoPopup(abc);
        KeyDef enter = special(KeyDef.KIND_ENTER, "\u23CE", "", 1.4f);
        enter.popupDir = KeyDef.DIR_NONE;
        keyNoPopup(enter);
        KeyDef comma = ch(",", "");
        comma.width = 1f;
        KeyDef period = ch(".", "");
        period.width = 1f;
        KeyDef space = special(KeyDef.KIND_SPACE, "space", " ", 5.2f);
        space.popupDir = KeyDef.DIR_NONE;
        keyNoPopup(space);
        rows.add(list(abc, comma, space, period, enter));

        return rows;
    }

    private static void keyNoPopup(KeyDef k) {
        k.popup = "";
        k.popupEnabled = false;
        k.popupDir = KeyDef.DIR_NONE;
    }

    private static List<KeyDef> list(KeyDef... ks) {
        List<KeyDef> l = new ArrayList<>();
        for (KeyDef k : ks) l.add(k);
        return l;
    }

    /* ---------------- serialisation ---------------- */

    public static String serialize(List<List<KeyDef>> rows) {
        try {
            JSONArray arr = new JSONArray();
            for (List<KeyDef> row : rows) {
                JSONArray r = new JSONArray();
                for (KeyDef k : row) r.put(k.toJson());
                arr.put(r);
            }
            return arr.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public static List<List<KeyDef>> deserialize(String json) {
        List<List<KeyDef>> rows = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONArray r = arr.getJSONArray(i);
                List<KeyDef> row = new ArrayList<>();
                for (int j = 0; j < r.length(); j++) row.add(KeyDef.fromJson(r.getJSONObject(j)));
                rows.add(row);
            }
        } catch (Exception ignored) {}
        return rows;
    }

    /* ---------------- prefs access (layers: true = letters, false = symbols) ---------------- */

    public static List<List<KeyDef>> load(Context c, boolean letters) {
        Prefs p = new Prefs(c);
        String json = p.raw().getString(letters ? Prefs.KEY_LAYOUT_ALPHA : Prefs.KEY_LAYOUT_SYMBOLS, null);
        if (json == null) {
            List<List<KeyDef>> d = letters ? defaultAlpha() : defaultSymbols();
            p.raw().edit().putString(letters ? Prefs.KEY_LAYOUT_ALPHA : Prefs.KEY_LAYOUT_SYMBOLS, serialize(d)).apply();
            return d;
        }
        List<List<KeyDef>> rows = deserialize(json);
        if (rows.isEmpty()) {
            List<List<KeyDef>> d = letters ? defaultAlpha() : defaultSymbols();
            p.raw().edit().putString(letters ? Prefs.KEY_LAYOUT_ALPHA : Prefs.KEY_LAYOUT_SYMBOLS, serialize(d)).apply();
            return d;
        }
        return rows;
    }

    public static void save(Context c, boolean letters, List<List<KeyDef>> rows) {
        new Prefs(c).raw().edit()
                .putString(letters ? Prefs.KEY_LAYOUT_ALPHA : Prefs.KEY_LAYOUT_SYMBOLS, serialize(rows)).apply();
    }

    public static void reset(Context c, boolean letters) {
        String json = serialize(letters ? defaultAlpha() : defaultSymbols());
        new Prefs(c).raw().edit()
                .putString(letters ? Prefs.KEY_LAYOUT_ALPHA : Prefs.KEY_LAYOUT_SYMBOLS, json).apply();
    }
}
