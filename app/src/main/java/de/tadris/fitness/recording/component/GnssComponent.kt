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
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Build
import androidx.annotation.RequiresApi
import de.tadris.fitness.recording.RecorderService
import de.tadris.fitness.recording.event.SatelliteCountEvent
import de.tadris.fitness.util.WorkoutLogger
import org.greenrobot.eventbus.EventBus

/**
 * Reads and publishes GNSS data
 */
@RequiresApi(Build.VERSION_CODES.N)
class GnssComponent : RecorderServiceComponent, GnssStatus.Callback() {

    companion object {
        private const val TAG = "GnssComponent"
    }

    private lateinit var service: RecorderService
    private var locationManager: LocationManager? = null

    override fun register(service: RecorderService) {
        this.service = service
        try {
            locationManager =
                service.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager?.registerGnssStatusCallback(this, null)
        } catch (e: SecurityException) {
            WorkoutLogger.log(TAG, "fail to request gnns status, no permission!")
        } catch (e: Exception) {
            WorkoutLogger.log(TAG, "fail to request gnns status, ignore (${e.message})")
        }
    }

    override fun unregister() {
        locationManager?.unregisterGnssStatusCallback(this)
    }

    override fun onStarted() {
        WorkoutLogger.log(TAG, "onStarted")
    }

    override fun onStopped() {
        WorkoutLogger.log(TAG, "onStopped")
    }

    override fun onFirstFix(ttffMillis: Int) {
        WorkoutLogger.log(TAG, "first fix after ${ttffMillis / 1000} seconds")
    }

    override fun onSatelliteStatusChanged(status: GnssStatus) {
        EventBus.getDefault().post(SatelliteCountEvent(status.satelliteCount))
    }

}