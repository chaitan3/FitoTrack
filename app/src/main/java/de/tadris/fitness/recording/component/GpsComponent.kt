/*
 * Copyright (c) 2026 Jannis Scheibe <jannis@tadris.de>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     FitoTrack is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.tadris.fitness.recording.component

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import de.tadris.fitness.recording.RecorderService
import de.tadris.fitness.recording.event.LocationChangeEvent
import de.tadris.fitness.util.WorkoutLogger
import org.greenrobot.eventbus.EventBus
import org.mapsforge.core.model.LatLong

/**
 * Collects and publishes GPS Locations
 */
class GpsComponent : RecorderServiceComponent, LocationListener {

    companion object {

        private const val TAG = "GpsComponent"
        private const val LOCATION_INTERVAL = 1000L

        /**
         * @param location the location whose geographical coordinates should be converted.
         * @return a new LatLong with the geographical coordinates taken from the given location.
         */
        @JvmStatic
        fun locationToLatLong(location: Location) = LatLong(location.latitude, location.longitude)

    }

    private val lastLocation: Location = Location(LocationManager.GPS_PROVIDER)

    private lateinit var service: RecorderService
    private var locationManager: LocationManager? = null

    override fun register(service: RecorderService) {
        this.service = service
        initializeLocationManager()
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_INTERVAL,
                0f,
                this
            )
            checkLastKnownLocation()
        } catch (ex: SecurityException) {
            WorkoutLogger.log(TAG, "fail to request location update, ignore (${ex.message})")
        } catch (ex: IllegalArgumentException) {
            WorkoutLogger.log(TAG, "gps provider does not exist " + ex.message)
        }
    }

    private fun initializeLocationManager() {
        WorkoutLogger.log(TAG, "initializeLocationManager")
        if (locationManager == null) {
            locationManager = service.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }
    }

    @Throws(SecurityException::class)
    private fun checkLastKnownLocation() {
        val location = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (location != null) {
            onLocationChanged(location)
        }
    }

    override fun unregister() {
        locationManager?.removeUpdates(this)
    }

    override fun onLocationChanged(location: Location) {
        Log.v(TAG, "onLocationChanged: $location") // don't write user location into log file
        lastLocation.set(location)
        EventBus.getDefault().postSticky(LocationChangeEvent(Location(location)))
    }

    override fun onProviderDisabled(provider: String) {
        WorkoutLogger.log(TAG, "onProviderDisabled: $provider")
    }

    override fun onProviderEnabled(provider: String) {
        WorkoutLogger.log(TAG, "onProviderEnabled: $provider")
    }

    // Must be overridden as long as minSDK < Android R
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) { }

}