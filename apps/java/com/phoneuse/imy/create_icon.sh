#!/bin/bash
# Create simple launcher icon for iMy app

cd "$(dirname "$0")/app/src/main/res"

# Create a simple colored square as icon (will be replaced later with proper icon)
for dir in mipmap-*; do
    if [ -d "$dir" ] && [ "$dir" != "mipmap-anydpi-v26" ]; then
        # Create a simple 1x1 PNG (minimal valid icon)
        echo -e '\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\nIDATx\x9cc\x00\x01\x00\x00\x05\x00\x01\r\n-\xdb\x00\x00\x00\x00IEND\xaeB`\x82' > "$dir/ic_launcher.png" 2>/dev/null || true
    fi
done

echo "Icon placeholders created"

