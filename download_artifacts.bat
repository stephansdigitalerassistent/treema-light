@echo off
setlocal

echo Checking for gh CLI...
where gh >nul 2>nul
if %errorlevel% neq 0 (
    echo Error: GitHub CLI 'gh' is not installed.
    echo Please install it from https://cli.github.com/
    echo Or manually download the 'rust-libs' and 'kotlin-bindings' artifacts from the latest GitHub Actions run.
    echo extract 'rust-libs' into app\build\generated\source\libthreema
    echo extract 'kotlin-bindings' into build\generated\source\libthreema
    exit /b 1
)

echo Finding latest run ID for workflow build.yml on branch main...
for /f "tokens=*" %%i in ('gh run list --workflow build.yml --branch main --limit 1 --json databaseId --jq ".[0].databaseId"') do set RUN_ID=%%i

if "%RUN_ID%"=="" (
    echo Error: Could not find a recent run.
    exit /b 1
)

echo Downloading artifacts from run %RUN_ID%...

echo Downloading rust-libs...
gh run download %RUN_ID% -n rust-libs -D app\build\generated\source\libthreema
if %errorlevel% neq 0 (
    echo Warning: Failed to download rust-libs. Maybe the build is still running or failed?
    exit /b 1
)

echo Downloading kotlin-bindings...
gh run download %RUN_ID% -n kotlin-bindings -D build\generated\source\libthreema
if %errorlevel% neq 0 (
    echo Warning: Failed to download kotlin-bindings.
    exit /b 1
)

echo Artifacts downloaded successfully.
echo You can now build locally with: 
echo   .\gradlew.bat assembleDebug -PusePrebuiltRust

endlocal
exit /b 0
