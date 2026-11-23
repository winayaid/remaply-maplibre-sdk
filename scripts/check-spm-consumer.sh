#!/usr/bin/env bash
# Builds a throwaway Swift package that depends on the SDK the way an app does (a Git URL and a
# version tag) and compiles the SwiftUI and UIKit code from docs/quickstart-ios.md against it.
#
# Usage:
#   scripts/check-spm-consumer.sh                      # local dry run: clones HEAD, tags it locally
#   scripts/check-spm-consumer.sh <git-url> <version>  # a real tag, e.g. after a release
set -euo pipefail
repo_root="$(cd "$(dirname "$0")/.." && pwd)"
work="$(mktemp -d "${TMPDIR:-/tmp}/spm-consumer.XXXXXX")"
trap 'rm -rf "$work"' EXIT

if [[ $# -eq 0 ]]; then
    version=$(sed -n 's/^VERSION_NAME=//p' "$repo_root/android/gradle.properties")
    git clone -q "$repo_root" "$work/sdk"
    git -C "$work/sdk" tag "$version"
    url="file://$work/sdk"
elif [[ $# -eq 2 ]]; then
    url="$1"
    version="$2"
else
    echo "usage: $0 [<git-url> <version>]" >&2
    exit 2
fi

# SPM names a dependency after the last path component of its URL.
package_name=$(basename "${url%.git}")

mkdir -p "$work/Consumer/Sources/Consumer"
cat > "$work/Consumer/Package.swift" <<EOF
// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "Consumer",
    platforms: [.iOS("15.5")],
    products: [.library(name: "Consumer", targets: ["Consumer"])],
    dependencies: [.package(url: "$url", exact: "$version")],
    targets: [.target(name: "Consumer", dependencies: [.product(name: "RemaplyMaps", package: "$package_name")])]
)
EOF

python3 - "$repo_root/docs/quickstart-ios.md" "$work/Consumer/Sources/Consumer" <<'EOF'
import pathlib, re, sys
blocks = [b for b in re.findall(r"```swift\n(.*?)```", pathlib.Path(sys.argv[1]).read_text(), re.S)
          if b.lstrip().startswith("import")]
if len(blocks) < 2:
    sys.exit("expected the SwiftUI and UIKit examples in docs/quickstart-ios.md")
for i, block in enumerate(blocks):
    (pathlib.Path(sys.argv[2]) / f"Example{i}.swift").write_text(block)
EOF

echo "Building a consumer of $url @ $version"
cd "$work/Consumer"
for destination in "generic/platform=iOS Simulator" "generic/platform=iOS"; do
    xcodebuild -scheme Consumer -destination "$destination" -derivedDataPath "$work/dd" \
        CODE_SIGNING_ALLOWED=NO build -quiet
    echo "ok   $destination"
done
