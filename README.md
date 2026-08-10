# Switcher 5G

<div align="center">

  <h3>5G Standalone (SA), 5G Non-Standalone (NSA), and 4G LTE Network Switcher for Android</h3>

  [![Download APK](https://img.shields.io/badge/Download_APK-GitHub_Releases-2ea44f?style=for-the-badge&logo=github&logoColor=white)](https://github.com/shreyagarwal72/switcher5G/releases/latest)
  [![Latest Release](https://img.shields.io/github/v/release/shreyagarwal72/switcher5G?color=0075ff&style=for-the-badge)](https://github.com/shreyagarwal72/switcher5G/releases/latest)
  [![Build Status](https://img.shields.io/github/actions/workflow/status/shreyagarwal72/switcher5G/build.yml?branch=main&style=for-the-badge)](https://github.com/shreyagarwal72/switcher5G/actions)
  [![License](https://img.shields.io/github/license/shreyagarwal72/switcher5G?color=purple&style=for-the-badge)](LICENSE)

  <br />

  <a href="https://github.com/shreyagarwal72/switcher5G/releases/latest">
    <img src="https://img.shields.io/badge/Download_Latest_Release_APK-1.0.0-green?style=for-the-badge&logo=android" alt="Download APK Button" />
  </a>

</div>

---

## Key Features

* **1-Tap Network Mode Switching**: Switch instantly between **5G SA (NR Only)**, **5G NSA (NR / LTE)**, and **4G LTE Only** without root access.
* **System Testing Menu Resolver**: Native fallback resolution for hidden testing activities across OEM devices (AOSP `RadioInfo`, `TestingSettings`, Qualcomm `MobileNetworkSettings`, MediaTek `EngineerMode`, Samsung `ServiceModeApp`).
* **Scalloped Stride Slider Controls**: Rolling scalloped slider controls with physical rotation dynamics for network mode selection, typography, color palettes, and color styles.
* **Dynamic Theming & Customization**:
  * 10 curated color palettes (Default, M3 Expressive, Violet, Emerald, Crimson, Sunset, Oceanic, Cyberpunk, Amber Gold, Midnight).
  * 9 Material You color styles (Tonal Spot, Neutral, Vibrant, Expressive, Rainbow, Fruit Salad, Monochrome, Fidelity, Content).
  * Dynamic wallpaper colors (Android 12+ Material You).
  * Pure AMOLED black mode (`#000000`).
  * 7 live typography fonts (System, Nunito, Inter, Outfit, Lexend, Manrope, Space Grotesk).
* **Refresh Progress Bar**: Rippling sine wave progress indicator (`LinearRipplingWavyProgressIndicator`) for pull-to-refresh, initial load, and network telemetry updates.
* **Backup & Restore Settings**: Export and import all app preferences in structured JSON format.
* **Automated Update Tracker**: Checks for release APK updates directly from GitHub Releases API with in-app 1-tap download and installation.
* **Quick Settings Tile & Deep Links**: Toggle network mode directly from Android status bar quick settings tiles or command-line broadcasts.

---

## Download

Get the latest signed release APK directly from GitHub Releases:

<div align="center">

| Release | Download Link | Type |
| :--- | :--- | :--- |
| **v1.0.0 (Initial Stable Release)** | [**Download switcher5g-release.apk**](https://github.com/shreyagarwal72/switcher5G/releases/latest) | Signed Production APK |

</div>

---

## Setup Instructions

### Option 1: Shizuku Service (Recommended — No Root Required)
1. Install and open the **[Shizuku App](https://shizuku.rikka.app/)**.
2. Start Shizuku via **Wireless Debugging** (Android 11+) or via PC terminal (`adb shell`).
3. Open **Switcher 5G** and tap **Request Shizuku Permission**.
4. Select your preferred network mode and tap **Apply**.

### Option 2: Manual 5G Switch (No Shizuku / No Root Required)
1. Tap **Manual 5G Switch (System RadioInfo)** on the Home screen.
2. Under **Set Preferred Network Type**, select `NR only` (5G SA), `NR/LTE` (5G NSA), or `LTE only` (4G).

---

## Command Line & Deep Link Triggers

Broadcast intent triggers from Termux or ADB terminal:

```bash
# Force 5G SA (NR Only)
am broadcast -a com.app.switcher5g.SET_NETWORK_MODE --es mode "NR_ONLY"

# Force 5G NSA (NR / LTE)
am broadcast -a com.app.switcher5g.SET_NETWORK_MODE --es mode "NR_LTE"

# Force 4G LTE Only
am broadcast -a com.app.switcher5g.SET_NETWORK_MODE --es mode "LTE_ONLY"

# Deep Link Trigger
am start -a android.intent.action.VIEW -d "switcher5g://switch?mode=NR_ONLY"
```

---

## Security & Signing Key

All official release builds are compiled using a permanent, locked cryptographic signing key. This guarantees that all future updates published on GitHub Releases can be installed seamlessly over existing installations without encountering signature mismatch errors.

---

## Acknowledgements & Credits

Special thanks to the following open-source projects and developers:

* **[Shizuku](https://github.com/RikkaApps/Shizuku)** by RikkaApps for system service IPC binder infrastructure.
* **[Stride](https://github.com/NikhilKain/stride)** by NikhilKain for slider design inspiration.
* **[5G](https://github.com/OpenAppsLabs/5G)** by OpenAppsLabs for system testing menu fallback research.
* **[Android-HiddenApiBypass](https://github.com/LSPosed/Android-HiddenApiBypass)** by LSPosed for system API reflection bypass.

---

## License

Distributed under the MIT License. See `LICENSE` for details.
