/* =====================================================================
   changelog.js — renders CHANGELOG.md as release cards.
   Source of truth: /assets/CHANGELOG.md (fetched). If the fetch fails
   (offline, file://), the embedded copy below is used instead.
   To update: replace the contents of assets/CHANGELOG.md. No build step.
   ===================================================================== */
(function () {
  "use strict";
  var EMBEDDED = "# Changelog\n\nAll notable changes to **Switcher 5G** are documented in this file.\n\n---\n\n## [2.0.0] - 2026-10-24\n\n### \ud83c\udfa8 Material 3 Expressive & Petal Redesign\n* **Petal Collapsing Header**: New collapsing header across every screen (`PetalHeader` / `PetalScreenScaffold`) with smooth back-button morphing, fluid scroll physics, and animated screen titles.\n* **Grouped Containment Architecture**: Replaced standalone cards with connected list rows featuring modern rounded outer corners (`28dp` top/bottom, `6dp` inner) and Material 3 Expressive shaped icon badges (`PetalGroupRow`, `PetalGroupToggleRow`, `PetalGroupControlRow`, `PetalSection`).\n* **Connected Choice Toggle Groups**: Replaced old filter chips, radio sliders, and dropdowns with connected choice toggle button groups (`PetalChoiceButtonGroup` / `PetalButtonGroupItem`) across all settings (activation mode, theme selection, font picker, and network modes).\n* **Predictive Back Navigation & Live Gesture Transitions**:\n  * Implemented `PetalPredictiveHost` with real-time gesture tracking, progressive scaling, corner radius clipping, and back-stack morphing.\n  * Added optional depth blur & scrim dimming for underlying screens during back swipes (customizable via Settings).\n* **Unified Petal Loading Indicators**:\n  * Streamlined all loaders across the entire application into Petal's contained loading indicator (`PetalContainedLoadingIndicator` & `PetalWebsiteLoadingIndicator`).\n  * Removed legacy uncontained progress bars and redundant circular spinner components.\n* **Expressive Buttons & Dialog Overhauls**:\n  * Replaced stock Material buttons with tactile `PetalButton` components supporting primary, tonal, secondary, and destructive styles.\n  * Modernized `ShizukuSetupDialog`, `Manual5gFirstTimeDialog`, `CellularSignalMonitorCard`, and `UpdateAvailableDialog` to match the expressive design system.\n* **Settings & Preferences**: Added toggles for predictive back navigation and depth blur animation with full JSON export/import backup support.\n\n---\n\n## [1.0.5] - 2026-09-19\n\n### \ud83c\udfa8 Material 3 Expressive UI & Petal Integration\n* **Petal Floating Navigation Bar**: Bottom navigation is now a direct port of Petal's `PetalBottomNavBar` (floating style): Material 3 Expressive `HorizontalFloatingToolbar` with a vibrant `surfaceContainer` container, primary-tinted shadow, Petal's gradient outline, spring-expanding `primaryContainer` pill with label reveal (`0.dp` to `72.dp`), bouncy press feedback and icon scale/rotation pops. The bar handles the navigation-bar inset itself, like in Petal.\n* **Petal Pull-to-Refresh**: Rebuilt from Petal's refresh behaviour: the same drag maths and thresholds as `PullToRefreshFrameLayout` (80dp pull distance, 0.55 damping, release past 70% refreshes, haptic tick at 75%) driving Petal's real `RefreshBarLoadingIndicator` with the Material 3 Expressive `ContainedLoadingIndicator`. Fires exactly once per pull, retracts correctly when dragging back up, ignores multi-touch, sits below the status bar and stays up until the refresh actually finishes.\n* **Petal Circular Wavy Loader**: Every circular spinner (SIM scan, Apply Network Mode, update check/download, Shizuku status checks, switching overlay) now uses Petal's `PetalCircularWavyProgressIndicator` (Material 3 Expressive `CircularWavyProgressIndicator`) instead of the plain rotating ring.\n* **Petal Website Loading Linear Wavy Progress Bar**: Replaced the linear loading indicator with Petal's exact website loading progress bar (`PetalFancyWebLoadingBar` powered by AndroidX Material 3 Expressive `LinearWavyProgressIndicator`), supporting both determinate and indeterminate wavy progress across refresh actions, SIM scanning, and APK update downloads.\n* **Borderless Material 3 Expressive Containments**:\n  * Removed all 1dp stroke borders across all surfaces and cards (`ElevatedCard`, `Surface`, dialogs, setup banners, SIM subscription cards, and mode sliders).\n  * Standardized surface hierarchies using `PetalContainments` defaults (`shape = 24.dp`, tonal elevation, and expressive padding).\n\n### \ud83d\udee0 Toolchain\n* Aligned with Petal Browser's toolchain so its Material 3 Expressive components can be used as-is: Kotlin 2.0.21 with the Compose compiler Gradle plugin, AGP 8.9.3, Gradle 8.11.1, Compose BOM 2026.06.01, Material 3 1.5.0-alpha17, compileSdk 36.\n\n### \ud83d\udc1b Bug Fixes & Platform Compatibility\n* **Airtel & Vodafone Idea (VI) 5G NSA/SA & 4G Switching Fix**:\n  * Fixed 5G NSA and 4G switching failures on Airtel and VI SIM cards by including carrier-specific preferred network types (mode 26 `NR_LTE_GSM_WCDMA`, mode 9 `LTE_GSM_WCDMA`, mode 27, 24, 23, and 33).\n  * Ensured allowed network types bitmask includes all legacy 2G/3G/4G bands along with NR, preventing connection drops on Indian non-standalone (Option 3x) cellular networks.\n  * Synchronized settings updates across `preferred_network_mode$subId`, `preferred_network_mode`, `preferred_network_mode1`, and `preferred_network_mode2`.\n* **Fix Pull to Refresh**:\n  * Pull tracking uses `Modifier.nestedScroll` (no gesture cancellation against the scrollable home screen) and now consumes only the distance it uses, releases reliably on finger-up and can no longer trigger a double refresh or get stuck.\n* **Fix Issue #2: Samsung Galaxy S25 Ultra (One UI 8.5) Shizuku Switching**:\n  * Enhanced `NetworkModeUserService` to handle Samsung Knox / One UI 8.5 telephony binder constraints.\n  * Added Samsung-specific shell fallbacks (`cmd phone set-preferred-network-type`, `--sub` and `-s` subscription targeting, allowed network type reason masks, and `preferred_network_mode_sub*` global settings).\n  * Expanded AIDL/IPC exception handling with automatic fallback to system telephony selection modes.\n* **Fix Issue #3: Quick Settings Tile Navigation & Band Locking**:\n  * Quick Settings tile now directly opens/navigates into the Switcher 5G app via `startActivityAndCollapse` (Android 14+ `PendingIntent` and Android 7\u201313 `Intent`).\n  * Double-tap on the Quick Settings tile triggers the hardware RadioInfo and Band Selection menu.\n  * Added dedicated **Lock Bands** shortcut button on the Home Screen and in `Manual5gSwitchHelper` supporting Samsung `ServiceModeApp`, AOSP `BandMode`, and Qualcomm/MediaTek band configuration activities.\n\n---\n\n## [1.0.2] - 2026-09-01\n* Initial stable release with Shizuku & Root network mode switching.\n* Dual-SIM slot management.\n* Quick settings tiles & home screen widget.\n";
  var mount = document.getElementById("releases"); if (!mount) return;
  var status = document.getElementById("changelog-status");

  function esc(s) { return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;"); }
  function inline(s) {
    return esc(s)
      .replace(/`([^`]+)`/g, "<code>$1</code>")
      .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
      .replace(/\[([^\]]+)\]\((https?:[^)\s]+)\)/g, '<a href="$2" rel="noopener">$1</a>');
  }
  function stripEmoji(s) { return s.replace(/^[^\p{L}\p{N}\[]+/u, "").trim(); }

  function parse(md) {
    var rel = [], cur = null, grp = null, stack = [];
    md.split(/\r?\n/).forEach(function (line) {
      var m;
      if ((m = line.match(/^##\s+\[?([^\]\s]+)\]?(?:\s*[-–]\s*(\d{4}-\d{2}-\d{2}))?\s*$/))) {
        cur = { version: m[1], date: m[2] || "", groups: [] }; rel.push(cur); grp = null; stack = []; return;
      }
      if (!cur) return;
      if ((m = line.match(/^###\s+(.*)$/))) { grp = { title: stripEmoji(m[1]), items: [] }; cur.groups.push(grp); stack = []; return; }
      if ((m = line.match(/^(\s*)[*-]\s+(.*)$/))) {
        if (!grp) { grp = { title: "Changes", items: [] }; cur.groups.push(grp); }
        var depth = Math.floor(m[1].length / 2), node = { text: m[2], kids: [] };
        if (depth === 0 || !stack.length) { grp.items.push(node); stack = [node]; }
        else { var parent = stack[Math.min(depth, stack.length) - 1]; parent.kids.push(node); stack[depth] = node; stack.length = depth + 1; }
      }
    });
    return rel.filter(function (r) { return r.groups.length; });
  }
  function list(items) {
    return "<ul>" + items.map(function (i) { return "<li>" + inline(i.text) + (i.kids.length ? list(i.kids) : "") + "</li>"; }).join("") + "</ul>";
  }
  function fmt(d) { try { return new Date(d + "T00:00:00").toLocaleDateString(undefined, { year: "numeric", month: "long", day: "numeric" }); } catch (e) { return d; } }
  function render(rels) {
    mount.innerHTML = rels.map(function (r, i) {
      return '<article class="release" aria-labelledby="v-' + esc(r.version) + '"><div class="release__head">' +
        '<h2 class="release__ver" id="v-' + esc(r.version) + '">' + (/^\d/.test(r.version) ? "v" : "") + esc(r.version) + "</h2>" +
        (i === 0 && /^\d/.test(r.version) ? '<span class="release__latest">Latest</span>' : "") +
        (r.date ? '<time datetime="' + esc(r.date) + '">' + esc(fmt(r.date)) + "</time>" : "") + "</div>" +
        r.groups.map(function (g) { return '<section class="release__group"><h3>' + inline(g.title) + "</h3>" + list(g.items) + "</section>"; }).join("") +
        "</article>";
    }).join("");
    mount.setAttribute("aria-busy", "false");
    if (status) status.remove();
  }
  function useEmbedded() { var r = parse(EMBEDDED); if (r.length) render(r); else mount.innerHTML = '<p>Changelog unavailable right now.</p>'; }

  if (!window.fetch) { useEmbedded(); return; }
  fetch("/assets/CHANGELOG.md", { cache: "no-cache" })
    .then(function (r) { if (!r.ok) throw new Error(r.status); return r.text(); })
    .then(function (t) { var r = parse(t); if (!r.length) throw new Error("empty"); render(r); })
    .catch(useEmbedded);
})();
