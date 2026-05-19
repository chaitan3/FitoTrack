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

package de.tadris.fitness.ui.record;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import de.tadris.fitness.R;

public class ChooseBluetoothDeviceDialog {

    private final Activity context;
    private final BluetoothDeviceSelectListener listener;
    private final List<BluetoothDevice> devices = new ArrayList<>();
    private ArrayAdapter<String> arrayAdapter;
    private final BluetoothAdapter adapter;
    private BluetoothLeScanner scanner;
    private final ScanCallback callback = new DeviceDialogScanCallback();

    public ChooseBluetoothDeviceDialog(Activity context, BluetoothDeviceSelectListener listener) throws BluetoothNotAvailableException {
        this.context = context;
        this.listener = listener;
        this.adapter = BluetoothAdapter.getDefaultAdapter();
        fetchDevices();
    }

    private void fetchDevices() throws BluetoothNotAvailableException {
        if (adapter == null || !adapter.isEnabled()) {
            throw new BluetoothNotAvailableException();
        }
        scanner = adapter.getBluetoothLeScanner();
    }

    public void show() {
        devices.addAll(adapter.getBondedDevices());

        if (scanner != null) {
            if (adapter.isDiscovering()) {
                adapter.cancelDiscovery();
            }
            scanner.startScan(new ArrayList<>(),
                    new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .build(),
                    callback);
            Toast.makeText(context, R.string.scanning, Toast.LENGTH_LONG).show();
        }


        adapter.startDiscovery();

        showSelection();
    }

    private void showSelection() {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);

        arrayAdapter = new ArrayAdapter<>(context, R.layout.select_dialog_singlechoice_material);
        for (BluetoothDevice device : devices) {
            arrayAdapter.add(getNameFor(device));
        }

        builderSingle.setTitle(R.string.selectBluetoothDevice);
        builderSingle.setAdapter(arrayAdapter, (dialog, which) -> {
            stopScan();
            listener.onSelectBluetoothDevice(devices.get(which));
        });
        builderSingle.setOnCancelListener(dialog -> stopScan());
        builderSingle.show();
    }

    private void addDevice(BluetoothDevice device) {
        devices.add(device);
        arrayAdapter.add(getNameFor(device));
    }

    private String getNameFor(BluetoothDevice device) {
        return ((device.getName() == null || device.getName().equals("null")) ? context.getString(R.string.unknown) : device.getName()) +
                " (" + device.getAddress() + ")";
    }

    private void stopScan() {
        if (scanner != null) {
            scanner.stopScan(callback);
        }
    }

    private boolean containsDevice(BluetoothDevice device) {
        for (BluetoothDevice other : devices) {
            if (other.getAddress().equals(device.getAddress())) {
                return true;
            }
        }
        return false;
    }

    private class DeviceDialogScanCallback extends ScanCallback {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device != null && !containsDevice(device)) {
                Log.d("DeviceScanner", "Found new device: " + getNameFor(device));
                addDevice(device);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                onScanResult(0, result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.d("DeviceScanner", "Failed to scan, error code: " + errorCode);
            Toast.makeText(context, R.string.scanFailed, Toast.LENGTH_LONG).show();
        }
    }

    public interface BluetoothDeviceSelectListener {
        void onSelectBluetoothDevice(BluetoothDevice device);
    }

    public static class BluetoothNotAvailableException extends Exception {
    }

}
