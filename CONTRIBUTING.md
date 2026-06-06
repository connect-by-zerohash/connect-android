# Contributing to Connect SDK for Android

Thank you for your interest in contributing to the Connect SDK for Android! This document provides guidelines and instructions for contributing.

## Code of Conduct

Please be respectful and professional in all interactions. We aim to maintain a welcoming and inclusive environment.

## How to Contribute

### Reporting Bugs

Before submitting a bug report:
1. Check existing issues to avoid duplicates
2. Verify the bug exists in the latest version
3. Collect relevant information (Android version, device, logs)

When submitting a bug report, include:
- Clear, descriptive title
- Steps to reproduce
- Expected vs actual behavior
- Code samples (if applicable)
- Android version and device info
- SDK version
- Logcat output (if available)

### Suggesting Features

Feature suggestions are welcome! Please:
1. Check existing feature requests
2. Describe the use case clearly
3. Explain why this feature benefits the SDK
4. Provide examples of how it would work

### Pull Requests

#### Before Submitting

1. Discuss significant changes in an issue first
2. Ensure your code follows the project style
3. Add tests for new functionality
4. Update documentation as needed

#### Pull Request Process

1. **Fork the repository**
   ```bash
   git clone https://github.com/your-username/connect-android.git
   cd connect-android
   ```

2. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make your changes**
   - Follow Kotlin coding conventions
   - Add KDoc comments for public APIs
   - Include unit tests
   - Update documentation

4. **Test your changes**
   ```bash
   ./gradlew test
   ./gradlew connectedAndroidTest
   ```

5. **Commit your changes**
   ```bash
   git add .
   git commit -m "Add feature: your feature description"
   ```

6. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```

7. **Create a Pull Request**
   - Use a clear, descriptive title
   - Reference related issues
   - Describe what changed and why
   - Include screenshots (if UI changes)

## Development Setup

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 17+
- Android SDK with API 21-34
- Gradle 8.2+

### Building the Project

1. **Clone the repository**
   ```bash
   git clone https://github.com/connect/connect-android.git
   cd connect-android
   ```

2. **Open in Android Studio**
   - File → Open → select `connect-android` directory

3. **Build the SDK**
   ```bash
   ./gradlew :connectsdk:build
   ```

4. **Run tests**
   ```bash
   ./gradlew :connectsdk:test
   ```

### Project Structure

```
connect-android/
├── connectsdk/              # Main SDK module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/        # Kotlin source files
│   │   │   ├── res/         # Android resources
│   │   │   └── AndroidManifest.xml
│   │   └── test/            # Unit tests
│   └── build.gradle.kts     # Module build configuration
├── build.gradle.kts         # Root build configuration
├── settings.gradle.kts      # Project settings
└── README.md
```

## Coding Standards

### Kotlin Style Guide

Follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html):

- Use 4 spaces for indentation
- Use camelCase for functions and variables
- Use PascalCase for classes
- Use UPPER_SNAKE_CASE for constants
- Maximum line length: 120 characters

### Documentation

- Add KDoc comments for all public APIs
- Include `@param`, `@return`, `@throws` where applicable
- Provide usage examples for complex functionality

Example:
```kotlin
/**
 * Configure and create an authentication session
 *
 * @param jwt JWT token for authentication
 * @param environment Environment to connect to (default: PRODUCTION)
 * @param theme UI theme (default: SYSTEM)
 * @param callbacks Callbacks for session events
 * @return ConnectAuthSession instance ready to be presented
 *
 * Example usage:
 * ```kotlin
 * val session = ConnectSDK.configureAuth(
 *     jwt = "your-jwt-token",
 *     callbacks = callbacks
 * )
 * ```
 */
fun configureAuth(...): ConnectAuthSession
```

### Testing

- Write unit tests for new functionality
- Aim for >80% code coverage
- Test error cases and edge cases
- Use descriptive test names

Example:
```kotlin
@Test
fun testEnvironmentValuesAreCorrect() {
    assertEquals("production", Environment.PRODUCTION.toWebValue())
    assertEquals("sandbox", Environment.SANDBOX.toWebValue())
}

@Test
fun testDepositEventParsingWithMissingFields() {
    val json = JSONObject().apply {
        put("depositId", "123")
        // Missing other fields
    }
    val event = DepositEvent.fromJSON(json)
    assertEquals("123", event.depositId)
    assertNull(event.amount)
}
```

### Commit Messages

Use clear, descriptive commit messages:

```
Add OAuth error handling for cancelled flows

- Add onCancel callback to OAuthHandler
- Update WebViewOAuthManager to handle cancellation
- Add user-facing message for cancelled OAuth
```

Format:
- First line: imperative mood, <72 chars
- Blank line
- Detailed explanation (if needed)
- Reference issues: "Fixes #123"

## Review Process

### What We Look For

- Code quality and readability
- Test coverage
- Documentation completeness
- Backward compatibility
- Performance impact
- Security considerations

### Review Timeline

- Initial review: 2-3 business days
- Follow-up reviews: 1-2 business days
- Merging: after approval and CI passes

## Release Process

Releases follow semantic versioning:

- **Major** (X.0.0): Breaking changes
- **Minor** (x.X.0): New features, backward compatible
- **Patch** (x.x.X): Bug fixes

## Getting Help

- **Questions**: Open a GitHub Discussion
- **Bugs**: Create a GitHub Issue
- **Security**: Email security@connect.xyz (do not open public issue)

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

## Recognition

Contributors will be recognized in:
- CONTRIBUTORS.md file
- Release notes
- Project README (for significant contributions)

Thank you for contributing to Connect SDK for Android!
