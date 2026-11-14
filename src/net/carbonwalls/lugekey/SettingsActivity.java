package net.carbonwalls.lugekey;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Settings + keyboard layout editor. Fully dependency-free, built in code. */
public class SettingsActivity extends Activity {

    private Prefs prefs;
    private final List<List<KeyDef>> alpha = new ArrayList<>();
    private final List<List<KeyDef>> symbols = new ArrayList<>();

    private LinearLayout editorHost;
    private LinearLayout accentHost;
    private LinearLayout dirHost;

    private static final String[] DIRS = {KeyDef.DIR_UP, KeyDef.DIR_DOWN, KeyDef.DIR_LEFT, KeyDef.DIR_RIGHT};
    private static final String[] KIND_NAMES = {
            "Letter / text", "Space", "Backspace", "Enter",
            "Shift", "Symbols toggle", "Switch IME", "Tab"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        prefs.buildLayoutIfMissing();
        alpha.clear();
        alpha.addAll(LayoutStore.load(this, true));
        symbols.clear();
        symbols.addAll(LayoutStore.load(this, false));
        setContentView(buildRoot());
    }

    /* ---------------- UI construction ---------------- */

    private View buildRoot() {
        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(false);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(32));
        sc.addView(root, new ViewGroup.LayoutParams(MATCH, MATCH));

        // header
        LinearLayout head = row(LinearLayout.VERTICAL);
        TextView t1 = label("LugeKey", 30, true, 0xFF11111A);
        TextView t2 = label("Minimal keyboard · swipe a key to reveal its extra characters", 13, false, 0xFF8A8A99);
        head.addView(t1);
        head.addView(t2, margin(0, dp(2), 0, 0));
        root.addView(head, margin(0, 0, 0, dp(8)));

        // quick typing test field — the keyboard is set as default, so tapping this shows LugeKey
        EditText test = new EditText(this);
        test.setHint("Tap here and type — swipe up on a letter for its extra characters");
        test.setSingleLine(false);
        test.setMinLines(2);
        test.setTextSize(16);
        test.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable tg = new GradientDrawable();
        tg.setColor(0xFFF3F4F9);
        tg.setCornerRadius(dp(10));
        tg.setStroke(dp(1), 0xFFE4E6EE);
        test.setBackground(tg);
        root.addView(test, margin(0, 0, 0, dp(2)));
        voidLine(root);

        buildAppearance(root);
        buildBehavior(root);

        // layout editors
        addSection(root, "Keyboard layout");
        editorHost = new LinearLayout(this);
        editorHost.setOrientation(LinearLayout.VERTICAL);
        root.addView(editorHost);
        renderEditors();

        // reset
        LinearLayout resetRow = row(LinearLayout.HORIZONTAL, Gravity.CENTER_HORIZONTAL);
        resetRow.addView(button("Reset letters to default", v -> {
            LayoutStore.reset(this, true);
            alpha.clear(); alpha.addAll(LayoutStore.load(this, true));
            renderEditors();
        }), margin(0, 0, dp(8), 0));
        resetRow.addView(button("Reset symbols to default", v -> {
            LayoutStore.reset(this, false);
            symbols.clear(); symbols.addAll(LayoutStore.load(this, false));
            renderEditors();
        }));
        root.addView(resetRow, margin(0, dp(12), 0, 0));

        TextView foot = label("To use: Settings → System → Languages & input → Keyboards → LugeKey.\n"
                + "The popup opens instantly when you swipe a key up (configurable per key).", 12, false, 0xFF9A9AA8);
        root.addView(foot, margin(0, dp(20), 0, 0));

        return sc;
    }

    private void buildAppearance(LinearLayout root) {
        addSection(root, "Appearance");

        Switch dark = new Switch(this);
        dark.setChecked(prefs.dark());
        dark.setOnCheckedChangeListener((b, v) -> prefs.setDark(v));
        root.addView(toggleRow("Dark keyboard", "Follows your taste (defaults off)", dark), margin(0, dp(2), 0, 0));

        accentHost = new LinearLayout(this);
        accentHost.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < Theme.ACCENTS.length; i++) {
            final int idx = i;
            View dot = accentDot(Theme.ACCENTS[i], i == prefs.accent());
            dot.setOnClickListener(v -> { prefs.setAccent(idx); renderAccent(); });
            accentHost.addView(dot, accentDotLP(i));
        }
        root.addView(accentHost, margin(0, dp(10), 0, 0));
        root.addView(label("Accent colour", 13, false, 0xFF8A8A99), margin(0, dp(2), 0, 0));

        RootedSeek height = new RootedSeek(this);
        height.configure("Keyboard height", 36, 64, prefs.heightDp(),
                v -> prefs.setHeightDp(v));
        root.addView(height, margin(0, dp(10), 0, 0));

        RootedSeek font = new RootedSeek(this);
        font.configure("Key text size", 80, 140, prefs.fontPercent(),
                v -> prefs.setFontPercent(v));
        root.addView(font, margin(0, dp(10), 0, 0));

        Switch numberRow = new Switch(this);
        numberRow.setChecked(prefs.numberRow());
        numberRow.setOnCheckedChangeListener((b, v) -> prefs.setNumberRow(v));
        root.addView(toggleRow("Top number row", "Digits 1–0 above the letters", numberRow), margin(0, dp(2), 0, 0));

        voidLine(root);
    }

    private void buildBehavior(LinearLayout root) {
        addSection(root, "Popup & behaviour");

        dirHost = new LinearLayout(this);
        dirHost.setOrientation(LinearLayout.HORIZONTAL);
        for (final String d : DIRS) {
            Button b = new Button(this);
            b.setText(cap(d));
            b.setAllCaps(false);
            b.setTextSize(13);
            b.setMinWidth(dp(56));
            b.setOnClickListener(v -> { prefs.setPopupDir(d); renderDir(); });
            dirHost.addView(b, margin(0, 0, dp(6), 0));
        }
        root.addView(dirHost, margin(0, dp(4), 0, 0));
        root.addView(label("Default swipe direction for popups (each key can override)", 12, false, 0xFF9A9AA8),
                margin(0, 0, 0, dp(6)));
        renderDir();

        Switch lp = new Switch(this);
        lp.setChecked(prefs.longPressPopup());
        lp.setOnCheckedChangeListener((b, v) -> prefs.setLongPressPopup(v));
        root.addView(toggleRow("Allow long-press popup too", "Optional fallback; off by default", lp), margin(0, dp(2), 0, 0));

        Switch ac = new Switch(this);
        ac.setChecked(prefs.autoCap());
        ac.setOnCheckedChangeListener((b, v) -> prefs.setAutoCap(v));
        root.addView(toggleRow("Auto-capitalise", "After . ? !", ac), margin(0, dp(2), 0, 0));

        Switch so = new Switch(this);
        so.setChecked(prefs.sound());
        so.setOnCheckedChangeListener((b, v) -> prefs.setSound(v));
        root.addView(toggleRow("Key click sound", null, so), margin(0, dp(2), 0, 0));

        Switch hh = new Switch(this);
        hh.setChecked(prefs.haptic());
        hh.setOnCheckedChangeListener((b, v) -> prefs.setHaptic(v));
        root.addView(toggleRow("Haptic feedback", null, hh), margin(0, dp(2), 0, 0));

        voidLine(root);
    }

    /* ---------------- layout editor ---------------- */

    private void renderEditors() {
        editorHost.removeAllViews();
        editorHost.addView(layerEditor(true));
        editorHost.addView(label(" ", 6, false, 0x00000000), margin(0, dp(8), 0, 0));
        editorHost.addView(layerEditor(false));
    }

    private LinearLayout layerEditor(final boolean letters) {
        final List<List<KeyDef>> rows = letters ? alpha : symbols;
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);

        TextView cap = label(letters ? "Letters layer  (tap a key to edit)" : "Symbols layer  (tap a key to edit)",
                14, true, 0xFF3A3A46);
        col.addView(cap, margin(0, dp(4), 0, dp(6)));

        for (int r = 0; r < rows.size(); r++) {
            final int rowIdx = r;
            final List<KeyDef> row = rows.get(r);

            LinearLayout head = row(LinearLayout.HORIZONTAL, Gravity.CENTER_VERTICAL);
            TextView rn = label("Row " + (r + 1), 12, true, 0xFF8A8A99);
            Button addK = new Button(this);
            addK.setText("+ key");
            addK.setAllCaps(false); addK.setTextSize(12); addK.setMinWidth(0);
            addK.setOnClickListener(v -> addKeyDialog(letters, rowIdx));
            Button delR = new Button(this);
            delR.setText("\u00D7 row");
            delR.setAllCaps(false); delR.setTextSize(12); delR.setMinWidth(0);
            delR.setTextColor(0xFFC4473D);
            delR.setOnClickListener(v -> {
                rows.remove(rowIdx);
                persist(letters);
                renderEditors();
            });
            head.addView(rn, margin(0, 0, dp(10), 0));
            head.addView(addK);
            head.addView(delR, margin(dp(6), 0, 0, 0));
            col.addView(head, margin(0, dp(6), 0, dp(2)));

            HorizontalScrollView hsc = new HorizontalScrollView(this);
            hsc.setHorizontalScrollBarEnabled(false);
            LinearLayout chips = new LinearLayout(this);
            chips.setOrientation(LinearLayout.HORIZONTAL);
            chips.setPadding(0, 0, dp(4), 0);
            for (int c = 0; c < row.size(); c++) {
                final int colIdx = c;
                chips.addView(keyChip(row.get(c), v -> editKeyDialog(letters, rowIdx, colIdx)), margin(0, 0, dp(5), 0));
            }
            hsc.addView(chips, new ViewGroup.LayoutParams(MATCH, WRAP));
            col.addView(hsc, margin(0, 0, 0, dp(6)));
        }

        Button addRow = new Button(this);
        addRow.setText("+ add row");
        addRow.setAllCaps(false);
        addRow.setOnClickListener(v -> addKeyDialog(letters, rows.size()));
        col.addView(addRow, margin(0, dp(2), 0, 0));
        return col;
    }

    private View keyChip(final KeyDef k, View.OnClickListener click) {
        TextView t = new TextView(this);
        String glyph = glyphFor(k);
        t.setText(glyph);
        t.setTextSize(13);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(0xFF1E1E28);
        t.setPadding(dp(12), dp(6), dp(12), dp(6));
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFFEFF1F6);
        g.setCornerRadius(dp(9));
        t.setBackground(g);
        t.setOnClickListener(click);
        if (k.popupEnabled && k.popup != null && !k.popup.isEmpty()) {
            t.setText(glyph + "  \u2733");
        }
        return t;
    }

    private String glyphFor(KeyDef k) {
        switch (k.kind) {
            case KeyDef.KIND_SHIFT: return "\u21E7";
            case KeyDef.KIND_BACKSPACE: return "\u232B";
            case KeyDef.KIND_ENTER: return "\u23CE";
            case KeyDef.KIND_SPACE: return "space";
            case KeyDef.KIND_SYMBOLS: return k.label;
            default: return k.label;
        }
    }

    /* ---------------- key dialogs ---------------- */

    private void editKeyDialog(final boolean letters, final int row, final int col) {
        List<List<KeyDef>> rows = letters ? alpha : symbols;
        final KeyDef k = rows.get(row).get(col);
        LinearLayout v = form();
        final FieldBox out = field("Output (typed on tap):", k.output == null ? "" : k.output);
        final FieldBox popup = field("Popup chars (swipe shows these):", k.popup == null ? "" : k.popup);
        final Switch en = sw("Popup enabled", k.popupEnabled);
        final Spinner dir = spinnerDir(k.popupDir);
        final Spinner kind = spinnerKind(k.kind);
        v.addView(out);
        v.addView(popup, margin(0, dp(8), 0, 0));
        v.addView(en, margin(0, dp(8), 0, dp(8)));
        v.addView(dir, margin(0, 0, 0, dp(8)));
        v.addView(kind);

        new AlertDialog.Builder(this)
                .setTitle("Edit: " + k.label)
                .setView(v)
                .setPositiveButton("Save", (d, w) -> {
                    k.output = out.edit.getText().toString();
                    k.popup = normalizePopup(popup.edit.getText().toString());
                    k.label = k.output.isEmpty() ? k.label : k.output;
                    k.popupEnabled = en.isChecked();
                    k.popupDir = (String) dir.getSelectedItem();
                    k.kind = spinnerKindValue(kind.getSelectedItemPosition());
                    persist(letters);
                    renderEditors();
                })
                .setNeutralButton("Delete", (d, w) -> {
                    rows.get(row).remove(col);
                    if (rows.get(row).isEmpty()) rows.remove(row);
                    persist(letters);
                    renderEditors();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addKeyDialog(final boolean letters, final int rowIdx) {
        LinearLayout v = form();
        final FieldBox out = field("Output (typed on tap):", "");
        final FieldBox popup = field("Popup chars (swipe shows these):", "");
        final Spinner dir = spinnerDir(prefs.popupDir());
        final Spinner kind = spinnerKind(KeyDef.KIND_CHAR);
        v.addView(out);
        v.addView(popup, margin(0, dp(8), 0, 0));
        v.addView(dir, margin(0, dp(8), 0, dp(8)));
        v.addView(kind);

        new AlertDialog.Builder(this)
                .setTitle("Add key")
                .setView(v)
                .setPositiveButton("Add", (d, w) -> {
                    KeyDef k = new KeyDef();
                    k.kind = spinnerKindValue(kind.getSelectedItemPosition());
                    k.output = out.edit.getText().toString();
                    k.label = k.output.isEmpty() ? "?" : k.output;
                    k.popup = normalizePopup(popup.edit.getText().toString());
                    k.popupDir = (String) dir.getSelectedItem();
                    k.popupEnabled = !k.popup.isEmpty();
                    k.width = 1f;
                    List<List<KeyDef>> rows = letters ? alpha : symbols;
                    if (rowIdx >= rows.size()) {
                        List<KeyDef> nr = new ArrayList<>();
                        nr.add(k);
                        rows.add(nr);
                    } else {
                        rows.get(rowIdx).add(k);
                    }
                    persist(letters);
                    renderEditors();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String normalizePopup(String raw) {
        if (raw == null) return "";
        StringBuilder sb = new StringBuilder();
        String[] toks = raw.trim().split("[\\s,]+");
        for (String t : toks) {
            if (t.isEmpty()) continue;
            if (t.codePointCount(0, t.length()) == 1) {
                sb.appendCodePoint(t.codePointAt(0));
            } else {
                int n = t.codePointCount(0, t.length());
                for (int i = 0; i < n; i++) sb.appendCodePoint(t.codePointAt(t.offsetByCodePoints(0, i)));
            }
        }
        return sb.toString();
    }

    private void persist(boolean letters) {
        LayoutStore.save(this, letters, letters ? alpha : symbols);
    }

    /* ---------------- small builders ---------------- */

    private LinearLayout form() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(dp(22), dp(14), dp(22), dp(4));
        return v;
    }

    private FieldBox field(String title, String value) {
        return new FieldBox(this, title, value);
    }

    private static class FieldBox extends LinearLayout {
        final EditText edit;
        FieldBox(Context c, String title, String value) {
            super(c);
            setOrientation(VERTICAL);
            TextView cap = new TextView(c);
            cap.setText(title);
            cap.setTextSize(12);
            cap.setTextColor(0xFF8A8A99);
            cap.setPadding(0, px(c, 8), 0, px(c, 2));
            edit = new EditText(c);
            edit.setText(value == null ? "" : value);
            edit.setSingleLine(true);
            addView(cap);
            addView(edit);
        }
        private static int px(Context c, int v) {
            return Math.round(v * c.getResources().getDisplayMetrics().density);
        }
    }

    private Switch sw(String title, boolean value) {
        Switch s = new Switch(this);
        s.setText(title);
        s.setTextSize(14);
        s.setChecked(value);
        return s;
    }

    private Spinner spinnerDir(String cur) {
        Spinner sp = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{KeyDef.DIR_UP, KeyDef.DIR_DOWN, KeyDef.DIR_LEFT, KeyDef.DIR_RIGHT, KeyDef.DIR_NONE});
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        sp.setSelection(indexOf(DIRS, cur, 0));
        return sp;
    }

    private Spinner spinnerKind(int kind) {
        Spinner sp = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, KIND_NAMES);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        sp.setSelection(kindToIndex(kind));
        return sp;
    }

    private int kindToIndex(int kind) {
        switch (kind) {
            case KeyDef.KIND_SPACE: return 1;
            case KeyDef.KIND_BACKSPACE: return 2;
            case KeyDef.KIND_ENTER: return 3;
            case KeyDef.KIND_SHIFT: return 4;
            case KeyDef.KIND_SYMBOLS: return 5;
            case KeyDef.KIND_SWITCH: return 6;
            case KeyDef.KIND_TAB: return 7;
            default: return 0;
        }
    }

    private int spinnerKindValue(int idx) {
        switch (idx) {
            case 1: return KeyDef.KIND_SPACE;
            case 2: return KeyDef.KIND_BACKSPACE;
            case 3: return KeyDef.KIND_ENTER;
            case 4: return KeyDef.KIND_SHIFT;
            case 5: return KeyDef.KIND_SYMBOLS;
            case 6: return KeyDef.KIND_SWITCH;
            case 7: return KeyDef.KIND_TAB;
            default: return KeyDef.KIND_CHAR;
        }
    }

    private int indexOf(String[] arr, String v, int def) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(v)) return i;
        return def;
    }

    private void renderAccent() {
        if (accentHost == null) return;
        accentHost.removeAllViews();
        for (int i = 0; i < Theme.ACCENTS.length; i++) {
            final int idx = i;
            View dot = accentDot(Theme.ACCENTS[i], i == prefs.accent());
            dot.setOnClickListener(v -> { prefs.setAccent(idx); renderAccent(); });
            accentHost.addView(dot, accentDotLP(i));
        }
    }

    private View accentDot(int color, boolean selected) {
        View v = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        if (selected) {
            g.setStroke(dp(3), 0xFF11111A);
        }
        v.setBackground(g);
        return v;
    }

    private LinearLayout.LayoutParams accentDotLP(int i) {
        return margin(i == 0 ? 0 : dp(12), 0, 0, dp(4));
    }

    private void renderDir() {
        if (dirHost == null) return;
        for (int i = 0; i < dirHost.getChildCount(); i++) {
            Button b = (Button) dirHost.getChildAt(i);
            boolean sel = prefs.popupDir().equals(DIRS[i]);
            b.setTextColor(sel ? Color.WHITE : 0xFF3A3A46);
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(10));
            g.setColor(sel ? 0xFF3D7BFF : 0xFFE9EBF1);
            b.setBackground(g);
        }
    }

    private LinearLayout toggleRow(String title, String sub, Switch toggle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(0, dp(6), 0, dp(6));
        TextView t = label(title, 15, false, 0xFF1E1E28);
        t.setPadding(0, 0, dp(8), 0);
        texts.addView(t);
        if (sub != null) {
            TextView s = label(sub, 12, false, 0xFF9A9AA8);
            texts.addView(s);
        }
        row.addView(texts, margin(0, 0, dp(8), 0));
        toggle.setGravity(Gravity.END);
        row.addView(toggle, margin(0, 0, dp(0), 0));
        return row;
    }

    private void addSection(LinearLayout parent, String title) {
        TextView t = label(title.toUpperCase(), 11, true, 0xFF9A9AA8);
        parent.addView(t, margin(0, dp(4), 0, dp(2)));
    }

    private void voidLine(LinearLayout parent) {
        View v = new View(this);
        v.setBackgroundColor(0xFFE4E6EE);
        parent.addView(v, new LinearLayout.LayoutParams(MATCH, dp(1), 1f));
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
        lp.setMargins(0, dp(12), 0, dp(10));
    }

    private TextView label(String s, float size, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        t.setTextColor(color);
        t.setLineSpacing(dp(1), 1f);
        return t;
    }

    private LinearLayout row(int orientation, int gravity) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(orientation);
        l.setGravity(gravity);
        return l;
    }

    private LinearLayout row(int orientation) {
        return row(orientation, Gravity.LEFT);
    }

    private static String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private Button button(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        return b;
    }

    private LinearLayout.LayoutParams margin(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
        lp.setMargins(l, t, r, b);
        return lp;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    /* Rooted seek slider with value label */
    private static class RootedSeek extends LinearLayout {
        private final TextView valueT;
        private int minV;

        RootedSeek(Context c) {
            super(c);
            setOrientation(VERTICAL);
            valueT = new TextView(c);
        }

        void configure(String title, int min, int max, int cur, final OnChange cb) {
            TextView cap = new TextView(getContext());
            cap.setText(title);
            cap.setTextSize(15);
            cap.setTextColor(0xFF1E1E28);
            valueT.setTextSize(12);
            valueT.setTextColor(0xFF9A9AA8);
            SeekBar sb = new SeekBar(getContext());
            sb.setMax(max - min);
            sb.setProgress(Math.max(0, Math.min(max - min, cur - min)));
            minV = min;
            valueT.setText(String.valueOf(cur));
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                    valueT.setText(String.valueOf(minV + p));
                    if (fromUser) cb.changed(minV + p);
                }
                @Override public void onStartTrackingTouch(SeekBar s) {}
                @Override public void onStopTrackingTouch(SeekBar s) {}
            });
            addView(cap);
            addView(valueT);
            addView(sb);
        }

        interface OnChange {
            void changed(int v);
        }
    }
}
