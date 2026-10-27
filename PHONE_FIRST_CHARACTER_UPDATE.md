# JARVIS phone-first character update

Changes in this package:
- Home character now switches between the existing scientist portrait and available happy, thinking, surprised/listening, sleeping, and wink images.
- Gentle breathing, floating, and mood tilt remain in place.
- Triple-tapping the home character within 650 ms toggles baby mode using the existing `character_baby` asset.
- Long-pressing the character sets Listening.
- The existing chat, floating service, and project configuration were retained.

Important:
- This is an illustration/Compose animation upgrade, not a true Live2D rig. A real Live2D model still requires layered source artwork and a rigged `.moc3` model.
- I could inspect the project and check ZIP integrity, but I could not run an Android build in this environment. Build with GitHub Actions before installing.
