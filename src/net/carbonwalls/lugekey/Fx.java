package net.carbonwalls.lugekey;

import android.content.Context;
import android.media.AudioManager;
import android.view.HapticFeedbackConstants;
import android.view.View;

/** Small feel helpers: key click sound + haptics. */
public final class Fx {

    private Fx() {}

    public static void click(Context c, Prefs prefs, View anchor) {
        if (anchor == null) return;
        if (prefs.sound()) {
            try {
                AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
                if (am != null) am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
            } catch (Exception ignored) {}
        }
        if (prefs.haptic()) {
            try {
                anchor.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
            } catch (Exception ignored) {}
        }
    }
}
