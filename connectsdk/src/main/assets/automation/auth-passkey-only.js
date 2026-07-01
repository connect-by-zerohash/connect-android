(function () {
  function has(sel) {
    return document.querySelector(sel) !== null;
  }

  var passkeyScreen =
    has('[data-testid="passkey-verify-button"]') ||
    has('[data-testid^="identity-multi-content-layout-content-wrapper-passkey-auth"]');
  if (!passkeyScreen) return false;

  // Any path that lets the user authenticate without a passkey.
  if (
    has('input[type="password"]') ||
    has('[data-testid="password-input"]') ||
    has('[data-testid="two-factor-button-PASSWORD"]')
  ) {
    return false;
  }

  // Any non-password 2FA factor offered as an alternate (TOTP, SMS, …).
  var others = document.querySelectorAll('[data-testid^="two-factor-button-"]');
  for (var i = 0; i < others.length; i++) {
    if (others[i].getAttribute("data-testid") !== "two-factor-button-PASSWORD") {
      return false;
    }
  }

  return true;
})();
