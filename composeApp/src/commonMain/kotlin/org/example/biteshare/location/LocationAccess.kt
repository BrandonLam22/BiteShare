package org.example.biteshare.location

import org.example.biteshare.domain.GeoPoint

interface LocationAccess {
    suspend fun requestCurrentLocation(): GeoPoint?
}

object NoopLocationAccess : LocationAccess {
    override suspend fun requestCurrentLocation(): GeoPoint? = null
}
