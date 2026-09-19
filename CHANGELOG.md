# Changelog

All notable changes to **Switcher 5G** are documented in this file.

---

## [1.0.5] - 2026-09-19

### 🎨 Material 3 Expressive UI & Petal Integration
* **Petal Floating Navigation Bar**: Recreated the bottom navigation bar with Petal's `HorizontalFloatingToolbar` featuring animated `FloatingNavTabItem` pill spring physics, smooth label expansion (`0.dp` to `72.dp`), tactile press feedback scaling (`0.90f`), and borderless `surfaceContainer` containment.
* **Elastic Pull to Refresh Container**: Integrated `ExpressivePullToRefreshBox` with Petal Browser's `ExpressivePullToRefreshWaterRipple` overscroll physics, offering real-time touch pull-down response with dynamic water ripple animations.
* **Linear Rippling Wavy Progress Loader**: Replaced the linear wavy progress indicator with Petal's exact continuous sine-wave canvas loader (`LinearRipplingWavyProgressIndicator`) featuring 4.5dp height, smooth 1100ms cycle animations, and dual-tone shimmer gradients.
* **Borderless Material 3 Expressive Containments**:
  * Removed all 1dp stroke borders across all surfaces and cards (`ElevatedCard`, `Surface`, dialogs, setup banners, SIM subscription cards, and mode sliders).
  * Standardized surface hierarchies using `PetalContainments` defaults (`shape = 24.dp`, tonal elevation, and expressive padding).

### 🐛 Bug Fixes & Platform Compatibility
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
