// swift-tools-version:5.9
//
// Remaply Maps SDK for iOS.
//
// This manifest lives at the repository root because Swift Package Manager can only consume
// a package whose Package.swift is at the root of a Git repository. Sources live under `ios/`.
//
// MapLibre is pinned exactly. Changing it is a spec change: see api-spec/API_SPEC.md §1 and §17.

import PackageDescription

let package = Package(
    name: "RemaplyMaps",
    platforms: [
        // MapLibre Native iOS 7.0.0 requires iOS 15.5 (API_SPEC.md §1).
        .iOS("15.5"),
    ],
    products: [
        .library(name: "RemaplyMaps", targets: ["RemaplyMaps"]),
    ],
    dependencies: [
        .package(
            url: "https://github.com/maplibre/maplibre-gl-native-distribution",
            exact: "7.0.0"
        ),
    ],
    targets: [
        .target(
            name: "RemaplyMaps",
            dependencies: [
                .product(name: "MapLibre", package: "maplibre-gl-native-distribution"),
            ],
            path: "ios/Sources/RemaplyMaps"
        ),
        .testTarget(
            name: "RemaplyMapsTests",
            dependencies: ["RemaplyMaps"],
            path: "ios/Tests/RemaplyMapsTests"
        ),
    ]
)
