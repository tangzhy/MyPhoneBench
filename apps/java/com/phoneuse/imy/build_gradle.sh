#!/bin/bash
# Build iMy app using Gradle

set -e

cd "$(dirname "$0")"

echo "Building iMy app with Gradle..."

# Check if Gradle is available
if ! command -v ./gradlew &> /dev/null && ! command -v gradle &> /dev/null; then
    echo "Gradle not found. Installing Gradle wrapper..."
    # Create a simple gradle wrapper
    mkdir -p gradle/wrapper
    cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF
    chmod +x gradlew 2>/dev/null || echo "Please install Gradle manually"
fi

# Use gradle wrapper if available, otherwise use system gradle
if [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
elif command -v gradle &> /dev/null; then
    GRADLE_CMD="gradle"
else
    echo "Error: Gradle not found. Please install Gradle or run: gradle wrapper"
    exit 1
fi

$GRADLE_CMD assembleDebug

echo ""
echo "✓ Build successful!"
echo ""
echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "To install on emulator:"
echo "  adb install app/build/outputs/apk/debug/app-debug.apk"

