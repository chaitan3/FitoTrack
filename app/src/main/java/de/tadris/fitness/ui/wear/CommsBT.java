/*
 * Copyright (c) 2025 Jannis Scheibe <jannis@tadris.de>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU General Public License as published by
 * 	the Free Software Foundation, either version 3 of the License, or
 * 	(at your option) any later version.
 *
 * 	FitoTrack is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 *
 * 	You should have received a copy of the GNU General Public License
 * 	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.tadris.fitness.ui.wear;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import de.tadris.fitness.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressLint("MissingPermission")//Permissions are handled in the activity
class CommsBT{
    private final String LOG_TAG = "FitoTrack.CommsBT";
    private final UUID FITOTRACK_UUID = UUID.fromString("44fb140d-74e8-4334-8df0-aa717f873db3");
    static final int COMMS_VERSION = 2;
    private SharedPreferences.Editor sharedPrefsEditor;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private CommsBTConnect commsBTConnect;
    private CommsBTConnected commsBTConnected;

    enum Status{DISCONNECTED, CONNECTING, CONNECTED}
    Status status = Status.DISCONNECTED;
    String deviceName;
    private boolean disconnect = false;
    private boolean startDone = false;
    private Set<String> known_device_addresses = new HashSet<>();
    private final JSONArray requestQueue = new JSONArray();
    private final List<BTInterface> listeners = new ArrayList<>();

    void startBT(Context context){
        Log.d(LOG_TAG, "startBT");
        BluetoothManager bm = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bm.getAdapter();
        if(bluetoothAdapter == null){
            onBTError(R.string.wear_error_denied);
            return;
        }

        if(bluetoothAdapter.getState() != BluetoothAdapter.STATE_ON){
            onBTError(R.string.wear_error_off);
            return;
        }
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPrefsEditor = sharedPrefs.edit();

        known_device_addresses = sharedPrefs.getStringSet("known_device_addresses", known_device_addresses);
        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
        Log.d(LOG_TAG, "startBT " + known_device_addresses.size() + " known devices");

        if(devices.isEmpty()){
            onBTError(R.string.wear_error_no_devices);
            return;
        }
        if(known_device_addresses.isEmpty()){
            onBTStartDone();
            return;
        }

        //Clean known devices
        for(String known_device_address : known_device_addresses){
            boolean stillBonded = false;
            for(BluetoothDevice device : devices){
                if(device.getAddress().equals(known_device_address)){
                    stillBonded = true;
                    break;
                }
            }
            if(!stillBonded) removeKnownBTAddress(known_device_address);
        }

        //Try to connect to known device
        for(BluetoothDevice device : devices){
            if(known_device_addresses.contains(device.getAddress())){
                connectDevice(device);
                return;
            }
        }
        onBTStartDone();
    }
    void stopBT(){
        disconnect = true;
        try{
            if(bluetoothSocket != null) bluetoothSocket.close();
        }catch(Exception e){
            Log.e(LOG_TAG, "stopBT bluetoothSocket: " + e.getMessage());
        }
        bluetoothSocket = null;
        commsBTConnect = null;
        commsBTConnected = null;
        listeners.clear();
    }

    Set<BluetoothDevice> getDevices(){
        if(bluetoothAdapter == null){
            onBTError(R.string.wear_error_denied);
            return null;
        }
        if(bluetoothAdapter.getState() != BluetoothAdapter.STATE_ON){
            onBTError(R.string.wear_error_off);
            return null;
        }
        return bluetoothAdapter.getBondedDevices();
    }
    List<BluetoothDevice> getKnownDevices(){
        ArrayList<BluetoothDevice> knownBTDevices = new ArrayList<>();
        if(bluetoothAdapter == null){
            onBTError(R.string.wear_error_denied);
            return knownBTDevices;
        }
        if(bluetoothAdapter.getState() != BluetoothAdapter.STATE_ON){
            onBTError(R.string.wear_error_off);
            return knownBTDevices;
        }
        for(BluetoothDevice device : bluetoothAdapter.getBondedDevices()){
            if(known_device_addresses.contains(device.getAddress())){
                knownBTDevices.add(device);
            }
        }
        return knownBTDevices;
    }
    void removeKnownBTAddress(String address){
        if(!known_device_addresses.contains(address)) return;
        known_device_addresses.remove(address);
        storeKnownBTAddresses();
    }
    private void storeKnownBTAddresses(){
        if(known_device_addresses.isEmpty()){
            sharedPrefsEditor.remove("known_device_addresses");
        }else{
            sharedPrefsEditor.putStringSet("known_device_addresses", known_device_addresses);
        }
        sharedPrefsEditor.apply();
    }

    void connectDevice(BluetoothDevice device){
        Log.d(LOG_TAG, "connectDevice: " + device.getName());
        if(bluetoothAdapter == null ||
                status != Status.DISCONNECTED ||
                bluetoothAdapter.getState() != BluetoothAdapter.STATE_ON ||
                commsBTConnect != null
        ) return;
        disconnect = false;
        onBTConnecting(device.getName());
        commsBTConnect = new CommsBT.CommsBTConnect(device);
        commsBTConnect.start();
    }

    void sendSyncRequest(){
        Log.d(LOG_TAG, "sendSyncRequest");
        try{
            JSONObject request = new JSONObject();
            request.put("version", COMMS_VERSION);
            request.put("requestType", "sync");
            requestQueue.put(request);
        }catch(Exception e){
            Log.e(LOG_TAG, "sendSyncRequest Exception: " + e.getMessage());
        }
    }
    void sendGetWorkoutRequest(long workoutId){
        Log.d(LOG_TAG, "sendGetWorkoutRequest");
        try{
            JSONObject request = new JSONObject();
            request.put("version", COMMS_VERSION);
            request.put("requestType", "getWorkout");
            request.put("workoutId", workoutId);
            requestQueue.put(request);
        }catch(Exception e){
            Log.e(LOG_TAG, "sendGetWorkoutRequest Exception: " + e.getMessage());
        }
    }

    private class CommsBTConnect extends Thread{
        private CommsBTConnect(BluetoothDevice device){
            Log.d(LOG_TAG, "CommsBTConnect " + device.getName());
            try{
                bluetoothSocket = device.createRfcommSocketToServiceRecord(FITOTRACK_UUID);
            }catch(Exception e){
                Log.e(LOG_TAG, "CommsBTConnect Exception: " + e.getMessage());
                onBTConnectFailed();
            }
        }
        public void run(){
            bluetoothAdapter.cancelDiscovery();
            try{
                bluetoothSocket.connect();
                commsBTConnected = new CommsBTConnected();
                commsBTConnected.start();
            }catch(Exception e){
                Log.d(LOG_TAG, "CommsBTConnect.run failed: " + e);
                Log.d(LOG_TAG, "CommsBTConnect.run failed: " + e.getMessage());
                try{
                    bluetoothSocket.close();
                }catch(Exception e2){
                    Log.d(LOG_TAG, "CommsBTConnect.run close failed: " + e2.getMessage());
                }
                onBTConnectFailed();
            }
        }
    }
    private class CommsBTConnected extends Thread{
        private InputStream inputStream;
        private OutputStream outputStream;

        CommsBTConnected(){
            Log.d(LOG_TAG, "CommsBTConnected");
            try{
                inputStream = bluetoothSocket.getInputStream();
            }catch(Exception e){
                Log.e(LOG_TAG, "CommsBTConnected getInputStream Exception: " + e.getMessage());
                onBTDisconnected();
                return;
            }
            try{
                outputStream = bluetoothSocket.getOutputStream();
            }catch(Exception e){
                Log.e(LOG_TAG, "CommsBTConnected getOutputStream Exception: " + e.getMessage());
                onBTDisconnected();
                return;
            }
            onBTConnected(bluetoothSocket.getRemoteDevice());
        }
        public void run(){process(Executors.newSingleThreadScheduledExecutor());}
        private void close(){
            Log.d(LOG_TAG, "CommsBTConnected.close");
            onBTDisconnected();
            try{
                for(int i=requestQueue.length(); i>0; i--){
                    requestQueue.remove(i-1);
                }
                if(bluetoothSocket != null) bluetoothSocket.close();
            }catch(Exception e){
                Log.e(LOG_TAG, "CommsBTConnected.close exception: " + e.getMessage());
            }
            commsBTConnected = null;
            commsBTConnect = null;
        }
        private void process(ScheduledExecutorService executor){
            if(disconnect){
                close();
                return;
            }
            try{
                outputStream.write("".getBytes());
            }catch(Exception e){
                Log.d(LOG_TAG, "Connection closed");
                close();
                return;
            }
            if(!sendNextRequest()){
                close();
                return;
            }
            read();
            executor.schedule(()->process(executor), 100, TimeUnit.MILLISECONDS);
        }
        private boolean sendNextRequest(){
            try{
                outputStream.write("".getBytes());
                if(requestQueue.length() < 1) return true;
                JSONObject request = (JSONObject) requestQueue.get(0);
                requestQueue.remove(0);
                Log.d(LOG_TAG, "CommsBTConnected.sendNextRequest: " + request.toString());
                outputStream.write(request.toString().getBytes());
            }catch(Exception e){
                Log.e(LOG_TAG, "CommsBTConnected.sendNextRequest Exception: " + e.getMessage());
                onBTError(R.string.wear_error_tech);
                return false;
            }
            return true;
        }
        private void read(){
            try{
                if(inputStream.available() < 5) return;
                long last_read_time = System.currentTimeMillis();
                String response = "";
                while(System.currentTimeMillis() - last_read_time < 3000){
                    if(inputStream.available() == 0){
                        sleep100();
                        continue;
                    }
                    byte[] buffer = new byte[inputStream.available()];
                    int numBytes = inputStream.read(buffer);
                    if(numBytes < 0){
                        Log.e(LOG_TAG, "CommsBTConnected.read read error: " + response);
                        onBTError(R.string.wear_error_tech);
                        return;
                    }
                    String temp = new String(buffer);
                    response += temp;
                    if(isValidJSON(response)){
                        Log.d(LOG_TAG, "CommsBTConnected.read got message: " + response);
                        onBTResponse(new JSONObject(response));
                        return;
                    }
                    onBTBytesReceived(response.length());
                    last_read_time = System.currentTimeMillis();
                }
                Log.e(LOG_TAG, "CommsBTConnected.read no valid message and no new data after 3 sec: " + response);
            }catch(Exception e){
                Log.e(LOG_TAG, "CommsBTConnected.read: " + e.getMessage());
            }
            onBTError(R.string.wear_error_tech);
        }
        private void sleep100(){
            try{
                Thread.sleep(100);
            }catch(Exception e){
                Log.e(LOG_TAG, "CommsBTConnected.sleep100 exception: " + e.getMessage());
            }
        }
        private boolean isValidJSON(String json){
            if(!json.endsWith("}")) return false;
            try{
                new JSONObject(json);
            }catch(JSONException e){
                return false;
            }
            return true;
        }
    }

    void addListener(BTInterface listener){listeners.add(listener);}
    void delListener(BTInterface listener){listeners.remove(listener);}
    private void onBTStartDone(){
        Log.d(LOG_TAG, "onBTStartDone");
        startDone = true;
        listeners.remove(null);
        for(int i=0; i<listeners.size(); i++) listeners.get(i).onBTStartDone();
    }
    private void onBTConnecting(String deviceName){
        Log.d(LOG_TAG, "onBTConnecting");
        status = Status.CONNECTING;
        listeners.remove(null);
        for(int i=0; i<listeners.size(); i++) listeners.get(i).onBTConnecting(deviceName);
    }
    private void onBTConnectFailed(){
        Log.d(LOG_TAG, "onBTConnectFailed");
        commsBTConnect = null;
        status = Status.DISCONNECTED;
        if(startDone){
            listeners.remove(null);
            for(int i=0; i<listeners.size(); i++) listeners.get(i).onBTConnectFailed();
        }else{
            onBTStartDone();
        }
    }
    private void onBTConnected(BluetoothDevice device){
        Log.d(LOG_TAG, "onBTConnected");
        status = Status.CONNECTED;
        listeners.remove(null);
        deviceName = device.getName();
        for(int i=0; i<listeners.size(); i++) listeners.get(i).onBTConnected(device.getName());
        if(!startDone) onBTStartDone();
        if(!known_device_addresses.contains(device.getAddress())){
            known_device_addresses.add(device.getAddress());
            storeKnownBTAddresses();
        }
    }
    private void onBTDisconnected(){
        Log.d(LOG_TAG, "onBTDisconnected");
        status = Status.DISCONNECTED;
        listeners.remove(null);
        for(int i=0; i<listeners.size(); i++) listeners.get(i).onBTDisconnected();
    }
    private void onBTResponse(JSONObject response){
        Log.d(LOG_TAG, "onBTResponse " + response);
        listeners.remove(null);
        for(int i=0; i<listeners.size(); i++) listeners.get(i).onBTResponse(response);
    }
    private void onBTBytesReceived(int length){
        //Log.d(LOG_TAG, "onBTBytesReceived " + length);
        listeners.remove(null);
        for(int i=0; i<listeners.size(); i++) listeners.get(i).onBTBytesReceived(length);
    }
    private void onBTError(int message){
        Log.d(LOG_TAG, "onBTError");
        listeners.remove(null);
        for(int i=0; i<listeners.size(); i++) listeners.get(i).onBTError(message);
    }
    public interface BTInterface{
        void onBTStartDone();
        void onBTConnecting(String deviceName);
        void onBTConnectFailed();
        void onBTConnected(String deviceName);
        void onBTDisconnected();
        void onBTResponse(JSONObject response);
        void onBTBytesReceived(int size);
        void onBTError(int message);
    }
}
