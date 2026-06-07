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
package de.tadris.fitness.ui.record

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewModelScope
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.recording.GpsState
import de.tadris.fitness.recording.RecordingState
import de.tadris.fitness.recording.autostart.AutoStartMode
import de.tadris.fitness.recording.autostart.AutoStartRequestEvent
import de.tadris.fitness.recording.component.GpsComponent
import de.tadris.fitness.recording.event.LocationChangeEvent
import de.tadris.fitness.recording.event.RecordingStateChangeEvent
import de.tadris.fitness.recording.event.SatelliteCountEvent
import de.tadris.fitness.recording.event.WorkoutGPSStateChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.mapsforge.core.model.LatLong

class GpsRecordingViewModel(application: Application) : RecordingViewModel(application) {

    private val _gpsState = MutableStateFlow(GpsState.SIGNAL_LOST)
    val gpsState: StateFlow<GpsState> = _gpsState.asStateFlow()

    private val _satelliteCount = MutableStateFlow(0)
    val satelliteCount: StateFlow<Int> = _satelliteCount.asStateFlow()

    private val _routePositions = MutableStateFlow<List<LatLong>>(emptyList())
    val routePositions: StateFlow<List<LatLong>> = _routePositions.asStateFlow()

    private val _currentPosition = MutableStateFlow<LatLong?>(null)
    val currentPosition: StateFlow<LatLong?> = _currentPosition.asStateFlow()

    private val _showNoGpsDialog = MutableStateFlow(false)
    val showNoGpsDialog: StateFlow<Boolean> = _showNoGpsDialog.asStateFlow()

    private val _canStart = MutableStateFlow(false)
    val canStart: StateFlow<Boolean> = _canStart.asStateFlow()

    override fun onServiceStarted() {
        checkGpsStatus()
        if (wasAlreadyRunning) {
            loadExistingRoute()
        }
    }

    override fun hasServicePermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            getApplication(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkGpsStatus() {
        val manager = getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            _showNoGpsDialog.value = true
        }
    }

    private fun loadExistingRoute() {
        viewModelScope.launch(Dispatchers.IO) {
            val workoutId = RecordingStateChangeEvent.getCurrent().workoutId
            val samples = instance.db.workoutDao().getAllSamplesOfWorkout(workoutId)
            val positions = samples.map { it.toLatLong() }
            withContext(Dispatchers.Main) {
                _routePositions.value = positions
            }
        }
    }

    fun dismissNoGpsDialog() {
        _showNoGpsDialog.value = false
    }

    fun beginAutoStart(mode: AutoStartMode, delayMs: Long = 0L) {
        EventBus.getDefault().post(AutoStartRequestEvent(mode, delayMs))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onGPSStateChanged(e: WorkoutGPSStateChanged) {
        _gpsState.value = e.newState
        if (e.newState == GpsState.SIGNAL_GOOD) {
            _canStart.value = true
        } else if (recordingState.value == RecordingState.IDLE) {
            _canStart.value = false
        }
    }

    @Subscribe
    fun onLocationChange(e: LocationChangeEvent) {
        val latLong = GpsComponent.locationToLatLong(e.location)
        _currentPosition.value = latLong
        if (recordingState.value == RecordingState.RUNNING) {
            _routePositions.value = _routePositions.value + latLong
        }
    }

    @Subscribe
    fun onSatelliteCountChanged(e: SatelliteCountEvent) {
        _satelliteCount.value = e.count
    }
}
