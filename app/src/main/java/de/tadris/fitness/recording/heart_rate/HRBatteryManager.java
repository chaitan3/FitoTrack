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
package de.tadris.fitness.recording.heart_rate;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import java.util.UUID;

import de.tadris.fitness.recording.event.HRBatteryLevelChangeEvent;
import de.tadris.fitness.util.BluetoothDevicePreferences;

public class HRBatteryManager {
    private static final UUID SERVICE_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    private static final UUID UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothDevicePreferences preferences;
    private final HRBatteryManagerCallback callback;
    private BluetoothGattCharacteristic characteristic;
    private Context context;
    private BluetoothGatt gatt;

    public HRBatteryManager(Context context, HRBatteryManagerCallback callback) {
        this.context = context;
        this.callback = callback;
        this.preferences = new BluetoothDevicePreferences(context);
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @SuppressLint("MissingPermission")
    public void start() {
        if (isConnectionPossible()) {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(getBluetoothAddress());
            gatt = device.connectGatt(context, true, new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    gatt.discoverServices();
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    BluetoothGattService service = gatt.getService(SERVICE_UUID);

                    if (service != null) {
                        characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                        gatt.readCharacteristic(characteristic);
                        gatt.setCharacteristicNotification(characteristic, true);

                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);
                    int batteryState = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);

                    callback.onHRBatteryMeasure(new HRBatteryLevelChangeEvent(
                            device,
                            batteryState));
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicChanged(gatt, characteristic);
                    int batteryState = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);

                    callback.onHRBatteryMeasure(new HRBatteryLevelChangeEvent(
                            device,
                            batteryState));
                }
            });
        }
    }

    @SuppressLint("MissingPermission")
    public void stop() {
        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
            gatt = null;
        }
    }

    public void reconnect() {
        stop();
        start();
    }

    public boolean isConnectionPossible() {
        return isBluetoothAddressAvailable() && bluetoothAdapter != null && bluetoothAdapter.isEnabled() && hasPermission();
    }

    public boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    public String getBluetoothAddress() {
        return preferences.getAddress(BluetoothDevicePreferences.DEVICE_HEART_RATE);
    }

    public boolean isBluetoothAddressAvailable() {
        return !getBluetoothAddress().isEmpty();
    }

    public interface HRBatteryManagerCallback {
        void onHRBatteryMeasure(HRBatteryLevelChangeEvent event);
    }
}