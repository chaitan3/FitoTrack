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

package de.tadris.fitness.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.collection.ArraySet;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import de.tadris.fitness.BuildConfig;
import de.tadris.fitness.aggregation.AggregationSpan;
import de.tadris.fitness.data.RecordingType;
import de.tadris.fitness.data.WorkoutType;
import de.tadris.fitness.data.WorkoutTypeManager;
import de.tadris.fitness.recording.autostart.AutoStartWorkout;

public class UserPreferences {
    private static final String USE_NFC_START_VARIABLE = "nfcStart";
    private static final String AUTO_START_DELAY_VARIABLE = "autoStartDelayPeriod";
    private static final String AUTO_START_MODE_VARIABLE = "autoStartMode";
    private static final String AUTO_TIMEOUT_VARIABLE = "autoTimeoutPeriod";
    private static final String USE_AUTO_PAUSE_VARIABLE = "autoPause";
    private static final String FIRST_DAY_OF_WEEK_VARIABLE = "firstDayOfWeek";
    private static final String ANNOUNCE_AUTO_START_COUNTDOWN = "announcement_countdown";
    private static final String USE_AVERAGE_FOR_CURRENT_SPEED = "useAverageForCurrentSpeed";
    private static final String TIME_FOR_CURRENT_SPEED = "timeForCurrentSpeed";
    private static final String HAS_LOWER_TARGET_SPEED_LIMIT = "hasLowerTargetSpeedLimit";
    private static final String LOWER_TARGET_SPEED_LIMIT = "lowerTargetSpeedLimit";
    private static final String HAS_UPPER_TARGET_SPEED_LIMIT = "hasUpperTargetSpeedLimit";
    private static final String UPPER_TARGET_SPEED_LIMIT = "upperTargetSpeedLimit";
    public static final String STEP_LENGTH = "stepLength";
    public static final String STATISTICS_AGGREGATION_SPAN = "statisticsAggregationSpan";
    public static final String STATISTICS_SELECTED_TYPES = "statisticsSelectedTypes";
    public static final String VOICE_ANNOUNCEMENTS_INTERVAL_TIME = "spokenUpdateTimePeriod";
    public static final String VOICE_ANNOUNCEMENTS_INTERVAL_DISTANCE = "spokenUpdateDistancePeriod2";
    public static final String ASK_FOR_COMMENT = "askForComment";

    /**
     * Default NFC start enable state if no other has been chosen
     */
    public static final boolean DEFAULT_USE_NFC_START = false;

    /**
     * Default auto start delay in seconds if no other has been chosen
     */
    public static final int DEFAULT_AUTO_START_DELAY_S = AutoStartWorkout.DEFAULT_DELAY_S;

    /**
     * Default auto start mode if no other has been chosen
     */
    public static final String DEFAULT_AUTO_START_MODE = AutoStartWorkout.Mode.getDefault().toString();

    /**
     * Default auto workout stop timeout in minutes if no other has been chosen
     */
    public static final int DEFAULT_AUTO_TIMEOUT_M = 20;

    /**
     * Default auto pause enable state if no other has been chosen
     */
    public static final boolean DEFAULT_USE_AUTO_PAUSE = true;

    /**
     * Default for using auto start countdown TTS announcements if no other has been chosen
     */
    public static final boolean DEFAULT_ANNOUNCE_AUTO_START_COUNTDOWN = true;

    /**
     * Default for using an average over a certain time instead of the last record for calculating
     * the current speed
     */
    private static final boolean DEFAULT_USE_AVERAGE_FOR_CURRENT_SPEED = false;

    /**
     * Default time for calculating the current speed when using average (i.e. when
     * DEFAULT_USE_AVERAGE_FOR_CURRENT_SPEED is true)
     */
    private static final int DEFAULT_TIME_FOR_CURRENT_SPEED = 15;

    /**
     * Default whether to warn the user when they are below a specified speed
     */
    private static final boolean DEFAULT_HAS_LOWER_TARGET_SPEED_LIMIT = false;

    /**
     * Default lower target speed range limit (in m/s)
     */
    private static final float DEFAULT_LOWER_TARGET_SPEED_LIMIT = 2;

    /**
     * Default whether to warn the user when they are above a specified speed
     */
    private static final boolean DEFAULT_HAS_UPPER_TARGET_SPEED_LIMIT = false;

    /**
     * Default upper target speed range limit (in m/s)
     */
    private static final float DEFAULT_UPPER_TARGET_SPEED_LIMIT = 3;

    /**
     * Default timespan to show in statistics (ShortStats View etc) value of 2 represents months
     */
    private static final int DEFAULT_STATISTICS_SPAN = AggregationSpan.MONTH.toInt();

    /**
     * Default selection of WorkoutType in statistics in means of typeID
     */
    private static final Set<String> DEFAULT_STATISTICS_SELECTED_TYPES = new ArraySet<>();

    /**
     * Default ask for comment enable state if no other has been chosen
     */
    public static final boolean DEFAULT_ASK_FOR_COMMENT = true;

    private final SharedPreferences preferences;
    private final RecordingScreenInformationPreferences recordingScreenInformationPreferences;
    private final SharingScreenInformationPreferences sharingScreenInformationPreferences;
    private final Context ctx;

    public UserPreferences(Context context) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.recordingScreenInformationPreferences = new RecordingScreenInformationPreferences(this.preferences);
        this.sharingScreenInformationPreferences = new SharingScreenInformationPreferences(this.preferences);
        this.ctx = context;
    }

    public RecordingScreenInformationPreferences getRecordingScreenInformationPreferences() {
        return this.recordingScreenInformationPreferences;
    }

    public SharingScreenInformationPreferences getSharingScreenInformationPreferences() {
        return this.sharingScreenInformationPreferences;
    }

    public int getUserWeight() {
        return preferences.getInt("weight", 80);
    }

    public int getSpokenUpdateTimePeriod() {
        return preferences.getInt(VOICE_ANNOUNCEMENTS_INTERVAL_TIME, 0);
    }

    /**
     * @return Distance interval for voice announcements in custom long distance unit (e.g. km or miles)
     */
    public float getSpokenUpdateDistancePeriod() {
        return preferences.getFloat(VOICE_ANNOUNCEMENTS_INTERVAL_DISTANCE, 0);
    }

    public String getMapStyle() {
        return preferences.getString("mapStyle", "osm.mapnik");
    }

    public String getTrackStyle() {
        return preferences.getString("trackStyle", "purple_rain");
    }

    public static final String STYLE_USAGE_ALWAYS = "always";
    public static final String STYLE_USAGE_DIAGRAM = "diagram";
    public static final String STYLE_USAGE_NEVER = "never";

    public String getTrackStyleMode() {
        return preferences.getString("trackStyleUsage", STYLE_USAGE_DIAGRAM);
    }

    public boolean intervalsIncludePauses() {
        return preferences.getBoolean("intervalsIncludePause", true);
    }

    @Deprecated()
    public String getIdOfDisplayedInformation(String mode, int slot) {
        String defValue = "";
        if (RecordingType.INDOOR.id.equals(mode)) {
            switch (slot) {
                case 0:
                    defValue = "avg_frequency";
                    break;
                case 1:
                    defValue = "energy_burned";
                    break;
                case 2:
                    defValue = "current_intensity";
                    break;
                case 3:
                    defValue = "pause_duration";
                    break;
            }
        } else {
            switch (slot) {
                case 0:
                    defValue = "distance";
                    break;
                case 1:
                    defValue = "energy_burned";
                    break;
                case 2:
                    defValue = "avgSpeedMotion";
                    break;
                case 3:
                    defValue = "pause_duration";
                    break;
            }
        }
        return preferences.getString("information_display_" + mode + "_" + slot, defValue);
    }

    public void setIdOfDisplayedInformation(String mode, int slot, String id) {
        preferences.edit().putString("information_display_" + mode + "_" + slot, id).apply();
    }

    public String getDateFormatSetting() {
        return preferences.getString("dateFormat", "system");
    }

    public String getTimeFormatSetting() {
        return preferences.getString("timeFormat", "system");
    }

    public int getFirstDayOfWeek() {

        if ( ! preferences.getString(FIRST_DAY_OF_WEEK_VARIABLE, "system").equals("system")){
            try {
                return Integer.parseInt(preferences.getString(FIRST_DAY_OF_WEEK_VARIABLE,"0"));
            } catch (NumberFormatException nfe){

            }

        }
        return Calendar.getInstance().getFirstDayOfWeek();
    }

    public String getDistanceUnitSystemId() {
        return preferences.getString("unitSystem", "1");
    }

    public String getEnergyUnit() {
        return preferences.getString("energyUnit", "kcal");
    }

    public boolean getShowOnLockScreen() {
        return preferences.getBoolean("showOnLockScreen", false);
    }

    public boolean getShowWorkoutZoomControls() {
        return preferences.getBoolean("showWorkoutZoomControls", true);
    }

    public boolean getZoomWithVolumeButtons() {
        return preferences.getBoolean("zoomWithVolumeButtons", true);
    }

    public boolean getAskForComment() {
        return preferences.getBoolean(ASK_FOR_COMMENT, DEFAULT_ASK_FOR_COMMENT);
    }

    public int getAutoBackupIntervalHours() {
        return Integer.parseInt(preferences.getString("autoBackupInterval", "168"));
    }

    public String getOfflineMapDirectoryName() {
        return preferences.getString("offlineMapDirectoryName", null);
    }

    /**
     * Check if NFC start is currently enabled
     *
     * @return whether NFC start is enabled or not
     */
    public boolean getUseNfcStart() {
        return preferences.getBoolean(USE_NFC_START_VARIABLE, DEFAULT_USE_NFC_START);
    }

    /**
     * Get the currently configured auto start delay
     * @return auto start delay in seconds
     */
    public int getAutoStartDelay() {
        return preferences.getInt(AUTO_START_DELAY_VARIABLE, DEFAULT_AUTO_START_DELAY_S);
    }

    /**
     * Change the currently configured auto start delay
     * @param delayS new auto start delay in seconds
     */
    public void setAutoStartDelay(int delayS) {
        preferences.edit().putInt(AUTO_START_DELAY_VARIABLE, delayS).apply();
    }

    /**
     * Get the currently configured auto start mode
     * @return auto start mode
     */
    public AutoStartWorkout.Mode getAutoStartMode() {
        try {
            return AutoStartWorkout.Mode.valueOf(preferences.getString(AUTO_START_MODE_VARIABLE,
                    DEFAULT_AUTO_START_MODE));
        } catch (IllegalArgumentException ex) {
            // use default mode instead if preferences are broken
            return AutoStartWorkout.Mode.getDefault();
        }
    }

    /**
     * Change the currently configured auto start mode
     * @param mode new auto start mode
     */
    public void setAutoStartMode(AutoStartWorkout.Mode mode) {
        preferences.edit().putString(AUTO_START_MODE_VARIABLE, mode.toString()).apply();
    }

    /**
     * Get the currently configured timeout after which a workout is automatically stopped
     * @return auto workout stop timeout in minutes
     */
    public int getAutoTimeout() {
        return preferences.getInt(AUTO_TIMEOUT_VARIABLE, DEFAULT_AUTO_TIMEOUT_M);
    }

    /**
     * Change the currently configured timeout after which a workout is automatically stopped
     * @param timeoutM new auto workout stop timeout in minutes
     */
    public void setAutoTimeout(int timeoutM) {
        preferences.edit().putInt(AUTO_TIMEOUT_VARIABLE, timeoutM).apply();
    }


    /**
     * Check if auto pause is currently enabled
     * @return whether auto pause is enabled or not
     */
    public boolean getUseAutoPause() {
        return preferences.getBoolean(USE_AUTO_PAUSE_VARIABLE, DEFAULT_USE_AUTO_PAUSE);
    }

    /**
     * Change the current state of auto pause
     * @param useAutoStart new auto pause enable state
     */
    public void setUseAutoPause(boolean useAutoStart) {
        preferences.edit().putBoolean(USE_AUTO_PAUSE_VARIABLE, useAutoStart).apply();
    }

    /**
     * Check if auto start countdown related announcements are enabled
     *
     * @return whether countdown announcements are enabled
     */
    public boolean isAutoStartCountdownAnnouncementsEnabled() {
        return preferences.getBoolean(ANNOUNCE_AUTO_START_COUNTDOWN, DEFAULT_ANNOUNCE_AUTO_START_COUNTDOWN);
    }

    /**
     * Check if the average speed over a certain time shall be used to calculate the current speed
     * (instead of the last record only)
     * @return whether the average shall be used
     */
    public boolean getUseAverageForCurrentSpeed() {
        return preferences.getBoolean(USE_AVERAGE_FOR_CURRENT_SPEED, DEFAULT_USE_AVERAGE_FOR_CURRENT_SPEED);
    }

    /**
     * Set if the average speed over a certain time shall be used to calculate the current speed
     * (instead of the last record only)
     * @param useAverage whether the average shall be used
     */
    public void setUseAverageForCurrentSpeed(boolean useAverage) {
        preferences.edit().putBoolean(USE_AVERAGE_FOR_CURRENT_SPEED, useAverage).apply();
    }

    /**
     * The time (in seconds) to take the average speed over when calculating the current speed
     * @return the time (in seconds) over which to take the average speed
     */
    public int getTimeForCurrentSpeed() {
        return preferences.getInt(TIME_FOR_CURRENT_SPEED, DEFAULT_TIME_FOR_CURRENT_SPEED);
    }

    /**
     * Set the time (in seconds) to take the average speed over when calculating the current speed
     * @param time the time (in seconds) over which to take the average speed
     */
    public void setTimeForCurrentSpeed(int time) {
        preferences.edit().putInt(TIME_FOR_CURRENT_SPEED, time).apply();
    }

    /**
     * Get whether to warn the user when they are below a specified speed
     * @return whether to warn the user
     */
    public boolean hasLowerTargetSpeedLimit() {
        return preferences.getBoolean(HAS_LOWER_TARGET_SPEED_LIMIT, DEFAULT_HAS_LOWER_TARGET_SPEED_LIMIT);
    }

    /**
     * Set whether to warn the user when they are below a specified speed
     * @param hasLowerSpeedLimit whether to warn the user
     */
    public void setHasLowerTargetSpeedLimit(boolean hasLowerSpeedLimit) {
        preferences.edit().putBoolean(HAS_LOWER_TARGET_SPEED_LIMIT, hasLowerSpeedLimit).apply();
    }

    /**
     * Get lower target speed range limit (in m/s)
     * @return lower speed limit (in m/s)
     */
    public float getLowerTargetSpeedLimit() {
        return preferences.getFloat(LOWER_TARGET_SPEED_LIMIT, DEFAULT_LOWER_TARGET_SPEED_LIMIT);
    }

    /**
     * Set lower target speed range limit (in m/s)
     * @param lowerTargetSpeedLimit lower speed limit (in m/s)
     */
    public void setLowerTargetSpeedLimit(float lowerTargetSpeedLimit) {
        preferences.edit().putFloat(LOWER_TARGET_SPEED_LIMIT, lowerTargetSpeedLimit).apply();
    }

    /**
     * Get whether to warn the user when they are above a specified speed
     * @return whether to warn the user
     */
    public boolean hasUpperTargetSpeedLimit() {
        return preferences.getBoolean(HAS_UPPER_TARGET_SPEED_LIMIT, DEFAULT_HAS_UPPER_TARGET_SPEED_LIMIT);
    }

    /**
     * Set whether to warn the user when they are above a specified speed
     * @param hasUpperSpeedLimit whether to warn the user
     */
    public void setHasUpperTargetSpeedLimit(boolean hasUpperSpeedLimit) {
        preferences.edit().putBoolean(HAS_UPPER_TARGET_SPEED_LIMIT, hasUpperSpeedLimit).apply();
    }

    /**
     * Get upper target speed range limit (in m/s)
     *
     * @return upper speed limit (in m/s)
     */
    public float getUpperTargetSpeedLimit() {
        return preferences.getFloat(UPPER_TARGET_SPEED_LIMIT, DEFAULT_UPPER_TARGET_SPEED_LIMIT);
    }

    /**
     * users step length in m
     * <p>
     * default value taken from https://www.livestrong.com/article/438170-the-average-walking-stride-length/ and converted to meters
     */
    public float getStepLength() {
        return preferences.getFloat(STEP_LENGTH, 0.79f);
    }

    public void setStepLength(float meters) {
        preferences.edit().putFloat(STEP_LENGTH, meters).apply();
    }

    /**
     * Set upper target speed range limit (in m/s)
     *
     * @param upperTargetSpeedLimit upper speed limit (in m/s)
     */
    public void setUpperTargetSpeedLimit(float upperTargetSpeedLimit) {
        preferences.edit().putFloat(UPPER_TARGET_SPEED_LIMIT, upperTargetSpeedLimit).apply();
    }

    public int getLastVersionCode() {
        return preferences.getInt("lastVersion", BuildConfig.VERSION_CODE);
    }

    public void updateLastVersionCode() {
        preferences.edit().putInt("lastVersion", BuildConfig.VERSION_CODE).apply();
    }

    public AggregationSpan getStatisticsAggregationSpan() {
        int statsAggregation = preferences.getInt(STATISTICS_AGGREGATION_SPAN, DEFAULT_STATISTICS_SPAN);
        return AggregationSpan.fromInt(statsAggregation);
    }

    public void setStatisticsAggregationSpan(AggregationSpan span) {
        preferences.edit().putInt(STATISTICS_AGGREGATION_SPAN, span.toInt()).apply();
    }

    public List<WorkoutType> getStatisticsSelectedTypes() {
        Set<String> typeIDs = preferences.getStringSet(STATISTICS_SELECTED_TYPES, DEFAULT_STATISTICS_SELECTED_TYPES);
        List<WorkoutType> types = new ArrayList<>();
        for(String type:typeIDs) {
            types.add(WorkoutTypeManager.getInstance().getWorkoutTypeById(ctx, type));
        }
        return types;
    }

    public void setStatisticsSelectedTypes(List<WorkoutType> types) {
        Set<String> typeIDs = new ArraySet<>();
        for(WorkoutType type:types) {
            typeIDs.add(type.id);
        }
        preferences.edit().putStringSet(STATISTICS_SELECTED_TYPES, typeIDs).apply();
    }
    public UserMeasurements getMeasurements() {
        return new UserMeasurements(
                getUserWeight(),
                getStepLength()
        );
    }

}
