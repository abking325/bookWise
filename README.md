# BookWise Offline

A distraction-free, 100% offline digital reading library for Android built with Kotlin, Jetpack Compose, Material 3, structured local file storage, and native offline Android Text-to-Speech.

## Key Capabilities

- **100% Offline Architecture**: Zero cloud dependencies, zero accounts, zero tracking, zero network permissions. All books, covers, editions, chapters, bookmarks, highlights, notes, and settings are stored directly in app-managed local folders (`filesDir/bookwise_library/`) with atomic file writes.
- **3D Rotating Book Carousel**: Continuous curved round-table carousel with perspective depth, realistic page-block thickness, spring momentum, and smooth touch interaction.
- **English & Amharic Dual Editions**: Link English and Amharic (`አማርኛ`) editions under a single book with bundled Ethiopic serif and sans typography (`Noto Serif Ethiopic`, `Noto Sans Ethiopic`).
- **Distraction-Free Progressive Reader**: Loads chapters lazily for large books, with 5 independent reader themes (`Pure White`, `Soft Paper`, `Sepia`, `Dark Charcoal`, `Pure Black`), adjustable fonts/sizes/spacing/margins/alignment, fullscreen mode, in-book search, bookmarks, highlights, and personal notes.
- **Native Offline Text-to-Speech**: Verified offline voices (`!voice.isNetworkConnectionRequired`), double-tap paragraph activation, movable/minimizable floating player (hidden until active), speed & pitch control, headphone-unplug pause, audio focus, and background/lock-screen media notification controls.
- **Book Editor, Importer & Migration**: Create books manually, paste full text with automatic chapter detection, crop & zoom cover images (2:3 ratio), or import `.txt`, `.epub`, `.pdf` (with scanned-PDF detection), and BookWise `.zip`/`.json`/`.html`/`.js` migrations.
- **Portable ZIP Backups**: Export individual books, your full library, or settings as portable ZIP/JSON files and restore with duplicate resolution (`Skip`, `Replace`, `Keep Both`).

## Project Structure

- `app/src/main/java/com/example/model/` — Domain and settings models
- `app/src/main/java/com/example/data/storage/` — Atomic file writer, JSON serializer, starter seeder, and folder repository
- `app/src/main/java/com/example/data/importer/` — Offline TXT/EPUB/PDF/BookWise importer & ZIP backup manager
- `app/src/main/java/com/example/tts/` — Native `OfflineTtsManager` and `TtsPlaybackService` media foreground service
- `app/src/main/java/com/example/ui/` — Theme, 3D carousel, cover cropper, floating TTS player, navigation shell, and all 8 core screens
- `.github/workflows/build-apk.yml` — Automated CI workflow validation, clean tests, debug APK build, signature/integrity checks, and artifact upload
- `.github/workflows/release-apk.yml` — Optional signed release APK workflow (`v*` tags / manual dispatch)

## Building & Installing

### Local Gradle Build (JDK 21 / Gradle 9.3.1 / AGP 9.1.1)
```bash
./gradlew :app:testDebugUnitTest
./gradlew assembleDebug
```
The installable APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

### GitHub Actions Automatic APK Generation
1. Push this repository to GitHub.
2. Open the **Actions** tab and run **Build BookWise Offline APK** (or push to any branch / trigger manually).
3. Download **`BookWise-Offline.apk`** from the workflow run's **Artifacts** section (SHA-256 checksum and test/build diagnostics are available in **`build-diagnostics`**).

### Consistent APK Signing Strategy
- **Debug Builds (`BookWise-Offline.apk`)**: Decodes the repository's shared `debug.keystore.base64` (falling back to `keytool` generation only if missing), ensuring a consistent signing certificate (`applicationId`: `com.aistudio.bookwise.kmpzqx`) so future APK updates install cleanly over previous builds without losing local books, reading progress, or settings.
- **Production Release Builds (Optional)**: Never commit private `.jks` signing keys. Configure repository secrets `KEYSTORE_BASE64`, `STORE_PASSWORD`, and `KEY_PASSWORD` to build `BookWise-Offline-release.apk` via `.github/workflows/release-apk.yml`.

## Backup & Migration Instructions

- **Before uninstalling**: Open **Settings -> Data Management -> Backup Entire Library (ZIP)** to save a portable `.zip` archive of all books, covers, chapters, bookmarks, highlights, notes, and settings.
- **Migrating from BookWise TrebEdit HTML/JS/JSON**: Open **Add Book -> Import TXT / EPUB / PDF** (or **Settings -> Restore Backup / Migrate BookWise**) and select your `.json`, `.html`, `.js`, or `.zip` export. Original titles, authors, categories, chapters, and language editions are preserved intact.
- **Offline Voices**: If Amharic or a preferred offline voice is unavailable on your device, open **Settings -> Offline Audio (TTS) -> Install Offline Voice Data** to download system offline voice packs in Android Text-to-Speech settings.
