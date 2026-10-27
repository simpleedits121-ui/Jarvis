# JARVIS Android Companion — cleaned starter

A local-first Android companion prototype built with Kotlin and Jetpack Compose.

## What is present now
- Compose workspace with Home, Chat, Models, Tools, and Settings sections.
- A bundled anime character-sheet image with a simple breathing animation and mood controls.
- A basic floating overlay service with drag, tap reactions, and Android Text-to-Speech.
- A small demo response engine and capability/core package boundaries.
- GitHub Actions workflow to build a debug APK in the cloud.

## Important limitations
- Chat replies are hard-coded demo responses, **not a real AI model**.
- The character is currently a 2D image, **not a rigged 3D/Live2D model**.
- Speech recognition, real model inference, long-term memory, and device-action tools are not connected.
- The overlay service is a prototype and may require overlay permission; Android background restrictions can affect persistence.
- The GitHub Actions workflow has been cleaned up, but the APK must be built by GitHub Actions to confirm the remote build succeeds. No successful build is claimed by this archive alone.

## Build APK using GitHub Actions from a phone
1. Upload the **contents** of this project to the root of a GitHub repository (not the ZIP itself).
2. Open the repository's **Actions** tab and enable workflows if asked.
3. Run **Build JARVIS APK** with **Run workflow**, or push a commit to `main`.
4. Open the completed run and download the `jarvis-debug-apk` artifact.
5. Extract the downloaded artifact ZIP and install `app-debug.apk` on your Android phone. Android may ask you to allow installation from that browser/file manager.

This produces a **debug/test APK**, not a signed Play Store release.

## Local build
Use Android Studio with JDK 17 and Android SDK platform 35. Open this folder as the project and run the `app` configuration.

## Next engineering milestones
1. Confirm a reproducible cloud build.
2. Add a genuine rigged 3D character renderer and a licensed/owned GLB model; keep a 2D fallback for slower devices.
3. Connect character state to real interactions and speech.
4. Add an offline model runtime only after selecting a model that fits the target device's RAM and storage.
5. Add privacy controls, memory management, health checks, and tests before any self-maintenance features.


## Character prototype upgrade
The Home panel now uses the approved JARVIS portrait with gentle breathing, floating, mood tilt and mood-state controls. This remains a flattened-art animation prototype; it is not a rigged Live2D model. See `PHONE_FIRST_LIVE2D_GUIDE.md` for phone-first build and next steps.
