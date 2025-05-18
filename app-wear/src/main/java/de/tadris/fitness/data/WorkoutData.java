/*
 * Copyright (c) 2025 Bart Vullings <dev@windkracht8.com>
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

package de.tadris.fitness.data;

import static de.tadris.fitness.util.Util.distanceDoubleToString;
import static de.tadris.fitness.util.Util.prettyDurationShort;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.health.services.client.ExerciseClient;
import androidx.health.services.client.data.DataType;
import androidx.health.services.client.data.ExerciseCapabilities;
import androidx.health.services.client.data.ExerciseType;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapsforge.core.model.LatLong;

import de.tadris.fitness.R;
import de.tadris.fitness.ui.Main;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class WorkoutData{
    private final SimpleDateFormat sdfGPXName = new SimpleDateFormat("E dd MMM HH:mm", Locale.ENGLISH);
    private ExerciseCapabilities capabilities;

    public long id = 0;
    public String description;
    public String name;

    public enum TargetType{NONE, DISTANCE, DURATION, SPEED, HEART_RATE}

    public TargetType primaryTargetType = TargetType.NONE;
    public int primaryTargetValue = 0;
    public int primaryTargetValueHigh = 0;
    public boolean targetAutoPause = false;
    public TargetType secondaryTargetType = TargetType.NONE;
    public int secondaryTargetValue = 0;
    public int secondaryTargetValueHigh = 0;

    public enum WorkoutType{BIKE, CLIMB, HIKE, RUN, SNOWBOARDING, SNOWSHOEING, SWIM_OUTDOOR, WALK, WINDSURFING, OTHER_INDOOR, OTHER_OUTDOOR}

    public WorkoutType workoutType = WorkoutType.OTHER_OUTDOOR;
    public ExerciseType exerciseType = ExerciseType.WALKING;
    public Double caloriesTotal = 0.0;
    public Double distanceTotal = 0.0;
    public final ArrayList<Checkpoint> checkpoints = new ArrayList<>();
    public long durationTotal = 0;
    public long pauseTotal = 0;
    public Double elevationGainTotal = 0.0;
    public Double elevationLossTotal = 0.0;
    public Double heartRateAvg = 0.0;
    public Double heartRateMax = 0.0;
    public Double speedAvg = 0.0;
    public Double speedMax = 0.0;

    public final ArrayList<LocationSample> locationSamples = new ArrayList<>();
    public final ArrayList<HeartRateSample> heartRateSamples = new ArrayList<>();

    public WorkoutData(ExerciseClient client, ListeningExecutorService listeningExecutorService){
        Futures.addCallback(
                client.getCapabilitiesAsync(),
                new FutureCallback<>(){
                    @Override public void onSuccess(ExerciseCapabilities result){capabilities = result;}
                    @Override public void onFailure(@NonNull Throwable throwable){Log.e(Main.LOG_TAG, "WorkoutData.getCapabilitiesAsync.onFailure: " + throwable);}
                },
                listeningExecutorService
        );

    }
    WorkoutData(JSONObject workoutData_json) throws JSONException{
        id = workoutData_json.getLong("id");
        description = workoutData_json.getString("desc");
        name = workoutData_json.getString("name");
        String workoutType_string = workoutData_json.getString("workoutType");
        setWorkoutType(workoutType_string);
        durationTotal = workoutData_json.getLong("durationTotal");
        if(workoutData_json.has("distanceTotal"))
            distanceTotal = workoutData_json.getDouble("distanceTotal");
        if(workoutData_json.has("elevationGainTotal"))
            elevationGainTotal = workoutData_json.getDouble("elevationGainTotal");
        if(workoutData_json.has("elevationLossTotal"))
            elevationLossTotal = workoutData_json.getDouble("elevationLossTotal");
        if(workoutData_json.has("pauseTotal")) pauseTotal = workoutData_json.getLong("pauseTotal");
        if(workoutData_json.has("speedAvg")) speedAvg = workoutData_json.getDouble("speedAvg");
        if(workoutData_json.has("speedMax")) speedMax = workoutData_json.getDouble("speedMax");
        if(workoutData_json.has("heartRateAvg"))
            heartRateAvg = workoutData_json.getDouble("heartRateAvg");
        if(workoutData_json.has("heartRateMax"))
            heartRateMax = workoutData_json.getDouble("heartRateMax");
        if(workoutData_json.has("caloriesTotal"))
            caloriesTotal = workoutData_json.getDouble("caloriesTotal");
        if(workoutData_json.has("checkpoints")){
            JSONArray checkpoints_json = workoutData_json.optJSONArray("checkpoints");
            if(checkpoints_json != null){
                for(int i = 0; i < checkpoints_json.length(); i++){
                    JSONObject checkpoint_json = checkpoints_json.getJSONObject(i);
                    addCheckpoint(checkpoint_json.getLong("time"), checkpoint_json.getLong("duration"));
                }
            }
        }
    }
    public static class SpeedCachePoint{
        public final long timestamp;
        public final int distance;
        public SpeedCachePoint(long timestamp, int distance){
            this.timestamp = timestamp;
            this.distance = distance;
        }
    }

    public static class Checkpoint{
        public final long time;
        public final long duration;
        private Checkpoint(long time, long duration){
            this.time = time;
            this.duration = duration;
        }
        private JSONObject toJson() throws JSONException{
            JSONObject checkpoint_json = new JSONObject();
            checkpoint_json.put("time", time);
            checkpoint_json.put("duration", duration);
            return checkpoint_json;
        }
    }
    public void addCheckpoint(long time, long duration){
        checkpoints.add(new Checkpoint(time, duration));
    }

    public static class LocationSample{
        public long time;
        public double lat = 0.0;
        public double lon = 0.0;
        public double ele = 0.0;
        public double speed = 0.0;
        public LocationSample(){}
        private LocationSample(JSONObject locationSample_json) throws JSONException{
            time = locationSample_json.getLong("time");
            lat = locationSample_json.getDouble("lat");
            lon = locationSample_json.getDouble("lon");
            ele = locationSample_json.getDouble("ele");
            speed = locationSample_json.getDouble("speed");
        }
        public LatLong toLatLong(){return new LatLong(lat, lon);}
        private JSONObject toJson() throws JSONException{
            JSONObject LocationSample_json = new JSONObject();
            LocationSample_json.put("time", time);
            LocationSample_json.put("lat", lat);
            LocationSample_json.put("lon", lon);
            LocationSample_json.put("ele", ele);
            LocationSample_json.put("speed", speed);
            return LocationSample_json;
        }
    }

    public static class HeartRateSample{
        public long time;
        public int heartRate = 0;
        public HeartRateSample(){}
        private HeartRateSample(JSONObject heartRateSample_json) throws JSONException{
            time = heartRateSample_json.getLong("time");
            heartRate = heartRateSample_json.getInt("heartRate");
        }
        private JSONObject toJson() throws JSONException{
            JSONObject LocationSample_json = new JSONObject();
            LocationSample_json.put("time", time);
            LocationSample_json.put("heartRate", heartRate);
            return LocationSample_json;
        }
    }

    public void setWorkoutType(WorkoutType workoutType){
        this.workoutType = workoutType;
        exerciseType = getExerciseType(workoutType);
    }
    private ExerciseType getExerciseType(WorkoutType workoutType){
        return switch(workoutType){
            case BIKE -> ExerciseType.BIKING;
            case CLIMB -> ExerciseType.ROCK_CLIMBING;
            case HIKE -> ExerciseType.HIKING;
            case RUN -> ExerciseType.RUNNING;
            case SNOWBOARDING -> ExerciseType.SNOWBOARDING;
            case SNOWSHOEING -> ExerciseType.SNOWSHOEING;
            case SWIM_OUTDOOR -> ExerciseType.SWIMMING_OPEN_WATER;
            case WINDSURFING -> ExerciseType.SAILING;//MET is comparable
            default -> ExerciseType.WALKING;
        };
    }
    public int getWorkoutTypeName(){return getWorkoutTypeName(workoutType);}
    public static int getWorkoutTypeName(WorkoutType workoutType){
        return switch(workoutType){
            case BIKE -> R.string.WorkoutType_BIKE;
            case CLIMB -> R.string.WorkoutType_CLIMB;
            case HIKE -> R.string.WorkoutType_HIKE;
            case RUN -> R.string.WorkoutType_RUN;
            case SNOWBOARDING -> R.string.WorkoutType_SNOWBOARDING;
            case SNOWSHOEING -> R.string.WorkoutType_SNOWSHOEING;
            case SWIM_OUTDOOR -> R.string.WorkoutType_SWIM_OUTDOOR;
            case WALK -> R.string.WorkoutType_WALK;
            case WINDSURFING -> R.string.WorkoutType_WINDSURFING;
            case OTHER_INDOOR -> R.string.WorkoutType_OTHER_INDOOR;
            default -> R.string.WorkoutType_OTHER_OUTDOOR;//OTHER_OUTDOOR, bug in jdk 21, return switch needs the default branch
        };
    }
    public int getWorkoutTypeIcon(){return getWorkoutTypeIcon(workoutType);}
    public static int getWorkoutTypeIcon(WorkoutType workoutType){
        return switch(workoutType){
            case BIKE -> R.drawable.ic_bike;
            case CLIMB -> R.drawable.ic_climb;
            case HIKE -> R.drawable.ic_hike;
            case RUN -> R.drawable.ic_run;
            case SNOWBOARDING -> R.drawable.ic_snowboarding;
            case SNOWSHOEING -> R.drawable.ic_snowshoeing;
            case SWIM_OUTDOOR -> R.drawable.ic_swim;
            case WALK -> R.drawable.ic_walk;
            case WINDSURFING -> R.drawable.ic_windsurfing;
            default -> R.drawable.ic_other;
        };
    }
    static WorkoutType getWorkoutTypeFromInt(int workoutTypeInt){
        return WorkoutData.WorkoutType.values()[workoutTypeInt];
    }
    public boolean isWorkoutTypeAutoPause(){
        return capabilities.getAutoPauseAndResumeEnabledExercises().contains(exerciseType);
    }
    public boolean hasElevation(){
        return capabilities.getExerciseTypeCapabilities(exerciseType).getSupportedDataTypes().contains(DataType.ABSOLUTE_ELEVATION);
    }
    public boolean hasElevationLoss(){
        return capabilities.getExerciseTypeCapabilities(exerciseType).getSupportedDataTypes().contains(DataType.ELEVATION_LOSS_TOTAL);
    }
    public boolean isWorkoutTypeGps(){
        return workoutType != WorkoutType.CLIMB && workoutType != WorkoutType.OTHER_INDOOR;
    }
    public boolean isWorkoutTypeEle(){
        return workoutType != WorkoutType.WINDSURFING && workoutType != WorkoutType.SWIM_OUTDOOR;
    }
    private void setWorkoutType(String workoutType){
        switch(workoutType){
            case "Biking" -> setWorkoutType(WorkoutType.BIKE);
            case "Climbing" -> setWorkoutType(WorkoutType.CLIMB);
            case "Hiking" -> setWorkoutType(WorkoutType.HIKE);
            case "Running" -> setWorkoutType(WorkoutType.RUN);
            case "Snowboarding" -> setWorkoutType(WorkoutType.SNOWBOARDING);
            case "Snowshoeing" -> setWorkoutType(WorkoutType.SNOWSHOEING);
            case "Swim (outdoor)" -> setWorkoutType(WorkoutType.SWIM_OUTDOOR);
            case "Walking" -> setWorkoutType(WorkoutType.WALK);
            case "Windsurfing" -> setWorkoutType(WorkoutType.WINDSURFING);
            case "Other (indoor)" -> setWorkoutType(WorkoutType.OTHER_INDOOR);
            default -> setWorkoutType(WorkoutType.OTHER_OUTDOOR);
        }
    }
    void setDescName(){
        if(distanceTotal > 1000){
            description = distanceDoubleToString(distanceTotal / 1000) + "km in " + prettyDurationShort(durationTotal);
        }else if(distanceTotal > 10){
            description = distanceTotal.intValue() + "m in " + prettyDurationShort(durationTotal);
        }else{
            int count = (checkpoints.size()+1)/2;
            if(count == 1){
                description = prettyDurationShort(durationTotal);
            }else{
                description = count + " sessions in " + prettyDurationShort(durationTotal);
            }
        }
        name = sdfGPXName.format(id);
    }

    String toJsonString(Context context) throws JSONException{
        return toJson(context).toString();
    }
    public JSONObject toJson(Context context) throws JSONException{
        JSONObject workoutData_json = new JSONObject();
        workoutData_json.put("id", id);
        workoutData_json.put("name", name);
        workoutData_json.put("desc", description);
        workoutData_json.put("workoutType", context.getString(getWorkoutTypeName(workoutType)));
        workoutData_json.put("durationTotal", durationTotal);
        if(distanceTotal > 0) workoutData_json.put("distanceTotal", distanceTotal);
        if(elevationGainTotal > 0) workoutData_json.put("elevationGainTotal", elevationGainTotal);
        if(elevationLossTotal > 0) workoutData_json.put("elevationLossTotal", elevationLossTotal);
        if(pauseTotal > 0) workoutData_json.put("pauseTotal", pauseTotal);
        if(speedAvg > 0) workoutData_json.put("speedAvg", speedAvg);
        if(speedMax > 0) workoutData_json.put("speedMax", speedMax);
        if(heartRateAvg > 0) workoutData_json.put("heartRateAvg", heartRateAvg);
        if(heartRateMax > 0) workoutData_json.put("heartRateMax", heartRateMax);
        if(caloriesTotal > 0) workoutData_json.put("caloriesTotal", caloriesTotal);
        if(!checkpoints.isEmpty()){
            JSONArray checkpoints_json = new JSONArray();
            for(int i = 0; i < checkpoints.size(); i++)
                checkpoints_json.put(checkpoints.get(i).toJson());
            workoutData_json.put("checkpoints", checkpoints_json);
        }
        return workoutData_json;
    }

    public String locationsToJsonString() throws JSONException{
        JSONArray locations = new JSONArray();
        for(int i=0; i<locationSamples.size(); i++) locations.put(locationSamples.get(i).toJson());
        return locations.toString();
    }
    void locationsFromJson(JSONArray locations) throws JSONException{
        for(int i=0; i < locations.length(); i++){
            locationSamples.add(new LocationSample(locations.getJSONObject(i)));
        }
    }
    public String heartRatesToJsonString() throws JSONException{
        JSONArray heartRates = new JSONArray();
        for(int i=0; i<heartRateSamples.size(); i++) heartRates.put(heartRateSamples.get(i).toJson());
        return heartRates.toString();
    }
    void heartRatesFromJson(JSONArray heartRates) throws JSONException{
        for(int i=0; i < heartRates.length(); i++){
            heartRateSamples.add(new HeartRateSample(heartRates.getJSONObject(i)));
        }
    }
}
