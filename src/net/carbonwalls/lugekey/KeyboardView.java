package net.carbonwalls.lugekey;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * The LugeKey keyboard. Flat, minimal, Futo-style. The headline interaction:
 * swiping on a key in its configured direction (default: up) instantly opens that
 * key's extra-character popup — no long-press needed. Every key is user editable.
 */
public class KeyboardView extends View {

    public interface Listener {
        void typeText(String text);
        void backspace();
        void enter();
        void switchIme();
    }

    private final Prefs prefs;
    private final Handler handler = new Handler();
    private Listener listener;
    private Theme ui;

    private List<List<KeyDef>> rows = new ArrayList<>();
    private final List<float[]> keyRects = new ArrayList<>(); // per row, 4 floats per key
    private float rowHeight, gap, corner, padX, padTop, padBottom, fontScale;
    private final float density;

    // state
    private boolean symbolsOn;
    private int shiftState;       // 0 off, 1 one-shot, 2 caps lock
    private long lastShiftPress = 0;

    // touch
    private boolean tracking;
    private KeyDef downKey;
    private int downRow = -1, downCol = -1;
    private float downX, downY;
    private boolean longPressFired;

    // popup fan
    private boolean popupMode;
    private boolean popupHorizontal;
    private float popupPanelL, popupPanelT, popupPanelR, popupPanelB;
    private float popupSlotW, popupSlotH, popupGap;
    private final List<String> popupSlots = new ArrayList<>();
    private final List<RectF> popupRects = new ArrayList<>();
    private int popupSel = -1;
    private float popupAnim = 1f;
    private ValueAnimator popupAnimator;

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

    public int getRowCount() { return rows.size(); }

    public void resetState() {
        shiftState = 0;
        symbolsOn = false;
        cancelAll();
        rebuild();
    }

    /** Reload layout + theme. Call after any settings/layout change. */
    public void rebuild() {
        ui = Theme.load(getContext(), prefs);
        rowHeight = dp(prefs.heightDp());
        gap = dp(2.5f);
        corner = dp(8f);
        padX = dp(3f);
        // A little reserved pocket above the top row so swipe-up gestures on the
        // top row can travel before leaving the IME window.
        padTop = dp(22f);
        padBottom = dp(8f);
        fontScale = prefs.fontPercent() / 100f;
        List<List<KeyDef>> base = LayoutStore.load(getContext(), !symbolsOn);
        List<List<KeyDef>> shown = new ArrayList<>();
        for (List<KeyDef> r : base) shown.add(new ArrayList<>(r));
        if (!symbolsOn && prefs.numberRow()) {
            List<KeyDef> digits = new ArrayList<>();
            for (int i = 1; i <= 9; i++) digits.add(new KeyDef(KeyDef.KIND_CHAR, "" + i, "" + i, "", 1f));
            digits.add(new KeyDef(KeyDef.KIND_CHAR, "0", "0", "", 1f));
            shown.add(0, digits);
        }
        rows = shown;
        relayout();
        invalidate();
    }

    private void relayout() {
        keyRects.clear();
        float width = getWidth();
        if (width <= 0) return;
        float w = width - padX * 2f;
        for (int r = 0; r < rows.size(); r++) {
            List<KeyDef> row = rows.get(r);
            int n = row.size();
            if (n == 0) continue;
            float wt = 0f;
            for (KeyDef k : row) wt += Math.max(0.05f, k.width);
            float avail = w - gap * (n - 1);
            float[] arr = new float[n * 4];
            float x = padX;
            float top = padTop + r * (rowHeight + gap);
            for (int i = 0; i < n; i++) {
                float kw = avail * Math.max(0.05f, row.get(i).width) / wt;
                arr[i * 4] = x;
                arr[i * 4 + 1] = top;
                arr[i * 4 + 2] = x + kw;
                arr[i * 4 + 3] = top + rowHeight;
                x += kw + gap;
            }
            keyRects.add(arr);
        }
    }

    private float dp(float v) { return v * density; }

    /* ---------------- touch ---------------- */

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                downRow = -1; downCol = -1; downKey = null; tracking = false;
                longPressFired = false;
                int[] hit = findKey(ev.getX(), ev.getY());
                if (hit != null) {
                    downRow = hit[0]; downCol = hit[1];
                    downKey = rows.get(downRow).get(downCol);
                    downX = ev.getX(); downY = ev.getY();
                    tracking = true;
                    Fx.click(getContext(), prefs, this);
                    if (downKey.kind == KeyDef.KIND_BACKSPACE) {
                        handler.removeCallbacks(repeatRunnable);
                        handler.postDelayed(repeatRunnable, 420);
                    }
                    handler.removeCallbacks(longPressRunnable);
                    handler.postDelayed(longPressRunnable, 500);
                    popupMode = false;
                    invalidate();
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (!tracking) return true;
                if (popupMode) {
                    popupSel = selectSlot(ev.getX(), ev.getY());
                    invalidate();
                    return true;
                }
                if (!longPressFired) {
                    float dx = ev.getX() - downX;
                    float dy = ev.getY() - downY;
                    boolean tr = swipeTriggered(downKey, dx, dy);
                    if (tr) {
                        tryEnterPopup(downKey, false);
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                if (!tracking) return true;
                float x = ev.getX(), y = ev.getY();
                if (popupMode) {
                    int sel = selectSlot(x, y);
                    if (sel >= 0) commitPopupSlot(sel);
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
        longPressFired = false;
        popupMode = false;
        popupSlots.clear();
        popupRects.clear();
        popupSel = -1;
        popupAnim = 1f;
        if (popupAnimator != null) { popupAnimator.cancel(); popupAnimator = null; }
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
            if (tracking && downKey != null && downKey.kind == KeyDef.KIND_BACKSPACE && !popupMode) {
                if (listener != null) listener.backspace();
                Fx.click(getContext(), prefs, KeyboardView.this);
                handler.postDelayed(this, 55);
            }
        }
    };

    private final Runnable longPressRunnable = new Runnable() {
        @Override public void run() {
            if (tracking && downKey != null && !popupMode) {
                if (prefs.longPressPopup() && tryEnterPopup(downKey, true)) {
                    longPressFired = true;
                }
            }
        }
    };

    private int[] findKey(float x, float y) {
        for (int r = 0; r < keyRects.size(); r++) {
            float[] a = keyRects.get(r);
            for (int i = 0; i < a.length / 4; i++) {
                if (x >= a[i * 4] && x <= a[i * 4 + 2] && y >= a[i * 4 + 1] && y <= a[i * 4 + 3]) {
                    return new int[]{r, i};
                }
            }
        }
        return null;
    }

    private boolean swipeTriggered(KeyDef k, float dx, float dy) {
        if (k == null) return false;
        String d = k.popupDir == null ? KeyDef.DIR_NONE : k.popupDir;
        float th = dp(16);
        switch (d) {
            case KeyDef.DIR_UP:
                return -dy >= th && Math.abs(dy) > Math.abs(dx);
            case KeyDef.DIR_DOWN:
                return dy >= th && Math.abs(dy) > Math.abs(dx);
            case KeyDef.DIR_LEFT:
                return -dx >= th && Math.abs(dx) > Math.abs(dy);
            case KeyDef.DIR_RIGHT:
                return dx >= th && Math.abs(dx) > Math.abs(dy);
            default:
                return false;
        }
    }

    /* ---------------- popup fan ---------------- */

    private boolean tryEnterPopup(KeyDef k, boolean fromLongPress) {
        if (k == null || !k.popupEnabled) return false;
        if (k.popup == null || k.popup.isEmpty()) return false;

        popupSlots.clear();
        popupSlots.add(displayLabel(k)); // base slot index 0
        int added = 0;
        String p = k.popup;
        int cpCount = p.codePointCount(0, p.length());
        for (int i = 0; i < cpCount && popupSlots.size() < 12; i++) {
            int cp = p.codePointAt(p.offsetByCodePoints(0, i));
            popupSlots.add(new String(Character.toChars(cp)));
            added++;
        }
        if (added == 0) return false;

        layoutPopup(k);
        popupSel = 0;
        popupMode = true;
        popupAnim = 0f;
        cancelAll();
        if (popupAnimator != null) popupAnimator.cancel();
        popupAnimator = ValueAnimator.ofFloat(0f, 1f);
        popupAnimator.setDuration(130);
        popupAnimator.addUpdateListener(a -> { popupAnim = (Float) a.getAnimatedValue(); invalidate(); });
        popupAnimator.start();
        invalidate();
        return true;
    }

    private void layoutPopup(KeyDef k) {
        popupRects.clear();
        int n = popupSlots.size();
        float[] a = keyRects.get(downRow);
        float kL = a[downCol * 4], kT = a[downCol * 4 + 1];
        float kR = a[downCol * 4 + 2], kB = a[downCol * 4 + 3];
        float kCx = (kL + kR) / 2f, kCy = (kT + kB) / 2f;
        String d = k.popupDir == null ? KeyDef.DIR_NONE : k.popupDir;
        boolean horizontal = d.equals(KeyDef.DIR_UP) || d.equals(KeyDef.DIR_DOWN);
        popupHorizontal = horizontal;

        if (horizontal) {
            popupSlotH = rowHeight * 1.18f;
            popupGap = dp(2f);
            float availW = getWidth() - padX * 2 - dp(4);
            popupSlotW = Math.max(dp(34), Math.min(dp(52), Math.min(availW, dp(52) * n + popupGap * (n - 1)) / Math.max(1, n)));
            float[] centers = fanCenters(kCx, n);
            // Anchoring: keep the base slot (index 0) centred on the key when possible.
            float minL = Float.MAX_VALUE, maxR = -Float.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                minL = Math.min(minL, centers[i] - popupSlotW / 2f);
                maxR = Math.max(maxR, centers[i] + popupSlotW / 2f);
            }
            float leftExtent = kCx - minL, rightExtent = maxR - kCx;
            float baseCenter = clamp(kCx, padX + leftExtent, getWidth() - padX - rightExtent);
            float left = baseCenter - leftExtent;
            popupPanelL = left;
            popupPanelR = left + (maxR - minL);
            if (d.equals(KeyDef.DIR_DOWN)) {
                popupPanelT = clamp(kB + dp(2), padTop, getHeight() - padBottom - popupSlotH);
            } else {
                popupPanelT = clamp(kT - popupSlotH - dp(2), padTop, getHeight() - padBottom - popupSlotH);
            }
            popupPanelB = popupPanelT + popupSlotH;
            for (int i = 0; i < n; i++) {
                float l = left + (centers[i] - minL);
                popupRects.add(new RectF(l, popupPanelT, l + popupSlotW, popupPanelB));
            }
        } else {
            popupSlotW = rowHeight * 1.25f;
            popupGap = dp(2f);
            float maxH = getHeight() - padTop - padBottom - dp(2);
            popupSlotH = Math.max(dp(30), Math.min(dp(42), (maxH - popupGap * (n - 1)) / Math.max(1, n)));
            float[] centers = fanCenters(kCy, n);
            float minT = Float.MAX_VALUE, maxB = -Float.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                minT = Math.min(minT, centers[i] - popupSlotH / 2f);
                maxB = Math.max(maxB, centers[i] + popupSlotH / 2f);
            }
            float topExtent = kCy - minT, bottomExtent = maxB - kCy;
            float baseCenter = clamp(kCy, padTop + topExtent, getHeight() - padBottom - bottomExtent);
            float top = baseCenter - topExtent;
            float cx = d.equals(KeyDef.DIR_LEFT) ? clamp(kL - popupSlotW - dp(2), padX, getWidth() - popupSlotW - padX)
                                                 : clamp(kR + dp(2), padX, getWidth() - popupSlotW - padX);
            popupPanelL = cx; popupPanelR = cx + popupSlotW;
            popupPanelT = top; popupPanelB = top + (maxB - minT);
            for (int i = 0; i < n; i++) {
                float t = top + (centers[i] - minT);
                popupRects.add(new RectF(cx, t, cx + popupSlotW, t + popupSlotH));
            }
        }
    }

    /** Centers for n slots with index 0 at `origin`, then extras alternating left/right (or up/down). */
    private float[] fanCenters(float origin, int n) {
        float[] c = new float[n];
        float pitch = 0f;
        if (popupHorizontal) pitch = popupSlotW + popupGap; else pitch = popupSlotH + popupGap;
        c[0] = origin;
        int side = 1; // 1 = left, -1 = right
        int step = 1;
        for (int i = 1; i < n; i++) {
            c[i] = origin + side * step * pitch;
            side = -side;
            if (side == 1) step++;
        }
        return c;
    }

    private int selectSlot(float x, float y) {
        if (popupRects.isEmpty()) return -1;
        if (popupHorizontal) {
            // generously snap along the fan, but back out if the finger dives well
            // below the panel (towards the keyboard bottom) or flies far above it
            float above = popupPanelT - popupSlotH * 2.2f;
            float below = popupPanelB + popupSlotH * 2.2f;
            if (y < above || y > below) return -1;
            return nearestByIndex(x, true);
        } else {
            float left = popupPanelL - popupSlotW * 2.2f;
            float right = popupPanelR + popupSlotW * 2.2f;
            if (x < left || x > right) return -1;
            return nearestByIndex(y, false);
        }
    }

    private int nearestByIndex(float v, boolean byX) {
        float best = Float.MAX_VALUE;
        int bi = -1;
        for (int i = 0; i < popupRects.size(); i++) {
            RectF r = popupRects.get(i);
            float c = byX ? r.centerX() : r.centerY();
            float dd = Math.abs(v - c);
            if (dd < best) { best = dd; bi = i; }
        }
        return bi;
    }

    private void commitPopupSlot(int sel) {
        if (downKey == null) return;
        if (sel == 0) {
            pressKey(downKey);
        } else if (sel < popupSlots.size()) {
            String s = popupSlots.get(sel);
            if (s != null && !s.isEmpty() && listener != null) listener.typeText(s);
            afterType(s);
            Fx.click(getContext(), prefs, this);
        }
    }

    /* ---------------- actions ---------------- */

    private void pressKey(KeyDef k) {
        if (k == null) return;
        Fx.click(getContext(), prefs, this);
        switch (k.kind) {
            case KeyDef.KIND_CHAR: {
                String out = k.output == null ? k.label : k.output;
                if (out.isEmpty()) out = k.label;
                if (shiftState != 0 && out.length() == 1) {
                    out = String.valueOf(Character.toUpperCase(out.charAt(0)));
                }
                if (listener != null) listener.typeText(out);
                afterType(out);
                break;
            }
            case KeyDef.KIND_SPACE:
                if (listener != null) listener.typeText(" ");
                afterType(" ");
                break;
            case KeyDef.KIND_TAB:
                if (listener != null) listener.typeText("\t");
                afterType("\t");
                break;
            case KeyDef.KIND_BACKSPACE:
                if (listener != null) listener.backspace();
                break;
            case KeyDef.KIND_ENTER:
                if (listener != null) listener.enter();
                shiftState = 0;
                invalidate();
                break;
            case KeyDef.KIND_SHIFT: {
                long now = SystemClock.uptimeMillis();
                if (shiftState == 2) {
                    shiftState = 0;
                } else if (shiftState == 1 && now - lastShiftPress < 450) {
                    shiftState = 2; // caps lock
                } else {
                    shiftState = 1;
                }
                lastShiftPress = now;
                invalidate();
                break;
            }
            case KeyDef.KIND_SYMBOLS:
                symbolsOn = !symbolsOn;
                shiftState = 0;
                rebuild();
                break;
            case KeyDef.KIND_SWITCH:
                if (listener != null) listener.switchIme();
                break;
        }
        invalidate();
    }

    private void afterType(String s) {
        if (shiftState == 1) shiftState = 0;
        if (prefs.autoCap() && s.length() == 1) {
            char c = s.charAt(0);
            if (c == '.' || c == '?' || c == '!' || c == ':') shiftState = 1;
        }
        invalidate();
    }

    private String displayLabel(KeyDef k) {
        if (k.kind == KeyDef.KIND_CHAR && shiftState != 0) {
            if (k.label != null && k.label.length() == 1) {
                char u = Character.toUpperCase(k.label.charAt(0));
                if (u != k.label.charAt(0)) return String.valueOf(u);
            }
        }
        return k.label == null ? "" : k.label;
    }

    private float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /* ---------------- drawing ---------------- */

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = Math.round(padTop + padBottom + rows.size() * rowHeight + Math.max(0, rows.size() - 1) * gap);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        relayout();
    }

    @Override
    protected void onDraw(Canvas c) {
        if (ui == null) return;
        c.drawColor(ui.bg);
        paint.setStyle(Paint.Style.FILL);

        for (int r = 0; r < keyRects.size(); r++) {
            float[] a = keyRects.get(r);
            List<KeyDef> row = rows.get(r);
            for (int i = 0; i < row.size(); i++) {
                KeyDef k = row.get(i);
                float l = a[i * 4], t = a[i * 4 + 1], rr = a[i * 4 + 2], b = a[i * 4 + 3];
                paint.setColor(keyColor(k, r, i));
                RectF rect = new RectF(l, t, rr, b);
                c.drawRoundRect(rect, corner, corner, paint);

                // subtle border
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(1));
                paint.setColor(ui.keyBorder);
                c.drawRoundRect(rect, corner, corner, paint);
                paint.setStyle(Paint.Style.FILL);

                drawHintDot(c, k, rr, t);
                drawKeyLabel(c, k, rect);
            }
        }

        if (popupMode && !popupRects.isEmpty()) {
            // scrim
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(ui.scrim);
            c.drawRect(0, 0, getWidth(), getHeight(), paint);

            // panel backdrop
            float p = popupAnim;
            RectF panel = new RectF(popupPanelL, popupPanelT, popupPanelR, popupPanelB);
            if (p < 1f) {
                float cx = panel.centerX(), cy = panel.centerY();
                float s = 0.7f + 0.3f * p;
                RectF grow = new RectF(cx - (cx - panel.left) * s, cy - (cy - panel.top) * s,
                        cx + (panel.right - cx) * s, cy + (panel.bottom - cy) * s);
                panel = grow;
            }
            paint.setColor(ui.dark ? 0xFF1E1F26 : 0xFFFFFFFF);
            c.drawRoundRect(panel, dp(10), dp(10), paint);

            // slots
            for (int i = 0; i < popupRects.size(); i++) {
                RectF sr = popupRects.get(i);
                RectF dr = scaleRect(sr, panel);
                boolean sel = (i == popupSel);
                paint.setColor(sel ? ui.accent : (ui.dark ? 0xFF2A2B33 : 0xFFF0F1F5));
                c.drawRoundRect(dr, dp(7), dp(7), paint);
                tp.setColor(sel ? 0xFFFFFFFF : ui.text);
                tp.setTextSize(sp(15) * fontScale);
                tp.setTypeface(ui.bold());
                tp.setTextAlign(Paint.Align.CENTER);
                String s = popupSlots.get(i);
                Paint.FontMetrics fm = tp.getFontMetrics();
                float baseline = dr.centerY() - (fm.ascent + fm.descent) / 2f;
                c.drawText(s, dr.centerX(), baseline, tp);
            }
        }
    }

    private RectF scaleRect(RectF src, RectF panel) {
        return new RectF(panel.left + (src.left - popupPanelL), panel.top + (src.top - popupPanelT),
                panel.right + (src.right - popupPanelR), panel.bottom + (src.bottom - popupPanelB));
    }

    private int keyColor(KeyDef k, int r, int i) {
        if (downKey == k && tracking && !popupMode) {
            if (k.kind == KeyDef.KIND_SHIFT && (shiftState == 2)) return ui.accent;
            return ui.keyPressed;
        }
        if (k.kind == KeyDef.KIND_SHIFT && shiftState != 0) return ui.accent;
        if (k.isSpecial()) return ui.keyFunc;
        return ui.key;
    }

    private void drawHintDot(Canvas c, KeyDef k, float right, float top) {
        if (k.popupEnabled && k.popup != null && !k.popup.isEmpty() && !popupMode && !k.isSpecial()) {
            float r = dp(2.2f);
            paint.setColor((ui.accent & 0x00FFFFFF) | 0xB0000000);
            paint.setAlpha(170);
            c.drawCircle(right - dp(7), top + dp(7), r, paint);
            paint.setAlpha(255);
        }
    }

    private String specialGlyph(KeyDef k) {
        switch (k.kind) {
            case KeyDef.KIND_SHIFT: return "\u21E7";
            case KeyDef.KIND_BACKSPACE: return "\u232B";
            case KeyDef.KIND_ENTER: return "\u23CE";
            default: return null;
        }
    }

    private void drawKeyLabel(Canvas c, KeyDef k, RectF rect) {
        if (k.kind == KeyDef.KIND_SPACE) return;
        String text;
        String glyph = specialGlyph(k);
        if (glyph != null) {
            text = glyph;
            tp.setColor(shiftState != 0 && k.kind == KeyDef.KIND_SHIFT ? 0xFFFFFFFF : ui.textFunc);
            tp.setTextSize(sp(16) * fontScale);
        } else {
            text = displayLabel(k);
            tp.setColor(k.kind == KeyDef.KIND_CHAR ? ui.text : ui.textFunc);
            tp.setTextSize(sp(15) * fontScale);
        }
        tp.setTypeface(ui.bold());
        tp.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = tp.getFontMetrics();
        float baseline = rect.centerY() - (fm.ascent + fm.descent) / 2f;
        c.drawText(text, rect.centerX(), baseline, tp);
    }

    private float sp(float v) {
        return v * getResources().getDisplayMetrics().scaledDensity;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacksAndMessages(null);
        if (popupAnimator != null) { popupAnimator.cancel(); popupAnimator = null; }
    }
}
