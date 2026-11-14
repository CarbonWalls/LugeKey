package net.carbonwalls.lugekey;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * LugeKey keyboard. FUTO-style minimal design + CleverKeys-style per-key
 * customization: swiping a key opens an instant radial subkey popover (no
 * long-press needed). Toolbar, number/arrow rows, hints and preview bubble
 * are all toggleable from settings.
 */
public class KeyboardView extends View {

    public interface Listener {
        void typeText(String text);
        void backspace();
        void enter();
        void tab();
        void deleteWord();
        void undo();
        void cursorLeft();
        void cursorRight();
        void home();
        void end();
        void switchIme();
        void openSettings();
        void paste(String text);
    }

    public static final int PAGE_ALPHA = 0;
    public static final int PAGE_SYMBOLS = 1;
    public static final int PAGE_EMOJI = 2;

    private final Prefs prefs;
    private final Handler handler = new Handler();
    private Listener listener;

    private Theme.Palette pal;
    private List<List<KeyDef>> rows = new ArrayList<>();
    private final List<float[]> keyRects = new ArrayList<>(); // per row, 4 floats per key
    private final List<Float> rowHeights = new ArrayList<>();
    private float rowHeight, toolbarH, gap, corner, padX, padTop, padBottom, fontScale;
    private final float density;

    private int page = PAGE_ALPHA;
    private int shiftState;          // 0 off, 1 once, 2 caps lock
    private long lastShiftPress;

    // touch
    private boolean tracking;
    private KeyDef downKey;
    private int downRow = -1, downCol = -1;
    private float downX, downY;
    private boolean consumeUp;
    private boolean longPressFired;

    // radial subkey popover
    private boolean popover;
    private List<String> popSlots = new ArrayList<>();
    private final List<RectF> popRects = new ArrayList<>();
    private float popCx, popCy, popRadius, popSlotSize;
    private int popSel = -1;
    private float popAnim = 1f;
    private ValueAnimator popAnimator;

    // clipboard panel
    private boolean clipPanel;
    private RectF clipPanelRect;
    private final List<RectF> clipItemRects = new ArrayList<>();
    private final List<String> clipItems = new ArrayList<>();
    private RectF clipClearRect, clipCloseRect;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    public KeyboardView(Context context) {
        super(context);
        prefs = new Prefs(context);
        density = getResources().getDisplayMetrics().density;
        setFocusable(false);
        rebuild();
    }

    public void setListener(Listener l) { listener = l; }

    public int getPage() { return page; }

    public void resetState() {
        shiftState = 0;
        page = PAGE_ALPHA;
        closeOverlays();
        rebuild();
    }

    private void closeOverlays() {
        popover = false;
        clipPanel = false;
        popSlots.clear();
        popRects.clear();
        clipItems.clear();
        clipItemRects.clear();
        cancelAll();
    }

    /* ---------------- layout ---------------- */

    public void rebuild() {
        pal = Theme.current(prefs);
        rowHeight = dp(prefs.heightDp());
        toolbarH = rowHeight * 0.78f;
        gap = dp(2.5f);
        corner = dp(9f);
        padX = dp(3f);
        padTop = dp(14f);
        padBottom = dp(6f);
        fontScale = prefs.fontPercent() / 100f;

        rows = new ArrayList<>();
        rowHeights.clear();
        keyRects.clear();

        if (page == PAGE_EMOJI) {
            for (List<KeyDef> r : LayoutStore.emojiRows()) {
                rows.add(r);
                rowHeights.add(rowHeight);
            }
        } else {
            boolean letters = page == PAGE_ALPHA;
            List<List<KeyDef>> base = LayoutStore.load(getContext(), letters);
            if (letters && prefs.toolbar()) {
                List<KeyDef> tb = new ArrayList<>();
                tb.add(act(KeyDef.CLIPBOARD, "\uD83D\uDCCB"));
                tb.add(act(KeyDef.UNDO, "\u21A9"));
                tb.add(act(KeyDef.EMOJI, "\uD83D\uDE00"));
                tb.add(act(KeyDef.CURSOR_LEFT, "\u25C0"));
                tb.add(act(KeyDef.CURSOR_RIGHT, "\u25B6"));
                tb.add(act(KeyDef.SETTINGS, "\u2699"));
                rows.add(tb);
                rowHeights.add(toolbarH);
            }
            if (letters && prefs.arrowRow()) {
                List<KeyDef> ar = new ArrayList<>();
                ar.add(act(KeyDef.CURSOR_LEFT, "\u25C0"));
                ar.add(act(KeyDef.HOME, "\u23EA"));
                ar.add(act(KeyDef.END, "\u23E9"));
                ar.add(act(KeyDef.CURSOR_RIGHT, "\u25B6"));
                rows.add(ar);
                rowHeights.add(rowHeight);
            }
            if (letters && prefs.numberRow()) {
                List<KeyDef> nr = new ArrayList<>();
                for (int i = 1; i <= 9; i++) nr.add(LayoutStore.digit("" + i));
                nr.add(LayoutStore.digit("0"));
                rows.add(nr);
                rowHeights.add(rowHeight);
            }
            for (List<KeyDef> r : base) {
                rows.add(new ArrayList<>(r));
                rowHeights.add(rowHeight);
            }
        }
        relayout();
        invalidate();
    }

    private static KeyDef act(int kind, String label) {
        KeyDef k = new KeyDef(kind, label, "", null, 1f);
        k.popupDir = KeyDef.DIR_NONE;
        k.id = (kind + label).hashCode();
        return k;
    }

    private void relayout() {
        keyRects.clear();
        float width = getWidth();
        if (width <= 0) return;
        float w = width - padX * 2f;
        float y = padTop;
        for (int r = 0; r < rows.size(); r++) {
            List<KeyDef> row = rows.get(r);
            float rh = r < rowHeights.size() ? rowHeights.get(r) : rowHeight;
            int n = row.size();
            float[] arr = new float[Math.max(0, n * 4)];
            if (n > 0) {
                float wt = 0f;
                for (KeyDef k : row) wt += Math.max(0.05f, k.width);
                float avail = w - gap * (n - 1);
                float x = padX;
                for (int i = 0; i < n; i++) {
                    float kw = avail * Math.max(0.05f, row.get(i).width) / wt;
                    arr[i * 4] = x;
                    arr[i * 4 + 1] = y;
                    arr[i * 4 + 2] = x + kw;
                    arr[i * 4 + 3] = y + rh;
                    x += kw + gap;
                }
            }
            keyRects.add(arr);
            y += rh + gap;
        }
    }

    private float dp(float v) { return v * density; }

    /* ---------------- touch ---------------- */

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                downRow = -1; downCol = -1; downKey = null; tracking = false;
                consumeUp = false; longPressFired = false;
                if (clipPanel) {
                    handleClipDown(ev.getX(), ev.getY());
                    return true;
                }
                int[] hit = findKey(ev.getX(), ev.getY());
                if (hit != null) {
                    downRow = hit[0]; downCol = hit[1];
                    downKey = rows.get(downRow).get(downCol);
                    downX = ev.getX(); downY = ev.getY();
                    tracking = true;
                    Fx.click(getContext(), prefs, this);
                    if (downKey.kind == KeyDef.BACKSPACE) {
                        handler.removeCallbacks(repeatRunnable);
                        handler.postDelayed(repeatRunnable, 420);
                    }
                    if (prefs.longPressPopup()) {
                        handler.removeCallbacks(longPressRunnable);
                        handler.postDelayed(longPressRunnable, 480);
                    }
                    invalidate();
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (!tracking) return true;
                if (popover) {
                    updatePopSel(ev.getX(), ev.getY());
                    invalidate();
                    return true;
                }
                if (!longPressFired && downKey != null) {
                    float dx = ev.getX() - downX;
                    float dy = ev.getY() - downY;
                    if (downKey.kind == KeyDef.BACKSPACE
                            && dx <= -dp(18f) && Math.abs(dx) > Math.abs(dy) * 1.2f) {
                        if (listener != null) listener.deleteWord();
                        consumeUp = true;
                    } else if (swipeTriggered(downKey, dx, dy) && downKey.hasSubkeys()) {
                        enterPopover(downKey);
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                if (!tracking) return true;
                if (popover) {
                    if (popSel >= 0 && popSel < popSlots.size()) {
                        commit(popSlots.get(popSel));
                    }
                } else if (consumeUp) {
                    // swipe on backspace already handled
                } else if (downKey != null && !longPressFired) {
                    pressKey(downKey);
                }
                cleanup();
                return true;
            }
            case MotionEvent.ACTION_CANCEL: {
                cleanup();
                return true;
            }
        }
        return true;
    }

    private void cleanup() {
        tracking = false;
        consumeUp = false;
        longPressFired = false;
        popover = false;
        popSlots.clear();
        popRects.clear();
        popSel = -1;
        popAnim = 1f;
        if (popAnimator != null) { popAnimator.cancel(); popAnimator = null; }
        cancelAll();
        downKey = null;
        invalidate();
    }

    private void cancelAll() {
        handler.removeCallbacks(repeatRunnable);
        handler.removeCallbacks(longPressRunnable);
    }

    private final Runnable repeatRunnable = new Runnable() {
        @Override public void run() {
            if (tracking && downKey != null && downKey.kind == KeyDef.BACKSPACE
                    && !popover && !clipPanel && !consumeUp) {
                if (listener != null) listener.backspace();
                Fx.click(getContext(), prefs, KeyboardView.this);
                handler.postDelayed(this, 55);
            }
        }
    };

    private final Runnable longPressRunnable = new Runnable() {
        @Override public void run() {
            if (tracking && downKey != null && !popover && downKey.hasSubkeys()) {
                if (enterPopover(downKey)) longPressFired = true;
            }
        }
    };

    private int[] findKey(float x, float y) {
        for (int r = 0; r < keyRects.size(); r++) {
            float[] a = keyRects.get(r);
            for (int i = 0; i + 3 < a.length; i += 4) {
                if (x >= a[i] && x <= a[i + 2] && y >= a[i + 1] && y <= a[i + 3]) return new int[]{r, i / 4};
            }
        }
        return null;
    }

    private boolean swipeTriggered(KeyDef k, float dx, float dy) {
        if (k == null) return false;
        String d = k.popupDir == null ? KeyDef.DIR_NONE : k.popupDir;
        float th = dp(15f);
        switch (d) {
            case KeyDef.DIR_UP:    return -dy >= th && Math.abs(dy) > Math.abs(dx);
            case KeyDef.DIR_DOWN:  return  dy >= th && Math.abs(dy) > Math.abs(dx);
            case KeyDef.DIR_LEFT:  return -dx >= th && Math.abs(dx) > Math.abs(dy);
            case KeyDef.DIR_RIGHT: return  dx >= th && Math.abs(dx) > Math.abs(dy);
            default: return false;
        }
    }

    /* ---------------- actions ---------------- */

    private void pressKey(KeyDef k) {
        if (k == null) return;
        Fx.click(getContext(), prefs, this);
        switch (k.kind) {
            case KeyDef.CONTENT: {
                String out = k.output == null ? "" : k.output;
                if (out.isEmpty()) out = k.label;
                if (shiftState != 0 && out.length() == 1) {
                    out = String.valueOf(Character.toUpperCase(out.charAt(0)));
                }
                commit(out);
                break;
            }
            case KeyDef.SPACE: commit(" "); break;
            case KeyDef.TAB: commit("\t"); break;
            case KeyDef.BACKSPACE: if (listener != null) listener.backspace(); break;
            case KeyDef.ENTER:
                if (listener != null) listener.enter();
                shiftState = 0;
                invalidate();
                break;
            case KeyDef.SHIFT: {
                long now = SystemClock.uptimeMillis();
                if (shiftState == 2) shiftState = 0;
                else if (shiftState == 1 && now - lastShiftPress < 450) shiftState = 2;
                else shiftState = 1;
                lastShiftPress = now;
                invalidate();
                break;
            }
            case KeyDef.SYMBOLS:
                page = (page == PAGE_ALPHA) ? PAGE_SYMBOLS : PAGE_ALPHA;
                shiftState = 0;
                rebuild();
                break;
            case KeyDef.EMOJI:
                page = (page == PAGE_EMOJI) ? PAGE_ALPHA : PAGE_EMOJI;
                shiftState = 0;
                rebuild();
                break;
            case KeyDef.SWITCH_IME: if (listener != null) listener.switchIme(); break;
            case KeyDef.DELETE_WORD: if (listener != null) listener.deleteWord(); break;
            case KeyDef.UNDO: if (listener != null) listener.undo(); break;
            case KeyDef.CURSOR_LEFT: if (listener != null) listener.cursorLeft(); break;
            case KeyDef.CURSOR_RIGHT: if (listener != null) listener.cursorRight(); break;
            case KeyDef.HOME: if (listener != null) listener.home(); break;
            case KeyDef.END: if (listener != null) listener.end(); break;
            case KeyDef.CLIPBOARD: openClipPanel(); break;
            case KeyDef.SETTINGS: if (listener != null) listener.openSettings(); break;
        }
    }

    /** Single funnel for everything that inserts text (keys, popover, clipboard). */
    private void commit(String s) {
        if (s == null || s.isEmpty()) return;
        if (listener != null) listener.typeText(s);
        afterType(s);
        invalidate();
    }

    private void afterType(String s) {
        if (shiftState == 1) shiftState = 0;
        if (prefs.autoCap() && s.length() == 1) {
            char c = s.charAt(0);
            if (c == '.' || c == '?' || c == '!' || c == ':') shiftState = 1;
        }
    }

    private String displayLabel(KeyDef k) {
        if (k.kind == KeyDef.CONTENT && shiftState != 0) {
            if (k.label != null && k.label.length() == 1) {
                char u = Character.toUpperCase(k.label.charAt(0));
                if (u != k.label.charAt(0)) return String.valueOf(u);
            }
        }
        return k.label == null ? "" : k.label;
    }

    /* ---------------- radial subkey popover ---------------- */

    private boolean enterPopover(KeyDef k) {
        popSlots.clear();
        for (String s : k.subkeys) {
            if (s != null && !s.isEmpty()) popSlots.add(s);
            if (popSlots.size() >= 8) break;
        }
        if (popSlots.isEmpty()) return false;

        float[] a = keyRects.get(downRow);
        float kL = a[downCol * 4], kT = a[downCol * 4 + 1];
        float kR = a[downCol * 4 + 2], kB = a[downCol * 4 + 3];
        popCx = (kL + kR) / 2f;
        popCy = (kT + kB) / 2f;
        popSlotSize = Math.max(dp(44f), rowHeight * 0.92f);
        popRadius = rowHeight * 1.55f;

        popRects.clear();
        for (int i = 0; i < popSlots.size(); i++) {
            float a0 = (float) (Math.PI / 4f * i - Math.PI / 2f); // N, NE, E, SE, S, SW, W, NW
            float cx = popCx + popRadius * (float) Math.cos(a0);
            float cy = popCy + popRadius * (float) Math.sin(a0);
            popRects.add(new RectF(cx - popSlotSize / 2f, cy - popSlotSize / 2f,
                    cx + popSlotSize / 2f, cy + popSlotSize / 2f));
        }
        popSel = -1;
        popover = true;
        cancelAll();
        popAnim = 0f;
        if (popAnimator != null) popAnimator.cancel();
        popAnimator = ValueAnimator.ofFloat(0f, 1f);
        popAnimator.setDuration(140);
        popAnimator.addUpdateListener(a2 -> { popAnim = (Float) a2.getAnimatedValue(); invalidate(); });
        popAnimator.start();
        invalidate();
        return true;
    }

    private void updatePopSel(float x, float y) {
        float dx = x - popCx, dy = y - popCy;
        float dist = (float) Math.hypot(dx, dy);
        if (dist < popRadius * 0.5f || popSlots.isEmpty()) {
            popSel = -1;
            return;
        }
        float best = popRadius * 2f;
        int bi = -1;
        for (int i = 0; i < popRects.size(); i++) {
            RectF r = popRects.get(i);
            float d = (float) Math.hypot(x - r.centerX(), y - r.centerY());
            if (d < best) { best = d; bi = i; }
        }
        popSel = (best <= popSlotSize * 1.15f) ? bi : -1;
    }

    /* ---------------- clipboard panel ---------------- */

    private void openClipPanel() {
        clipItems.clear();
        for (String s : ClipboardHistory.load(getContext())) {
            clipItems.add(s);
            if (clipItems.size() >= 8) break;
        }
        float w = getWidth() - dp(24f);
        float h = dp(36f) + clipItems.size() * dp(46f) + dp(72f);
        h = Math.min(h, getHeight() * 0.85f);
        float l = (getWidth() - w) / 2f;
        float t = (getHeight() - h) / 2f;
        clipPanelRect = new RectF(l, t, l + w, t + h);
        clipItemRects.clear();
        float y = t + dp(34f);
        for (int i = 0; i < clipItems.size(); i++) {
            clipItemRects.add(new RectF(l + dp(10f), y, l + w - dp(10f), y + dp(42f)));
            y += dp(46f);
        }
        clipClearRect = new RectF(clipPanelRect.left + dp(12f), clipPanelRect.bottom - dp(50f),
                clipPanelRect.left + dp(90f), clipPanelRect.bottom - dp(12f));
        clipCloseRect = new RectF(clipPanelRect.right - dp(90f), clipPanelRect.bottom - dp(50f),
                clipPanelRect.right - dp(12f), clipPanelRect.bottom - dp(12f));
        clipPanel = true;
        cancelAll();
        invalidate();
    }

    private void handleClipDown(float x, float y) {
        for (int i = 0; i < clipItemRects.size(); i++) {
            if (clipItemRects.get(i).contains(x, y)) {
                if (listener != null) listener.paste(clipItems.get(i));
                clipPanel = false;
                invalidate();
                return;
            }
        }
        if (clipClearRect != null && clipClearRect.contains(x, y)) {
            ClipboardHistory.clear(getContext());
            openClipPanel();
            return;
        }
        if (clipCloseRect != null && clipCloseRect.contains(x, y)) {
            clipPanel = false;
            invalidate();
        }
    }

    /* ---------------- drawing ---------------- */

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        float h = padTop + padBottom;
        for (int i = 0; i < rowHeights.size(); i++) {
            h += rowHeights.get(i);
            if (i + 1 < rowHeights.size()) h += gap;
        }
        setMeasuredDimension(w, Math.round(h));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        relayout();
    }

    @Override
    protected void onDraw(Canvas c) {
        if (pal == null) return;
        c.drawColor(pal.bg);

        int n = rows.size();
        for (int r = 0; r < n; r++) {
            float[] a = keyRects.get(r);
            List<KeyDef> row = rows.get(r);
            for (int i = 0; i * 4 + 3 < a.length; i++) {
                KeyDef k = row.get(i);
                RectF rect = new RectF(a[i * 4], a[i * 4 + 1], a[i * 4 + 2], a[i * 4 + 3]);
                drawKey(c, k, rect, r, i);
            }
        }

        if (popover && !popRects.isEmpty()) drawPopover(c);
        if (clipPanel && clipPanelRect != null) drawClipPanel(c);
    }

    private void drawKey(Canvas c, KeyDef k, RectF rect, int r, int i) {
        boolean pressed = isDown(r, i);
        boolean shiftKey = k.kind == KeyDef.SHIFT && shiftState != 0;
        int fill;
        if (shiftKey) fill = pal.accent;
        else if (pressed) fill = k.isSpecial() ? pal.funcPressed() : pal.pressed;
        else fill = k.isSpecial() ? pal.keyFunc : pal.key;

        paint.setColor(fill);
        c.drawRoundRect(rect, corner, corner, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1f));
        paint.setColor(pal.keyBorder);
        c.drawRoundRect(rect, corner, corner, paint);
        paint.setStyle(Paint.Style.FILL);

        // symbol hint (first subkey) at top-right
        if (prefs.hints() && k.kind == KeyDef.CONTENT && !pressed && shiftState == 0) {
            String h = k.firstSubkey();
            if (h != null) {
                tp.setColor(pal.hint);
                tp.setTextSize(sp(9f) * fontScale);
                tp.setTextAlign(Paint.Align.RIGHT);
                String hs = h.length() > 2 ? h.substring(0, 2) : h;
                c.drawText(hs, rect.right - dp(6f), rect.top + dp(12f), tp);
            }
        }

        String label = keyText(k);
        tp.setColor(shiftKey ? 0xFFFFFFFF : (k.isSpecial() ? pal.textFunc : pal.text));
        tp.setTextSize(sp(k.isSpecial() ? 14f : 16f) * fontScale);
        tp.setTypeface(Typeface.DEFAULT_BOLD);
        tp.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = tp.getFontMetrics();
        c.drawText(label, rect.centerX(), rect.centerY() - (fm.ascent + fm.descent) / 2f, tp);
    }

    private boolean isDown(int r, int i) {
        return tracking && downRow == r && downCol == i && !popover && !clipPanel;
    }

    private String keyText(KeyDef k) {
        switch (k.kind) {
            case KeyDef.SHIFT: return shiftState == 2 ? "\u21EA" : "\u21E7";
            case KeyDef.BACKSPACE: return "\u232B";
            case KeyDef.ENTER: return "\u23CE";
            case KeyDef.TAB: return "\u21E5";
            case KeyDef.SYMBOLS: {
                String l = displayLabel(k);
                return l.isEmpty() ? "?123" : l;
            }
            default: {
                String l = displayLabel(k);
                return l.isEmpty() ? "\u00B7" : l;
            }
        }
    }

    private void drawPopover(Canvas c) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(pal.dark ? 0xB3000000 : 0x66000000);
        c.drawRect(0, 0, getWidth(), getHeight(), paint);

        float s = popAnim;
        RectF mid = new RectF(popCx - popSlotSize / 2f * s, popCy - popSlotSize / 2f * s,
                popCx + popSlotSize / 2f * s, popCy + popSlotSize / 2f * s);
        paint.setColor(pal.dark ? 0xFF1E1F26 : 0xFFFFFFFF);
        c.drawRoundRect(mid, dp(9f), dp(9f), paint);
        if (downKey != null) {
            tp.setColor(pal.text);
            tp.setTextSize(sp(16f) * fontScale);
            tp.setTextAlign(Paint.Align.CENTER);
            tp.setTypeface(Typeface.DEFAULT_BOLD);
            Paint.FontMetrics fm = tp.getFontMetrics();
            c.drawText(downKey.label, popCx, popCy - (fm.ascent + fm.descent) / 2f, tp);
        }

        for (int i = 0; i < popRects.size(); i++) {
            RectF base = popRects.get(i);
            RectF tile = new RectF(
                    popCx + (base.left - popCx) * s, popCy + (base.top - popCy) * s,
                    popCx + (base.right - popCx) * s, popCy + (base.bottom - popCy) * s);
            boolean sel = (i == popSel);
            paint.setColor(sel ? pal.accent : (pal.dark ? 0xFF23242B : 0xFFFFFFFF));
            c.drawRoundRect(tile, dp(9f), dp(9f), paint);
            tp.setColor(sel ? 0xFFFFFFFF : pal.text);
            tp.setTextSize(sp(15f) * fontScale);
            tp.setTextAlign(Paint.Align.CENTER);
            tp.setTypeface(Typeface.DEFAULT);
            String txt = popSlots.get(i);
            String shown = txt.substring(0, Math.min(txt.length(), 6));
            if (txt.length() > 6) shown += "\u2026";
            Paint.FontMetrics fm = tp.getFontMetrics();
            c.drawText(shown, tile.centerX(), tile.centerY() - (fm.ascent + fm.descent) / 2f, tp);
        }
    }

    private void drawClipPanel(Canvas c) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(pal.dark ? 0xCC000000 : 0x66000000);
        c.drawRect(0, 0, getWidth(), getHeight(), paint);

        paint.setColor(pal.dark ? 0xFF1C1D24 : 0xFFFFFFFF);
        c.drawRoundRect(clipPanelRect, dp(14f), dp(14f), paint);

        tp.setColor(pal.text);
        tp.setTextSize(sp(14f));
        tp.setTextAlign(Paint.Align.LEFT);
        tp.setTypeface(Typeface.DEFAULT_BOLD);
        Paint.FontMetrics fm = tp.getFontMetrics();
        c.drawText("Clipboard history", clipPanelRect.left + dp(14f),
                clipPanelRect.top + dp(22f) - (fm.ascent + fm.descent) / 2f, tp);

        for (int i = 0; i < clipItemRects.size(); i++) {
            RectF r = clipItemRects.get(i);
            paint.setColor(pal.dark ? 0xFF26272F : 0xFFF0F1F5);
            c.drawRoundRect(r, dp(8f), dp(8f), paint);
            String s = clipItems.get(i).replace('\n', ' ');
            if (s.length() > 28) s = s.substring(0, 28) + "\u2026";
            tp.setColor(pal.text);
            tp.setTextSize(sp(13f));
            tp.setTextAlign(Paint.Align.CENTER);
            tp.setTypeface(Typeface.DEFAULT);
            c.drawText(s, r.centerX(), r.centerY() - (fm.ascent + fm.descent) / 2f, tp);
        }

        drawPanelButton(c, clipClearRect, "Clear", 0xFFFF5F6D);
        drawPanelButton(c, clipCloseRect, "Close", pal.accent);
    }

    private void drawPanelButton(Canvas c, RectF r, String label, int tint) {
        if (r == null) return;
        paint.setColor(tint);
        c.drawRoundRect(r, dp(9f), dp(9f), paint);
        tp.setColor(0xFFFFFFFF);
        tp.setTextSize(sp(13f));
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setTypeface(Typeface.DEFAULT_BOLD);
        Paint.FontMetrics fm = tp.getFontMetrics();
        c.drawText(label, r.centerX(), r.centerY() - (fm.ascent + fm.descent) / 2f, tp);
    }

    private float sp(float v) {
        return v * getResources().getDisplayMetrics().scaledDensity;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacksAndMessages(null);
        if (popAnimator != null) { popAnimator.cancel(); popAnimator = null; }
    }
}
