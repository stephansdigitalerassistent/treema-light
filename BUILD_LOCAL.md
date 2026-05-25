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

The project relies on Huawei AppGallery Connect (AGC) dependencies for the HMS (Huawei Mobile Services) build variants. These dependencies must be present locally under `app/libs/` (e.g., `app/libs/agconnect-core-1.9.1.303.aar` and `app/libs/agconnect-crash-symbol-lib-1.9.1.303.jar`) for offline builds and local vendoring support.

### Version Discrepancy

There is a version discrepancy between the local vendored files and the Gradle build configuration:
- The local files in `app/libs/` are currently at version **`1.9.1.303`**.
- The root `build.gradle.kts` and `app/build.gradle.kts` files reference version **`1.9.5.302`** for the AGCP plugin and core runtime libraries (resolving remotely via the Huawei Maven Repository).

### Managing the Dependencies

- **Online Builds**: During normal online builds, Gradle will automatically fetch and resolve version `1.9.5.302` from the Huawei Maven repository (`https://developer.huawei.com/repo/`), ignoring the older local versions.
- **Offline / Vendored Builds**: If you need to build the HMS variant offline, or if you prefer to have matching local/vendored dependencies, you must manually manage these. You should download the `1.9.5.302` (and related `1.9.5.301` crash symbol lib) artifacts from the Huawei Maven repository and replace the older files in `app/libs/`. See [agconnect-update-plan.md](file:///home/ubuntu/treema-light/agconnect-update-plan.md) for more details.

## Notes

- The downloaded artifacts are **not** committed to git (they're in `.gitignore`)
- You must re-download after pulling changes that affect Rust or Protobuf
- The artifacts are architecture-specific and match what GitHub Actions builds
