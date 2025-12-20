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

## Notes

- The downloaded artifacts are **not** committed to git (they're in `.gitignore`)
- You must re-download after pulling changes that affect Rust or Protobuf
- The artifacts are architecture-specific and match what GitHub Actions builds
