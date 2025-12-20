@echo off
set DIR=.\build\generated\source\protobuf\main
if not exist "%DIR%" mkdir "%DIR%"
if not exist "%DIR%\kotlin" mkdir "%DIR%\kotlin"
if not exist "%DIR%\java" mkdir "%DIR%\java"

echo Compiling protobuf...
set PROTOC=..\tools\protoc\bin\protoc.exe

if not exist "%PROTOC%" (
    echo Error: protoc.exe not found at %PROTOC%
    exit /b 1
)

for %%f in (protobuf\*.proto) do (
    echo Building %%f...
    "%PROTOC%" --proto_path=protobuf/ --kotlin_out=lite:"%DIR%/kotlin" --java_out=lite:"%DIR%/java" -I=protobuf/ "%%f"
    if errorlevel 1 goto fail
)
echo OK
exit /b 0

:fail
echo Failed to compile protobuf
exit /b 1
