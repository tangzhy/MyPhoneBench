#!/usr/bin/env bash
# Create and start an Android emulator for MyPhoneBench.
#
# Prerequisites:
#   - Android SDK installed with sdkmanager, avdmanager, emulator in PATH
#   - System image: system-images;android-34;google_apis;x86_64
#
# Usage:
#   ./scripts/setup_emulator.sh

set -euo pipefail

AVD_NAME="${AVD_NAME:-MyPhoneBench}"
API_LEVEL="${API_LEVEL:-34}"
SYS_IMAGE="system-images;android-${API_LEVEL};google_apis;x86_64"

echo "=== MyPhoneBench Emulator Setup ==="

# Step 1: Install system image if needed
echo "Ensuring system image is installed..."
sdkmanager --install "$SYS_IMAGE" 2>/dev/null || true

# Step 2: Create AVD if it doesn't exist
if avdmanager list avd 2>/dev/null | grep -q "Name: $AVD_NAME"; then
    echo "AVD '$AVD_NAME' already exists."
else
    echo "Creating AVD '$AVD_NAME'..."
    echo "no" | avdmanager create avd \
        --name "$AVD_NAME" \
        --package "$SYS_IMAGE" \
        --device "pixel_6" \
        --force
fi

# Step 3: Start emulator
echo "Starting emulator..."
emulator -avd "$AVD_NAME" \
    -no-snapshot \
    -no-audio \
    -gpu swiftshader_indirect \
    -memory 4096 &

EMULATOR_PID=$!
echo "Emulator PID: $EMULATOR_PID"

# Step 4: Wait for boot
echo "Waiting for emulator to boot..."
adb wait-for-device
adb shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done'

echo ""
echo "Emulator is ready! Serial: $(adb devices | grep emulator | awk '{print $1}')"
echo "You can now run: ./scripts/install_apks.sh"
