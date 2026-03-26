package com.getaltair.kairos.domain.model

/**
 * Represents a geographic point with a radius.
 * Used for location-based habit triggers (AT_LOCATION anchor type).
 */
data class GeoPoint(val lat: Double, val lon: Double, val radiusMeters: Int) {
    init {
        require(lat in -90.0..90.0) {
            "Latitude must be between -90 and 90, but was $lat"
        }
        require(lon in -180.0..180.0) {
            "Longitude must be between -180 and 180, but was $lon"
        }
    }

    companion object {
        /**
         * Creates a GeoPoint from a latitude/longitude pair in decimal degrees.
         *
         * @param latitude Latitude in decimal degrees (-90 to 90)
         * @param longitude Longitude in decimal degrees (-180 to 180)
         * @param radiusMeters Radius in meters
         * @return a new GeoPoint instance
         */
        fun fromDecimalDegrees(latitude: Double, longitude: Double, radiusMeters: Int): GeoPoint = GeoPoint(
            lat = latitude,
            lon = longitude,
            radiusMeters = radiusMeters
        )
    }
}
