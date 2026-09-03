# Changelog

All notable changes to the Connect SDK for Android will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.2.0] - 2026-09-03

### Fixed
- `DepositEvent.success` reported `false` for every successful deposit on
  platforms not running zerohash with auto-convert. The web SDK shows its
  success screen at `CONFIRMED` (its `DepositStatusValue.COMPLETED` is the
  string `'CONFIRMED'`), and only at `PROCESSED` when auto-convert is on. That
  profile flag never reaches the bridge, so both statuses now count as success —
  the same fix `WithdrawalEvent` already carried. `success` now also honours the
  account-matching validation the web flow checks first, so `PENDING`
  (verifying), `INVALID` and `ERROR` no longer report success (AUTH-4336).

### Added
- `DepositEvent.accountMatchingStatus` and `DepositEvent.accountMatchingReason`,
  so a host seeing `success == false` can tell a deposit that is still verifying
  from a name mismatch. On a mismatch that reason is the only explanation
  available anywhere in the stack. Both are new positional parameters on the
  `DepositEvent` data class, before `rawData`; nothing outside the SDK is
  expected to construct one.

## [1.1.0] - 2026-08-31

### Added
- Published to Maven Central as `xyz.connect:connect-android`, signed, on a
  version-tag push (SEC-7126).
- Identity/liveness step and SDK telemetry, ported from zerohash-android
  (AUTH-3907, AUTH-4125).
- Coinbase automation parity with zerohash-android.
- Sign in with Apple support in the Coinbase automation login WebView
  (`AuthPopupWindow`), hosting Coinbase's `window.open` Apple popup in a child
  WebView that shares the login cookie jar. Provider-agnostic, so future
  provider logins (e.g. Kraken) reuse it unchanged (AUTH-3437).
- Dynamic and TON Connect domains in the navigation allow-list (AUTH-3763).

### Changed
- A hold-blocked withdrawal is rejected as `funds_not_available` instead of
  timing out (AUTH-4220).

### Fixed
- Web content ran under the system bars and the keyboard on host apps targeting
  SDK 35+, where Android 15+ enforces edge-to-edge. The close button sat beneath
  the status bar and stopped receiving taps, the deposit amount screen's primary
  action was hidden behind the keyboard, and the Coinbase login page was clipped
  with an illegible status bar (AUTH-4319).
- Five Coinbase gating screens the automation did not recognise (AUTH-4245).
- Liveness overlay rendering, the liveness microphone, and the funds-on-hold
  timeout (AUTH-4245, AUTH-4220).
- WalletConnect relay, verify, and Reown catalog hosts were blocked by the
  navigation allow-list (AUTH-3763).
- Deposit success is derived from `status.value` (AUTH-4077).
- Coinbase BASE network-acceptance warning was not handled (AUTH-3960).
- Apple social login was previously hidden on Android because the embedded
  login WebView had no `window.open` popup support; the "OR" area on the
  Coinbase login screen appeared empty (AUTH-3437).

### Documentation
- `INTEGRATION.md` and `EXAMPLE_USAGE.md` advertised the dependency as
  `xyz.connect:connect-sdk`, an artifact that has never been published. The
  coordinates are `xyz.connect:connect-android` on Maven Central and
  `com.github.connect-by-zerohash:connect-android` on JitPack.

## [1.0.0] - 2024-01-14

### Added
- Initial release of Connect SDK for Android
- Authentication session management with JWT
- WebView-based integration with Connect Auth platform
- Native OAuth flows using Chrome Custom Tabs
- JavaScript↔Kotlin communication bridge
- Theme support (light/dark/system)
- Environment configuration (sandbox/production)
- Type-safe event handling (error, generic, deposit events)
- Loading UI with animated dots
- Comprehensive callback interface
- ProGuard rules for release builds
- Full documentation and examples

### Features
- **ConnectSDK**: Simple factory API for session creation
- **ConnectAuthSession**: Session lifecycle management
- **WebViewActivity**: Main WebView container with manager coordination
- **WebViewMessageHandler**: Bidirectional JavaScript bridge
- **WebViewOAuthManager**: OAuth flow handling with Chrome Custom Tabs
- **WebViewLoadingManager**: Animated loading state
- **OAuthHandler**: Chrome Custom Tabs integration
- **AuthCallbacks**: Typed callback interface
- **ConnectError**: Sealed class hierarchy for errors
- **Theme**: Light/dark/system theme support
- **Environment**: Production/sandbox configuration

### Technical Details
- Minimum SDK: API 21 (Android 5.0)
- Target SDK: API 34 (Android 14)
- Language: Kotlin 1.9+
- Build System: Gradle 8.2+
- Dependencies: AndroidX, Chrome Custom Tabs, Gson

### Documentation
- README.md with usage instructions
- EXAMPLE_USAGE.md with complete examples
- ARCHITECTURE.md with detailed architecture documentation
- Inline code documentation with KDoc

### Architecture
- Composite manager pattern for separation of concerns
- Delegate pattern for loose coupling
- Factory pattern for simple API
- Sealed classes for type-safe errors
- Intent-based Activity navigation
