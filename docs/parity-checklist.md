# Parity checklist (Phase 4)

Every public API item from [`api-spec/API_SPEC.md`](../api-spec/API_SPEC.md) §18, verified on both platforms.

**Status: complete. 45 items verified, 0 open.** Verified on 2026-11-19 against MapLibre Android 13.7.1 (Pixel 9 emulator, Android 16) and MapLibre iOS 7.0.0 (iPhone 17 Pro simulator, iOS 26.1).

## How parity is verified

| Code | Method | Where | Runs in CI |
|---|---|---|---|
| **N** | API name parity: types, members, enum cases, and parameter names are compared from source. Fails on any difference that isn't on the documented allowlist. 173 items are identical. | [`scripts/check-api-parity.py`](../scripts/check-api-parity.py) | ✅ `repo.yml` |
| **U** | Unit tests. The two suites mirror each other test for test (Android 69, iOS 70). | `android/maps/src/test/…`, `ios/Tests/RemaplyMapsTests/` | ✅ |
| **T** | Behavior trace. Both samples run the same script through the public API and log a normalized event trace, which must equal [`parity/expected-trace.txt`](parity/expected-trace.txt) line for line. It includes a render probe that lists the marker ids actually drawn on screen. | [`scripts/run-parity-trace.sh`](../scripts/run-parity-trace.sh) `android` / `ios` | Manual (needs a device; automated in Phase 5) |
| **M** | Manual check on device, side by side, with screenshots. | Phase 2–4 sessions | — |

Trace stability: 4 Android runs and 2 iOS runs produced byte-identical traces (61 lines, MD5 `215b634b…`).

## Checklist

✅ = verified on that platform. "Steps" refers to step names in the expected trace.

| ID | Item | Android | iOS | Evidence |
|---|---|---|---|---|
| API-001 | `GeoCoordinate` | ✅ | ✅ | N; U `GeoCoordinateTest` / `ValueTypeTests`; T `errors` (bad coordinate → `INVALID_ARGUMENT`) |
| API-002 | `CameraPosition` | ✅ | ✅ | N; U `CameraMathTest`; T every `change`/`idle` line |
| API-003 | `CameraUpdate` + validation | ✅ | ✅ | N; U validation and clamping tests; T `errors`, `jumpClamped` (zoom 30 / tilt 80 / bearing −90 → 22 / 60 / 270) |
| API-004 | `MapStyle` url / bundled | ✅ | ✅ | N; U `StyleValidationTest`; T `load`, `styleErrors`, `styleReload` |
| API-005 | `MapOptions` + runtime changes | ✅ | ✅ | N; U defaults and sanitize tests; T `optionsClamp` (maxZoom 13 → programmatic change and idle), `optionsRestore` (no events) |
| API-006 | `Marker` + rendering contract | ✅ | ✅ | N; U `MarkerStoreTest`; T `rendered*` probes; M upright pins under bearing 45°, drawn above all layers |
| API-007 | `MarkerIcon` | ✅ | ✅ | N; U identity equality; M same 28 × 40 pin artwork |
| API-008 | `MarkerAnchor` | ✅ | ✅ | N (9 cases each); M bottom-anchored pins land on their coordinates |
| API-009 | `CameraChangeReason` | ✅ | ✅ | N; T `PROGRAMMATIC` on every API move; M drag shows `USER_GESTURE` on both |
| API-010 | `MapLifecycleState` | ✅ | ✅ | N; U `LifecycleStateMachineTest` / `LifecycleStateMachineTests`; iOS U create/destroy test |
| API-011 | `isMapReady` | ✅ | ✅ | N; U bound/unbound tests; T `load`, `state` |
| API-012 | `style` | ✅ | ✅ | N; U; T `state` (`bundled:styles/demo-bundled.json`) |
| API-013 | `camera` | ✅ | ✅ | N; U unbound/stored/live tests; T `state` |
| API-014 | `markers` | ✅ | ✅ | N; U insertion order; T `markers`, `markerEdit` |
| API-015 | `setStyle` | ✅ | ✅ | N; U forwarding; T `styleErrors`, `styleReload`, `styleSame` (no-op, no events) |
| API-016 | `setCamera` | ✅ | ✅ | N; U; T `jump` (exactly one change and one idle), `jumpSame` (no events) |
| API-017 | `animateCamera` | ✅ | ✅ | N; U curve and interpolation tests (CSS reference values); T `animate` (change+, idle, `completion true`), `cancel` (idle, `completion false`, then the new jump), `emptyAnimate`; M a touch cancels with `completion false` |
| API-018 | `getMarker` | ✅ | ✅ | N; U; T `markerEdit` |
| API-019 | `addMarker` | ✅ | ✅ | N; U validation order; T `errors` (`DUPLICATE_MARKER_ID`, `INVALID_ARGUMENT` × 2) |
| API-020 | `updateMarker` | ✅ | ✅ | N; U recency ordering; T `errors` (`MARKER_NOT_FOUND`), `markerEdit` |
| API-021 | `removeMarker` | ✅ | ✅ | N; U; T `errors` (`removeMarker(unknown)=false`) |
| API-022 | `removeAllMarkers` | ✅ | ✅ | N; U; T `markerEdit` (`markers=0`) |
| API-030 | `RemaplyMapView` (Android) | ✅ | n/a | M XML inflation, automatic lifecycle, rotation keeps the controller (id-based), background/resume |
| API-031 | `RemaplyMap` (Compose) | ✅ | n/a | M sample screen: ready, taps, animation, rotation |
| API-032 | `rememberMapController` | ✅ | n/a | M rotation keeps 3 markers and the camera (no duplicates) |
| API-033 | `RemaplyMapView` (UIKit) | n/a | ✅ | U create/destroy releases the view; T runs in the UIKit screen; M background/resume |
| API-034 | `RemaplyMap` (SwiftUI) | n/a | ✅ | M sample screen: ready, taps, animation, rotation |
| API-035 | Shared input semantics | ✅ | ✅ | N (`RemaplyMap` parameters identical except Compose `modifier`); M style, options, and callbacks updates |
| API-040 | `MapListener` | ✅ | ✅ | N (7 callbacks, same parameter names) |
| API-041 | `onMapReady` | ✅ | ✅ | T `load` (after `styleLoaded`, no camera events at startup) |
| API-042 | `onMapLoadError` | ✅ | ✅ | T `styleErrors`; M 404 and unreachable host → `STYLE_LOAD_FAILED`, bad URL → `INVALID_ARGUMENT`, missing file → `STYLE_NOT_FOUND` |
| API-043 | `onStyleLoaded` | ✅ | ✅ | T `load`, `styleReload` (markers re-added: `markers=4`) |
| API-044 | `onMapTap` | ✅ | ✅ | M coordinates match the tapped location |
| API-045 | `onMarkerTap` | ✅ | ✅ | M tap on `b` → `onMarkerTap(b)` and no map tap; U topmost selection |
| API-046 | `onCameraChange` | ✅ | ✅ | U `CameraEventGate` tests; T change lines and reasons |
| API-047 | `onCameraIdle` | ✅ | ✅ | U; T exactly one idle per movement |
| API-050 | Lifecycle state machine + automatic lifecycle | ✅ | ✅ | U (7 tests each, identical); M background/resume on both |
| API-051 | State retention | ✅ | ✅ | M rotation and backgrounding keep camera and markers on both. Android process death: implemented, device test in Phase 5. |
| API-060 | `MapError` | ✅ | ✅ | N; U |
| API-061 | `MapErrorCode` | ✅ | ✅ | N (9 codes each); T error lines use identical codes |
| API-070 | `LogLevel` | ✅ | ✅ | N; U filtering tests |
| API-071 | `MapLogger` | ✅ | ✅ | N; U every SDK error reaches the logger; M MapLibre logs forwarded with tag `MapLibre` |
| API-080 | `RemaplyMaps` | ✅ | ✅ | N; U version and pin tests; `scripts/check-versions.sh` |
| API-090 | User-location extension point | ✅ | ✅ | Documentation only (spec §13.1); reserved names unused on both |
| API-091 | `withNativeMap` (experimental) | ✅ | ✅ | N; used by the T render probe on both platforms. Exempt from parity by spec. |

## Differences found and fixed in Phase 4

| # | Difference | Platforms | Fix |
|---|---|---|---|
| 1 | After a style request rejected before reaching MapLibre (`INVALID_ARGUMENT`, `STYLE_NOT_FOUND`), marker changes stopped rendering until a later style loaded. Found in code review and confirmed by the new render probe. | Android + iOS | Marker sync now renders into whichever style is on screen, instead of requiring the latest request to have loaded. |
| 2 | A marker change could intermittently fail to render (the render probe showed `a,b,c` instead of `a,b,c,d`). MapLibre Android applies GeoJSON updates on a worker thread, and an update can be lost when it races with other map work. | Android | The marker source uses synchronous updates (`setOverrideSynchronousUpdate(true)`). Verified with 4 consecutive identical Android traces. |

## Documented, accepted platform differences

These differences are inside the wrapper or come from native platform behavior. None of them change the public contract.

| Difference | Why it's accepted | Spec |
|---|---|---|
| Gesture physics, such as fling deceleration distance, follow each platform's MapLibre. iOS coasts further than Android. | Native feel is expected. The event contract (`USER_GESTURE` changes, then exactly one idle) is identical. | §8.2 note |
| MapLibre iOS release binaries have no debug/verbose log levels. MapLibre's own messages at those levels aren't produced on iOS. | MapLibre limitation. The SDK's own logging is identical on both platforms. | §11 note |
| Thread enforcement: Android throws `WRONG_THREAD` at runtime; iOS rejects off-main calls at compile time (`@MainActor`). | Spec §3.4. | §3.4 |
| Listener reference: Android strong (cleared in `onDestroy`), iOS weak. | Each platform's idiom for avoiding leaks. | §8.1 |
| Default log output: Logcat vs `os.Logger`. | Spec §11. | §11 |
| Android-only members: `onSaveInstanceState`, `onCreate(Bundle?)`, XML constructors, Compose `modifier`, `rememberMapController`, `ExperimentalRemaplyApi`. iOS-only members: `init(frame:)`, `init(coder:)`, `MapError.description`, SwiftUI `body`. | Platform idioms (spec §7, §19 Q4). Enforced by the allowlist in `check-api-parity.py`. | §7 |

## Re-running the checks

```bash
python3 scripts/check-api-parity.py          # API names (also runs in CI)

(cd samples/android && ./gradlew assembleDebug)
scripts/run-parity-trace.sh android          # needs a running emulator

xcodebuild build -project samples/ios/RemaplySample.xcodeproj -scheme RemaplySample \
  -destination "$(scripts/ios-sim-destination.sh)" -derivedDataPath build/ios-sample-dd CODE_SIGNING_ALLOWED=NO
scripts/run-parity-trace.sh ios              # needs a booted simulator
```

When behavior changes on purpose, update both SDKs, regenerate the trace on both platforms, confirm the two are identical, and commit the new `docs/parity/expected-trace.txt` together with a spec and CHANGELOG entry.
