# Quick Start - How to Test the Connect SDK

This guide shows how to run the example app and test the Connect SDK for Android.

## Prerequisites

1. **Android Studio** (Arctic Fox or newer)
   - Download: https://developer.android.com/studio

2. **JDK 17+**
   - Check: `java -version`
   - Android Studio already includes JDK

3. **Emulator or Android Device**
   - API 21 (Android 5.0) or higher

## Option 1: Run in Android Studio (Recommended)

### Step 1: Open the Project

1. Open Android Studio
2. Click **File → Open**
3. Navigate to `/Users/alysson.cordeiro/Documents/connect-android`
4. Click **Open**
5. Wait for Gradle Sync to finish (may take a few minutes the first time)

### Step 2: Configure JWT Token

Before running, you need a valid JWT token:

1. Obtain the JWT from your backend or the Connect platform
2. You can:
   - **Option A**: Edit the file and set a fixed JWT
     - Open: `app/src/main/java/com/example/connectdemo/MainActivity.kt`
     - Line 18: Replace `"your-jwt-token-here"` with your JWT

   - **Option B**: Enter the JWT in the app when running
     - The app has a text field for you to paste the JWT

### Step 3: Run the App

1. **Select a device**:
   - At the top of Android Studio, click the device dropdown
   - Choose an emulator or connected device
   - If you don't have an emulator: **Tools → Device Manager → Create Device**

2. **Execute the app**:
   - Click the green Run button or press `Shift + F10`
   - Wait for build and installation

3. **Use the app**:
   - Paste or type your JWT token
   - Select Environment (Production/Sandbox)
   - Select Theme (System/Light/Dark)
   - Click **Connect Account**
   - The WebView will open with the Connect platform

## Option 2: Run via Command Line

### Step 1: Verify Installation

```bash
cd /Users/alysson.cordeiro/Documents/connect-android

# Check if gradlew is executable
ls -la gradlew

# Should show: -rwxr-xr-x (with 'x' for executable)
```

### Step 2: List Devices

```bash
# List available emulators
~/Library/Android/sdk/emulator/emulator -list-avds

# List connected devices
~/Library/Android/sdk/platform-tools/adb devices
```

### Step 3: Start Emulator (if needed)

```bash
# Replace 'Pixel_5_API_33' with your emulator name
~/Library/Android/sdk/emulator/emulator -avd Pixel_5_API_33 &
```

### Step 4: Build and Install

```bash
# Build the project
./gradlew assembleDebug

# Install on device/emulator
./gradlew installDebug

# Or do everything at once
./gradlew installDebug
```

### Step 5: Open the App

The app will be installed as "Connect Demo". Open it manually on the device or use:

```bash
~/Library/Android/sdk/platform-tools/adb shell am start -n com.example.connectdemo/.MainActivity
```

## Troubleshooting

### Issue: "SDK location not found"

**Solution**: Create `local.properties` file:

```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
```

### Issue: "Gradle sync failed"

**Solution**:
1. In Android Studio: **File → Invalidate Caches → Invalidate and Restart**
2. Or delete: `rm -rf .gradle build app/build connectsdk/build`
3. Sync again

### Issue: "Could not find gradle-wrapper.jar"

**Solution**: Download the wrapper JAR:

```bash
mkdir -p gradle/wrapper
curl -L https://github.com/gradle/gradle/raw/v8.2.0/gradle/wrapper/gradle-wrapper.jar -o gradle/wrapper/gradle-wrapper.jar
```

### Issue: "No devices found"

**Solution**:
1. Check if emulator is running: `adb devices`
2. Restart adb: `adb kill-server && adb start-server`
3. Create new emulator in Android Studio: **Tools → Device Manager**

### Issue: "JWT token is required"

**Solution**: You need a valid JWT to test. Options:

1. **Get from your backend**: If you have a backend integrated with Connect
2. **Test with development token**: Contact Connect support
3. **Demo mode**: Some errors are expected without a valid JWT, but you can see the UI

## What to Test

### Test 1: App Interface
- JWT field appears
- Environment options (Production/Sandbox)
- Theme options (System/Light/Dark)
- "Connect Account" button
- Event log at the bottom

### Test 2: Authentication Flow
1. Paste a valid JWT
2. Click "Connect Account"
3. See the log showing "Starting Connect session..."
4. WebView should open with animated loading (3 yellow dots)
5. Connect platform content should load

### Test 3: Callbacks
Observe the event log showing:
- `Event`: Generic events
- `Deposit`: When a deposit is completed
- `Error`: Errors that occur
- `Session closed`: When closing the WebView

### Test 4: Themes
Test each theme and see the changes:
- **Light**: White background, dark icons
- **Dark**: Dark background (#111113), light icons
- **System**: Follows system theme

### Test 5: OAuth Flow
If the flow includes OAuth:
1. Chrome Custom Tabs should open
2. You authenticate with the provider (Google, etc)
3. Automatically returns to the app
4. Log shows "OAuth success"

## Logs and Debug

### View logs in Android Studio

1. Open the **Logcat** tab (bottom)
2. Select your device
3. Filter by tag: `ConnectDemo` or `ConnectSDK`

### View logs via command line

```bash
# View all app logs
adb logcat | grep -E "ConnectDemo|Connect"

# View specific logs
adb logcat ConnectDemo:D ConnectSDK:D *:S

# Clear logs
adb logcat -c
```

### Check if WebView loaded

```bash
adb logcat | grep -i "webview"
```

## Customize the Test App

### Change colors

Edit: `app/src/main/res/values/colors.xml`

### Change layout

Edit: `app/src/main/res/layout/activity_main.xml`

### Add more logs

Edit: `app/src/main/java/com/example/connectdemo/MainActivity.kt`

Add more logs in the `addLog()` function:

```kotlin
addLog("Custom log message")
```

## Build for Release

### Generate Debug APK

```bash
./gradlew assembleDebug

# APK generated at:
# app/build/outputs/apk/debug/app-debug.apk
```

### Install APK manually

```bash
# On connected device
adb install app/build/outputs/apk/debug/app-debug.apk

# On specific device
adb -s <device-id> install app/build/outputs/apk/debug/app-debug.apk
```

### Share APK

The APK can be copied and installed on any Android device:

```bash
# Copy to desktop
cp app/build/outputs/apk/debug/app-debug.apk ~/Desktop/ConnectDemo.apk
```

## Next Steps

1. **Integrate into your app**:
   - Copy the `connectsdk` module to your project
   - Add `implementation(project(":connectsdk"))` to dependencies

2. **Customize the SDK**:
   - Edit colors in `connectsdk/src/main/res/values/colors.xml`
   - Adjust animations in `WebViewLoadingManager.kt`
   - Modify constants in `Constants.kt`

3. **Add features**:
   - See `ARCHITECTURE.md` to understand the structure
   - See `CONTRIBUTING.md` for contribution guidelines

## Additional Resources

- **README.md**: Complete SDK documentation
- **EXAMPLE_USAGE.md**: Code examples
- **ARCHITECTURE.md**: Detailed architecture
- **Android Developer**: https://developer.android.com

## Need Help?

1. Check the logs in Logcat
2. Review the troubleshooting above
3. Consult the complete documentation in README.md
4. Contact support
