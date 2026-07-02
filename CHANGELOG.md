# Changelog

All notable changes to the Connect SDK for Android will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Sign in with Apple support in the Coinbase automation login WebView
  (`AuthPopupWindow`), hosting Coinbase's `window.open` Apple popup in a child
  WebView that shares the login cookie jar. Provider-agnostic, so future
  provider logins (e.g. Kraken) reuse it unchanged (AUTH-3437).

### Fixed
- Apple social login was previously hidden on Android because the embedded
  login WebView had no `window.open` popup support; the "OR" area on the
  Coinbase login screen appeared empty (AUTH-3437).

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
