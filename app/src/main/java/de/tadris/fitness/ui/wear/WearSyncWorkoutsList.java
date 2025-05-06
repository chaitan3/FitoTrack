/*
 * Copyright (c) 2025 Jannis Scheibe <jannis@tadris.de>
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
package de.tadris.fitness.ui.wear;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.data.AppDatabase;
import de.tadris.fitness.data.GpsSample;
import de.tadris.fitness.data.GpsWorkout;
import de.tadris.fitness.data.IndoorSample;
import de.tadris.fitness.data.IndoorWorkout;
import de.tadris.fitness.data.RecordingType;
import de.tadris.fitness.data.WorkoutType;
import de.tadris.fitness.data.WorkoutTypeManager;

public class WearSyncWorkoutsList extends Fragment implements CommsBT.BTInterface{
    private WearSyncActivity activity;
    private TextView title;
    private LinearLayout list;

    private Handler handler;
    private AppDatabase db;
    private double minHeight;
    private double maxHeight;
    private final List<WearSyncWorkout> wearSyncWorkouts = new ArrayList<>();
    private WearSyncWorkout wearSyncWorkoutInProgress;

    @Override public void onAttach(@NonNull Context context){
        super.onAttach(context);
        try{
            activity = (WearSyncActivity) context;
            handler = new Handler(Looper.getMainLooper());
        }catch(ClassCastException e){
            Log.e(WearSyncActivity.LOG_TAG, "WearSyncWorkoutsList.onAttach exception: " + e.getMessage());
        }
    }
    @Override public @Nullable View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        View rootView = inflater.inflate(R.layout.fragment_wear_sync, container, false);
        title = rootView.findViewById(R.id.wearSyncTitle);
        list = rootView.findViewById(R.id.wearSyncList);
        list.setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.wearSyncIcon).setVisibility(View.GONE);
        return rootView;
    }

    @Override public void onResume(){
        super.onResume();
        if(activity.commsBT == null) activity.finishAndRemoveTask();
        title.setText(getString(R.string.wear_connected_to).replace("watch", activity.commsBT.deviceName));
        activity.commsBT.addListener(this);
        activity.runInBackground(()->{
            handler.postDelayed(onBTTimeout, 5000);
            activity.commsBT.sendSyncRequest();
        });
    }
    @Override public void onPause(){
        super.onPause();
        activity.commsBT.delListener(this);
    }

    @SuppressLint("MissingPermission")//Permissions are handled in WearSyncActivity.onCreate
    @Override public void onBTStartDone(){}
    @Override public void onBTConnecting(String deviceName){}
    @Override public void onBTConnectFailed(){}
    @Override public void onBTConnected(String deviceName){}
    @Override public void onBTBytesReceived(int length){
        handler.removeCallbacks(onBTTimeout);
        handler.postDelayed(onBTTimeout, 5000);
        if(wearSyncWorkoutInProgress != null)
            activity.runOnUiThread(()->wearSyncWorkoutInProgress.setDownloading(length));
    }
    @Override public void onBTDisconnected(){
        Log.d(WearSyncActivity.LOG_TAG, "onBTDisconnected");
        activity.runOnUiThread(()->title.setText(R.string.wear_disconnected));
    }
    @Override public void onBTResponse(JSONObject response){
        Log.d(WearSyncActivity.LOG_TAG, "onBTResponse");
        handler.removeCallbacks(onBTTimeout);
        try{
            if(response.getInt("version") != CommsBT.COMMS_VERSION){onBTError(R.string.wear_error_update);return;}
            switch(response.getString("requestType")){
                case "sync":
                    syncResponse(response);
                    break;
                case "getWorkout":
                    getWorkoutResponse(response);
                    break;
                default:
                    onBTError(R.string.wear_error_tech);
            }
        }catch(Exception e){
            Log.e(WearSyncActivity.LOG_TAG, "onBTResponse exception: " + e);
            Log.e(WearSyncActivity.LOG_TAG, "onBTResponse exception: " + e.getMessage());
            onBTError(R.string.wear_error_tech);
        }
    }
    private void syncResponse(JSONObject response){
        Log.d(WearSyncActivity.LOG_TAG, "syncResponse");
        try{
            JSONArray workouts = response.getJSONArray("workouts");
            for(int i=0; i<workouts.length(); i++) addWearSyncWorkout(workouts.getJSONObject(i));
            Thread.sleep(500);//give the UI thread some time to catch up
            sync();
        }catch(Exception e){
            Log.e(WearSyncActivity.LOG_TAG, "syncResponse exception: " + e);
            Log.e(WearSyncActivity.LOG_TAG, "syncResponse exception: " + e.getMessage());
            onBTError(R.string.wear_error_tech);
        }
    }
    private void addWearSyncWorkout(JSONObject watchWorkout) throws JSONException{
        String name = watchWorkout.getString("name") + " " + watchWorkout.getString("workoutType");
        long workoutId = watchWorkout.getLong("id");
        boolean exists = checkWorkoutExists(workoutId);
        activity.runOnUiThread(()->{
            WearSyncWorkout wearSyncWorkout = new WearSyncWorkout(activity, name, workoutId, exists);
            wearSyncWorkouts.add(wearSyncWorkout);
            list.addView(wearSyncWorkout);
        });
    }
    private boolean checkWorkoutExists(long workoutId){
        return getDB().gpsWorkoutDao().getWorkoutById(workoutId) != null || getDB().indoorWorkoutDao().getWorkoutById(workoutId) != null;
    }
    private void sync(){
        for(WearSyncWorkout wearSyncWorkout : wearSyncWorkouts){
            if(!wearSyncWorkout.done){
                wearSyncWorkoutInProgress = wearSyncWorkout;
                handler.postDelayed(onBTTimeout, 5000);
                activity.commsBT.sendGetWorkoutRequest(wearSyncWorkout.workoutId);
                activity.runOnUiThread(()->wearSyncWorkoutInProgress.setDownloading(0));
                return;
            }
        }
        activity.finishAndRemoveTask();
    }

    private void getWorkoutResponse(JSONObject response){
        Log.d(WearSyncActivity.LOG_TAG, "getWorkoutResponse");
        long workoutId = 0;
        try{
            if(response.has("error")){
                onBTError(R.string.wear_error_tech);
                return;
            }
            activity.runOnUiThread(wearSyncWorkoutInProgress::setProcessing);
            JSONObject watchWorkout = response.getJSONObject("workout");
            workoutId = watchWorkout.getLong("id");
            if(checkWorkoutExists(workoutId)){
                Log.e(WearSyncActivity.LOG_TAG, "getWorkoutResponse somehow already in db, skipping");
                doneNext();
                return;
            }
            String type = watchWorkout.getString("workoutType");
            if (getWorkoutType(type).getRecordingType() == RecordingType.GPS) {
                List<GpsSample> samples = getGpsSamples(watchWorkout, workoutId);
                GpsWorkout gpsWorkout = toGpsWorkout(watchWorkout, workoutId);
                getDB().gpsWorkoutDao().insertWorkoutAndSamples(gpsWorkout, samples.toArray(new GpsSample[0]));
            }else{
                List<IndoorSample> samples = getIndoorSamples(watchWorkout, workoutId);
                IndoorWorkout indoorWorkout = toIndoorWorkout(watchWorkout, workoutId);
                getDB().indoorWorkoutDao().insertWorkoutAndSamples(indoorWorkout, samples.toArray(new IndoorSample[0]));
            }
            doneNext();
        }catch(Exception e){
            Log.e(WearSyncActivity.LOG_TAG, "getWorkoutResponse exception: " + e);
            Log.e(WearSyncActivity.LOG_TAG, "getWorkoutResponse exception: " + e.getMessage());
            if(workoutId > 0){
                activity.runOnUiThread(()->{
                    wearSyncWorkoutInProgress.setFail();
                    activity.runInBackground(this::sync);
                });
            }else{
                onBTError(R.string.wear_error_tech);
            }
        }
    }
    private void doneNext(){
        activity.runOnUiThread(()->{
            wearSyncWorkoutInProgress.setDone();
            activity.runInBackground(this::sync);
        });
    }
    private AppDatabase getDB(){
        if(db == null) db = Instance.getInstance(activity).db;
        return db;
    }
    private IndoorWorkout toIndoorWorkout(JSONObject watchWorkout, long workoutId) throws JSONException{
        IndoorWorkout indoorWorkout = new IndoorWorkout();
        indoorWorkout.edited = false;
        indoorWorkout.setWorkoutType(getWorkoutType(watchWorkout.getString("workoutType")));
        indoorWorkout.id = workoutId;
        indoorWorkout.start = workoutId;
        indoorWorkout.duration = watchWorkout.getLong("durationTotal");
        indoorWorkout.repetitions = (watchWorkout.getJSONArray("checkpoints").length()+1)/2;
        if(watchWorkout.has("pauseTotal")) indoorWorkout.pauseDuration = watchWorkout.getLong("pauseTotal");
        indoorWorkout.end = indoorWorkout.start + indoorWorkout.duration + indoorWorkout.pauseDuration;
        if(watchWorkout.has("heartRateAvg")) indoorWorkout.avgHeartRate = (int) watchWorkout.getDouble("heartRateAvg");
        if(watchWorkout.has("heartRateMax")) indoorWorkout.maxHeartRate = watchWorkout.getInt("heartRateMax");
        if(watchWorkout.has("caloriesTotal")) indoorWorkout.calorie = (int) watchWorkout.getDouble("caloriesTotal");
        return indoorWorkout;
    }
    private List<IndoorSample> getIndoorSamples(JSONObject watchWorkout, long workoutId) throws Exception{
        List<IndoorSample> samples = new ArrayList<>();
        //get checkpoints
        JSONArray checkpoints = watchWorkout.getJSONArray("checkpoints");
        int icp = 0;
        JSONObject checkpoint = checkpoints.getJSONObject(icp);
        long icpTime = checkpoint.getLong("time");
        int icpDuration = checkpoint.getInt("duration");
        long ncpTime = checkpoints.length() > 1 ? checkpoints.getJSONObject(1).getLong("time") : 0;

        //get heart rates
        JSONArray heartRates = gzipToJSONArray(watchWorkout.getString("heartRates"));
        for(int ihr=0; ihr<heartRates.length(); ihr++){
            JSONObject heartRate = heartRates.getJSONObject(ihr);
            long ihrTime = heartRate.getLong("time");
            int ihrHeartRate = heartRate.getInt("heartRate");

            while(ncpTime > 0 && ncpTime < ihrTime){
                icp++;
                checkpoint = checkpoints.getJSONObject(icp);
                icpTime = checkpoint.getLong("time");
                icpDuration = checkpoint.getInt("duration");
                ncpTime = checkpoints.length() > icp+1 ? checkpoints.getJSONObject(icp+1).getLong("time") : 0;
            }

            IndoorSample sample = new IndoorSample();
            sample.absoluteTime = ihrTime;
            sample.relativeTime = ihrTime - icpTime + icpDuration;
            sample.heartRate = ihrHeartRate;
            //sample.repetitions
            //sample.intensity
            //sample.frequency
            sample.id = workoutId + ihr + 1;
            sample.workoutId = workoutId;
            samples.add(sample);
        }
        return samples;
    }
    private GpsWorkout toGpsWorkout(JSONObject watchWorkout, long workoutId) throws JSONException{
        GpsWorkout gpsWorkout = new GpsWorkout();
        gpsWorkout.edited = false;
        gpsWorkout.setWorkoutType(getWorkoutType(watchWorkout.getString("workoutType")));
        gpsWorkout.id = workoutId;
        gpsWorkout.start = workoutId;
        gpsWorkout.duration = watchWorkout.getLong("durationTotal");
        if(watchWorkout.has("pauseTotal")) gpsWorkout.pauseDuration = watchWorkout.getLong("pauseTotal");
        gpsWorkout.end = gpsWorkout.start + gpsWorkout.duration + gpsWorkout.pauseDuration;
        if(watchWorkout.has("heartRateAvg")) gpsWorkout.avgHeartRate = (int) watchWorkout.getDouble("heartRateAvg");
        if(watchWorkout.has("heartRateMax")) gpsWorkout.maxHeartRate = watchWorkout.getInt("heartRateMax");
        if(watchWorkout.has("caloriesTotal")) gpsWorkout.calorie = (int) watchWorkout.getDouble("caloriesTotal");
        if(watchWorkout.has("distanceTotal")) gpsWorkout.length = (int) watchWorkout.getDouble("distanceTotal");
        if(watchWorkout.has("elevationGainTotal")) gpsWorkout.ascent = watchWorkout.getInt("elevationGainTotal");
        if(watchWorkout.has("elevationLossTotal")) gpsWorkout.descent = watchWorkout.getInt("elevationLossTotal");
        if(watchWorkout.has("speedAvg")){
            gpsWorkout.avgSpeed = watchWorkout.getDouble("speedAvg");
            gpsWorkout.avgPace = 1/(gpsWorkout.avgSpeed/1000*60);
        }
        if(watchWorkout.has("speedMax")) gpsWorkout.topSpeed = watchWorkout.getDouble("speedMax");
        gpsWorkout.minElevationMSL = (float) minHeight;
        gpsWorkout.maxElevationMSL = (float) maxHeight;
        return gpsWorkout;
    }
    private List<GpsSample> getGpsSamples(JSONObject watchWorkout, long workoutId) throws Exception{
        List<GpsSample> samples = new ArrayList<>();

        //get checkpoints
        JSONArray checkpoints = watchWorkout.getJSONArray("checkpoints");
        int icp = 0;
        JSONObject checkpoint = checkpoints.getJSONObject(icp);
        long icpTime = checkpoint.getLong("time");
        int icpDuration = checkpoint.getInt("duration");
        long ncpTime = checkpoints.length() > 1 ? checkpoints.getJSONObject(1).getLong("time") : 0;

        //get heart rates
        JSONArray heartRates = gzipToJSONArray(watchWorkout.getString("heartRates"));
        int ihr = 0;
        int ihrHeartRate = 0;
        if(heartRates.length() > 0){
            JSONObject heartRate = heartRates.getJSONObject(ihr);
            ihrHeartRate = heartRate.getInt("heartRate");
        }
        long nhrTime = heartRates.length() > 1 ? heartRates.getJSONObject(1).getLong("time") : 0;

        //get locations
        JSONArray locations = gzipToJSONArray(watchWorkout.getString("locations"));
        minHeight = 100000;
        maxHeight = 0;
        double startElevation = 0.0;
        for(int il=0; il<locations.length(); il++){
            JSONObject location = locations.getJSONObject(il);
            if(location.getDouble("ele") != 0.0){
                startElevation = location.getDouble("ele");
                break;
            }
        }
        for(int il=0; il<locations.length(); il++){
            JSONObject location = locations.getJSONObject(il);
            long ilTime = location.getLong("time");
            while(ncpTime > 0 && ncpTime < ilTime){
                icp++;
                checkpoint = checkpoints.getJSONObject(icp);
                icpTime = checkpoint.getLong("time");
                icpDuration = checkpoint.getInt("duration");
                ncpTime = checkpoints.length() > icp+1 ? checkpoints.getJSONObject(icp+1).getLong("time") : 0;
            }
            while(nhrTime > 0 && nhrTime < ilTime){
                ihr++;
                JSONObject heartRate = heartRates.getJSONObject(ihr);
                ihrHeartRate = heartRate.getInt("heartRate");
                nhrTime = heartRates.length() > ihr + 1 ? heartRates.getJSONObject(ihr + 1).getLong("time") : 0;
            }

            GpsSample sample = new GpsSample();
            sample.lat = location.getDouble("lat");
            sample.lon = location.getDouble("lon");
            sample.elevation = location.getDouble("ele");
            if(sample.elevation == 0.0){
                sample.elevation = startElevation;
            }else{
                if(sample.elevation < minHeight) minHeight = sample.elevation;
                if(sample.elevation > maxHeight) maxHeight = sample.elevation;
            }
            sample.elevationMSL = sample.elevation;
            sample.speed = location.getDouble("speed");
            sample.absoluteTime = ilTime;
            sample.relativeTime = ilTime - icpTime + icpDuration;
            //sample.pressure
            sample.heartRate = ihrHeartRate;
            //sample.intervalTriggered
            sample.id = workoutId + il + 1;
            sample.workoutId = workoutId;
            samples.add(sample);
        }
        if(minHeight == 100000) minHeight = maxHeight;
        return samples;
    }
    private WorkoutType getWorkoutType(String input){
        String workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_OTHER;
        switch(input){
            case "Biking":
                workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_CYCLING;
                break;
            case "Climbing":
                workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_CLIMBING;
                break;
            case "Hiking":
                workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_HIKING;
                break;
            case "Running":
                workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_RUNNING;
                break;
            case "Snowboarding":
                workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_SNOWBOARDING;
                break;
            case "Snowshoeing":
                workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_SNOWSHOEING;
                break;
            case "Swim (outdoor)":
                workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_SWIMMING;
                break;
            case "Walking":
                workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_WALKING;
                break;
            case "Windsurfing":
                workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_WINDSURFING;
                break;
        }
        WorkoutType workoutType = WorkoutTypeManager.getInstance().getWorkoutTypeById(activity, workoutTypeId);
        if(Objects.equals(workoutTypeId, WorkoutTypeManager.WORKOUT_TYPE_ID_OTHER)){
            workoutType.title = input;
            workoutType.recordingType = RecordingType.GPS.id;
        }
        return workoutType;
    }
    private JSONArray gzipToJSONArray(String input) throws Exception{
        ByteArrayInputStream bin = new ByteArrayInputStream(Base64.decode(input, Base64.DEFAULT));
        BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(bin)));
        return new JSONArray(br.readLine());
    }
    @Override public void onBTError(int message){
        Log.d(WearSyncActivity.LOG_TAG, "onBTError: " + getString(message));
        activity.runOnUiThread(()->title.setText(message));
    }
    private final Runnable onBTTimeout = ()->onBTError(R.string.wear_error_tech);
}
