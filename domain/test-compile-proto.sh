#!/usr/bin/env bash

set -euo pipefail

# Ensure we are in the directory of the test script (domain/)
cd "$(dirname "$0")"

# We will create temporary directories inside domain/build/tmp/ to keep all edits within the project dir
mkdir -p build/tmp

# Convert current directory to absolute path
ABS_DOMAIN_DIR=$(pwd)

TEMP_BIN_DIR=""
TEST_WS=""

cleanup() {
    # Clean up temporary directories
    if [ -n "${TEST_WS:-}" ] && [ -d "$TEST_WS" ]; then
        rm -rf "$TEST_WS"
    fi
    if [ -n "${TEMP_BIN_DIR:-}" ] && [ -d "$TEMP_BIN_DIR" ]; then
        rm -rf "$TEMP_BIN_DIR"
    fi
}
trap cleanup EXIT

# 1. Setup protoc in PATH if not already available
if ! command -v protoc &> /dev/null; then
    echo "protoc not found in PATH, searching Gradle cache..."
    GRADLE_PROTOC=$(find "$HOME/.gradle/caches" -name "protoc*" -type f -executable 2>/dev/null | head -n 1)
    if [ -n "$GRADLE_PROTOC" ]; then
        echo "Found cached protoc at: $GRADLE_PROTOC"
        TEMP_BIN_DIR=$(mktemp -d -p "$ABS_DOMAIN_DIR/build/tmp" temp-bin.XXXXXX)
        ln -sf "$GRADLE_PROTOC" "$TEMP_BIN_DIR/protoc"
        export PATH="$TEMP_BIN_DIR:$PATH"
    else
        echo "Error: protoc executable not found in PATH or Gradle cache" >&2
        exit 1
    fi
fi

# 2. Setup a temporary test workspace
TEST_WS=$(mktemp -d -p "$ABS_DOMAIN_DIR/build/tmp" test-ws.XXXXXX)

# 3. Recreate the structure required by compile-proto.sh
mkdir -p "$TEST_WS/protocol/src"
cp ../app/protobuf/extra.proto "$TEST_WS/protocol/src/"
cp ../app/protobuf/key-storage.proto "$TEST_WS/protocol/src/"
cp compile-proto.sh "$TEST_WS/"

# 4. Invoke the compile script and assert it exits with 0
echo "Running compile-proto.sh against the test schemas..."
(
    cd "$TEST_WS"
    ./compile-proto.sh
)
EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
    echo "Test passed: compile-proto.sh successfully compiled the schemas and exited with 0."
    exit 0
else
    echo "Test failed: compile-proto.sh exited with non-zero status code: $EXIT_CODE" >&2
    exit 1
fi
