@echo off
setlocal enabledelayedexpansion

echo ========================================
echo Downloading Pre-built Artifacts
echo ========================================
echo.

REM Check if GitHub CLI is installed
where gh >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: GitHub CLI 'gh' is not installed.
    echo.
    echo Please install it from: https://cli.github.com/
    echo Or use: winget install --id GitHub.cli
    echo.
    echo After installation, run: gh auth login
    pause
    exit /b 1
)

REM Check if authenticated
gh auth status >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Not authenticated with GitHub CLI.
    echo Please run: gh auth login
    pause
    exit /b 1
)

echo Finding latest successful build...
echo.

REM Get the latest successful workflow run ID
for /f "tokens=*" %%i in ('gh run list --workflow=build.yml --status=success --limit=1 --json databaseId --jq ".[0].databaseId"') do set RUN_ID=%%i

if "%RUN_ID%"=="" (
    echo ERROR: No successful builds found.
    echo Please ensure the GitHub Actions workflow has completed successfully.
    pause
    exit /b 1
)

echo Found build run: %RUN_ID%
echo.

REM Create directories
echo Creating directories...
if not exist "app\build\generated\source\libthreema" mkdir "app\build\generated\source\libthreema"
if not exist "build\generated\source\libthreema" mkdir "build\generated\source\libthreema"
if not exist "app\build\generated\source\protobuf" mkdir "app\build\generated\source\protobuf"
if not exist "domain\build\generated\source\protobuf" mkdir "domain\build\generated\source\protobuf"

REM Download artifacts
echo.
echo Downloading Rust native libraries...
gh run download %RUN_ID% --name rust-native-libs --dir app\build\generated\source\libthreema

echo Downloading Kotlin bindings...
gh run download %RUN_ID% --name kotlin-bindings --dir build\generated\source\libthreema

echo Downloading Protobuf files (app)...
gh run download %RUN_ID% --name protobuf-app --dir app\build\generated\source\protobuf

echo Downloading Protobuf files (domain)...
gh run download %RUN_ID% --name protobuf-domain --dir domain\build\generated\source\protobuf

echo.
echo ========================================
echo Download Complete!
echo ========================================
echo.
echo Pre-built artifacts have been downloaded to:
echo   - app\build\generated\source\libthreema\
echo   - build\generated\source\libthreema\
echo   - app\build\generated\source\protobuf\
echo   - domain\build\generated\source\protobuf\
echo.
echo You can now build locally with:
echo   gradlew.bat assembleDebug -PusePrebuiltRust
echo.

endlocal
exit /b 0
