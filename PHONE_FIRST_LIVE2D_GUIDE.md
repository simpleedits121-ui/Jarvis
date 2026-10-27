# JARVIS phone-first character upgrade

## What changed in this upgrade
- Added `character_portrait.png`, the approved JARVIS scientist portrait, without deleting the original character assets.
- Updated the Home character panel with breathing-scale, gentle floating, mood-based tilt, animated panel glow, tap-to-cycle mood states, and Idle/Listen/Think/Happy/Sleep controls.
- Preserved the chat, floating companion service, existing character engine files, and existing assets.
- Enabled the GitHub Actions build workflow on both `main` and `test/triple-tap`; manual workflow dispatch remains enabled.

## Important limitation
This is an animated illustration prototype, not a true Live2D model. The portrait is a flattened image, so it cannot independently blink, move pupils, deform the mouth, or simulate hair physics. Those features require separately painted transparent parts and a rigged Cubism model (`.moc3`, model JSON, textures, physics and expressions).

## Build on a phone
1. Upload the project ZIP to your GitHub repository or replace files in your existing `test/triple-tap` branch.
2. Open the repository's **Actions** tab.
3. Select **Build JARVIS APK** and tap **Run workflow**. Choose `test/triple-tap` if prompted.
4. Open the completed run and download the `jarvis-debug-apk` artifact.
5. Extract the APK and install it on Android.

If the build fails, share the first red error section from the workflow log so the exact issue can be fixed.
