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

import static de.tadris.fitness.data.SettingsStore.WorkoutSettings.WorkoutSetting;
import static de.tadris.fitness.data.SettingsStore.WorkoutSettings.WorkoutSetting.*;

import android.content.Context;
import android.content.SharedPreferences;

import de.tadris.fitness.data.WorkoutData.WorkoutType;

public class SettingsStore{
    public static class WorkoutSettings{
        public enum WorkoutSetting{ENABLED, DISABLED, ENABLE, DISABLE, UNAVAILABLE}
        public final WorkoutType workoutType;
        public WorkoutSetting screenOn = DISABLED;
        public WorkoutSetting autoPause = DISABLED;
        public WorkoutSetting screenMap = DISABLED;
        public WorkoutSetting screenCurrent = ENABLED;
        public WorkoutSetting screenTotal = DISABLED;
        public WorkoutSetting screenSpeed = DISABLED;
        public WorkoutSetting screenElevation = DISABLED;
        public WorkoutSetting screenHeartRate = DISABLED;
        public WorkoutSettings(Context context, WorkoutType workoutType){
            this.workoutType = workoutType;
            switch(workoutType){
                case BIKE:
                    autoPause =  ENABLED;
                    screenMap =  ENABLED;
                    break;
                case CLIMB, OTHER_INDOOR:
                    screenCurrent =  DISABLED;
                    screenHeartRate =  ENABLED;
                    break;
                case HIKE, SNOWBOARDING, SNOWSHOEING:
                    screenTotal =  ENABLED;
                case WALK:
                    screenMap =  ENABLED;
                    break;
                case RUN, SWIM_OUTDOOR, WINDSURFING, OTHER_OUTDOOR:
                    break;
            }
            SharedPreferences sharedPreferences = getSharedPreferences(context, workoutType);
            screenOn = sharedPreferences.getBoolean("screenOn", screenOn == ENABLED) ? ENABLED : DISABLED;
            autoPause = sharedPreferences.getBoolean("autoPause", autoPause == ENABLED) ? ENABLED : DISABLED;
            screenMap = sharedPreferences.getBoolean("screenMap", screenMap == ENABLED) ? ENABLED : DISABLED;
            screenCurrent = sharedPreferences.getBoolean("screenCurrent", screenCurrent == ENABLED) ? ENABLED : DISABLED;
            screenTotal = sharedPreferences.getBoolean("screenTotal", screenTotal == ENABLED) ? ENABLED : DISABLED;
            screenSpeed = sharedPreferences.getBoolean("screenSpeed", screenSpeed == ENABLED) ? ENABLED : DISABLED;
            screenElevation = sharedPreferences.getBoolean("screenElevation", screenElevation == ENABLED) ? ENABLED : DISABLED;
            screenHeartRate = sharedPreferences.getBoolean("screenHeartRate", screenHeartRate == ENABLED) ? ENABLED : DISABLED;
        }
    }
    public static boolean isSettingEnabled(WorkoutSetting settingsState){
        return settingsState == ENABLE || settingsState == ENABLED;
    }
    public static boolean isSettingChanged(WorkoutSetting settingsState){
        return settingsState == ENABLE || settingsState == DISABLE;
    }
    public static void storeSetting(Context context, String name, WorkoutType type, boolean value){
        SharedPreferences.Editor editor = getEditor(context, type);
        editor.putBoolean(name, value);
        editor.apply();
    }

    public static boolean getBTEnabled(Context context){
        return getSharedPreferences(context, null).getBoolean("BTEnabled", false);
    }
    public static void storeBTEnabled(Context context, boolean value){
        SharedPreferences.Editor editor = getEditor(context, null);
        editor.putBoolean("BTEnabled", value);
        editor.apply();
    }
    public static boolean getMapSource(Context context){
        return getSharedPreferences(context, null).getBoolean("MapSource", false);
    }
    public static void storeMapSource(Context context, boolean value){
        SharedPreferences.Editor editor = getEditor(context, null);
        editor.putBoolean("MapSource", value);
        editor.apply();
    }
    public static WorkoutType getLastWorkoutType(Context context){
        SharedPreferences sharedPreferences = getSharedPreferences(context, null);
        int lastWorkoutTypeInt = sharedPreferences.getInt("lastWorkoutType", -1);
        if(lastWorkoutTypeInt == -1) return WorkoutType.WALK;
        return WorkoutData.getWorkoutTypeFromInt(lastWorkoutTypeInt);
    }
    public static void storeLastWorkoutType(Context context, WorkoutType workoutType){
        SharedPreferences.Editor editor = getEditor(context, null);
        editor.putInt("lastWorkoutType", workoutType.ordinal());
        editor.apply();
    }

    private static SharedPreferences.Editor getEditor(Context context, WorkoutType workoutType){
        return getSharedPreferences(context, workoutType).edit();
    }
    private static SharedPreferences getSharedPreferences(Context context, WorkoutType workoutType){
        return context.getSharedPreferences(
                workoutType == null ? "main" : context.getString(WorkoutData.getWorkoutTypeName(workoutType))
                ,Context.MODE_PRIVATE
        );
    }
}