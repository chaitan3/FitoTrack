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

import android.bluetooth.BluetoothDevice
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import de.tadris.fitness.R
import de.tadris.fitness.recording.RecorderService
import de.tadris.fitness.recording.event.HeartRateChangeEvent
import de.tadris.fitness.recording.event.HeartRateConnectionChangeEvent
import de.tadris.fitness.recording.event.ReconnectBluetoothDeviceEvent
import de.tadris.fitness.recording.heart_rate.HRManager
import de.tadris.fitness.recording.heart_rate.HRManager.HRManagerCallback
import de.tadris.fitness.util.WorkoutLogger
import no.nordicsemi.android.ble.observer.ConnectionObserver
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Heart rate and sensor battery
 */
class HeartRateComponent : RecorderServiceComponent {

    companion object {
        private const val TAG = "HeartRateComponent"
    }

    private lateinit var hrManager: HRManager
    private lateinit var heartRateListener: HeartRateListener

    override fun register(service: RecorderService) {
        heartRateListener = HeartRateListener()
        hrManager = HRManager(service, heartRateListener)
        hrManager.setConnectionObserver(heartRateListener)
        hrManager.start()

        EventBus.getDefault().register(this)
    }

    override fun unregister() {
        EventBus.getDefault().unregister(this)
        hrManager.stop()
        heartRateListener.publishState(HeartRateConnectionState.DISCONNECTED)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onReconnectBluetoothDevice(@Suppress("UNUSED_PARAMETER") event: ReconnectBluetoothDeviceEvent) {
        hrManager.reconnect()
    }

    private class HeartRateListener : HRManagerCallback, ConnectionObserver {
        override fun onHeartRateMeasure(event: HeartRateChangeEvent) {
            EventBus.getDefault().post(event)
        }

        override fun onDeviceConnecting(device: BluetoothDevice) {
            WorkoutLogger.log(TAG, "Connecting to ${device.address}")
            publishState(HeartRateConnectionState.CONNECTING)
        }

        override fun onDeviceConnected(device: BluetoothDevice) {
            WorkoutLogger.log(TAG, "Connected to ${device.address}")
            publishState(HeartRateConnectionState.CONNECTED)
        }

        override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
            WorkoutLogger.log(TAG, "Failed to connect to ${device.address}, reason=$reason")
            publishState(HeartRateConnectionState.CONNECTION_FAILED)
        }

        override fun onDeviceReady(device: BluetoothDevice) {
            WorkoutLogger.log(TAG, "Device ready: ${device.address}")
            publishState(HeartRateConnectionState.CONNECTED)
        }

        override fun onDeviceDisconnecting(device: BluetoothDevice) {}
        override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
            WorkoutLogger.log(TAG, "Disconnected from ${device.address}, reason=$reason")
            publishState(HeartRateConnectionState.DISCONNECTED)
        }

        fun publishState(state: HeartRateConnectionState) {
            EventBus.getDefault().postSticky(HeartRateConnectionChangeEvent(state))
        }
    }

    enum class HeartRateConnectionState(
        @ColorRes val colorRes: Int,
        @DrawableRes val iconRes: Int
    ) {
        DISCONNECTED(R.color.heartRateStateUnavailable, R.drawable.ic_bluetooth),
        CONNECTING(R.color.heartRateStateConnecting, R.drawable.ic_bluetooth_connecting),
        CONNECTED(R.color.heartRateStateAvailable, R.drawable.ic_bluetooth_connected),
        CONNECTION_FAILED(R.color.heartRateStateFailed, R.drawable.ic_bluetooth_off);
    }

}