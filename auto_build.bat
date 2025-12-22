@echo off
set RUN_ID=20401472561

echo Waiting for build %RUN_ID%...
gh run watch %RUN_ID% --exit-status
if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    exit /b 1
)
echo Build finished!

echo Cleaning directories...
if exist "app\build\generated\source\libthreema" rd /s /q "app\build\generated\source\libthreema"
mkdir "app\build\generated\source\libthreema"

if exist "build\generated\source\libthreema" rd /s /q "build\generated\source\libthreema"
mkdir "build\generated\source\libthreema"

if exist "app\build\generated\source\protobuf" rd /s /q "app\build\generated\source\protobuf"
mkdir "app\build\generated\source\protobuf"

if exist "domain\build\generated\source\protobuf" rd /s /q "domain\build\generated\source\protobuf"
mkdir "domain\build\generated\source\protobuf"

if exist "domain\build\generated\source\libthreema" rd /s /q "domain\build\generated\source\libthreema"
mkdir "domain\build\generated\source\libthreema"

echo Downloading artifacts...
gh run download %RUN_ID% --name rust-native-libs --dir app\build\generated\source\libthreema || echo WARNING: Failed rust-native-libs
gh run download %RUN_ID% --name kotlin-bindings --dir build\generated\source\libthreema || echo WARNING: Failed kotlin-bindings
gh run download %RUN_ID% --name protobuf-app --dir app\build\generated\source\protobuf || echo WARNING: Failed protobuf-app
gh run download %RUN_ID% --name protobuf-domain --dir domain\build\generated\source\protobuf || echo WARNING: Failed protobuf-domain

echo Copying Kotlin bindings to domain build dir...
xcopy build\generated\source\libthreema domain\build\generated\source\libthreema /s /i /y

echo Starting Build...
call gradlew.bat assembleDebug -PusePrebuiltRust --console=plain
