#!/usr/bin/env bash
# Install all MyPhoneBench APKs to the connected emulator.
#
# Usage:
#   ./scripts/install_apks.sh
#   ADB_SERIAL=emulator-5556 ./scripts/install_apks.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APK_DIR="$SCRIPT_DIR/../apps/apks"
SERIAL="${ADB_SERIAL:-emulator-5554}"

if ! command -v adb &>/dev/null; then
    echo "ERROR: adb not found in PATH. Please install Android SDK platform-tools."
    exit 1
fi

echo "Installing APKs to $SERIAL ..."

INSTALLED=0
FAILED=0

for apk in "$APK_DIR"/*.apk; do
    name="$(basename "$apk")"
    echo -n "  $name ... "
    if adb -s "$SERIAL" install -r "$apk" 2>&1 | grep -q "Success"; then
        echo "OK"
        INSTALLED=$((INSTALLED + 1))
    else
        echo "FAILED"
        FAILED=$((FAILED + 1))
    fi
done

echo ""
echo "Done: $INSTALLED installed, $FAILED failed."
