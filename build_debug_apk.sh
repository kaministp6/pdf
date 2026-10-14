#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle not found. Open the project in Android Studio, sync Gradle, then build the APK." >&2
  exit 1
fi
gradle --no-daemon assembleDebug
echo "APK output: app/build/outputs/apk/debug/app-debug.apk"
