/* =====================================================================
   page.js — injects the shared header, mobile dock, menu + appearance
   sheets and footer so every page stays consistent. Runs before app.js.
   ===================================================================== */
(function () {
  "use strict";
  var REPO = "shreyagarwal72/switcher5G";
  var I = {
    home: '<path d="M3 11.5 12 4l9 7.5"/><path d="M5 10v10h5v-6h4v6h5V10"/>',
    download: '<path d="M12 3v12"/><path d="m7 10 5 5 5-5"/><path d="M5 20h14"/>',
    log: '<path d="M6 3h9l4 4v14H6z"/><path d="M9 12h7M9 16h7M9 8h3"/>',
    faq: '<circle cx="12" cy="12" r="9"/><path d="M9.5 9.5a2.5 2.5 0 1 1 3.6 2.2c-.7.4-1.1 1-1.1 1.8"/><path d="M12 17h.01"/>',
    support: '<path d="M4 12a8 8 0 0 1 16 0v5a2 2 0 0 1-2 2h-2v-6h4"/><path d="M4 13h4v6H6a2 2 0 0 1-2-2z"/>',
    menu: '<path d="M4 7h16M4 12h16M4 17h10"/>',
    palette: '<path d="M12 3a9 9 0 1 0 0 18c1.2 0 1.8-.8 1.8-1.7 0-1.3-1.1-1.5-1.1-2.7 0-1 .8-1.6 1.8-1.6H17a4 4 0 0 0 4-4C21 6.7 17 3 12 3z"/><circle cx="7.5" cy="11" r="1"/><circle cx="10.5" cy="7.5" r="1"/><circle cx="15" cy="7.5" r="1"/>',
    close: '<path d="M6 6l12 12M18 6 6 18"/>',
    shield: '<path d="M12 3 5 6v6c0 4.5 3 7.6 7 9 4-1.4 7-4.5 7-9V6z"/><path d="m9 12 2 2 4-4"/>',
    doc: '<path d="M6 3h9l4 4v14H6z"/><path d="M14 3v5h5"/>',
    scale: '<path d="M12 4v16M6 20h12M5 8h14"/><path d="m5 8-3 7a3 3 0 0 0 6 0zM19 8l-3 7a3 3 0 0 0 6 0z"/>',
    brush: '<path d="M18.4 3.6a2 2 0 0 1 0 2.8L11 14l-3-3 7.6-7.4a2 2 0 0 1 2.8 0z"/><path d="M8 11c-2.5 0-4 1.5-4 4 0 2-1 3-2 3.5 3 1 7 0 7.5-3.5A3 3 0 0 0 8 11z"/>',
    flow: '<rect x="3" y="3" width="7" height="6" rx="2"/><rect x="14" y="15" width="7" height="6" rx="2"/><path d="M10 6h4a3 3 0 0 1 3 3v6"/>',
    heart: '<path d="M12 20s-8-4.6-8-10.2A4.3 4.3 0 0 1 12 7.6a4.3 4.3 0 0 1 8 2.2C20 15.4 12 20 12 20z"/>',
    sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>',
    moon: '<path d="M20 14.5A8 8 0 0 1 9.5 4 8 8 0 1 0 20 14.5z"/>',
    auto: '<circle cx="12" cy="12" r="9"/><path d="M12 3v18"/><path d="M12 3a9 9 0 0 1 0 18z" fill="currentColor"/>'
  };
  window.S5G_ICONS = I;
  function svg(name, cls) {
    return '<svg class="icon ' + (cls || "") + '" viewBox="0 0 24 24" aria-hidden="true" focusable="false">' + I[name] + "</svg>";
  }
  window.s5gIcon = svg;

  var PAGES = [
    ["/", "Home", "home", "index"],
    ["/download", "Download", "download", "download"],
    ["/changelog", "Changelog", "log", "changelog"],
    ["/faq", "FAQ", "faq", "faq"],
    ["/support", "Support", "support", "support"]
  ];
  var MORE = [
    ["/design", "Design language", "brush", "design"],
    ["/architecture", "Architecture", "flow", "architecture"],
    ["/privacy", "Privacy", "shield", "privacy"],
    ["/terms", "Terms", "doc", "terms"],
    ["/licenses", "Licenses and credits", "scale", "licenses"],
    ["/code-of-conduct", "Code of conduct", "heart", "code-of-conduct"]
  ];
  var current = document.documentElement.getAttribute("data-page") || "index";

  function cur(id) { return id === current ? ' aria-current="page"' : ""; }

  var header =
    '<header class="site-header" id="top-bar"><div class="site-header__bar">' +
    '<a class="brand" href="/" aria-label="Switcher 5G home"><img src="/assets/icons/icon-192.png" width="44" height="44" alt="" decoding="async"><span>Switcher 5G</span></a>' +
    '<nav class="nav" aria-label="Main">' +
    PAGES.map(function (p) { return '<a href="' + p[0] + '"' + cur(p[3]) + ">" + p[1] + "</a>"; }).join("") +
    "</nav>" +
    '<div class="header-actions">' +
    '<button class="icon-btn" type="button" data-open="appearance" aria-label="Appearance: theme and colors">' + svg("palette") + "</button>" +
    '<button class="icon-btn" type="button" data-open="menu" aria-label="More pages">' + svg("menu") + "</button>" +
    "</div></div></header>";

  var dock =
    '<nav class="dock" aria-label="Quick navigation">' +
    PAGES.map(function (p) {
      return '<a href="' + p[0] + '"' + cur(p[3]) + ' aria-label="' + p[1] + '">' + svg(p[2]) + '<span class="label">' + p[1] + "</span></a>";
    }).join("") +
    "</nav>";

  var menu =
    '<dialog class="sheet" id="sheet-menu" aria-labelledby="menu-title"><div class="sheet__head"><h2 id="menu-title">All pages</h2>' +
    '<button class="icon-btn" type="button" data-close aria-label="Close menu">' + svg("close") + "</button></div>" +
    '<div class="sheet__body"><ul class="menu-list">' +
    PAGES.concat(MORE).map(function (p) { return '<li><a href="' + p[0] + '"' + cur(p[3]) + ">" + svg(p[2]) + p[1] + "</a></li>"; }).join("") +
    "</ul></div></dialog>";

  var appearance =
    '<dialog class="sheet" id="sheet-appearance" aria-labelledby="app-title"><div class="sheet__head"><h2 id="app-title">Appearance</h2>' +
    '<button class="icon-btn" type="button" data-close aria-label="Close appearance">' + svg("close") + "</button></div>" +
    '<div class="sheet__body">' +
    '<p class="sheet__label" id="lbl-mode">Theme</p>' +
    '<div class="tbg" role="group" aria-labelledby="lbl-mode">' +
    '<button type="button" data-mode="light" aria-pressed="false">' + svg("sun") + "Light</button>" +
    '<button type="button" data-mode="dark" aria-pressed="false">' + svg("moon") + "Dark</button>" +
    '<button type="button" data-mode="system" aria-pressed="false">' + svg("auto") + "Auto</button></div>" +
    '<p class="sheet__label" id="lbl-pal">Color palette</p>' +
    '<div class="swatches" role="group" aria-labelledby="lbl-pal">' +
    [["tide", "Tide", "cookie"], ["zen", "Zen", "sunny"], ["ember", "Ember", "clover"], ["forest", "Forest", "flower"]].map(function (p) {
      return '<button type="button" class="swatch" data-palette="' + p[0] + '" aria-pressed="false"><span class="shape shape--' + p[2] + '" data-swatch="' + p[0] + '" aria-hidden="true"></span>' + p[1] + "</button>";
    }).join("") +
    "</div>" +
    '<p class="sheet__label">Dark options</p>' +
    '<label class="switch-row"><span>AMOLED black<small>Pure black background in dark mode</small></span><input type="checkbox" id="amoled-toggle"><span class="switch" aria-hidden="true"></span></label>' +
    "</div></dialog>";

  var footer =
    '<footer class="site-footer"><div class="container"><div class="footer-grid">' +
    '<div class="footer-brand"><a class="brand" href="/"><img src="/assets/icons/icon-192.png" width="44" height="44" alt="" loading="lazy"><span>Switcher 5G</span></a>' +
    '<p>Free, open-source 5G SA, 5G NSA and 4G LTE switching for Android. MIT licensed.</p></div>' +
    '<div class="footer-col"><h2>App</h2><ul><li><a href="/download">Download</a></li><li><a href="/changelog">Changelog</a></li><li><a href="/faq">FAQ</a></li><li><a href="/support">Support</a></li></ul></div>' +
    '<div class="footer-col"><h2>Project</h2><ul><li><a href="/design">Design language</a></li><li><a href="/architecture">Architecture</a></li><li><a href="/licenses">Licenses and credits</a></li><li><a href="/code-of-conduct">Code of conduct</a></li></ul></div>' +
    '<div class="footer-col"><h2>Community</h2><ul><li><a href="https://t.me/championworkspace" rel="noopener">Telegram @championworkspace</a></li><li><a href="https://github.com/' + REPO + '" rel="noopener">Source on GitHub</a></li><li><a href="/privacy">Privacy</a></li><li><a href="/terms">Terms</a></li></ul></div>' +
    "</div>" +
    '<div class="footer-base"><span>&copy; <span data-year>2026</span> Switcher 5G contributors. MIT License.</span>' +
    "<span>Built with Material 3 Expressive and Petal design language.</span></div></div></footer>";

  var loader =
    '<div class="page-loader" id="page-loader" role="status" aria-live="polite"><div class="loader loader--lg"></div><span class="sr-only">Loading</span></div>';

  var body = document.body;
  var main = document.getElementById("main");

  body.insertAdjacentHTML("afterbegin", header);
  body.insertAdjacentHTML("afterbegin", '<a class="skip-link" href="#main">Skip to content</a>');
  if (main) {
    main.insertAdjacentHTML("afterend", footer);
  } else { body.insertAdjacentHTML("beforeend", footer); }
  body.insertAdjacentHTML("beforeend", dock + menu + appearance + '<div class="toast" id="toast" role="status" aria-live="polite"></div>');
  if (!document.getElementById("page-loader")) body.insertAdjacentHTML("afterbegin", loader);
})();
