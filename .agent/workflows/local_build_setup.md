---
description: Local Build Setup Guide
---

# Local Build Setup for Threema Android

This guide explains how to set up your local environment to build the Threema Android project.

## Prerequisites

The project requires:
- **Java Development Kit (JDK) 17**
- **Android SDK** with Build Tools 35.0.0
- **Android NDK** version 28.2.13676358
- **Rust toolchain** (stable)
- **Protocol Buffers compiler** (protoc)
- **Python 3**

## Option 1: WSL2 on Windows (Recommended)

### 1. Install WSL2

```powershell
# Run in PowerShell as Administrator
wsl --install
# Restart your computer
```

### 2. Install Ubuntu in WSL2

```powershell
wsl --install -d Ubuntu
```

### 3. Set up the build environment in WSL2

```bash
# Update package list
sudo apt update && sudo apt upgrade -y

# Install Java 17
sudo apt install openjdk-17-jdk -y

# Install build essentials
sudo apt install build-essential git curl wget unzip -y

# Install Rust
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
source $HOME/.cargo/env

# Add Android targets for Rust
rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android

# Install Protocol Buffers
sudo apt install protobuf-compiler -y

# Install Python 3
sudo apt install python3 python3-pip -y
```

### 4. Install Android SDK in WSL2

```bash
# Download Android command-line tools
cd ~
mkdir -p android-sdk/cmdline-tools
cd android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip commandlinetools-linux-9477386_latest.zip
mv cmdline-tools latest

# Set environment variables
echo 'export ANDROID_HOME=$HOME/android-sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools' >> ~/.bashrc
source ~/.bashrc

# Accept licenses and install required packages
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
sdkmanager "ndk;28.2.13676358"
```

### 5. Clone and build the project

```bash
# Clone the repository (if not already done)
cd ~
git clone https://github.com/Stephan-Heuscher/treema-light.git
cd treema-light

# Build the project
./gradlew assembleDebug
```

## Option 2: Native Windows Build

This is more complex due to Rust and shell script dependencies.

### 1. Install Chocolatey (Package Manager)

```powershell
# Run in PowerShell as Administrator
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
```

### 2. Install Dependencies

```powershell
# Install JDK 17
choco install openjdk17 -y

# Install Rust
choco install rustup.install -y
# After install, open a new terminal and run:
rustup default stable
rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android

# Install Protocol Buffers
choco install protoc -y

# Install Python 3
choco install python -y

# Install Git (if not already installed)
choco install git -y
```

### 3. Install Android SDK

1. Download Android Studio from https://developer.android.com/studio
2. Install Android Studio
3. Open Android Studio and go to Settings → Appearance & Behavior → System Settings → Android SDK
4. Install:
   - Android SDK Platform 35
   - Android SDK Build-Tools 35.0.0
   - NDK (Side by side) version 28.2.13676358

### 4. Set Environment Variables

```powershell
# Set ANDROID_HOME
[System.Environment]::SetEnvironmentVariable('ANDROID_HOME', 'C:\Users\YOUR_USERNAME\AppData\Local\Android\Sdk', 'User')

# Add to PATH
$path = [System.Environment]::GetEnvironmentVariable('Path', 'User')
$newPath = "$path;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\cmdline-tools\latest\bin"
[System.Environment]::SetEnvironmentVariable('Path', $newPath, 'User')
```

### 5. Create Windows-compatible build scripts

You'll need to create `.bat` versions of the shell scripts:

**app/compile-proto.bat:**
```batch
@echo off
protoc --proto_path=protobuf --java_out=src/main/java --kotlin_out=src/main/java protobuf/*.proto
```

**domain/compile-proto.bat:**
```batch
@echo off
protoc --proto_path=protocol/src --java_out=src/main/java --kotlin_out=src/main/java protocol/src/*.proto
```

### 6. Modify build.gradle.kts for Windows

Update the `compileProto` tasks to detect Windows:

```kotlin
tasks.register<Exec>("compileProto") {
    group = "build"
    description = "generate class bindings from protobuf files"
    workingDir(project.projectDir)
    if (System.getProperty("os.name").lowercase().contains("windows")) {
        commandLine("cmd", "/c", "compile-proto.bat")
    } else {
        commandLine("./compile-proto.sh")
    }
}
```

### 7. Build the project

```powershell
# In the project directory
.\gradlew.bat assembleDebug
```

## Option 3: Use GitHub Actions (No Local Build)

If local setup is too complex, you can rely on GitHub Actions:

1. Push changes to your repository
2. GitHub Actions will build automatically
3. Download the APK from the Actions artifacts

## Troubleshooting

### Common Issues:

**"NDK not found"**
- Ensure NDK 28.2.13676358 is installed via Android Studio SDK Manager
- Set `ANDROID_NDK_HOME` environment variable

**"cargo: command not found"**
- Ensure Rust is installed: `rustup --version`
- Add Rust to PATH: `source $HOME/.cargo/env` (Linux) or restart terminal (Windows)

**"protoc: command not found"**
- Install Protocol Buffers compiler
- Verify: `protoc --version`

**Gradle daemon issues**
- Stop all daemons: `./gradlew --stop`
- Clear cache: `rm -rf ~/.gradle/caches` (Linux) or delete `%USERPROFILE%\.gradle\caches` (Windows)

## Recommended Approach

For Windows users, **WSL2 is strongly recommended** because:
- Native Linux environment matches CI/CD
- Easier dependency management
- Better compatibility with shell scripts
- Faster builds (native Unix tools)

The project is designed for Linux/macOS, so WSL2 provides the smoothest experience on Windows.
