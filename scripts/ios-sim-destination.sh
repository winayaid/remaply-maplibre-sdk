#!/usr/bin/env bash
# Prints an xcodebuild -destination value for an available iPhone simulator.
#
# Simulator names change with every Xcode release, so CI and local scripts pick one
# dynamically instead of hardcoding "iPhone NN". Prefers the newest iOS runtime.
set -euo pipefail

xcrun simctl list devices available --json | python3 -c '
import json, sys
data = json.load(sys.stdin)["devices"]
def runtime_version(key):
    # Example key: com.apple.CoreSimulator.SimRuntime.iOS-26-1
    tail = key.rsplit(".", 1)[-1]
    if not tail.startswith("iOS-"):
        return None
    return tuple(int(p) for p in tail[4:].split("-"))
best = None
for runtime, devices in data.items():
    ver = runtime_version(runtime)
    if ver is None:
        continue
    for d in devices:
        if d.get("isAvailable") and d["name"].startswith("iPhone"):
            if best is None or ver > best[0]:
                best = (ver, d["udid"], d["name"])
if best is None:
    sys.exit("No available iPhone simulator found")
print(f"platform=iOS Simulator,id={best[1]}")
version = ".".join(map(str, best[0]))
print(f"Selected simulator: {best[2]} (iOS {version})", file=sys.stderr)
'
