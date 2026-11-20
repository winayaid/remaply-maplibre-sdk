import Foundation

/// The map style to load. The SDK never adds a default tile provider, API key, or headers.
///
/// Problems with a style are never thrown; they are reported through
/// ``MapListener/onMapLoadError(error:)``.
///
/// Spec: API-004.
public enum MapStyle: Equatable, Hashable, Sendable {
    /// A style loaded from an absolute `http` or `https` URL. If the provider needs a key, put it in the URL.
    case url(String)
    /// A style file shipped in the app, resolved relative to `Bundle.main`'s resource directory
    /// (add the folder as a folder reference to keep its structure). URLs inside the style must be absolute.
    case bundled(String)
}
