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

import android.util.Log
import de.tadris.fitness.recording.RecorderService
import de.tadris.fitness.recording.RecordingState
import de.tadris.fitness.recording.event.AggregatedRecordingValuesEvent
import io.rebble.pebblekit2.client.DefaultPebbleSender
import io.rebble.pebblekit2.client.PebbleSender
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.UUID

class PebbleIntegrationComponent : RecorderServiceComponent {

    companion object {
        private const val TAG = "PebbleIntegration"
        private val APP_UUID = UUID.fromString("b51c1f19-2950-47a8-bc27-1c601109d3a7")

        /**
         * Pebble AppMessage keys — must match messageKeys array in
         * fitotrack/package.json (Pebble project).
         *
         * Key   Name              Type    Unit/Scaling
         * 10000 elapsed_time      uint32  seconds (activeDuration / 1000)
         * 10001 distance          uint32  meters (rounded)
         * 10002 heart_rate        int32   BPM (-1 = invalid)
         * 10003 workout_state     int32   0=idle, 1=running, 2=paused, 3=stopped
         * 10004 pause_duration    uint32  seconds (pauseDuration / 1000)
         * 10005 ascent            int32   cm (m * 100)
         * 10006 descent           int32   cm (m * 100)
         * 10007 energy            uint32  joules
         * 10008 current_speed     int32   cm/s (m/s * 100)
         * 10009 current_frequency int32   0.01 Hz (Hz * 100)
         * 10010 current_intensity int32   0.01% (% * 100)
         * 10011 repetitions       int32   count
         * 10012 sample_count      uint32  samples received
         * 10013 recording_type    int32   0=GPS, 1=INDOOR (RecordingType.ordinal)
         */
        private const val KEY_ELAPSED_TIME = 10000u
        private const val KEY_DISTANCE = 10001u
        private const val KEY_HEART_RATE = 10002u
        private const val KEY_WORKOUT_STATE = 10003u
        private const val KEY_PAUSE_DURATION = 10004u
        private const val KEY_ASCENT = 10005u
        private const val KEY_DESCENT = 10006u
        private const val KEY_ENERGY = 10007u
        private const val KEY_CURRENT_SPEED = 10008u
        private const val KEY_CURRENT_FREQUENCY = 10009u
        private const val KEY_CURRENT_INTENSITY = 10010u
        private const val KEY_REPETITIONS = 10011u
        private const val KEY_SAMPLE_COUNT = 10012u
        private const val KEY_RECORDING_TYPE = 10013u
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sender: PebbleSender? = null

    override fun register(service: RecorderService) {
        EventBus.getDefault().register(this)
        try {
            sender = DefaultPebbleSender(service)
            Log.d(TAG, "PebbleIntegrationComponent registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Pebble sender", e)
        }
    }

    override fun unregister() {
        EventBus.getDefault().unregister(this)
        try {
            sender?.close()
            sender = null
            scope.cancel()
        }catch (e: Exception){
            Log.e(TAG, "Failed to unregister Pebble sender")
        }
        Log.d(TAG, "PebbleIntegrationComponent unregistered")
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onAggregatedValues(event: AggregatedRecordingValuesEvent) {
        val elapsedSec = (event.activeDuration / 1000).toUInt()
        val distM = (event.distance + 0.5).toInt().toUInt()
        val hr = event.currentHeartRate

        val stateValue = when (event.currentState) {
            RecordingState.RUNNING -> 1
            RecordingState.AUTO_PAUSE, RecordingState.MANUAL_PAUSE -> 2
            RecordingState.STOPPED -> 3
            RecordingState.IDLE -> 0
        }

        val data = mapOf(
            KEY_ELAPSED_TIME to PebbleDictionaryItem.UInt32(elapsedSec),
            KEY_DISTANCE to PebbleDictionaryItem.UInt32(distM),
            KEY_HEART_RATE to PebbleDictionaryItem.Int32(hr),
            KEY_WORKOUT_STATE to PebbleDictionaryItem.Int32(stateValue),
            KEY_PAUSE_DURATION to PebbleDictionaryItem.UInt32((event.pauseDuration / 1000).toUInt()),
            KEY_ASCENT to PebbleDictionaryItem.Int32((event.ascent * 100).toInt()),
            KEY_DESCENT to PebbleDictionaryItem.Int32((event.descent * 100).toInt()),
            KEY_ENERGY to PebbleDictionaryItem.UInt32(event.energy.toUInt()),
            KEY_CURRENT_SPEED to PebbleDictionaryItem.Int32((event.currentSpeed * 100).toInt()),
            KEY_CURRENT_FREQUENCY to PebbleDictionaryItem.Int32((event.currentFrequency * 100).toInt()),
            KEY_CURRENT_INTENSITY to PebbleDictionaryItem.Int32((event.currentIntensity * 100).toInt()),
            KEY_REPETITIONS to PebbleDictionaryItem.Int32(event.repetitions),
            KEY_SAMPLE_COUNT to PebbleDictionaryItem.UInt32(event.sampleCount.toUInt()),
            KEY_RECORDING_TYPE to PebbleDictionaryItem.Int32(event.recordingType.ordinal)
        )

        scope.launch {
            try {
                val result = sender?.sendDataToPebble(APP_UUID, data)
                if (result != null) {
                    Log.d(TAG, "Sent to Pebble: elapsed=${elapsedSec}s dist=${distM}m hr=$hr -> $result")
                } else {
                    Log.w(TAG, "Pebble app not reachable")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send to Pebble", e)
            }
        }
    }
}
