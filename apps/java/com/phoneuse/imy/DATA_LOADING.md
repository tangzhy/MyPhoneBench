# Data Loading Guide for iMy Privacy App

## Overview

The iMy app supports loading profile items and app permissions from a JSON file, allowing you to easily test different data configurations without rebuilding the app.

## JSON File Format

The app expects a JSON file at `/sdcard/imy_data.json` with the following structure:

```json
{
  "privacy_mode": "full_control",
  "profile_items": [
    {"level": "low", "key": "name", "value": "John Doe"},
    {"level": "low", "key": "language", "value": "English"},
    {"level": "low", "key": "city", "value": "New York"},
    {"level": "high", "key": "id_number", "value": "1234567890"},
    {"level": "high", "key": "bank_card", "value": "1234567890123456"}
  ],
  "app_permissions": [
    {"app_name": "calendar", "level": "low"},
    {"app_name": "contacts", "level": "high"},
    {"app_name": "sms", "level": "high"},
    {"app_name": "camera", "level": "high"}
  ]
}
```

**Note**: The `privacy_mode` field can be either `"full_control"` or `"semi_control"`. If not specified, it defaults to `"full_control"`.

## Loading Test Data

### Method 1: Using the provided script

```bash
# Load the example data
./load_test_data.sh

# Or load a custom JSON file
./load_test_data.sh path/to/your/data.json
```

### Method 2: Using ADB directly

```bash
# Push your JSON file to the device
adb push your_data.json /sdcard/imy_data.json

# Restart the app to load the new data
adb shell am force-stop com.phoneuse.imy
adb shell am start -n com.phoneuse.imy/.app.MainActivity
```

## How It Works

1. **On App Start**: The app checks for `/sdcard/imy_data.json`
2. **If Found**: Loads profile items and app permissions from the JSON file
3. **If Not Found**: Falls back to default hardcoded data
4. **Data Updates**: To see changes, restart the app after updating the JSON file

## Example JSON Files

An example JSON file is provided at:
```
apps/java/com/phoneuse/imy/example_data.json
```

## Notes

- The JSON file must be valid JSON format
- If the file is malformed, the app will fall back to default data
- Changes to the JSON file require an app restart to take effect
- The app will continue to work normally if the JSON file doesn't exist

