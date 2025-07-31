/*
 * Copyright (c) 2022 Jannis Scheibe <jannis@tadris.de>
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
import de.tadris.fitness.recording.event.HRBatteryLevelChangeEvent
import de.tadris.fitness.recording.event.HRBatteryLevelConnectionEvent
import de.tadris.fitness.recording.event.HeartRateChangeEvent
import de.tadris.fitness.recording.event.HeartRateConnectionChangeEvent
import de.tadris.fitness.recording.heart_rate.HRBatteryManager
import de.tadris.fitness.recording.heart_rate.HRBatteryManager.HRBatteryManagerCallback
import de.tadris.fitness.recording.heart_rate.HRManager
import de.tadris.fitness.recording.heart_rate.HRManager.HRManagerCallback
import no.nordicsemi.android.ble.observer.ConnectionObserver
import org.greenrobot.eventbus.EventBus

/**
 * Heart rate and sensor battery
 */
class HeartRateComponent : RecorderServiceComponent {

    private lateinit var hrManager: HRManager
    private lateinit var hrBatteryManager: HRBatteryManager
    private lateinit var heartRateListener: HeartRateListener
    private lateinit var heartRateBatteryListener: HRBatteryListener

    override fun register(service: RecorderService) {
        heartRateListener = HeartRateListener()
        hrManager = HRManager(service, heartRateListener)
        hrManager.setConnectionObserver(heartRateListener)
        hrManager.start()

        heartRateBatteryListener = HRBatteryListener()
        hrBatteryManager = HRBatteryManager(service, heartRateBatteryListener)
        hrBatteryManager.start()
    }

    override fun unregister() {
        hrManager.stop()
        hrBatteryManager.stop()
        heartRateListener.publishState(HeartRateConnectionState.DISCONNECTED)
        heartRateBatteryListener.publishState(HeartRateConnectionState.DISCONNECTED)
    }

    private class HeartRateListener : HRManagerCallback, ConnectionObserver {
        override fun onHeartRateMeasure(event: HeartRateChangeEvent) {
            EventBus.getDefault().post(event)
        }

        override fun onDeviceConnecting(device: BluetoothDevice) {
            publishState(HeartRateConnectionState.CONNECTING)
        }

        override fun onDeviceConnected(device: BluetoothDevice) {
            publishState(HeartRateConnectionState.CONNECTED)
        }

        override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
            publishState(HeartRateConnectionState.CONNECTION_FAILED)
        }

        override fun onDeviceReady(device: BluetoothDevice) {
            publishState(HeartRateConnectionState.CONNECTED)
        }

        override fun onDeviceDisconnecting(device: BluetoothDevice) {}
        override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
            publishState(HeartRateConnectionState.DISCONNECTED)
        }

        fun publishState(state: HeartRateConnectionState) {
            EventBus.getDefault().postSticky(HeartRateConnectionChangeEvent(state))
        }
    }

    private class HRBatteryListener : HRBatteryManagerCallback,
        ConnectionObserver {
        override fun onHRBatteryMeasure(event: HRBatteryLevelChangeEvent) {
            EventBus.getDefault().post(event)
        }

        override fun onDeviceConnecting(device: BluetoothDevice) {
            publishState(HeartRateConnectionState.CONNECTING)
        }

        override fun onDeviceConnected(device: BluetoothDevice) {
            publishState(HeartRateConnectionState.CONNECTED)
        }

        override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
            publishState(HeartRateConnectionState.CONNECTION_FAILED)
        }

        override fun onDeviceReady(device: BluetoothDevice) {
            publishState(HeartRateConnectionState.CONNECTED)
        }

        override fun onDeviceDisconnecting(device: BluetoothDevice) {}
        override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
            publishState(HeartRateConnectionState.DISCONNECTED)
        }

        fun publishState(state: HeartRateConnectionState) {
            EventBus.getDefault().postSticky(HRBatteryLevelConnectionEvent(state))
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