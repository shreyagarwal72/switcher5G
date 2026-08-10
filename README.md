# Switcher5G

Standalone Kotlin/Compose app that switches cellular network mode (5G SA / 5G NSA / LTE)
via Shizuku, with a Quick Settings tile and a Termux/ADB entry point.

## What's actually implemented vs. what needs on-device verification

**Solid / standard:**
- Compose UI, theme, the three custom components (slider, liquid progress, floating nav)
- Gradle/module structure, Manifest wiring, QS Tile, deep link, Termux receiver

**Needs testing on real hardware — this is reflection against hidden framework
methods, not public API:**
- `NetworkModeUserService.setNetworkMode()` calls `TelephonyManager.setAllowedNetworkTypesForReason()`
  reflectively. It's a real AOSP `@SystemApi` method, but:
  - OEM skins (OneUI, HyperOS, ColorOS, MIUI) have a history of altering or
    additionally gating telephony SystemApis. Test per-OEM before trusting it.
  - `TelephonyManager.getDefault()` (used to obtain a base instance without a
    Context) is itself a hidden method reached via reflection — brittle across
    Android versions. If it breaks, the fix is passing a real `Context` into
    the Shizuku user service instead (Shizuku supports this via a custom
    `Parcel` in `UserServiceArgs`, not shown here to keep the scaffold readable).
  - I have not run or compiled this — there's no Android SDK/emulator in the
    environment I built it in. Treat the reflection call sites as the first
    place to debug if `switchTo()` returns a Failure.

## Setup

1. Open in Android Studio (Koala+), let it sync — it will offer to generate
   the Gradle wrapper jar if missing.
2. Install/enable **Shizuku** on the target device (either pair over ADB
   wireless debugging, or run it via root if the device is rooted).
3. Build & install the app, open it, tap "Grant Shizuku permission."
4. Select a mode, tap Apply.

## Termux usage

```bash
# Requires the device to have Shizuku already running — Termux itself does
# not need root, it just needs to be able to run `am broadcast`.
am broadcast -a com.app.switcher5g.SET_NETWORK_MODE --es mode "NR_ONLY"
am broadcast -a com.app.switcher5g.SET_NETWORK_MODE --es mode "NR_LTE"
am broadcast -a com.app.switcher5g.SET_NETWORK_MODE --es mode "LTE_ONLY"

# Deep link (opens the app then applies the mode):
am start -a android.intent.action.VIEW -d "switcher5g://switch?mode=NR_ONLY"
```

`am broadcast` from Termux only works without extra setup if Termux is running
plain shell (not `su`) — it relies on the `shell` UID being allowed to hit a
`signature|privileged`-protected receiver, which is standard AOSP behavior,
not something this app grants.

## Security notes

- The `SET_NETWORK_MODE` permission tag is `signature|privileged`, which
  blocks ordinary third-party apps from broadcasting to `TermuxReceiver` —
  they'd need to be signed with your platform key or be a privileged system
  app, which nothing on a normal device is. Shell (adb/Termux) bypasses this
  by design, same as other privileged broadcasts on stock Android.
- Nobody but you can grant Shizuku permission to this app — that's a
  device-local, per-app grant, not something remote.

## Known gaps / things I did not build

- No icon set beyond placeholder vector drawables — swap in real launcher/tile
  icons before shipping.
- No per-OEM bitmask fallback table — if `setAllowedNetworkTypesForReason`
  is blocked outright on a given skin, there's currently no secondary path
  (e.g. falling back to `RadioInfo` intent) other than the error surfaced to
  the user.
- Not wired into Rootless Android Tweaker as a module, since you asked for
  standalone — the network/tile/receiver packages are structured so you could
  lift them into that project later if you change your mind again.
