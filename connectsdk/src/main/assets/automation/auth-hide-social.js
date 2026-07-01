(function () {
  var STYLE_ID = "zh-hide-social";
  // Google + all passkey buttons can't complete in an embedded WebView.
  // Apple is additionally hidden on Android: Sign in with Apple is unsupported
  // here (iOS keeps it via a native popup; Android has no equivalent), so we
  // hide it like the other OAuth options rather than strand the user on it.
  var CSS =
    '[data-testid="sign-in-with-google"]{display:none !important;}' +
    '[data-testid="sign-in-with-apple"]{display:none !important;}' +
    'button[data-testid*="passkey" i]{display:none !important;}';

  function inject() {
    if (document.getElementById(STYLE_ID)) return;
    var style = document.createElement("style");
    style.id = STYLE_ID;
    style.textContent = CSS;
    (document.head || document.documentElement).appendChild(style);
  }

  inject();
  // documentElement exists at documentStart; <head> may not yet. Re-inject on
  // DOMContentLoaded as a backstop (the CSS rule itself covers later renders).
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", inject, { once: true });
  }
})();
