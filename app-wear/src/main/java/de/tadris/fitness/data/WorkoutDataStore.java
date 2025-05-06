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

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;

import javax.annotation.Nullable;

import de.tadris.fitness.R;
import de.tadris.fitness.ui.Main;
import de.tadris.fitness.ui.workouts.WorkoutActivity;
import de.tadris.fitness.ui.workouts.WorkoutsActivity;

public class WorkoutDataStore{
    public static final ArrayList<WorkoutData> workouts = new ArrayList<>();
    public static long lastWorkoutID;

    public static WorkoutData getWorkout(long id){//Thread: UI
        for(WorkoutData workoutData : workouts) if(workoutData.id == id) return workoutData;
        return null;
    }

    public static void deleteWorkout(Activity activity, long id){//Thread: BG
        for(WorkoutData workoutData : workouts){
            if(workoutData.id == id){
                workouts.remove(workoutData);
                break;
            }
        }
        if(!activity.deleteFile(idToWorkoutFilename(id))){
            Log.e(Main.LOG_TAG, "WorkoutDataStore.deleteWorkout failed, filename: " + idToWorkoutFilename(id));
            toast(activity, R.string.error_workout_delete);
        }
        if(!activity.deleteFile(idToLocationFilename(id))){
            Log.w(Main.LOG_TAG, "WorkoutDataStore.deleteWorkout failed, filename: " + idToLocationFilename(id));
        }
        if(!activity.deleteFile(idToHeartRateFilename(id))){
            Log.w(Main.LOG_TAG, "WorkoutDataStore.deleteWorkout failed, filename: " + idToHeartRateFilename(id));
        }
    }
    public static void readWorkouts(WorkoutsActivity workoutsActivity){//Thread: BG
        readWorkouts(workoutsActivity.getApplicationContext());
        workoutsActivity.loadItems();
    }
    public static void readWorkouts(Context context){//Thread: BG
        File[] files = context.getFilesDir().listFiles();
        if(files == null) return;
        Arrays.sort(files);
        workouts.clear();
        for(int i = files.length-1; i>=0; i--){
            String filename = files[i].getName();
            if(filename.startsWith("workout_") && filename.endsWith(".json")){
                WorkoutData workoutData = readWorkout(context, filename);
                if(workoutData != null) workouts.add(workoutData);
            }
        }
    }
    public static WorkoutData getFullWorkout(Context context, long id){//Thread: BG
        try{
            WorkoutData workoutData = readWorkout(context, idToWorkoutFilename(id));
            if(workoutData == null) return null;
            if(workoutData.isWorkoutTypeGps()){
                JSONArray locations = readLocations(context, id);
                if(locations != null) workoutData.locationsFromJson(locations);
            }
            JSONArray heartRates = readHeartRates(context, id);
            if(heartRates != null) workoutData.heartRatesFromJson(heartRates);
            return workoutData;
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "WorkoutDataStore.getFullWorkout exception: " + e.getMessage());
        }
        return null;
    }
    private static WorkoutData readWorkout(Context context, String filename){//Thread: BG
        try{
            FileInputStream fis = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder text = new StringBuilder();
            String line;
            while((line = br.readLine()) != null){text.append(line);}
            br.close();
            JSONObject workoutData_json = new JSONObject(text.toString());
            return new WorkoutData(workoutData_json);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "WorkoutDataStore.readWorkout exception: " + e.getMessage());
        }
        return null;
    }
    public static void readLocations(WorkoutActivity workoutActivity, WorkoutData workoutData){//Thread: BG
        try{
            JSONArray locations = readLocations(workoutActivity, workoutData.id);
            if(locations == null) return;
            workoutData.locationsFromJson(locations);
            workoutActivity.locationsRead(workoutData);
        }catch(Exception e){
            Log.w(Main.LOG_TAG, "WorkoutDataStore.readLocations exception: " + e.getMessage());
        }
    }
    private static JSONArray readLocations(Context context, long workoutDataId){//Thread: BG
        try{
            String filename = idToLocationFilename(workoutDataId);
            FileInputStream fis = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder text = new StringBuilder();
            String line;
            while((line = br.readLine()) != null){text.append(line);}
            br.close();
            return new JSONArray(text.toString());
        }catch(Exception e){
            Log.w(Main.LOG_TAG, "WorkoutDataStore.readLocations exception: " + e.getMessage());
        }
        return null;
    }
    public static void readHeartRates(WorkoutActivity workoutActivity, WorkoutData workoutData){//Thread: BG
        try{
            JSONArray heartRates = readHeartRates(workoutActivity, workoutData.id);
            if(heartRates == null) return;
            workoutData.heartRatesFromJson(heartRates);
            workoutActivity.heartRatesRead(workoutData);
        }catch(Exception e){
            Log.w(Main.LOG_TAG, "WorkoutDataStore.readHeartRates exception: " + e.getMessage());
        }
    }
    private static JSONArray readHeartRates(Context context, long workoutDataId){//Thread: BG
        try{
            String filename = idToHeartRateFilename(workoutDataId);
            FileInputStream fis = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder text = new StringBuilder();
            String line;
            while((line = br.readLine()) != null){text.append(line);}
            br.close();
            return new JSONArray(text.toString());
        }catch(Exception e){
            Log.w(Main.LOG_TAG, "WorkoutDataStore.readHeartRates exception: " + e.getMessage());
        }
        return null;
    }
    public static void storeWorkout(Activity activity, WorkoutData workoutData){//Thread: UI
        workoutData.setDescName();
        workouts.add(0, workoutData);
        lastWorkoutID = workoutData.id;
        Main.runInBackground(()->writeWorkout(activity, workoutData));
    }
    private static void writeWorkout(Activity activity, WorkoutData workoutData){//Thread: BG
        try{
            String workoutData_json = workoutData.toJsonString(activity);
            if(workoutData_json.length() < 10){
                toast(activity, R.string.error_workout_write);
                return;
            }
            FileOutputStream fos = activity.openFileOutput(idToWorkoutFilename(workoutData.id), Context.MODE_PRIVATE);
            OutputStreamWriter osr = new OutputStreamWriter(fos);
            osr.write(workoutData_json);
            osr.flush();
            fos.close();

            String location_json = workoutData.locationsToJsonString();
            if(location_json.length() > 10){
                fos = activity.openFileOutput(idToLocationFilename(workoutData.id), Context.MODE_PRIVATE);
                osr = new OutputStreamWriter(fos);
                osr.write(location_json);
                osr.flush();
                fos.close();
            }

            String heartRate_json = workoutData.heartRatesToJsonString();
            if(heartRate_json.length() > 10){
                fos = activity.openFileOutput(idToHeartRateFilename(workoutData.id), Context.MODE_PRIVATE);
                osr = new OutputStreamWriter(fos);
                osr.write(heartRate_json);
                osr.flush();
                fos.close();
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "WorkoutDataStore.writeWorkout exception: " + e.getMessage());
            toast(activity, R.string.error_workout_write);
        }
    }

    private static String idToWorkoutFilename(long id){return "workout_" + id + ".json";}
    private static String idToLocationFilename(long id){return "location_" + id + ".json";}
    private static String idToHeartRateFilename(long id){return "heart_" + id + ".json";}
    private static void toast(@Nullable Activity activity, int message){
        if(activity == null) return;
        activity.runOnUiThread(()->Toast.makeText(activity, message, Toast.LENGTH_LONG).show());
    }
}