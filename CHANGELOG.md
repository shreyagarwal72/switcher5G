# Changelog

All notable changes to **Switcher 5G** are documented in this file.

---

## [1.0.5] - 2026-09-19

### 🎨 Material 3 Expressive UI & Petal Integration
* **Petal Floating Navigation Bar**: Bottom navigation is now a direct port of Petal's `PetalBottomNavBar` (floating style): Material 3 Expressive `HorizontalFloatingToolbar` with a vibrant `surfaceContainer` container, primary-tinted shadow, Petal's gradient outline, spring-expanding `primaryContainer` pill with label reveal (`0.dp` to `72.dp`), bouncy press feedback and icon scale/rotation pops. The bar handles the navigation-bar inset itself, like in Petal.
* **Petal Pull-to-Refresh**: Rebuilt from Petal's refresh behaviour: the same drag maths and thresholds as `PullToRefreshFrameLayout` (80dp pull distance, 0.55 damping, release past 70% refreshes, haptic tick at 75%) driving Petal's real `RefreshBarLoadingIndicator` with the Material 3 Expressive `ContainedLoadingIndicator`. Fires exactly once per pull, retracts correctly when dragging back up, ignores multi-touch, sits below the status bar and stays up until the refresh actually finishes.
* **Petal Circular Wavy Loader**: Every circular spinner (SIM scan, Apply Network Mode, update check/download, Shizuku status checks, switching overlay) now uses Petal's `PetalCircularWavyProgressIndicator` (Material 3 Expressive `CircularWavyProgressIndicator`) instead of the plain rotating ring.
* **Petal Website Loading Linear Wavy Progress Bar**: Replaced the linear loading indicator with Petal's exact website loading progress bar (`PetalFancyWebLoadingBar` powered by AndroidX Material 3 Expressive `LinearWavyProgressIndicator`), supporting both determinate and indeterminate wavy progress across refresh actions, SIM scanning, and APK update downloads.
* **Borderless Material 3 Expressive Containments**:
  * Removed all 1dp stroke borders across all surfaces and cards (`ElevatedCard`, `Surface`, dialogs, setup banners, SIM subscription cards, and mode sliders).
  * Standardized surface hierarchies using `PetalContainments` defaults (`shape = 24.dp`, tonal elevation, and expressive padding).

### 🛠 Toolchain
* Aligned with Petal Browser's toolchain so its Material 3 Expressive components can be used as-is: Kotlin 2.0.21 with the Compose compiler Gradle plugin, AGP 8.9.3, Gradle 8.11.1, Compose BOM 2026.06.01, Material 3 1.5.0-alpha17, compileSdk 36.

### 🐛 Bug Fixes & Platform Compatibility
* **Airtel & Vodafone Idea (VI) 5G NSA/SA & 4G Switching Fix**:
  * Fixed 5G NSA and 4G switching failures on Airtel and VI SIM cards by including carrier-specific preferred network types (mode 26 `NR_LTE_GSM_WCDMA`, mode 9 `LTE_GSM_WCDMA`, mode 27, 24, 23, and 33).
  * Ensured allowed network types bitmask includes all legacy 2G/3G/4G bands along with NR, preventing connection drops on Indian non-standalone (Option 3x) cellular networks.
  * Synchronized settings updates across `preferred_network_mode$subId`, `preferred_network_mode`, `preferred_network_mode1`, and `preferred_network_mode2`.
* **Fix Pull to Refresh**:
  * Pull tracking uses `Modifier.nestedScroll` (no gesture cancellation against the scrollable home screen) and now consumes only the distance it uses, releases reliably on finger-up and can no longer trigger a double refresh or get stuck.
* **Fix Issue #2: Samsung Galaxy S25 Ultra (One UI 8.5) Shizuku Switching**:
  * Enhanced `NetworkModeUserService` to handle Samsung Knox / One UI 8.5 telephony binder constraints.
  * Added Samsung-specific shell fallbacks (`cmd phone set-preferred-network-type`, `--sub` and `-s` subscription targeting, allowed network type reason masks, and `preferred_network_mode_sub*` global settings).
  * Expanded AIDL/IPC exception handling with automatic fallback to system telephony selection modes.
* **Fix Issue #3: Quick Settings Tile Navigation & Band Locking**:
  * Quick Settings tile now directly opens/navigates into the Switcher 5G app via `startActivityAndCollapse` (Android 14+ `PendingIntent` and Android 7–13 `Intent`).
  * Double-tap on the Quick Settings tile triggers the hardware RadioInfo and Band Selection menu.
  * Added dedicated **Lock Bands** shortcut button on the Home Screen and in `Manual5gSwitchHelper` supporting Samsung `ServiceModeApp`, AOSP `BandMode`, and Qualcomm/MediaTek band configuration activities.

---

## [1.0.2] - 2026-09-01
* Initial stable release with Shizuku & Root network mode switching.
* Dual-SIM slot management.
* Quick settings tiles & home screen widget.
