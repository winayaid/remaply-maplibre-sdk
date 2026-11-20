# MapLibre upgrade: <old version> → <new version> (<platform>)

Copy this template to `docs/upgrades/<date>-<platform>-<new version>.md` for every MapLibre upgrade.
A MapLibre upgrade is a spec change (API_SPEC.md §1, §17): it needs a pull request that updates the spec,
the CHANGELOG, and this note.

## Summary

- **Platform:** Android / iOS
- **From → to:** `<old>` → `<new>`
- **Artifact:** `org.maplibre.gl:android-sdk-opengl` / `maplibre-gl-native-distribution` (exact pin)
- **Why:** <security fix, bug fix, new OS support, …>
- **Minimum OS change:** none / <old> → <new> (if changed, update API_SPEC.md §1 and README)
- **Release notes read:** <links to every MapLibre release between the two versions>

## Breaking changes in MapLibre

| MapLibre change | Affects the SDK? | Action |
|---|---|---|
| <renamed / removed / changed API> | yes / no | <code change> |

## Checklist

- [ ] Pin updated: `android/gradle/libs.versions.toml` (`maplibre`) or `Package.swift` (`exact:`)
- [ ] `RemaplyMaps.mapLibreVersion` updated (iOS: `RemaplyMaps.swift`; Android reads the catalog)
- [ ] Unit test expectation updated (`RemaplyMapsTest` / `RemaplyMapsTests`)
- [ ] `LICENSE` third-party section updated (version, and the license text if it changed)
- [ ] `scripts/check-versions.sh` passes
- [ ] Not a prerelease or `-globe` build
- [ ] Android: still the OpenGL ES artifact, unless the renderer choice is deliberately revisited (see docs/test-report.md)
- [ ] Android: merged manifest checked; location permissions still removed (`android/maps/src/main/AndroidManifest.xml`)
- [ ] Unit, instrumented/integration, and UI tests pass on both platforms
- [ ] Leak tests pass (the SDK's overhead compared with plain MapLibre)
- [ ] Parity trace matches `docs/parity/expected-trace.txt` on both platforms (`scripts/run-parity-trace.sh`)
- [ ] API name parity passes (`scripts/check-api-parity.py`)
- [ ] API reference builds without warnings (Dokka and DocC)
- [ ] Samples run on both platforms
- [ ] API_SPEC.md §1 and CHANGELOG updated

## Behavior differences observed

<Anything that changed visibly or in events: rendering, gestures, camera limits, error messages.
If behavior changed, update the spec and the expected parity trace together.>

## Results

| Check | Android | iOS |
|---|---|---|
| Unit tests | | |
| Instrumented / integration tests | | |
| UI tests | | |
| Parity trace | | |
| Leak test (SDK vs MapLibre baseline) | | |
