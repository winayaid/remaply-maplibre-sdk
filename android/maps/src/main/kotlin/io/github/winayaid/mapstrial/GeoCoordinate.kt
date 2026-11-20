package io.github.winayaid.mapstrial

/**
 * A geographic coordinate in decimal degrees (WGS 84).
 *
 * Construction never fails. Validity is checked where the value is used: passing an invalid
 * coordinate to a throwing API throws [MapError] with [MapErrorCode.INVALID_ARGUMENT].
 * Coordinates delivered by the SDK are always valid, with longitude wrapped into `[-180, 180]`.
 *
 * Spec: API-001.
 *
 * @property latitude Latitude in degrees, valid range `[-90, 90]`.
 * @property longitude Longitude in degrees, valid range `[-180, 180]`.
 */
public data class GeoCoordinate(
    public val latitude: Double,
    public val longitude: Double,
) {
    /** `true` when both values are finite and within their valid ranges. */
    public val isValid: Boolean
        get() =
            latitude.isFinite() &&
                longitude.isFinite() &&
                latitude in -90.0..90.0 &&
                longitude in -180.0..180.0
}
