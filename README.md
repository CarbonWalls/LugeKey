# LugeKey — a minimal, fully customizable Android keyboard

A blend of two ideas:

- **Futo Keyboard** styling — flat, compact, clean, modern, distraction-free.
- **CleverKeys**-style customization — every key is fully editable: what it
  types, what it shows, and what extra characters live behind it.

And the headline interaction difference:

> **Swipe a key** (default: swipe **up**) and its extra-character popup opens
> **instantly**. No long-press required.

Swipe direction is configurable per key (up / down / left / right / none), plus
a global default, and (optionally) a long-press fallback if you still want it.

## Screenshots

![Settings](screenshots/settings.png)

## What it does

- Full Android **IME** (`InputMethodService`) — enable it in
  *Settings → System → Languages & input → On-screen keyboard*.
- QWERTY + symbols layers out of the box, with sensible accents/symbols behind
  the letters (à é ü …).
- **Popup on swipe**: swipe up on `a` → the `á à â ä …` fan appears at once;
  slide to a character and release to type it.
- **Key editor** (Settings): change any key's *output text*, its *label*,
  its *popup characters*, its *swipe direction*, and its *kind*
  (letter, space, backspace, enter, shift, symbols toggle, switch IME, tab).
- Add/remove **rows** and **keys**, per layer (letters & symbols).
- Flat minimal theme with **light/dark**, accent color, keyboard height, key
  text size, top number row, key click sound, haptics, auto-capitalisation.
- Hold **⌫** to repeat. Tap **⇧** for one capital, double-tap for caps lock.

## Build (no Gradle — just the plain SDK tools)

The project builds with the Android SDK command-line tools directly
(`aapt2` → `javac` → `d8` → `zipalign` → `apksigner`). No wrapper, no
dependency downloads.

Prereqs: Android Studio (or cmdline-tools) with SDK platforms 35 and
build-tools 35.0.0 installed.

```powershell
.\tools\build.ps1
# output: LugeKey-0.1.5.apk  (signed with tools/debug.keystore, runs on any ABI incl. arm64/aarch64)
```

`minSdk 26` (Android 8.0+), `targetSdk 35`.

## Project layout

```
src/net/carbonwalls/lugekey/
  LugeIME.java        InputMethodService — hosts the keyboard, commits input
  KeyboardView.java   rendering + gestures + the swipe popup fan
  KeyDef.java         the editable key model
  LayoutStore.java    default layouts + JSON persistence
  Prefs.java          settings + layout prefs
  Theme.java          flat colour palettes
  Fx.java             click sound + haptics
  SettingsActivity.java  settings + the visual layout editor, built in code
tools/build.ps1       offline build script
```

## Notes

- The APK contains no native code, so it runs on every ABI, including
  `arm64-v8a` (aarch64).
- The debug signing key is generated on first build and committed so
  re-installs keep the same signature.
