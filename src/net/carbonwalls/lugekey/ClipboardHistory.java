package net.carbonwalls.lugekey;

import android.content.Context;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/** Simple persistent clipboard history (most recent first). */
public final class ClipboardHistory {

    private static final int MAX = 60;

    private ClipboardHistory() {}

    public static List<String> load(Context c) {
        List<String> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(new Prefs(c).raw().getString(Prefs.KEY_CLIP_HISTORY, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                String s = arr.optString(i, "");
                if (!s.isEmpty()) list.add(s);
            }
        } catch (Exception ignored) {}
        return list;
    }

    /** Add an item (dedupe move-to-front), cap size, persist. */
    public static void add(Context c, String text) {
        if (text == null || text.trim().isEmpty()) return;
        String t = text.trim();
        if (t.length() > 5000) t = t.substring(0, 5000);
        List<String> list = new ArrayList<>();
        list.add(t);
        for (String old : load(c)) {
            if (!old.equals(t)) list.add(old);
            if (list.size() >= MAX) break;
        }
        persist(c, list);
    }

    public static void remove(Context c, int index) {
        List<String> list = load(c);
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            persist(c, list);
        }
    }

    public static void clear(Context c) {
        persist(c, new ArrayList<String>());
    }

    private static void persist(Context c, List<String> list) {
        JSONArray arr = new JSONArray();
        for (String s : list) arr.put(s);
        new Prefs(c).raw().edit().putString(Prefs.KEY_CLIP_HISTORY, arr.toString()).apply();
    }
}
