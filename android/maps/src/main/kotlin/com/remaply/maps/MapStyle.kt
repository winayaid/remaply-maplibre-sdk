package com.remaply.maps

/**
 * The map style to load. The SDK never adds a default tile provider, API key, or headers.
 *
 * Create one with [MapStyle.url] or [MapStyle.bundled]. Problems with a style are never thrown;
 * they are reported through [MapListener.onMapLoadError].
 *
 * Spec: API-004.
 */
public sealed class MapStyle {
    /**
     * A style loaded from an absolute `http` or `https` URL.
     *
     * @property url The style JSON URL. If the provider needs a key, put it in the URL.
     */
    public data class Url(
        public val url: String,
    ) : MapStyle()

    /**
     * A style file shipped inside the app, resolved relative to `src/main/assets/`.
     *
     * URLs inside the style (sources, sprites, glyphs) must be absolute.
     *
     * @property path Relative path, for example `"styles/basic.json"`.
     */
    public data class Bundled(
        public val path: String,
    ) : MapStyle()

    /** Factories with the same call sites as on iOS. */
    public companion object {
        /** A style loaded from an absolute `http` or `https` [url]. */
        @JvmStatic
        public fun url(url: String): MapStyle = Url(url)

        /** A style file at [path] inside the app's `assets/` folder. */
        @JvmStatic
        public fun bundled(path: String): MapStyle = Bundled(path)
    }
}
