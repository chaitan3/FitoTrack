/*
 * Copyright (c) 2025 Bart Vullings <dev@windkracht8.com>
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

package de.tadris.fitness.util;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import de.tadris.fitness.R;
import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.data.WorkoutDataStore;

public class CommsBT extends Service{
    private final String LOG_TAG = "FitoTrack.CommsBT";
    private final UUID FITOTRACK_UUID = UUID.fromString("44fb140d-74e8-4334-8df0-aa717f873db3");
    private final int COMMS_VERSION = 2;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothServerSocket bluetoothServerSocket;
    private BluetoothSocket bluetoothSocket;
    private CommsBTConnect commsBTConnect;
    private CommsBTConnected commsBTConnected;

    private static boolean disconnect = true;
    private final JSONArray responseQueue = new JSONArray();

    @Nullable @Override public IBinder onBind(Intent intent){return null;}

    @Override public void onCreate(){
        super.onCreate();
        startBT();
    }

    @Override public void onDestroy(){
        stopBT();
        super.onDestroy();
    }

    private void gotRequest(String request){
        Log.d(LOG_TAG, "gotRequest: " + request);
        try{
            JSONObject requestMessage = new JSONObject(request);
            String requestType = requestMessage.getString("requestType");
            if(requestType.equals("sync")){
                sendResponseSync();
            }else if(requestType.equals("getWorkout")){
                sendResponseGetWorkout(requestMessage.getLong("workoutId"));
            }else{
                Log.e(LOG_TAG, "gotRequest Unknown requestType: " + requestType);
            }
        }catch(Exception e){
            Log.e(LOG_TAG, "gotRequest Exception: " + e.getMessage());
            Toast.makeText(this, R.string.error_bt_request, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendResponseSync(){
        try{
            JSONObject response = new JSONObject();
            response.put("version", COMMS_VERSION);
            response.put("requestType", "sync");
            JSONArray jsonWorkouts = new JSONArray();
            WorkoutDataStore.readWorkouts(this);
            for(int i=0; i<WorkoutDataStore.workouts.size(); i++){
                jsonWorkouts.put(WorkoutDataStore.workouts.get(i).toJson(this));
            }
            response.put("workouts", jsonWorkouts);
            responseQueue.put(response);
        }catch(Exception e){
            Log.e(LOG_TAG, "sendResponse JSONObject Exception: " + e.getMessage());
            Toast.makeText(this, R.string.error_bt_response, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendResponseGetWorkout(long id){
        try{
            JSONObject response = new JSONObject();
            response.put("version", COMMS_VERSION);
            response.put("requestType", "getWorkout");
            WorkoutData workoutData = WorkoutDataStore.getFullWorkout(this, id);
            if(workoutData == null){
                response.put("error", 404);
            }else{
                JSONObject workoutData_json = workoutData.toJson(this);

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                GZIPOutputStream gos = new GZIPOutputStream(bos);
                gos.write(workoutData.locationsToJsonString().getBytes());
                gos.close();
                workoutData_json.put("locations", Base64.getEncoder().encodeToString(bos.toByteArray()));

                bos.reset();
                gos = new GZIPOutputStream(bos);
                gos.write(workoutData.heartRatesToJsonString().getBytes());
                gos.close();
                workoutData_json.put("heartRates", Base64.getEncoder().encodeToString(bos.toByteArray()));

                response.put("workout", workoutData_json);
            }
            responseQueue.put(response);
        }catch(Exception e){
            Log.e(LOG_TAG, "sendResponse JSONObject Exception: " + e.getMessage());
            Toast.makeText(this, R.string.error_bt_response, Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver btStateReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent){
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){
                int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if(btState == BluetoothAdapter.STATE_TURNING_OFF){
                    Log.d(LOG_TAG, "btStateReceiver: stop");
                    stopBT();
                }else if(btState == BluetoothAdapter.STATE_ON){
                    Log.d(LOG_TAG, "btStateReceiver: start");
                    startBT();
                }
            }
        }
    };

    private void startBT(){
        Log.d(LOG_TAG, "startBT");
        disconnect = false;
        if(bluetoothAdapter == null){
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
            if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
                Log.d(LOG_TAG, "startBT bluetooth disabled");
                return;
            }
            registerReceiver(btStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        }
        if(commsBTConnect == null){
            Log.d(LOG_TAG, "startBT start listening");
            commsBTConnect = new CommsBTConnect();
            commsBTConnect.start();
        }
    }

    private void stopBT(){
        Log.d(LOG_TAG, "stopBT");
        disconnect = true;
        try{
            unregisterReceiver(btStateReceiver);
        }catch(Exception e){
            Log.i(LOG_TAG, "stopBT unregisterReceiver: " + e.getMessage());
        }
        try{
            if(bluetoothServerSocket != null) bluetoothServerSocket.close();
        }catch(Exception e){
            Log.i(LOG_TAG, "stopBT bluetoothServerSocket: " + e.getMessage());
        }
        try{
            if(bluetoothSocket != null) bluetoothSocket.close();
        }catch(Exception e){
            Log.i(LOG_TAG, "stopBT bluetoothSocket: " + e.getMessage());
        }
        bluetoothServerSocket = null;
        bluetoothSocket = null;
        bluetoothAdapter = null;
        commsBTConnect = null;
        commsBTConnected = null;
    }

    private class CommsBTConnect extends Thread{
        @SuppressLint("MissingPermission")//Permissions are handled in Main
        private CommsBTConnect(){
            try{
                Log.d(LOG_TAG, "CommsBTConnect");
                bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("FitoTrack", FITOTRACK_UUID);
            }catch(Exception e){
                if(disconnect) return;
                Log.e(LOG_TAG, "CommsBTConnect Exception: " + e.getMessage());
            }
        }
        public void run(){
            try{
                bluetoothSocket = bluetoothServerSocket.accept();
                if(disconnect) return;
                Log.d(LOG_TAG, "CommsBTConnect.run accepted");
                commsBTConnected = new CommsBTConnected();
                commsBTConnected.start();
            }catch(Exception e){
                if(disconnect) return;
                Log.e(LOG_TAG, "CommsBTConnect.run Exception: " + e.getMessage());
            }
        }
    }

    private class CommsBTConnected extends Thread{
        private InputStream inputStream;
        private OutputStream outputStream;

        CommsBTConnected(){
            try{
                Log.d(LOG_TAG, "CommsBTConnected.getInputStream");
                inputStream = bluetoothSocket.getInputStream();
            }catch(Exception e){
                Log.e(LOG_TAG, "CommsBTConnected getInputStream Exception: " + e.getMessage());
            }
            try{
                Log.d(LOG_TAG, "CommsBTConnected.getOutputStream");
                outputStream = bluetoothSocket.getOutputStream();
            }catch(Exception e){
                Log.e(LOG_TAG, "CommsBTConnected getOutputStream Exception: " + e.getMessage());
            }
        }
        public void run(){
            Log.d(LOG_TAG, "CommsBTConnected.run");
            process(Executors.newSingleThreadScheduledExecutor());
        }
        private void process(ScheduledExecutorService executor){
            if(disconnect){return;}
            read();
            try{
                outputStream.write("".getBytes());
            }catch(Exception e){
                stopBT();
                startBT();
                return;
            }
            if(responseQueue.length() > 0 && !sendNextResponse()){
                stopBT();
                startBT();
                return;
            }
            executor.schedule(()->process(executor), 100, TimeUnit.MILLISECONDS);
        }
        private boolean sendNextResponse(){
            try{
                JSONObject response = (JSONObject) responseQueue.get(0);
                responseQueue.remove(0);
                Log.d(LOG_TAG, "CommsBTConnected.sendNextResponse: " + response.toString());
                outputStream.write(response.toString().getBytes());
            }catch(Exception e){
                Log.e(LOG_TAG, "CommsBTConnected.sendNextResponse Exception: " + e.getMessage());
                Toast.makeText(getBaseContext(), R.string.error_bt_response, Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }
        private void read(){
            try{
                if(inputStream.available() < 5) return;
                long last_read_time = System.currentTimeMillis();
                String request = "";
                while(System.currentTimeMillis() - last_read_time < 3000){
                    if(inputStream.available() == 0){
                        sleep100();
                        continue;
                    }
                    byte[] buffer = new byte[inputStream.available()];
                    int numBytes = inputStream.read(buffer);
                    if(numBytes < 0){
                        Log.e(LOG_TAG, "CommsBTConnected.read read error, request: " + request);
                        Toast.makeText(getBaseContext(), R.string.error_bt_request, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String temp = new String(buffer);
                    request += temp;
                    if(isValidJSON(request)){
                        gotRequest(request);
                        return;
                    }
                    last_read_time = System.currentTimeMillis();
                }
                Log.e(LOG_TAG, "CommsBTConnected.read no valid message and no new data after 3 sec: " + request);
            }catch(Exception e){
                Log.e(LOG_TAG, "CommsBTConnected.read Exception: " + e.getMessage());
            }
            Toast.makeText(getBaseContext(), R.string.error_bt_request, Toast.LENGTH_SHORT).show();
        }
        private void sleep100(){
            try{
                Thread.sleep(100);
            }catch(Exception e){
                Log.e(LOG_TAG, "CommsBTConnected.sleep100 exception: " + e.getMessage());
            }
        }
        private boolean isValidJSON(String json){
            try{
                new JSONObject(json);
            }catch(JSONException e){
                return false;
            }
            return true;
        }
    }
}
