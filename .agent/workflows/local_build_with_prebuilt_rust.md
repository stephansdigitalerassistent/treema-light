---
description: How to build the app locally on Windows without installing Rust/NDK
---
# Local Build with Prebuilt Rust Artifacts

Since the local environment lacks the specific Rust toolchain and NDK version required to compile the core `libthreema` crypto library, we rely on GitHub Actions to build these native components.

## Prerequisites
- **GitHub CLI (`gh`)**: Installed and authenticated to download artifacts. (Or download manually from GitHub Actions).
- **Git**: To push changes that trigger the cloud build.

## Workflow

1.  **Trigger Cloud Build**:
    Push your changes to the branch (e.g., `main`). This triggers the "Build Treema Light" workflow on GitHub.

2.  **Wait for Completion**:
    Wait for the GitHub Actions run to finish successfully.

3.  **Download Artifacts**:
    Run the helper script in the root directory:
    ```powershell
    .\download_artifacts.bat
    ```
    *This script finds the latest successful run on `main` and downloads/extracts `rust-libs` and `kotlin-bindings` to the correct locations.*

    **Manual Alternative:**
    - Download `rust-libs.zip` and extract to `app/build/generated/source/libthreema/`
    - Download `kotlin-bindings.zip` and extract to `build/generated/source/libthreema/`

4.  **Run Local Build**:
    Use the `usePrebuiltRust` property to skip native compilation:
    ```powershell
    .\gradlew.bat assembleDebug -PusePrebuiltRust
    ```

## Important Files
- `download_artifacts.bat`: Automates artifact retrieval.
- `app/build.gradle.kts` & `domain/build.gradle.kts`: containing logic for `-PusePrebuiltRust`.
