# VoiceGram Android App

A voice-to-text Telegram application for Android.

## Building with GitHub Actions

This project is set up to build automatically using GitHub Actions:

### Option 1: Automatic Build on Push
1. Initialize git repository (if not already done):
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   ```

2. Create a new repository on GitHub

3. Connect and push:
   ```bash
   git remote add origin https://github.com/YOUR_USERNAME/voicegram.git
   git branch -M main
   git push -u origin main
   ```

4. The GitHub Actions workflow will automatically start building

5. Download the APK from the Actions tab in your GitHub repository

### Option 2: Manual Trigger
1. Push the code to GitHub as above
2. Go to Actions tab in your GitHub repository
3. Select "Android Build" workflow
4. Click "Run workflow" button

### Downloading the APK
After the build completes:
1. Go to the Actions tab in your GitHub repository
2. Click on the completed workflow run
3. Scroll down to "Artifacts" section
4. Download either:
   - `app-debug` (for testing)
   - `app-release` (for production)

## Local Development

To build locally:
```bash
# Using Gradle wrapper (recommended)
./gradlew assembleDebug

# Or using system Gradle
gradle assembleDebug
```

## Project Structure
- `app/src/main/java/com/voicegram/app/` - Java source code
- `app/src/main/res/` - Android resources
- `app/build.gradle` - App-level build configuration
- `build.gradle` - Project-level build configuration
- `.github/workflows/android-build.yml` - GitHub Actions workflow

## Requirements
- Android SDK 24+
- Java 8+
- Gradle 8.0+
