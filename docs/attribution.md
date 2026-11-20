# Attribution and licensing

What an app that uses this SDK must show and include. This is guidance, not legal advice; check the
terms of every style, tile, and data provider you use.

## 1. License notices (required)

The SDK (BSD 2-Clause) and MapLibre Native (BSD 2-Clause) both require that binary distributions
reproduce their copyright notices and license text "in the documentation and/or other materials".
In practice, add both to your app's **open-source licenses** or **acknowledgements** screen:

- Remaply Maps SDK: the first section of [`LICENSE`](../LICENSE).
- MapLibre Native: the "MapLibre Native — BSD 2-Clause License" section of [`LICENSE`](../LICENSE).
- The third-party components bundled with MapLibre Native, listed with each MapLibre release.

On Android, tools such as the OSS Licenses Gradle plugin collect most notices automatically. Check that
MapLibre's appear.

## 2. Map data attribution (required by most providers)

The **style you load** decides which data the map shows, and the **data provider's terms** decide what
attribution is required. Most styles carry their attribution text; MapLibre shows it behind the ⓘ
button in the bottom-right corner.

- The SDK always keeps that attribution button visible. Don't cover it with your own UI.
- OpenStreetMap-based data (including OpenFreeMap, used in the samples) requires
  "© OpenStreetMap contributors". Some providers also require their own name or logo.

## 3. MapLibre logo (optional)

MapLibre's license doesn't require the logo. The SDK keeps it visible in v1 (bottom-left).

## Where the SDK places map controls

| Control | Position |
|---|---|
| Compass (shown when rotated) | Top-right |
| MapLibre logo | Bottom-left |
| Attribution button | Bottom-right |

Leave about 64 dp/pt clear at the top-right, as the samples do, so your UI doesn't cover the compass.
