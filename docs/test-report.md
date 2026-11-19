# Test report (Phase 5)

| | |
|---|---|
| Date | 2026-11-19 |
| SDK | 0.1.0 (pre-release), branch `phase-5-testing` |
| MapLibre | Android 13.7.1, iOS 7.0.0 |
| Result | **All local items pass.** Two items are pending the first CI run, and four are open (see the end). |

## 1. Automated tests

| Suite | Platform | Where it runs | Tests | Result |
|---|---|---|---|---|
| Unit tests (API logic) | Android | JVM, CI | 69 | ✅ Pass |
| Unit tests (API logic) | iOS | Simulator, CI | 70 | ✅ Pass |
| Instrumented: style, marker, camera, taps, touch-cancel, errors, recreation, saved state, lifecycle | Android | Pixel 9 (Android 16) and Medium Tablet (Android 15, 2560×1600) | 7 per device | ✅ Pass on both |
| Instrumented: leak checks (50 view cycles, 10 activity cycles) | Android | Same two devices | 2 per device | ✅ Pass on both |
| Instrumented, CI matrix | Android | Emulator, API 24 (minimum) and API 36 | 9 per API | ⏳ Pending first CI run |
| Integration: style, marker, camera, errors, rejected style, new view on same controller, lifecycle | iOS | iPhone 17 Pro simulator (iOS 26.1), CI | 5 | ✅ Pass |
| Integration: leak check (50 view cycles) | iOS | iPhone 17 Pro simulator, CI | 1 | ✅ Pass |
| UI tests: marker tap, map tap, animation, touch-cancel, rotation | iOS | iPhone 16e, iPhone 17 Pro, iPad Pro 13" (iOS 26.1) | 5 per device | ✅ Pass on all three |
| API name parity | Both | CI | 173 items | ✅ Pass |
| Behavior parity trace | Both | Pixel 9 and iPhone 17 Pro | 61 lines | ✅ Identical (Phase 4) |

Instrumented and integration tests serve map styles from an in-process HTTP stub (`https://remaply.test/…`), so they don't depend on the network. Android uses an OkHttp interceptor and iOS a `URLProtocol`.

### Required scenario: load a style, add a marker, move the camera

| Step | Android (`MapInstrumentedTest`) | iOS (`MapIntegrationTests`) |
|---|---|---|
| `onStyleLoaded` → `onMapReady`, no camera events at startup | ✅ | ✅ |
| `addMarker` is drawn (checked by querying rendered features) | ✅ | ✅ |
| `setCamera` emits exactly one change and one idle; `camera` reads back | ✅ | ✅ |
| `animateCamera` emits several changes, then idle, then `completion(true)` | ✅ | ✅ |

### Memory (API_SPEC.md §9.4)

| Check | Android | iOS |
|---|---|---|
| 50 map-view create/destroy cycles: every SDK view and native map view released | ✅ 0 of 50 reachable | ✅ 0 of 50 alive |
| Activity cycles (Android): every Activity released | ✅ 0 of 10 reachable | n/a |
| Long-lived controller bound to each short-lived view keeps no binding afterwards | ✅ | ✅ |
| Memory growth of the SDK compared with plain MapLibre doing the same 50 cycles | ✅ SDK 225.3 MB vs MapLibre 238.9 MB (native heap) | ✅ SDK 0.8 MB vs MapLibre 1.0 MB (footprint) |

**Measurement method.** On the Android emulator, plain MapLibre itself retains about 4.8 MB of native memory per map instance; the same growth appears with no SDK code involved. The leak test therefore compares the SDK with a plain-MapLibre baseline in the same process and allows at most 16 MB of SDK overhead. The SDK added none. See open issue 2.

## 2. Manual test notes

### Rotation

| Platform | Screen | Result |
|---|---|---|
| Android, Compose screen | Pixel 9, portrait ↔ landscape | ✅ Camera kept, 3 markers (no duplicates), `onMapReady` fired again on the new view |
| Android, classic View (XML, with id) | Pixel 9 | ✅ Controller retained; all markers kept |
| Android | Medium Tablet, portrait ↔ landscape | ✅ Map and markers render in both orientations |
| iOS, SwiftUI | iPhone 17 Pro | ✅ Same view kept; camera and markers unchanged |
| iOS, UI test | iPhone 16e, iPhone 17 Pro, iPad Pro 13" | ✅ Marker still tappable after rotation |

### Backgrounding

| Platform | Result |
|---|---|
| Android (Compose and View) | ✅ Home and return: state unchanged, no `onMapReady` repeat |
| iOS (SwiftUI) | ✅ Home and return: state unchanged |
| iOS lifecycle under app notifications (integration test) | ✅ `resumed` → `started` → `created` → `started`, and back on window re-attach |

### Process death (Android only)

| Screen | Steps | Result |
|---|---|---|
| Compose (`rememberMapController`) | Animate to zoom 13 / bearing 45° → Home → `am kill` (pid 2524 gone) → relaunch (pid 2667) | ✅ Restored at zoom 13 / bearing 45°; sample re-adds markers in `onMapReady`. **Fixed in this phase**: Compose didn't save state before. |
| Classic View (with id) | Same steps | ✅ Restored at zoom 13 / bearing 45° |

### Low memory

| Platform | Steps | Result |
|---|---|---|
| Android, Pixel 9 | `am send-trim-memory … RUNNING_CRITICAL` in the foreground, then tap the map | ✅ Same pid, no crash, tap handled |
| Android, Medium Tablet | Same | ✅ Same pid, no crash, map ready |
| iOS | `didReceiveMemoryWarningNotification` posted to a live map (integration test) | ✅ No crash; map stays usable. MapLibre releases its caches internally. |
| iOS Simulator "Simulate Memory Warning" menu | Not driven (no command-line hook) | ➖ Covered by the notification test above |

### Screen sizes and OS versions

| Platform | Devices tested | OS versions |
|---|---|---|
| Android | Phone 1080×2424 (Pixel 9); tablet 2560×1600 (Medium Tablet) | Android 16 (API 36), Android 15 (API 35). API 24 and 36 run in CI (pending). |
| iOS | iPhone 16e, iPhone 17 Pro, iPad Pro 13" | iOS 26.1 only (see open issue 1) |

### Other observations

- **Wrong device clock.** The tablet emulator booted with its clock about six weeks behind, so TLS certificate validation failed. The SDK reported `STYLE_LOAD_FAILED` ("Chain validation failed") instead of crashing. After the clock was synced, the style loaded. This is a useful real-world check of the error path.
- **Flaky UI test, fixed.** The iOS rotation UI test failed about 1 run in 4: it tapped while the rotation animation was still running, and UIKit drops touches during rotation. The test now waits for the window to settle in the new orientation. Afterwards, 5 of 5 consecutive runs passed.
- **Slow rotation on the tablet emulator.** A screenshot taken during rotation showed an empty frame; the system logged surface-sync timeouts. A few seconds later both orientations rendered correctly. This is an emulator rendering delay, not an SDK problem.

## 3. Changes made in this phase

| Change | Why |
|---|---|
| Compose `rememberMapController` now saves style and camera with the screen state (`rememberSaveable`) and restores them into a fresh controller | Process death on a Compose screen used to reset the map to its initial camera, unlike the classic View. Spec §9.3 updated. |
| Android instrumented tests and iOS integration, leak, and UI tests added | Phase 5 deliverable |
| CI: Android emulator matrix (API 24, 36) and iOS sample UI tests | Run the new tests on every pull request, including Android's minimum OS |

## 3a. Found by CI on the first run

| Finding | Fix |
|---|---|
| **API 24 emulator crashed**: `java.lang.Error: No Vulkan compatible GPU found` on MapLibre's render thread. The default `org.maplibre.gl:android-sdk` artifact uses the Vulkan renderer, and Vulkan is optional on Android 7–9, so real devices at the minimum OS could crash the same way. | The SDK now depends on `org.maplibre.gl:android-sdk-opengl` (OpenGL ES), as spec §1 already stated. All 9 instrumented tests pass locally with it; the CI rerun covers API 24. |
| **iOS touch-cancel UI test** saw `Animation finished: true`: on the slower CI machine, the 1-second animation ended before the drag reached the map. | In UI-testing mode the sample animates for 5 seconds, which leaves a wide window for the touch. |

## 4. Open issues

1. **iOS 15.5–18 not tested.** Only the iOS 26.1 simulator runtime is installed locally, and the CI image uses iOS 26.x. Older runtimes need a separate download, and current Xcode may not support simulators that old. Recommendation: test on a physical device running iOS 15.5–17 during the 0.1.0 beta (Phase 7).
2. **MapLibre native retention on the Android emulator.** Plain MapLibre retains about 4.8 MB of native memory per map instance on the emulator. The SDK adds nothing on top, but an app that creates many maps would see this growth. Recommendation: repeat on a physical device. If it reproduces, report it upstream to MapLibre with the baseline test as the reproduction.
3. **Pending CI results.** The Android API 24 and API 36 emulator jobs run for the first time when this branch is pushed. API 24 uses a software GPU in CI; if MapLibre can't render there, the job will show `RENDERER_INIT_FAILED` or timeouts, and the matrix will need adjusting.
4. **Physical devices.** All results above are from emulators and simulators. Phase 7's beta should include at least one physical Android device and one physical iPhone.

## 5. How to re-run

```bash
# Android unit + lint
(cd android && ./gradlew check)
# Android instrumented (needs a running emulator or device)
(cd android && ./gradlew :maps-android:connectedDebugAndroidTest)

# iOS unit + integration + leak
xcodebuild test -scheme RemaplyMaps -destination "$(scripts/ios-sim-destination.sh)"
# iOS UI tests
xcodebuild test -project samples/ios/RemaplySample.xcodeproj -scheme RemaplySample \
  -destination "$(scripts/ios-sim-destination.sh)"

# Parity
python3 scripts/check-api-parity.py
scripts/run-parity-trace.sh android && scripts/run-parity-trace.sh ios
```
