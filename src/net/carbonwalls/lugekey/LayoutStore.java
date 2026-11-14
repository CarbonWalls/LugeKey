package net.carbonwalls.lugekey;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Default layers + JSON persistence for user layouts, and the emoji page. */
public final class LayoutStore {

    private LayoutStore() {}

    /* ---------------- helpers ---------------- */

    private static KeyDef ct(String label, String output, List<String> sub, float w) {
        KeyDef k = new KeyDef(KeyDef.CONTENT, label, output == null ? label : output, sub, w);
        k.id = label.hashCode();
        return k;
    }

    private static KeyDef act(int kind, String label, float w) {
        KeyDef k = new KeyDef(kind, label, "", null, w);
        k.id = label.hashCode() + kind;
        return k;
    }

    private static List<String> sk(String... s) {
        return new ArrayList<>(Arrays.asList(s));
    }

    private static List<KeyDef> row(KeyDef... ks) {
        return new ArrayList<>(Arrays.asList(ks));
    }

    /* ---------------- default letters ---------------- */

    public static List<List<KeyDef>> defaultAlpha() {
        List<List<KeyDef>> rows = new ArrayList<>();

        rows.add(row(
                ct("q", "q", sk("1", "«"), 1f),
                ct("w", "w", sk("2", "@"), 1f),
                ct("e", "e", sk("3", "\u00E9\u00E8\u00EA\u00EB"), 1f),
                ct("r", "r", sk("4", "\u0159"), 1f),
                ct("t", "t", sk("5", "\u0165"), 1f),
                ct("y", "y", sk("6", "\u00FD\u00FF"), 1f),
                ct("u", "u", sk("7", "\u00FA\u00F9\u00FB\u00FC"), 1f),
                ct("i", "i", sk("8", "\u00ED\u00EC\u00EE\u00EF"), 1f),
                ct("o", "o", sk("9", "\u00F3\u00F2\u00F4\u00F5\u00F6"), 1f),
                ct("p", "p", sk("0", "\u00F0"), 1f)));

        rows.add(row(
                ct("a", "a", sk("@", "\u00E1\u00E0\u00E2\u00E4\u00E3\u00E5\u00E6"), 1f),
                ct("s", "s", sk("#", "\u0161\u00DF\u00A7"), 1f),
                ct("d", "d", sk("$", "\u0111\u00F0"), 1f),
                ct("f", "f", sk("%", "\u0192"), 1f),
                ct("g", "g", sk("&", "\u011F"), 1f),
                ct("h", "h", sk("-", "\u0125"), 1f),
                ct("j", "j", sk("+", "\u0135"), 1f),
                ct("k", "k", sk("(", "\u0137"), 1f),
                ct("l", "l", sk(")", "\u0142"), 1f)));

        KeyDef shift = act(KeyDef.SHIFT, "shift", 1.6f);
        shift.popupDir = KeyDef.DIR_NONE;
        KeyDef bs = act(KeyDef.BACKSPACE, "backspace", 1.6f);
        bs.popupDir = KeyDef.DIR_NONE;
        rows.add(row(
                shift,
                ct("z", "z", sk("[", "\u017E\u017A"), 1f),
                ct("x", "x", sk("]", "\u00D7"), 1f),
                ct("c", "c", sk("{", "\u00E7\u00A2"), 1f),
                ct("v", "v", sk("}", "v"), 1f),
                ct("b", "b", sk("\\", "\u00DF"), 1f),
                ct("n", "n", sk("~", "\u00F1"), 1f),
                ct("m", "m", sk("^", "\u00B5"), 1f),
                bs));

        KeyDef sym = act(KeyDef.SYMBOLS, "?123", 1.5f);
        sym.popupDir = KeyDef.DIR_NONE;
        KeyDef cls = act(KeyDef.CLIPBOARD, "clipboard", 1.1f);
        cls.popupDir = KeyDef.DIR_NONE;
        KeyDef comma = ct(",", ",", sk("!"), 1f);
        KeyDef space = act(KeyDef.SPACE, "space", 4.6f);
        space.popupDir = KeyDef.DIR_NONE;
        KeyDef dot = ct(".", ".", sk("?"), 1f);
        KeyDef enter = act(KeyDef.ENTER, "enter", 1.5f);
        enter.popupDir = KeyDef.DIR_NONE;
        rows.add(row(sym, cls, comma, space, dot, enter));

        return rows;
    }

    /* ---------------- default symbols ---------------- */

    public static List<List<KeyDef>> defaultSymbols() {
        List<List<KeyDef>> rows = new ArrayList<>();

        rows.add(row(
                ct("1", "1", sk("!"), 1f),
                ct("2", "2", sk("@"), 1f),
                ct("3", "3", sk("#"), 1f),
                ct("4", "4", sk("$"), 1f),
                ct("5", "5", sk("%"), 1f),
                ct("6", "6", sk("^"), 1f),
                ct("7", "7", sk("&"), 1f),
                ct("8", "8", sk("*"), 1f),
                ct("9", "9", sk("("), 1f),
                ct("0", "0", sk(")"), 1f)));

        rows.add(row(
                ct("\u20AC", "\u20AC", sk("$", "\u00A3", "\u00A5", "\u00A2"), 1f),
                ct("_", "_", sk("-", "\u2013", "\u2014"), 1f),
                ct(":", ":", sk(";"), 1f),
                ct("'", "'", sk("\u2018", "\u2019"), 1f),
                ct("\"", "\"", sk("\u201C", "\u201D"), 1f),
                ct("/", "/", sk("\\"), 1f),
                ct("?", "?", sk("\u00BF"), 1f),
                ct("!", "!", sk("\u00A1"), 1f),
                ct("<", "<", sk("\u2264", "\u00AB"), 1f),
                ct(">", ">", sk("\u2265", "\u00BB"), 1f)));

        KeyDef shift = act(KeyDef.SHIFT, "shift", 1.6f);
        shift.popupDir = KeyDef.DIR_NONE;
        KeyDef bs = act(KeyDef.BACKSPACE, "backspace", 1.6f);
        bs.popupDir = KeyDef.DIR_NONE;
        rows.add(row(
                shift,
                ct("[", "[", sk("{"), 1f),
                ct("]", "]", sk("}"), 1f),
                ct("{", "{", sk("[", "("), 1f),
                ct("}", "}", sk("]", ")"), 1f),
                ct("(", "(", sk("["), 1f),
                ct(")", ")", sk("]"), 1f),
                ct("*", "*", sk("\u00D7"), 1f),
                bs));

        KeyDef abc = act(KeyDef.SYMBOLS, "ABC", 1.5f);
        abc.popupDir = KeyDef.DIR_NONE;
        KeyDef comma = ct(",", ",", sk(";"), 1f);
        KeyDef space = act(KeyDef.SPACE, "space", 4.6f);
        space.popupDir = KeyDef.DIR_NONE;
        KeyDef dot = ct(".", ".", sk("!"), 1f);
        KeyDef enter = act(KeyDef.ENTER, "enter", 1.5f);
        enter.popupDir = KeyDef.DIR_NONE;
        rows.add(row(abc, comma, space, dot, enter));

        return rows;
    }

    /* ---------------- emoji page (fixed) ---------------- */

    public static List<List<KeyDef>> emojiRows() {
        String[][] groups = {
                {"\uD83D\uDE00","\uD83D\uDE03","\uD83D\uDE04","\uD83D\uDE01","\uD83D\uDE06","\uD83D\uDE09","\uD83D\uDE0A","\uD83D\uDE0D"},
                {"\uD83D\uDE17","\uD83D\uDE15","\uD83D\uDE22","\uD83D\uDE2D","\uD83D\uDE29","\uD83D\uDE12","\uD83D\uDE44","\uD83D\uDE37"},
                {"\u2764\uFE0F","\uD83D\uDC93","\uD83D\uDC96","\uD83D\uDC97","\uD83D\uDC99","\uD83D\uDC9B","\uD83D\uDDA4","\uD83E\uDE79"},
                {"\uD83D\uDC4D","\uD83D\uDC4E","\uD83D\uDC4F","\uD83D\uDCAF","\u270B","\uD83E\uDD1D","\uD83D\uDCAA","\uD83E\uDD1F"},
                {"\uD83D\uDE80","\u2708\uFE0F","\uD83D\uDE95","\uD83C\uDF89","\uD83C\uDF81","\uD83C\uDF82","\uD83C\uDF93","\u2B50"},
                {"\uD83D\uDE49","\uD83D\uDE48","\uD83D\uDE4A","\uD83D\uDD4A\uFE0F","\uD83D\uDE8E","\uD83C\uDF43","\uD83C\uDF38","\uD83C\uDF3B"},
        };
        List<List<KeyDef>> rows = new ArrayList<>();
        for (String[] g : groups) {
            List<KeyDef> r = new ArrayList<>();
            for (String e : g) r.add(ct(e, e, null, 1f));
            rows.add(r);
        }
        // bottom row: back + return to letters
        KeyDef back = act(KeyDef.BACKSPACE, "backspace", 2f);
        back.popupDir = KeyDef.DIR_NONE;
        KeyDef abc = act(KeyDef.SYMBOLS, "ABC", 2f);
        abc.popupDir = KeyDef.DIR_NONE;
        KeyDef space = act(KeyDef.SPACE, "space", 4f);
        space.popupDir = KeyDef.DIR_NONE;
        rows.add(row(back, space, abc));
        return rows;
    }

    /* ---------------- tools ---------------- */

    public static KeyDef digit(String d) {
        return ct(d, d, null, 1f);
    }

    public static KeyDef arrow(KeyDef arrowKey, int kind) {
        return act(kind, "", 1f);
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

    /* ---------------- prefs access ---------------- */

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
