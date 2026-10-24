/* =====================================================================
   app.js — theme, header collapse, sheets, loader, press feedback,
   carousel, accordion, live release card, copy buttons.
   Vanilla JS, no dependencies.
   ===================================================================== */
(function () {
  "use strict";
  var doc = document.documentElement;
  var REPO = "shreyagarwal72/switcher5G";
  var RELEASES = "https://github.com/" + REPO + "/releases/latest";
  var API = "https://api.github.com/repos/" + REPO + "/releases/latest";
  var FALLBACK = { version: "2.0.0", date: "2026-10-24", url: RELEASES, size: null, sha: null };
  var reduce = window.matchMedia("(prefers-reduced-motion: reduce)");
  var mqDark = window.matchMedia("(prefers-color-scheme: dark)");
  var $ = function (s, r) { return (r || document).querySelector(s); };
  var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };
  var store = {
    get: function (k) { try { return localStorage.getItem(k); } catch (e) { return null; } },
    set: function (k, v) { try { localStorage.setItem(k, v); } catch (e) {} }
  };

  /* ---------- Theme: data-theme-mode, data-palette, data-amoled ---------- */
  var SWATCH = { tide: ["#006960", "#82d5c8", "#9ef2e4"], zen: ["#445e91", "#adc6ff", "#d8e2ff"], ember: ["#a63d00", "#ffb598", "#ffdbcc"], forest: ["#3b6939", "#a1d399", "#bcf0b4"] };
  function resolvedMode(pref) { return pref === "system" ? (mqDark.matches ? "dark" : "light") : pref; }
  function applyTheme() {
    var pref = store.get("s5g-mode") || "system";
    var pal = store.get("s5g-palette") || "tide";
    var amoled = store.get("s5g-amoled") === "true";
    doc.setAttribute("data-theme-mode", resolvedMode(pref));
    doc.setAttribute("data-palette", pal);
    doc.setAttribute("data-amoled", amoled ? "true" : "false");
    var meta = $('meta[name="theme-color"]');
    if (meta) { var cs = getComputedStyle(doc).getPropertyValue("--bg").trim(); if (cs) meta.setAttribute("content", cs); }
    $$("[data-mode]").forEach(function (b) { b.setAttribute("aria-pressed", String(b.dataset.mode === pref)); });
    $$("[data-palette]").forEach(function (b) { if (b.tagName === "BUTTON") b.setAttribute("aria-pressed", String(b.dataset.palette === pal)); });
    var am = $("#amoled-toggle");
    if (am) { am.checked = amoled; am.disabled = resolvedMode(pref) !== "dark"; }
  }
  function paintSwatches() {
    var dark = doc.getAttribute("data-theme-mode") === "dark";
    $$("[data-swatch]").forEach(function (el) { var c = SWATCH[el.dataset.swatch]; el.style.background = dark ? c[1] : c[0]; el.style.color = dark ? "#000" : "#fff"; });
  }
  mqDark.addEventListener && mqDark.addEventListener("change", function () { applyTheme(); paintSwatches(); });

  /* ---------- Sheets ---------- */
  function setupSheets() {
    $$("[data-open]").forEach(function (b) {
      b.addEventListener("click", function () {
        var d = $("#sheet-" + b.dataset.open);
        if (d && d.showModal) { d.showModal(); paintSwatches(); applyTheme(); }
      });
    });
    $$("dialog.sheet").forEach(function (d) {
      d.addEventListener("click", function (e) { if (e.target === d || e.target.closest("[data-close]")) d.close(); });
    });
    $$("[data-mode]").forEach(function (b) { b.addEventListener("click", function () { store.set("s5g-mode", b.dataset.mode); applyTheme(); paintSwatches(); }); });
    $$("button[data-palette]").forEach(function (b) { b.addEventListener("click", function () { store.set("s5g-palette", b.dataset.palette); applyTheme(); paintSwatches(); }); });
    var am = $("#amoled-toggle");
    if (am) am.addEventListener("change", function () { store.set("s5g-amoled", String(am.checked)); applyTheme(); });
  }

  /* ---------- Collapsing header ---------- */
  function setupHeader() {
    var h = $("#top-bar"); if (!h) return;
    var ticking = false;
    function update() { h.setAttribute("data-collapsed", String(window.scrollY > 24)); ticking = false; }
    window.addEventListener("scroll", function () { if (!ticking) { ticking = true; requestAnimationFrame(update); } }, { passive: true });
    update();
  }

  /* ---------- Page loader (page load) ---------- */
  function hideLoader() {
    var l = $("#page-loader"); if (!l) return;
    var min = reduce.matches ? 0 : 420;
    var t0 = window.__s5gT0 || performance.now();
    var wait = Math.max(0, min - (performance.now() - t0));
    setTimeout(function () { l.classList.add("is-done"); setTimeout(function () { l.remove(); }, 500); }, wait);
  }

  /* ---------- Press feedback (keyboard + pointer) ---------- */
  function setupPress() {
    document.addEventListener("keydown", function (e) {
      if ((e.key === " " || e.key === "Enter") && e.target.classList && e.target.classList.contains("btn")) e.target.classList.add("is-pressed");
    });
    document.addEventListener("keyup", function (e) { if (e.target.classList && e.target.classList.contains("btn")) e.target.classList.remove("is-pressed"); });
  }

  /* ---------- Toast + copy ---------- */
  var toastT;
  function toast(msg) {
    var t = $("#toast"); if (!t) return; t.textContent = msg; t.classList.add("is-on");
    clearTimeout(toastT); toastT = setTimeout(function () { t.classList.remove("is-on"); }, 2200);
  }
  function setupCopy() {
    $$("[data-copy]").forEach(function (b) {
      b.addEventListener("click", function () {
        var target = document.getElementById(b.dataset.copy); if (!target) return;
        var text = target.textContent.trim();
        (navigator.clipboard ? navigator.clipboard.writeText(text) : Promise.reject()).then(function () { toast("Copied"); }, function () {
          var r = document.createRange(); r.selectNodeContents(target); var s = getSelection(); s.removeAllRanges(); s.addRange(r); toast("Press copy to finish");
        });
      });
    });
  }

  /* ---------- Accordion (button + region, arrow-key navigation) ---------- */
  function setupAccordion() {
    $$(".accordion").forEach(function (acc) {
      var btns = $$(".acc-btn", acc);
      btns.forEach(function (b, i) {
        b.addEventListener("click", function () { toggle(b); });
        b.addEventListener("keydown", function (e) {
          var n = null;
          if (e.key === "ArrowDown") n = btns[(i + 1) % btns.length];
          else if (e.key === "ArrowUp") n = btns[(i - 1 + btns.length) % btns.length];
          else if (e.key === "Home") n = btns[0];
          else if (e.key === "End") n = btns[btns.length - 1];
          if (n) { e.preventDefault(); n.focus(); }
        });
      });
    });
    function toggle(b) {
      var open = b.getAttribute("aria-expanded") === "true";
      b.setAttribute("aria-expanded", String(!open));
      var item = b.closest(".acc-item"); item.classList.toggle("is-open", !open);
      var panel = document.getElementById(b.getAttribute("aria-controls"));
      if (panel) panel.setAttribute("aria-hidden", String(open));
      if (panel) panel.inert = open;
    }
    $$(".acc-panel").forEach(function (p) { p.inert = true; });
    if (location.hash) { var t = $(location.hash + " .acc-btn") || $(location.hash); if (t && t.classList.contains("acc-btn")) toggle(t); else if (t && t.classList.contains("acc-item")) toggle($(".acc-btn", t)); }
  }

  /* ---------- Carousel ---------- */
  function setupCarousel() {
    $$(".carousel").forEach(function (c) {
      var track = $(".carousel__track", c), slides = $$(".carousel__slide", c), dots = $$(".carousel__dot", c);
      var idx = 0, timer = null;
      function go(i, smooth) {
        idx = (i + slides.length) % slides.length;
        track.scrollTo({ left: track.clientWidth * idx, behavior: smooth === false || reduce.matches ? "auto" : "smooth" });
      }
      function mark() {
        var i = Math.round(track.scrollLeft / track.clientWidth); idx = i;
        dots.forEach(function (d, k) { d.setAttribute("aria-current", String(k === i)); });
        slides.forEach(function (s, k) { s.setAttribute("aria-hidden", String(k !== i)); });
      }
      track.addEventListener("scroll", function () { requestAnimationFrame(mark); }, { passive: true });
      dots.forEach(function (d, k) { d.addEventListener("click", function () { go(k); stop(); }); });
      var p = $(".carousel__nav--prev", c), n = $(".carousel__nav--next", c);
      if (p) p.addEventListener("click", function () { go(idx - 1); stop(); });
      if (n) n.addEventListener("click", function () { go(idx + 1); stop(); });
      c.addEventListener("keydown", function (e) {
        if (e.key === "ArrowRight") { e.preventDefault(); go(idx + 1); stop(); }
        if (e.key === "ArrowLeft") { e.preventDefault(); go(idx - 1); stop(); }
      });
      function start() { if (reduce.matches || timer) return; timer = setInterval(function () { go(idx + 1); }, 4200); }
      function stop() { clearInterval(timer); timer = null; }
      c.addEventListener("pointerdown", stop); c.addEventListener("focusin", stop);
      var io = "IntersectionObserver" in window ? new IntersectionObserver(function (en) { en[0].isIntersecting ? start() : stop(); }, { threshold: .5 }) : null;
      io ? io.observe(c) : null;
      mark();
    });
  }

  /* ---------- Hero mode demo ---------- */
  function setupModeDemo() {
    var group = $("#mode-demo"); if (!group) return;
    var out = $("#mode-readout-title"), sub = $("#mode-readout-sub"), badge = $("#mode-readout-badge");
    var copy = {
      sa: ["5G SA (NR only)", "Standalone 5G. Phone uses only the 5G core network.", "5G"],
      nsa: ["5G NSA (NR / LTE)", "Non-standalone 5G. 5G radio with an LTE anchor.", "5G+"],
      lte: ["4G LTE only", "Stays on LTE. Useful where 5G drains the battery or drops.", "4G"]
    };
    $$("button", group).forEach(function (b) {
      b.addEventListener("click", function () {
        $$("button", group).forEach(function (o) { o.setAttribute("aria-pressed", String(o === b)); });
        var c = copy[b.dataset.m]; out.textContent = c[0]; sub.textContent = c[1]; badge.textContent = c[2];
      });
    });
  }

  /* ---------- Latest release (GitHub Releases API with fallback) ---------- */
  function fmtSize(n) { return n ? (n / 1048576).toFixed(1) + " MB" : "See release page"; }
  function fmtDate(s) { try { return new Date(s).toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" }); } catch (e) { return s; } }
  function fillRelease(r, live) {
    $$("[data-release-version]").forEach(function (e) { e.textContent = "v" + r.version; });
    $$("[data-release-date]").forEach(function (e) { e.textContent = fmtDate(r.date); });
    $$("[data-release-size]").forEach(function (e) { e.textContent = fmtSize(r.size); });
    $$("[data-release-link]").forEach(function (e) { e.href = r.url; });
    $$("[data-release-status]").forEach(function (e) { e.textContent = live ? "Latest release, fetched just now" : "Could not reach GitHub. Showing the last known version."; });
    $$("[data-release-sha]").forEach(function (e) {
      if (r.sha) { e.textContent = r.sha; e.classList.remove("placeholder"); }
    });
  }
  function setupRelease() {
    var cards = $$("[data-release-version],[data-release-link]"); if (!cards.length) return;
    fillRelease(FALLBACK, false);
    var btns = $$("[data-release-btn]");
    btns.forEach(function (b) { b.setAttribute("aria-busy", "true"); });
    var ctl = "AbortController" in window ? new AbortController() : null;
    var to = setTimeout(function () { ctl && ctl.abort(); }, 7000);
    fetch(API, { headers: { Accept: "application/vnd.github+json" }, signal: ctl ? ctl.signal : undefined })
      .then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); })
      .then(function (j) {
        var apk = (j.assets || []).filter(function (a) { return /\.apk$/i.test(a.name); })[0];
        fillRelease({
          version: String(j.tag_name || FALLBACK.version).replace(/^v/i, ""),
          date: j.published_at || FALLBACK.date,
          url: apk ? apk.browser_download_url : (j.html_url || RELEASES),
          size: apk ? apk.size : null,
          sha: apk && apk.digest ? String(apk.digest).replace(/^sha256:/, "") : null
        }, true);
      })
      .catch(function () { fillRelease(FALLBACK, false); })
      .finally(function () { clearTimeout(to); btns.forEach(function (b) { b.removeAttribute("aria-busy"); }); });
  }

  /* ---------- Misc ---------- */
  function setupMisc() {
    $$("[data-year]").forEach(function (e) { e.textContent = new Date().getFullYear(); });
    $$("[data-loader-demo]").forEach(function (b) {
      b.addEventListener("click", function () { b.setAttribute("aria-busy", "true"); setTimeout(function () { b.removeAttribute("aria-busy"); }, 2200); });
    });
    $$("[data-toast]").forEach(function (b) { b.addEventListener("click", function () { toast(b.dataset.toast); }); });
    $$("[data-tbg]").forEach(function (g) {
      $$("button", g).forEach(function (b) { b.addEventListener("click", function () { $$("button", g).forEach(function (o) { o.setAttribute("aria-pressed", String(o === b)); }); }); });
    });
  }

  function init() {
    applyTheme(); paintSwatches();
    setupSheets(); setupHeader(); setupPress(); setupCopy(); setupAccordion(); setupCarousel(); setupModeDemo(); setupRelease(); setupMisc();
    if (document.fonts && document.fonts.ready) document.fonts.ready.then(hideLoader); else hideLoader();
    window.addEventListener("load", hideLoader, { once: true });
  }
  window.s5gToast = toast;
  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", init); else init();
})();
