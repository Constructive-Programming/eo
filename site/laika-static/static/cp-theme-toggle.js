// cp-theme-toggle.js — manual dark/light toggle on top of Helium's
// OS-driven dark mode. Stores the user's choice in localStorage so it
// persists across page loads; absence of the key means "follow the OS".
//
// The button is mounted inside the existing #top-bar .row.links group,
// to the right of the GitHub icon. We don't need a dependency on the
// page being fully parsed for the data-theme attribute change to take
// effect (the FOUC-prevention block at the top runs synchronously),
// but the DOM mount is deferred until DOMContentLoaded.
(function () {
  var STORAGE_KEY = "cp-theme";
  var root = document.documentElement;

  function stored() {
    try { return localStorage.getItem(STORAGE_KEY); } catch (_) { return null; }
  }
  function persist(value) {
    try {
      if (value) localStorage.setItem(STORAGE_KEY, value);
      else localStorage.removeItem(STORAGE_KEY);
    } catch (_) {}
  }
  function osPrefersDark() {
    return window.matchMedia &&
      window.matchMedia("(prefers-color-scheme: dark)").matches;
  }
  function effective() {
    return root.getAttribute("data-theme") ||
      (osPrefersDark() ? "dark" : "light");
  }
  function apply(theme) {
    if (theme === "light" || theme === "dark") {
      root.setAttribute("data-theme", theme);
    } else {
      root.removeAttribute("data-theme");
    }
  }

  // Apply persisted choice synchronously to avoid a flash of wrong
  // theme on first paint.
  apply(stored());

  function mount() {
    var topBar = document.getElementById("top-bar");
    if (!topBar) return;
    var links = topBar.querySelector(".row.links") || topBar;

    var btn = document.createElement("a");
    btn.id = "cp-theme-toggle";
    btn.href = "#";
    btn.title = "Toggle theme";
    btn.setAttribute("aria-label", "Toggle theme");

    function refreshGlyph() {
      // Show the destination, not the current state — clicking the sun
      // means "go to light". Half-circle pair was chosen for cross-platform
      // font legibility; matches the constructive.dev navbar toggle.
      btn.textContent = effective() === "dark" ? "◑" : "◐";
    }
    btn.addEventListener("click", function (event) {
      event.preventDefault();
      var next = effective() === "dark" ? "light" : "dark";
      apply(next);
      persist(next);
      refreshGlyph();
    });
    refreshGlyph();
    links.appendChild(btn);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", mount);
  } else {
    mount();
  }
})();
