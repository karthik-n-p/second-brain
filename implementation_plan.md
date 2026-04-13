# In-App AI Model Downloader Implementation

The current setup relies on manual file pushing via the Android `Downloads` folder, which is unreliable due to strict Scoped Storage permissions, Keras vs. TFLite format confusion, and corrupted web downloads. 

This plan implements a **native, in-app downloader** that streams the AI model directly into the app's secure internal memory—making setup a one-click process.

## User Review Required

> [!IMPORTANT]
> Because Google's Gemma models require you to accept a license on Kaggle, the app cannot just blindly hardcode the download link (Kaggle blocks scripts without API keys). 
> **My proposed design:** When the user first opens the app, they see a beautiful Setup Screen asking them to paste a **Direct Download Link**. Once they paste it, they click Download, and the app handles the rest (showing a progress bar, verifying the 1.5GB size, and securely moving it to the AI Engine).
> Do you approve this approach?

## Proposed Changes

### 1. `in.karthiknp.secondbrain.ai.ModelDownloader` [NEW]
Create a dedicated network class using Kotlin Coroutines and HTTP streams to download massive files.
- Downloads straight to the app's private internal storage (bypassing the need for `MANAGE_EXTERNAL_STORAGE` entirely).
- Emits a StateFlow of `DownloadProgress` (0% to 100%) so the UI can react.
- Automatically rejects tiny/HTML files to prevent the C++ engine crash.

### 2. `in.karthiknp.secondbrain.ui.chat.ChatViewModel` [MODIFY]
- Integrate the `ModelDownloader`.
- Expose the download progress value to the Compose UI.
- Transition state from `Downloading` -> `Ready` so the chat immediately active without restarting the app.

### 3. `in.karthiknp.secondbrain.ui.chat.ChatScreen` [MODIFY]
- **Empty State Overlay:** If `ModelState` is `ModelNotFound`, hide the chat input and instead show a sleek "Setup AI" card.
- Add an `OutlinedTextField` for the user to paste their direct model URL.
- Add a visual **Progress Bar** that fills up as the gigabytes download, along with a percentage readout.

### 4. `in.karthiknp.secondbrain.ai.LocalLlmEngine` [MODIFY]
- Remove the hacky `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)` copy logic.
- Simplify initialization: it now strictly looks in the private internal `models/` directory!

## Open Questions

- Do you want to try downloading a fully open-source model (like Microsoft's Phi-2) so we can theoretically hard-code the URL, or do you want to stick with Google Gemma and just paste the Kaggle link? 

## Verification Plan

### Automated/Code Verification
- I will verify the Kotlin Coroutine stream correctly pipes the bytes without loading the entire 1.5GB file into RAM (preventing `OutOfMemoryError`).
- Ensure the downloaded file securely registers via `.exists()` before triggering the C++ `LlmInference.createFromOptions()`.
