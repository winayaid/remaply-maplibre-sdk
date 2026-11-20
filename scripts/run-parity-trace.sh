#!/usr/bin/env bash
# Runs the parity script (samples/*/ParityScript) on a device and compares its event trace with
# docs/parity/expected-trace.txt. The same expected trace applies to both platforms.
#
# Usage:
#   scripts/run-parity-trace.sh android   # needs a running emulator/device and the debug APK built
#   scripts/run-parity-trace.sh ios       # needs a booted simulator and the sample app built
#
# Build first:
#   (cd samples/android && ./gradlew assembleDebug)
#   xcodebuild build -project samples/ios/RemaplySample.xcodeproj -scheme RemaplySample \
#     -destination "$(scripts/ios-sim-destination.sh)" -derivedDataPath build/ios-sample-dd CODE_SIGNING_ALLOWED=NO
set -euo pipefail
cd "$(dirname "$0")/.."

PLATFORM="${1:?usage: $0 android|ios}"
EXPECTED="docs/parity/expected-trace.txt"
OUT_DIR="build/parity"
mkdir -p "$OUT_DIR"
ACTUAL="$OUT_DIR/trace-$PLATFORM.txt"
WAIT_SECONDS="${PARITY_WAIT_SECONDS:-45}"
APP_ID="com.remaply.maps.sample"

case "$PLATFORM" in
android)
    adb install -r samples/android/app/build/outputs/apk/debug/app-debug.apk >/dev/null
    adb shell am force-stop "$APP_ID"
    adb logcat -c
    adb shell am start -n "$APP_ID/.MainActivity" --ez parity true >/dev/null
    sleep "$WAIT_SECONDS"
    adb logcat -d -s PARITY:I -v raw | grep '|' | tr -d '\r' > "$ACTUAL"
    ;;
ios)
    DEST="$(scripts/ios-sim-destination.sh 2>/dev/null)"
    SIM="${DEST##*id=}"
    APP="build/ios-sample-dd/Build/Products/Debug-iphonesimulator/RemaplySample.app"
    xcrun simctl terminate "$SIM" "$APP_ID" 2>/dev/null || true
    sleep 2
    xcrun simctl install "$SIM" "$APP"
    RAW="$OUT_DIR/console-ios.txt"
    xcrun simctl launch --console-pty --terminate-running-process "$SIM" "$APP_ID" -parity YES > "$RAW" 2>&1 &
    LAUNCH_PID=$!
    sleep "$WAIT_SECONDS"
    kill "$LAUNCH_PID" 2>/dev/null || true
    grep -a '^PARITY ' "$RAW" | sed 's/^PARITY //' | tr -d '\r' > "$ACTUAL"
    ;;
*)
    echo "unknown platform: $PLATFORM" >&2
    exit 2
    ;;
esac

if diff -u "$EXPECTED" "$ACTUAL"; then
    echo "Parity trace ($PLATFORM): identical to $EXPECTED ($(wc -l < "$ACTUAL" | tr -d ' ') lines)"
else
    echo "Parity trace ($PLATFORM): DIFFERS from $EXPECTED" >&2
    exit 1
fi
