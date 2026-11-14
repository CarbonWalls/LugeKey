package net.carbonwalls.lugekey;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import java.util.ArrayDeque;
import java.util.Deque;

/** The LugeKey input method. Hosts KeyboardView, commits edits, tracks undo + clipboard. */
public class LugeIME extends InputMethodService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "LugeKey";
    private static final int MAX_UNDO = 200;

    private KeyboardView keyboardView;
    private Prefs prefs;
    private final Handler handler = new Handler();
    private final Deque<String> undoStack = new ArrayDeque<>();
    private ClipboardManager clipboard;
    private final ClipboardManager.OnPrimaryClipChangedListener clipListener = () -> {
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData cd = clipboard.getPrimaryClip();
            if (cd != null && cd.getItemCount() > 0 && cd.getItemAt(0).getText() != null) {
                ClipboardHistory.add(this, cd.getItemAt(0).getText().toString());
            }
        }
    };

    @Override
    public boolean onEvaluateFullscreenMode() {
        // We draw our own minimal keyboard; never expand into the giant
        // fullscreen IME window with an empty candidate area.
        return false;
    }

    @Override
    public View onCreateInputView() {
        prefs = new Prefs(this);
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        try {
            if (keyboardView == null) {
                keyboardView = new KeyboardView(this);
                keyboardView.setListener(new KeyboardView.Listener() {
                    @Override public void typeText(String text) { type(text, true); }
                    @Override public void backspace() { sendKey(KeyEvent.KEYCODE_DEL); }
                    @Override public void enter() { sendKey(KeyEvent.KEYCODE_ENTER); }
                    @Override public void tab() { sendKey(KeyEvent.KEYCODE_TAB); }
                    @Override public void deleteWord() { deleteWord(); }
                    @Override public void undo() { undo(); }
                    @Override public void cursorLeft() { sendKey(KeyEvent.KEYCODE_DPAD_LEFT); }
                    @Override public void cursorRight() { sendKey(KeyEvent.KEYCODE_DPAD_RIGHT); }
                    @Override public void home() { sendKey(KeyEvent.KEYCODE_MOVE_HOME); }
                    @Override public void end() { sendKey(KeyEvent.KEYCODE_MOVE_END); }
                    @Override public void switchIme() { switchToNextInputMethod(false); }
                    @Override public void openSettings() {
                        Intent i = new Intent(LugeIME.this, SettingsActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try { startActivity(i); } catch (Exception e) { Log.e(TAG, "openSettings", e); }
                    }
                    @Override public void paste(String text) {
                        commit(text);
                        if (clipboard != null) {
                            try {
                                clipboard.setPrimaryClip(ClipData.newPlainText("LugeKey", text));
                            } catch (Exception ignored) {}
                        }
                    }
                });
            } else {
                keyboardView.rebuild();
            }
        } catch (Throwable t) {
            Log.e(TAG, "onCreateInputView failed", t);
        }
        return keyboardView;
    }

    /** Normal committed text. Undo-tracked; optionally remembered as a clip. */
    private void type(String text, boolean remember) {
        commit(text);
        if (remember && prefs != null && prefs.saveTyped()) {
            String t = text.trim();
            if (t.length() >= 2 && !t.startsWith("\n")) ClipboardHistory.add(this, t);
        }
    }

    private void commit(String text) {
        if (text == null || text.isEmpty()) return;
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.commitText(text, 1);
        pushUndo(text);
    }

    private void pushUndo(String text) {
        if (text == null || text.isEmpty()) return;
        undoStack.addLast(text);
        while (undoStack.size() > MAX_UNDO) undoStack.removeFirst();
    }

    private void undo() {
        if (undoStack.isEmpty()) return;
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        String s = undoStack.removeLast();
        try {
            ic.deleteSurroundingText(s.length(), 0);
        } catch (Exception ignored) {}
    }

    private void deleteWord() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        try {
            CharSequence beforeCs = ic.getTextBeforeCursor(1024, 0);
            if (beforeCs == null) return;
            String before = beforeCs.toString();
            int i = before.length();
            if (i == 0) return;
            while (i > 0 && Character.isWhitespace(before.charAt(i - 1))) i--;
            while (i > 0 && !Character.isWhitespace(before.charAt(i - 1))) i--;
            int count = before.length() - i;
            if (count > 0) ic.deleteSurroundingText(count, 0);
        } catch (Exception ignored) {}
    }

    private void sendKey(int code) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        try {
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
        } catch (Exception ignored) {}
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        if (keyboardView != null) keyboardView.resetState();
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (keyboardView != null) keyboardView.resetState();
    }

    @Override
    public void onWindowShown() {
        super.onWindowShown();
        if (prefs == null) prefs = new Prefs(this);
        prefs.raw().registerOnSharedPreferenceChangeListener(this);
        if (clipboard == null) clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            try { clipboard.addPrimaryClipChangedListener(clipListener); } catch (Exception ignored) {}
        }
    }

    @Override
    public void onWindowHidden() {
        super.onWindowHidden();
        if (prefs != null) prefs.raw().unregisterOnSharedPreferenceChangeListener(this);
        if (clipboard != null) {
            try { clipboard.removePrimaryClipChangedListener(clipListener); } catch (Exception ignored) {}
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (keyboardView != null && isInputViewShown()) keyboardView.rebuild();
    }
}
