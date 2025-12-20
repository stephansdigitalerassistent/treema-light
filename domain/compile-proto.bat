@echo off
set DIR=.\build\generated\source\proto\main
if not exist "%DIR%" mkdir "%DIR%"
if not exist "%DIR%\kotlin" mkdir "%DIR%\kotlin"
if not exist "%DIR%\java" mkdir "%DIR%\java"

echo Compiling protobuf (domain)...
set PROTOC=..\tools\protoc\bin\protoc.exe

if not exist "%PROTOC%" (
    echo Error: protoc.exe not found at %PROTOC%
    exit /b 1
)

for %%f in (protocol\src\*.proto) do (
    echo Building %%f...
    "%PROTOC%" --proto_path=protocol/src/ --kotlin_out=lite:"%DIR%/kotlin" --java_out=lite:"%DIR%/java" -I=protocol/src/ "%%f"
    if errorlevel 1 goto fail
)
echo OK
exit /b 0

:fail
echo Failed to compile protobuf
exit /b 1
