(function () {
  if (location.hostname !== "login.coinbase.com") return;

  var SELECTOR = '[data-testid="two-factor-button-PASSWORD"]';
  var clicked = false;
  var observer = null;

  function tryClick() {
    if (clicked) return;
    var btn = document.querySelector(SELECTOR);
    if (!btn) return;
    clicked = true;
    if (observer) observer.disconnect();
    btn.click();
  }

  function start() {
    tryClick();
    if (clicked) return;
    observer = new MutationObserver(tryClick);
    observer.observe(document.documentElement, { childList: true, subtree: true });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", start, { once: true });
  } else {
    start();
  }
})();
