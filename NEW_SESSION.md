# SmartPriceShop — Session Info

Generated: 2026-08-31T10:05:06+02:00

## Overview
SmartPriceShop is a grocery price-comparison Android app. Key goals: fast price logging, AI-assisted price scanning, side-by-side product and price-tag photos, per-user Gemini AI key, and ZIP-based import/export including images.

## Key features
- Dual-image model: Good.imageUri = product picture; PriceRecord.photoUri = price/tag photo.
- Add/Edit Price dialog persisted in ViewModel (survives rotation/camera flows).
- AI Price Scanner: optional per-device Gemini API key (stored in app preferences).
- Import/Export: ZIP file containing manifest.json (BackupDataDto) and images/ folder.
- Sort & dark-mode fixes: additional sort options and improved contrast handling.

## Quick dev setup
1. Open project in Android Studio (Windows): root = C:\Users\oleksandrst\StudioProjects\SmartPriceShop
2. Build: Gradle -> assembleDebug or run from Android Studio.
3. If Kotlin compilation errors appear, run a full Gradle build to view logs and fix missing imports or deprecated APIs.

## How to use Gemini key (UX)
- Settings → Enter Gemini API key. The key is stored locally (SharedPreferences). This is per-device — installing the app on another device does NOT share your key.
- Users can obtain API keys from the Gemini/OpenAI dashboard. A free tier may be available depending on provider terms.

## Import / Export (ZIP)
- Export creates a ZIP with manifest.json and an images/ folder. Use app UI (ImportExport dialog) to Save (CreateDocument) and Restore (GetContent).
- Current export builds ZIP in memory; recommend streaming to SAF OutputStream for large backups.

## Camera & photos
- Camera capture currently uses TakePicturePreview (Bitmap). Recommended migration: TakePicture with FileProvider and provider_paths.xml for persistent file URIs.

## Known issues & next steps
- Two-way binding: synchronize AddEditPriceDialog edits into ViewModel draft (recommended next patch).
- Migrate camera to FileProvider + TakePicture for resilience across process death.
- Stream ZIP export to SAF to avoid memory pressure.
- Consider EncryptedSharedPreferences or Keystore for production Gemini key storage.

## Important files
- app/src/main/java/.../PriceRepository.kt (export/import ZIP)
- app/src/main/java/.../PriceTrackerViewModel.kt (DraftPriceForm, dialog state)
- app/src/main/java/.../ImportExportDialog.kt
- app/src/main/java/.../HomeScreen.kt
- app/src/main/java/.../AddEditPriceDialog.kt

## Contact
Repository: Alex-Start/SmartPriceShop

(Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>)
