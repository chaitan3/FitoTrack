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

        private const val KEY_ELAPSED_TIME = 10000u
        private const val KEY_DISTANCE = 10001u
        private const val KEY_HEART_RATE = 10002u
        private const val KEY_WORKOUT_STATE = 10003u
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sender: PebbleSender? = null

    override fun register(service: RecorderService) {
        try {
            sender = DefaultPebbleSender(service)
            EventBus.getDefault().register(this)
            Log.d(TAG, "PebbleIntegrationComponent registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Pebble sender", e)
        }
    }

    override fun unregister() {
        EventBus.getDefault().unregister(this)
        sender?.close()
        sender = null
        scope.cancel()
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
            KEY_WORKOUT_STATE to PebbleDictionaryItem.Int32(stateValue)
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
