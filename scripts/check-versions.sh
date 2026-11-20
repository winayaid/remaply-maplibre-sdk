#!/usr/bin/env bash
# Fails if the SDK version or the pinned MapLibre versions disagree across the repository.
#
# Checked:
#   - SDK version: android/gradle.properties == ios RemaplyMaps.swift, and it appears in CHANGELOG.md
#   - Android MapLibre: libs.versions.toml == RemaplyMapsTest expectation (via BuildConfig) == LICENSE
#   - iOS MapLibre: Package.swift exact pin == RemaplyMaps.mapLibreVersion == LICENSE
set -euo pipefail
cd "$(dirname "$0")/.."

fail=0
check() { # name, expected, actual
    if [[ "$2" != "$3" ]]; then
        echo "MISMATCH $1: expected '$2', found '$3'"
        fail=1
    else
        echo "ok       $1 = $3"
    fi
}

android_version=$(sed -n 's/^VERSION_NAME=//p' android/gradle.properties)
ios_version=$(sed -n 's/.*static let version: String = "\(.*\)".*/\1/p' ios/Sources/RemaplyMaps/RemaplyMaps.swift)
check "SDK version (iOS vs Android)" "$android_version" "$ios_version"
if ! grep -q "$android_version" CHANGELOG.md; then
    echo "MISMATCH CHANGELOG.md does not mention version $android_version"
    fail=1
fi

android_maplibre=$(sed -n 's/^maplibre = "\(.*\)"/\1/p' android/gradle/libs.versions.toml)
android_test=$(sed -n 's/.*assertEquals("\(.*\)", RemaplyMaps.mapLibreVersion).*/\1/p' \
    android/maps/src/test/kotlin/io/github/winayaid/mapstrial/RemaplyMapsTest.kt)
check "Android MapLibre (unit test)" "$android_maplibre" "$android_test"
android_artifact=$(sed -n 's/^maplibre-android = { module = "\([^"]*\)".*/\1/p' android/gradle/libs.versions.toml)
grep -q "$android_artifact:$android_maplibre" LICENSE \
    || { echo "MISMATCH LICENSE does not name $android_artifact:$android_maplibre"; fail=1; }

ios_pin=$(tr -d '\n ' < Package.swift | sed -n 's/.*maplibre-gl-native-distribution",exact:"\([^"]*\)".*/\1/p')
ios_const=$(sed -n 's/.*static let mapLibreVersion: String = "\(.*\)".*/\1/p' ios/Sources/RemaplyMaps/RemaplyMaps.swift)
check "iOS MapLibre (Package.swift vs RemaplyMaps.swift)" "$ios_pin" "$ios_const"
grep -q "maplibre-gl-native-distribution $ios_pin" LICENSE \
    || { echo "MISMATCH LICENSE does not name iOS distribution $ios_pin"; fail=1; }

for v in "$android_maplibre" "$ios_pin"; do
    if [[ "$v" =~ (pre|alpha|beta|rc|globe) ]]; then
        echo "MISMATCH MapLibre version '$v' is a prerelease; pin a stable release (API_SPEC.md §1)"
        fail=1
    fi
done

exit $fail
