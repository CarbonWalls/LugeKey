package net.carbonwalls.lugekey;

import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

/** The LugeKey input method. Hosts KeyboardView and commits edits. */
public class LugeIME extends InputMethodService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "LugeKey";

    private KeyboardView keyboardView;
    private Prefs prefs;

    @Override
    public boolean onEvaluateFullscreenMode() {
        // We draw our own minimal keyboard; never expand into the giant
        // fullscreen IME window with an empty candidate area.
        return false;
    }

    @Override
    public View onCreateInputView() {
        prefs = new Prefs(this);
        try {
            if (keyboardView == null) {
                keyboardView = new KeyboardView(this);
                keyboardView.setListener(new KeyboardView.Listener() {
                    @Override public void typeText(String text) {
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) ic.commitText(text, 1);
                    }
                    @Override public void backspace() { sendKey(KeyEvent.KEYCODE_DEL); }
                    @Override public void enter() { sendKey(KeyEvent.KEYCODE_ENTER); }
                    @Override public void switchIme() { switchToNextInputMethod(false); }
                });
            } else {
                keyboardView.rebuild();
            }
        } catch (Throwable t) {
            Log.e(TAG, "onCreateInputView failed", t);
        }
        return keyboardView;
    }

    private void sendKey(int code) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
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
    }

    @Override
    public void onWindowHidden() {
        super.onWindowHidden();
        if (prefs != null) prefs.raw().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (keyboardView != null && isInputViewShown()) keyboardView.rebuild();
    }
}
