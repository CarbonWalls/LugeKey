package net.carbonwalls.lugekey;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/** LugeKey settings: themes, keyboard behaviour, clipboard manager and the layout/key editor. */
public class SettingsActivity extends Activity {

    private Prefs prefs;
    private final List<List<KeyDef>> alpha = new ArrayList<>();
    private final List<List<KeyDef>> symbols = new ArrayList<>();

    private LinearLayout root;
    private LinearLayout themeHost;
    private LinearLayout dirHost;
    private LinearLayout editorHost;
    private LinearLayout clipHost;

    private static final String[] KIND_NAMES = {
            "Text / letter", "Space", "Backspace", "Enter", "Shift",
            "Symbols page", "Switch IME", "Tab", "Delete word", "Undo",
            "Cursor left", "Cursor right", "Home", "End", "Clipboard", "Emoji", "Open settings"};

    private static final int[] KIND_VALUES = {
            KeyDef.CONTENT, KeyDef.SPACE, KeyDef.BACKSPACE, KeyDef.ENTER, KeyDef.SHIFT,
            KeyDef.SYMBOLS, KeyDef.SWITCH_IME, KeyDef.TAB, KeyDef.DELETE_WORD, KeyDef.UNDO,
            KeyDef.CURSOR_LEFT, KeyDef.CURSOR_RIGHT, KeyDef.HOME, KeyDef.END, KeyDef.CLIPBOARD,
            KeyDef.EMOJI, KeyDef.SETTINGS};

    private static final String[] DIRS = {KeyDef.DIR_UP, KeyDef.DIR_DOWN, KeyDef.DIR_LEFT, KeyDef.DIR_RIGHT, KeyDef.DIR_NONE};

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

    private View buildRoot() {
        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(false);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(36));
        sc.addView(root, new ViewGroup.LayoutParams(MATCH, MATCH));

        root.addView(label("LugeKey", 30, true, 0xFF17181C));
        root.addView(label("FUTO-minimal keyboard with CleverKeys-style keys. Swipe a key to reveal its subkeys.",
                13, false, 0xFF8A8F99), margin(dp(0), dp(2), 0, 0));

        EditText test = new EditText(this);
        test.setHint("Tap here, then swipe up on a letter to reveal subkeys");
        test.setMinLines(2);
        test.setSingleLine(false);
        test.setTextSize(15);
        test.setPadding(dp(12), dp(10), dp(12), dp(10));
        test.setBackground(round(0xFFFFFFFF, dp(10)));
        root.addView(test, marginW(dp(0), dp(14), 0, 0));

        buildTheme();
        buildKeyboard();
        buildPopups();
        buildSound();

        addSection("Clipboard history");
        clipHost = new LinearLayout(this);
        clipHost.setOrientation(LinearLayout.VERTICAL);
        root.addView(cardWrap(clipHost), marginW(0, dp(4), 0, 0));
        renderClips();

        addSection("Keyboard layout");
        editorHost = new LinearLayout(this);
        editorHost.setOrientation(LinearLayout.VERTICAL);
        root.addView(editorHost, marginW(0, dp(4), 0, 0));
        renderEditors();

        LinearLayout resetRow = row(Gravity.CENTER_HORIZONTAL);
        resetRow.addView(btn("Reset letters", v -> {
            LayoutStore.reset(this, true);
            alpha.clear(); alpha.addAll(LayoutStore.load(this, true));
            renderEditors();
        }), margin(0, dp(10), dp(8), 0));
        resetRow.addView(btn("Reset symbols", v -> {
            LayoutStore.reset(this, false);
            symbols.clear(); symbols.addAll(LayoutStore.load(this, false));
            renderEditors();
        }), margin(0, dp(10), 0, 0));
        root.addView(resetRow);

        root.addView(label("Enable LugeKey: Settings \u2192 System \u2192 Languages & input \u2192 Keyboards.\n"
                + "Subkeys open instantly when you swipe a key (long-press optional).", 12, false, 0xFF9AA0AC),
                marginW(0, dp(16), 0, 0));

        return sc;
    }

    /* ---------------- theme picker ---------------- */

    private void buildTheme() {
        addSection("Theme");
        themeHost = new LinearLayout(this);
        themeHost.setOrientation(LinearLayout.VERTICAL);
        root.addView(themeHost, marginW(0, dp(4), 0, 0));
        renderThemes();
    }

    private void renderThemes() {
        themeHost.removeAllViews();
        for (int i = 0; i < Theme.PALETTES.length; i += 2) {
            LinearLayout r = row(0);
            r.addView(themeCard(i), weight());
            if (i + 1 < Theme.PALETTES.length) {
                r.addView(themeCard(i + 1), weight());
            } else {
                r.addView(new View(this), weight());
            }
            themeHost.addView(r, marginW(0, 0, 0, dp(6)));
        }
    }

    private View themeCard(final int index) {
        Theme.Palette p = Theme.PALETTES[index];
        boolean selected = prefs.theme() == index;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(4), dp(12), dp(4), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(p.bg);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(selected ? 3 : 1), selected ? p.accent : 0xFFD5D8E0);
        card.setBackground(bg);

        LinearLayout keys = new LinearLayout(this);
        keys.setOrientation(LinearLayout.HORIZONTAL);
        keys.setGravity(Gravity.CENTER);
        keys.addView(keyDot(p.accent, p.key));
        keys.addView(keyDot(p.keyFunc, p.text), margin(dp(4), 0, dp(4), 0));
        keys.addView(keyDot(p.key, p.text));
        card.addView(keys);

        TextView name = new TextView(this);
        name.setText(p.name);
        name.setTextSize(12);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setTextColor(p.text);
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, dp(7), 0, 0);
        card.addView(name);

        card.setOnClickListener(v -> {
            prefs.setTheme(index);
            renderThemes();
        });
        return card;
    }

    private View keyDot(int fill, int stroke) {
        View v = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(fill);
        g.setStroke(dp(2), stroke);
        v.setBackground(g);
        return v;
    }

    /* ---------------- sections ---------------- */

    private void buildKeyboard() {
        addSection("Keyboard");
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.addView(toggle("Toolbar", "Clipboard, undo, emoji, cursor, settings", prefs.toolbar(), prefs::setToolbar));
        card.addView(toggle("Number row", "Digits 1-0 above the letters", prefs.numberRow(), prefs::setNumberRow));
        card.addView(toggle("Arrow row", "\u25C0 \u2302 Home/End \u25B6 row", prefs.arrowRow(), prefs::setArrowRow));
        card.addView(toggle("Symbol hints", "Small hint on each letter key", prefs.hints(), prefs::setHints));
        card.addView(toggle("Auto-capitalise", "After . ? !", prefs.autoCap(), prefs::setAutoCap));
        card.addView(seek("Keyboard height", 38, 66, prefs.heightDp(), prefs::setHeightDp));
        card.addView(seek("Key text size", 80, 140, prefs.fontPercent(), prefs::setFontPercent));
        root.addView(cardWrap(card), marginW(0, dp(4), 0, 0));
    }

    private void buildPopups() {
        addSection("Swipe popup");
        dirHost = new LinearLayout(this);
        dirHost.setOrientation(LinearLayout.HORIZONTAL);
        for (final String d : DIRS) {
            Button b = new Button(this);
            b.setText(d);
            b.setAllCaps(false);
            b.setTextSize(12);
            b.setMinWidth(0);
            b.setPadding(dp(10), 0, dp(10), 0);
            b.setOnClickListener(v -> { prefs.setPopupDir(d); renderDir(); });
            dirHost.addView(b, margin(0, 0, dp(6), 0));
        }
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.addView(dirHost);
        renderDir();
        card.addView(label("Default swipe direction that opens subkeys (each key can override)", 11, false, 0xFF8A8F99),
                margin(0, dp(2), 0, dp(4)));
        card.addView(toggle("Long-press also opens subkeys", "Optional; off by default", prefs.longPressPopup(), prefs::setLongPressPopup));
        root.addView(cardWrap(card), marginW(0, dp(4), 0, 0));
    }

    private void buildSound() {
        addSection("Feedback");
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.addView(toggle("Key click sound", null, prefs.sound(), prefs::setSound));
        card.addView(toggle("Haptic feedback", null, prefs.haptic(), prefs::setHaptic));
        card.addView(toggle("Save typed words to clipboard", "Builds history from long words you type", prefs.saveTyped(), prefs::setSaveTyped));
        root.addView(cardWrap(card), marginW(0, dp(4), 0, dp(6)));
    }

    /* ---------------- clipboard manager ---------------- */

    private void renderClips() {
        clipHost.removeAllViews();
        List<String> items = ClipboardHistory.load(this);
        if (items.isEmpty()) {
            clipHost.addView(label("Nothing copied yet. Copy text in any app or type long words through LugeKey.", 12, false, 0xFF8A8F99));
        }
        for (int i = 0; i < items.size(); i++) {
            final int idx = i;
            final String s = items.get(i);
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setBackground(round(0xFFF4F5F9, dp(8)));
            item.setPadding(dp(10), dp(7), dp(10), dp(7));

            TextView tv = new TextView(this);
            tv.setText(s.replace('\n', ' '));
            tv.setSingleLine(true);
            tv.setTextSize(13);
            tv.setTextColor(0xFF33343B);
            tv.setEllipsize(TextUtils.TruncateAt.END);
            item.addView(tv, new LinearLayout.LayoutParams(0, WRAP, 1f));

            Button cp = miniBtn("copy", v -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("LugeKey", s));
            });
            Button del = miniBtn("del", v -> {
                ClipboardHistory.remove(this, idx);
                renderClips();
            });
            del.setTextColor(0xFFFF5F6D);
            item.addView(cp, margin(dp(6), 0, dp(4), 0));
            item.addView(del);
            clipHost.addView(item, marginW(0, 0, 0, dp(6)));
        }
        if (!items.isEmpty()) {
            Button clear = new Button(this);
            clear.setText("Clear all");
            clear.setAllCaps(false);
            clear.setTextSize(13);
            clear.setTextColor(0xFFFF5F6D);
            clear.setBackground(round(0xFFFDEBED, dp(8)));
            clear.setOnClickListener(v -> {
                ClipboardHistory.clear(this);
                renderClips();
            });
            clipHost.addView(clear);
        }
    }

    /* ---------------- layout editor ---------------- */

    private void renderEditors() {
        editorHost.removeAllViews();
        editorHost.addView(layerEditor(true));
        editorHost.addView(space(dp(10)));
        editorHost.addView(layerEditor(false));
    }

    private LinearLayout layerEditor(final boolean letters) {
        final List<List<KeyDef>> rows = letters ? alpha : symbols;
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);

        col.addView(label(letters ? "\u2022 Letters layer (tap a key to edit)" : "\u2022 Symbols layer (tap a key to edit)",
                13, true, 0xFF44464F), margin(0, dp(2), 0, dp(6)));

        for (int r = 0; r < rows.size(); r++) {
            final int rowIdx = r;
            final List<KeyDef> row = rows.get(r);

            LinearLayout head = row(Gravity.CENTER_VERTICAL);
            head.addView(label("Row " + (r + 1), 11, true, 0xFF8A8F99), margin(0, 0, dp(10), 0));
            head.addView(miniBtn("+ key", v -> addKeyDialog(letters, rowIdx)), margin(0, 0, dp(6), 0));
            Button delR = miniBtn("\u00D7 row", v -> {
                rows.remove(rowIdx);
                persist(letters);
                renderEditors();
            });
            delR.setTextColor(0xFFC4473D);
            head.addView(delR);
            col.addView(head, margin(0, dp(6), 0, dp(2)));

            HorizontalScrollView hsc = new HorizontalScrollView(this);
            hsc.setHorizontalScrollBarEnabled(false);
            LinearLayout chips = new LinearLayout(this);
            chips.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < row.size(); c++) {
                final int colIdx = c;
                chips.addView(keyChip(row.get(c), v -> editKeyDialog(letters, rowIdx, colIdx)), margin(0, 0, dp(5), 0));
            }
            hsc.addView(chips, new ViewGroup.LayoutParams(MATCH, WRAP));
            col.addView(hsc, marginW(0, 0, 0, dp(6)));
        }

        Button addRow = new Button(this);
        addRow.setText("+ add row");
        addRow.setAllCaps(false);
        addRow.setTextSize(12);
        addRow.setTextColor(0xFF3A3B42);
        addRow.setBackground(round(0xFFE9EBF1, dp(8)));
        addRow.setOnClickListener(v -> addKeyDialog(letters, rows.size()));
        col.addView(addRow, margin(0, dp(2), 0, 0));
        return col;
    }

    private View keyChip(final KeyDef k, View.OnClickListener click) {
        TextView t = new TextView(this);
        t.setText(chipGlyph(k));
        t.setTextSize(12);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(0xFF24252C);
        t.setPadding(dp(11), dp(7), dp(11), dp(7));
        t.setBackground(round(0xFFE9EBF1, dp(9)));
        t.setOnClickListener(click);
        return t;
    }

    private String chipGlyph(KeyDef k) {
        switch (k.kind) {
            case KeyDef.SHIFT: return "\u21E7";
            case KeyDef.BACKSPACE: return "\u232B";
            case KeyDef.ENTER: return "\u23CE";
            case KeyDef.SPACE: return "space";
            case KeyDef.SYMBOLS: return k.label;
            case KeyDef.DELETE_WORD: return "\u232B\u00D7";
            case KeyDef.UNDO: return "\u21A9";
            case KeyDef.CURSOR_LEFT: return "\u25C0";
            case KeyDef.CURSOR_RIGHT: return "\u25B6";
            case KeyDef.HOME: return "\u23EA";
            case KeyDef.END: return "\u23E9";
            case KeyDef.CLIPBOARD: return "\uD83D\uDCCB";
            case KeyDef.EMOJI: return "\uD83D\uDE00";
            case KeyDef.SETTINGS: return "\u2699";
            case KeyDef.SWITCH_IME: return "\uD83C\uDF10";
            case KeyDef.TAB: return "\u21E5";
            default: {
                String h = k.firstSubkey();
                return h == null ? k.label : (k.label + " " + h);
            }
        }
    }

    /* ---------------- key dialogs ---------------- */

    private void editKeyDialog(final boolean letters, final int row, final int col) {
        List<List<KeyDef>> rows = letters ? alpha : symbols;
        final KeyDef k = rows.get(row).get(col);
        LinearLayout v = form();
        final Spinner kind = spinnerKind(k.kind);
        final FieldBox label = field("Label shown", k.label);
        final FieldBox output = field("Output (typed on tap)", k.output);
        final FieldBox subs = field("Subkeys (comma-separated: chars, words or emoji)", join(k.subkeys));
        final Spinner dir = spinnerDir(k.popupDir);
        final FieldBox width = field("Width (1.0 default)", String.valueOf(k.width));
        v.addView(kind);
        v.addView(label);
        v.addView(output);
        v.addView(subs);
        v.addView(dir, margin(0, dp(6), 0, 0));
        v.addView(width, margin(0, dp(6), 0, 0));

        new AlertDialog.Builder(this)
                .setTitle("Edit key")
                .setView(v)
                .setPositiveButton("Save", (d, w) -> {
                    k.kind = KIND_VALUES[clamp(kind.getSelectedItemPosition())];
                    k.label = label.edit.getText().toString();
                    k.output = output.edit.getText().toString();
                    k.subkeys = split(subs.edit.getText().toString());
                    k.popupDir = String.valueOf(dir.getSelectedItem());
                    try { k.width = Float.parseFloat(width.edit.getText().toString().trim()); } catch (Exception ignored) {}
                    if (k.label.isEmpty()) k.label = k.output.isEmpty() ? "?" : k.output;
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
        final Spinner kind = spinnerKind(KeyDef.CONTENT);
        final FieldBox label = field("Label shown", "K");
        final FieldBox output = field("Output (typed on tap)", "");
        final FieldBox subs = field("Subkeys (comma-separated)", "");
        final Spinner dir = spinnerDir(prefs.popupDir());
        v.addView(kind);
        v.addView(label);
        v.addView(output);
        v.addView(subs);
        v.addView(dir, margin(0, dp(6), 0, 0));

        new AlertDialog.Builder(this)
                .setTitle("Add key")
                .setView(v)
                .setPositiveButton("Add", (d, w) -> {
                    KeyDef k = new KeyDef();
                    k.kind = KIND_VALUES[clamp(kind.getSelectedItemPosition())];
                    k.label = label.edit.getText().toString();
                    k.output = output.edit.getText().toString();
                    k.subkeys = split(subs.edit.getText().toString());
                    k.popupDir = String.valueOf(dir.getSelectedItem());
                    if (k.label.isEmpty()) k.label = k.output.isEmpty() ? "K" : k.output;
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

    private static int clamp(int i) {
        return Math.max(0, Math.min(KIND_VALUES.length - 1, i));
    }

    private String join(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(s);
        }
        return sb.toString();
    }

    private List<String> split(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String t : raw.split("[,]")) {
            String s = t.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private void persist(boolean letters) {
        LayoutStore.save(this, letters, letters ? alpha : symbols);
    }

    /* ---------------- builders ---------------- */

    private LinearLayout form() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(dp(22), dp(12), dp(22), dp(4));
        return v;
    }

    private static class FieldBox extends LinearLayout {
        final EditText edit;
        FieldBox(Context c, String title, String value) {
            super(c);
            setOrientation(VERTICAL);
            TextView cap = new TextView(c);
            cap.setText(title);
            cap.setTextSize(11);
            cap.setTextColor(0xFF8A8F99);
            cap.setPadding(0, px(c, 6), 0, px(c, 2));
            edit = new EditText(c);
            edit.setText(value == null ? "" : value);
            edit.setSingleLine(true);
            edit.setTextSize(14);
            addView(cap);
            addView(edit);
        }
        private static int px(Context c, int v) {
            return Math.round(v * c.getResources().getDisplayMetrics().density);
        }
    }

    private FieldBox field(String title, String value) {
        return new FieldBox(this, title, value);
    }

    private Spinner spinnerKind(int kind) {
        Spinner sp = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, KIND_NAMES);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        for (int i = 0; i < KIND_VALUES.length; i++) {
            if (KIND_VALUES[i] == kind) { sp.setSelection(i); break; }
        }
        return sp;
    }

    private Spinner spinnerDir(String cur) {
        Spinner sp = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, DIRS);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        for (int i = 0; i < DIRS.length; i++) {
            if (DIRS[i].equals(cur)) { sp.setSelection(i); break; }
        }
        return sp;
    }

    private void renderDir() {
        if (dirHost == null) return;
        String cur = prefs.popupDir();
        for (int i = 0; i < dirHost.getChildCount(); i++) {
            Button b = (Button) dirHost.getChildAt(i);
            boolean sel = DIRS[i].equals(cur);
            b.setTextColor(sel ? Color.WHITE : 0xFF4A4B53);
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(9));
            g.setColor(sel ? 0xFF6C63FF : 0xFFE9EBF1);
            b.setBackground(g);
        }
    }

    private View toggle(String title, String sub, boolean value, final BoolChange cb) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(0, dp(5), dp(6), dp(5));
        texts.addView(label(title, 15, false, 0xFF24252C));
        if (sub != null) texts.addView(label(sub, 11, false, 0xFF8A8F99));
        Switch sw = new Switch(this);
        sw.setChecked(value);
        sw.setOnCheckedChangeListener((b, v) -> cb.change(v));
        row.addView(texts, new LinearLayout.LayoutParams(0, WRAP, 1f));
        row.addView(sw);
        return row;
    }

    interface BoolChange { void change(boolean v); }

    private View seek(String title, int min, int max, int cur, final IntChange cb) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        final TextView val = label(title + ": " + cur, 14, false, 0xFF3A3B42);
        SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        sb.setProgress(cur - min);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                val.setText(title + ": " + (min + p));
                if (fromUser) cb.change(min + p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        box.addView(val);
        box.addView(sb);
        return box;
    }

    interface IntChange { void change(int v); }

    private void addSection(String title) {
        root.addView(label(title.toUpperCase(), 11, true, 0xFF9AA0AC), margin(0, dp(14), 0, dp(2)));
    }

    private LinearLayout cardWrap(View inner) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14), dp(8), dp(14), dp(8));
        c.setBackground(round(0xFFFFFFFF, dp(14)));
        c.addView(inner, new LinearLayout.LayoutParams(MATCH, WRAP));
        return c;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
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

    private Button btn(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setTextColor(0xFF3A3B42);
        b.setBackground(round(0xFFE9EBF1, dp(8)));
        b.setOnClickListener(l);
        return b;
    }

    private Button miniBtn(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(11);
        b.setMinWidth(0);
        b.setTextColor(0xFF3A3B42);
        b.setPadding(dp(8), dp(3), dp(8), dp(3));
        b.setBackground(round(0xFFE9EBF1, dp(7)));
        b.setOnClickListener(l);
        return b;
    }

    private LinearLayout row(int gravity) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(gravity);
        return l;
    }

    private View space(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(MATCH, dp(h)));
        return v;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, WRAP, 1f);
    }

    private LinearLayout.LayoutParams margin(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
        lp.setMargins(l, t, r, b);
        return lp;
    }

    private LinearLayout.LayoutParams marginW(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.setMargins(l, t, r, b);
        return lp;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
}
