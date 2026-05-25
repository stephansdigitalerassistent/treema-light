# Building Locally with Pre-built Artifacts

This guide explains how to build the project locally on Windows without installing Rust, NDK, or Protocol Buffers.

## Prerequisites

- **Java 17** (you already have this ✓)
- **Android SDK** (install via Android Studio)
- **GitHub CLI** (`gh`) for downloading artifacts

## Quick Start

### 1. Install GitHub CLI

```powershell
winget install --id GitHub.cli
```

After installation, authenticate:
```powershell
gh auth login
```

### 2. Download Pre-built Artifacts

Run the download script:
```powershell
.\download_artifacts.bat
```

This will download:
- Rust native libraries (`.so` files)
- Kotlin bindings generated from Rust
- Protobuf generated Java/Kotlin files

### 3. Build the Project

```powershell
.\gradlew.bat assembleDebug -PusePrebuiltRust
```

The `-PusePrebuiltRust` flag tells Gradle to skip Rust compilation and use the downloaded artifacts.

## What Gets Downloaded?

The script downloads these artifacts from the latest successful GitHub Actions build:

1. **rust-native-libs** → `app/build/generated/source/libthreema/`
   - Native `.so` libraries for all Android architectures (arm64, armv7, x86, x86_64)

2. **kotlin-bindings** → `build/generated/source/libthreema/`
   - Kotlin bindings generated from Rust code via UniFFI

3. **protobuf-app** → `app/build/generated/source/protobuf/`
   - Java/Kotlin classes generated from Protocol Buffer definitions (app module)

4. **protobuf-domain** → `domain/build/generated/source/protobuf/`
   - Java/Kotlin classes generated from Protocol Buffer definitions (domain module)

## Troubleshooting

### "gh: command not found"
Install GitHub CLI: `winget install --id GitHub.cli`

### "No successful builds found"
Ensure the GitHub Actions workflow has completed successfully. Check: https://github.com/Stephan-Heuscher/treema-light/actions

### "Authentication required"
Run: `gh auth login` and follow the prompts

### Build fails with "missing files"
Re-run `.\download_artifacts.bat` to ensure all artifacts are downloaded

## When to Re-download Artifacts

You need to re-download artifacts when:
- You pull new changes that modify Rust code
- You pull new changes that modify `.proto` files
- The GitHub Actions build produces new artifacts

## Building Without Pre-built Artifacts

If you want to build everything from scratch (requires Rust, NDK, protoc):
```powershell
.\gradlew.bat assembleDebug
```

(Without the `-PusePrebuiltRust` flag)

## Huawei AGConnect Dependencies

The project relies on Huawei AppGallery Connect (AGC) dependencies for the HMS (Huawei Mobile Services) build variants.

### Remote Dependency Resolution (Current Setup)

The build configuration has been fully migrated to remote Maven resolution. The AGC plugin and core runtime libraries resolve remotely via the Huawei Maven Repository (`https://developer.huawei.com/repo/`) at version **`1.9.5.302`**:
- **AGCP plugin**: Resolved remotely at `1.9.5.302` via the root `build.gradle.kts`.
- **AGConnect Core**: Resolved remotely at `1.9.5.302` in `app/build.gradle.kts`.

Following the migration outlined in [agconnect-update-plan.md](file:///home/ubuntu/treema-light/agconnect-update-plan.md) and [agconnect-dependency-audit.md](file:///home/ubuntu/treema-light/agconnect-dependency-audit.md), the four obsolete local AGC binaries (version `1.9.1.303`) have been deleted from `app/libs/` to resolve Gradle configuration warnings and avoid git bloat. Additionally:
- Standalone crash-symbol upload has been dropped.
- APM (App Performance Management) plugin has been dropped.

### Offline / Vendored Builds

If you need to build the HMS variant completely offline, you must manually vendor the dependencies:
1. Download the required artifacts from the Huawei Maven Repository:
   - `com.huawei.agconnect:agcp:1.9.5.302` (save as `agcp-1.9.5.302.jar`)
   - `com.huawei.agconnect:agconnect-core:1.9.5.302` (save as `agconnect-core-1.9.5.302.aar`)
   - `com.huawei.agconnect:agconnect-crash-symbol-lib:1.9.5.301` (save as `agconnect-crash-symbol-lib-1.9.5.301.jar`, if crash symbol upload is needed)
2. Place these downloaded files in `app/libs/`.
3. Update the Gradle configurations to reference the local files instead of remote coordinates, as detailed in Option B of [agconnect-update-plan.md](file:///home/ubuntu/treema-light/agconnect-update-plan.md).

## Notes

- The downloaded pre-built Rust and Protobuf artifacts are **not** committed to git (they're in `.gitignore`).
- The obsolete local Huawei AGConnect artifacts (which were previously tracked in git) have been removed.
- You must re-download the pre-built Rust/Protobuf artifacts after pulling changes that affect Rust or Protobuf.
- The pre-built artifacts are architecture-specific and match what GitHub Actions builds.
