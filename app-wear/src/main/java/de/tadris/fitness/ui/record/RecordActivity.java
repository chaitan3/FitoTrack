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

package de.tadris.fitness.ui.record;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.health.services.client.ExerciseClient;
import androidx.health.services.client.ExerciseUpdateCallback;
import androidx.health.services.client.HealthServices;
import androidx.health.services.client.HealthServicesClient;
import androidx.health.services.client.data.Availability;
import androidx.health.services.client.data.CumulativeDataPoint;
import androidx.health.services.client.data.DataPoint;
import androidx.health.services.client.data.DataPointContainer;
import androidx.health.services.client.data.DataType;
import androidx.health.services.client.data.DataTypeAvailability;
import androidx.health.services.client.data.DeltaDataType;
import androidx.health.services.client.data.ExerciseConfig;
import androidx.health.services.client.data.ExerciseEvent;
import androidx.health.services.client.data.ExerciseLapSummary;
import androidx.health.services.client.data.ExerciseState;
import androidx.health.services.client.data.ExerciseUpdate;
import androidx.health.services.client.data.LocationAvailability;
import androidx.health.services.client.data.LocationData;
import androidx.health.services.client.data.SampleDataPoint;
import androidx.health.services.client.data.StatisticalDataPoint;
import androidx.health.services.client.data.WarmUpConfig;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;

import de.tadris.fitness.R;
import de.tadris.fitness.data.SettingsStore;
import de.tadris.fitness.data.SettingsStore.WorkoutSettings;
import static de.tadris.fitness.data.SettingsStore.WorkoutSettings.WorkoutSetting.*;
import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.data.WorkoutData.HeartRateSample;
import de.tadris.fitness.data.WorkoutData.LocationSample;
import de.tadris.fitness.data.WorkoutData.SpeedCachePoint;
import de.tadris.fitness.data.WorkoutData.WorkoutType;
import de.tadris.fitness.data.WorkoutDataStore;
import de.tadris.fitness.ui.Main;
import de.tadris.fitness.ui.workouts.WorkoutActivity;

public class RecordActivity extends FragmentActivity{
    private long bootTime;
    static boolean hasLocationPermission = false;
    static boolean hasHRPermission = false;
    static boolean locationEnabled = false;
    private final ArrayList<SpeedCachePoint> speedCache = new ArrayList<>();
    private long lastLocationTime = 0;
    private double lastSpeed = 0;
    private double lastElevation = 0;
    private boolean clearSpeedCache = true;
    private long checkpointTime = 0;
    private long checkpointDuration = 0;
    public enum TargetAchieved{NOT, _50, _100}
    private TargetAchieved targetAchieved = TargetAchieved.NOT;
    static WorkoutData workoutData;
    static WorkoutSettings workoutSettings;
    static boolean isPreparing = true;
    String currentTime = "00:00";

    ExerciseClient exerciseClient;
    private ExerciseState exerciseState = ExerciseState.PREPARING;
    private ListeningExecutorService listeningExecutorService;
    private GestureDetector gestureDetector;
    private FragmentManager fragmentManager;
    private Handler handler;
    private Vibrator vibrator;
    private final VibrationEffect ve_pattern = VibrationEffect.createWaveform(new long[]{300, 500, 300, 500, 300, 500}, -1);
    private final SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.ENGLISH);

    private Prepare prepare;
    private ScreenStop screenStop;
    private Screen activeScreen;
    private final ArrayList<Screen> screens = new ArrayList<>();

    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record);
        handler = new Handler(Looper.getMainLooper());
        listeningExecutorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
        gestureDetector = new GestureDetector(this, simpleOnGestureListener, handler);
        bootTime = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        isPreparing = true;
        if(Build.VERSION.SDK_INT >= 31){
            vibrator = ((VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator();
        }else{
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
        if(Screen.colorPrimary == -1){
            Screen.colorPrimary = getColor(R.color.colorPrimary);
            Screen.colorProgressDone = getColor(R.color.progressDone);
            Screen.colorProgressDoneDark = getColor(R.color.progressDoneDark);
            Screen.colorActive = getColor(R.color.textWhite);
            Screen.colorInactive = getColor(R.color.textInactive);
            Screen.colorCircleFill = getColor(R.color.locationCircleFill);
        }

        HealthServicesClient healthServicesClient = HealthServices.getClient(this);
        exerciseClient = healthServicesClient.getExerciseClient();
        exerciseClient.setUpdateCallback(exerciseUpdateCallback);

        workoutData = new WorkoutData(exerciseClient, listeningExecutorService);
        updateWorkoutType(this, SettingsStore.getLastWorkoutType(this));

        prepare = new Prepare();
        fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.screens, prepare).commit();

        checkPermissions();
        prepareExercise();
    }

    static void updateWorkoutType(Context context, WorkoutType workoutType){
        workoutData.setWorkoutType(workoutType);
        workoutSettings = new WorkoutSettings(context, workoutType);
    }
    @Override public void onResume(){
        super.onResume();
        if(prepare != null && !workoutData.isWorkoutTypeGps()){
            prepare.locationDisabled();
        }else if(prepare != null && workoutData.isWorkoutTypeGps() && hasLocationPermission){
            prepare.locationPermissionGranted();
        }

        if(SettingsStore.isSettingChanged(workoutSettings.screenOn)) setScreenOnFlag();

        if(workoutData.id < 10) return;
        updateTime.run();
        update.run();

        int progress = 0;
        if(workoutData.primaryTargetType == WorkoutData.TargetType.DURATION){
            progress = (int) ((workoutData.durationTotal / 600) / workoutData.primaryTargetValue);
        }else if(workoutData.primaryTargetType == WorkoutData.TargetType.DISTANCE){
            progress = (int) ((workoutData.distanceTotal/10) / workoutData.primaryTargetValue);
        }
        switch(targetAchieved){
            case NOT:
                break;
            case _100:
                screens.forEach(Screen::setTargetUnAchieved);
                screenStop.setTargetUnAchieved();
                if(progress < 100) targetAchieved = TargetAchieved._50;
            case _50:
                if(progress < 50) targetAchieved = TargetAchieved.NOT;
                break;
        }
        activeScreen.updateTargetProgress(progress);

        if(workoutSettings.autoPause == ENABLE){
            workoutSettings.autoPause = ENABLED;
            exerciseClient.overrideAutoPauseAndResumeForActiveExerciseAsync(true);
        }else if(workoutSettings.autoPause == DISABLE){
            workoutSettings.autoPause = DISABLED;
            exerciseClient.overrideAutoPauseAndResumeForActiveExerciseAsync(false);
        }
        if(workoutSettings.screenMap == ENABLE){
            workoutSettings.screenMap = ENABLED;
            if(findScreen(ScreenMap.class) == null) addScreen(new ScreenMap());
        }else if(workoutSettings.screenMap == DISABLE){
            workoutSettings.screenMap = DISABLED;
            removeScreen(findScreen(ScreenMap.class));
        }
        if(workoutSettings.screenCurrent == ENABLE){
            workoutSettings.screenCurrent = ENABLED;
            if(findScreen(ScreenCurrent.class) == null) addScreen(new ScreenCurrent());
        }else if(workoutSettings.screenCurrent == DISABLE){
            workoutSettings.screenCurrent = DISABLED;
            removeScreen(findScreen(ScreenCurrent.class));
        }
        if(workoutSettings.screenTotal == ENABLE){
            workoutSettings.screenTotal = ENABLED;
            if(findScreen(ScreenTotal.class) == null) addScreen(new ScreenTotal());
        }else if(workoutSettings.screenTotal == DISABLE){
            workoutSettings.screenTotal = DISABLED;
            removeScreen(findScreen(ScreenTotal.class));
        }
        if(workoutSettings.screenSpeed == ENABLE){
            workoutSettings.screenSpeed = ENABLED;
            if(findScreen(ScreenSpeed.class) == null) addScreen(new ScreenSpeed());
        }else if(workoutSettings.screenSpeed == DISABLE){
            workoutSettings.screenSpeed = DISABLED;
            removeScreen(findScreen(ScreenSpeed.class));
        }
        if(workoutSettings.screenElevation == ENABLE){
            workoutSettings.screenElevation = ENABLED;
            if(findScreen(ScreenElevation.class) == null) addScreen(new ScreenElevation());
        }else if(workoutSettings.screenElevation == DISABLE){
            workoutSettings.screenElevation = DISABLED;
            removeScreen(findScreen(ScreenElevation.class));
        }
        if(workoutSettings.screenHeartRate == ENABLE){
            workoutSettings.screenHeartRate = ENABLED;
            if(findScreen(ScreenHeartRate.class) == null) addScreen(new ScreenHeartRate());
        }else if(workoutSettings.screenHeartRate == DISABLE){
            workoutSettings.screenHeartRate = DISABLED;
            removeScreen(findScreen(ScreenHeartRate.class));
        }
        if(workoutSettings.screenTarget == ENABLE){
            workoutSettings.screenTarget = ENABLED;
            if(findScreen(ScreenTarget.class) == null) addScreen(new ScreenTarget());
        }else if(workoutSettings.screenTarget == DISABLE){
            workoutSettings.screenTarget = DISABLED;
            removeScreen(findScreen(ScreenTarget.class));
        }
    }
    private Screen findScreen(Class<? extends Screen> cls){
        for(int i=0; i<screens.size(); i++) if(screens.get(i).getClass() == cls) return screens.get(i);
        return null;
    }
    private void addScreen(Screen screen){
        if(screen.getClass() == ScreenMap.class){
            screens.add(0, screen);
        }else{
            screens.add(screen);
        }
        fragmentManager.beginTransaction().add(R.id.screens, screen).hide(screen).commit();
    }
    private void removeScreen(Screen screen){
        if(screen == null) return;
        fragmentManager.beginTransaction().remove(screen).commit();
        screens.remove(screen);
    }
    void reloadTargetScreen(Screen screen){
        removeScreen(screen);
        addScreen(new ScreenTarget());
    }
    @Override public void onPause(){
        handler.removeCallbacksAndMessages(null);
        super.onPause();
    }
    @Override public void onDestroy(){
        if(!exerciseState.isEnding() && !exerciseState.isEnded()) exerciseClient.endExerciseAsync();
        super.onDestroy();
    }

    private void prepareExercise(){
        if(hasLocationPermission && !locationEnabled){
            Toast.makeText(this, R.string.record_location_off, Toast.LENGTH_LONG).show();
        }
        if((!hasLocationPermission || !locationEnabled) && !hasHRPermission) return;
        Set<DeltaDataType<?, ?>> dataTypes = new HashSet<>();
        if(locationEnabled && hasLocationPermission) dataTypes.add(DataType.LOCATION);
        if(hasHRPermission) dataTypes.add(DataType.HEART_RATE_BPM);

        Futures.addCallback(
                exerciseClient.prepareExerciseAsync(new WarmUpConfig(workoutData.exerciseType, dataTypes)),
                startCallback,
                listeningExecutorService
        );
    }

    void startExercise(){
        workoutData.id = System.currentTimeMillis();
        targetAchieved = TargetAchieved.NOT;

        ExerciseConfig.Builder configBuilder = new ExerciseConfig.Builder(workoutData.exerciseType);
        boolean autoPause = workoutData.isWorkoutTypeAutoPause()
                && SettingsStore.isSettingEnabled(workoutSettings.autoPause);
        workoutSettings.autoPause = autoPause ? ENABLED : DISABLED;
        configBuilder.setIsAutoPauseAndResumeEnabled(autoPause);

        Screen.resetStaticFields();

        screenStop = new ScreenStop();
        fragmentManager.beginTransaction().replace(R.id.screens, screenStop).commit();
        activeScreen = screenStop;
        prepare = null;
        isPreparing = false;

        //https://developer.android.com/reference/androidx/health/services/client/data/DataType
        Set<DataType<?, ?>> dataTypes = new HashSet<>();
        if(hasLocationPermission && locationEnabled && workoutData.isWorkoutTypeGps()){
            if(workoutSettings.screenMap == ENABLE) workoutSettings.screenMap = ENABLED;
            if(workoutSettings.screenMap == ENABLED) addScreen(new ScreenMap());
            if(workoutSettings.screenCurrent == ENABLE) workoutSettings.screenCurrent = ENABLED;
            if(workoutSettings.screenCurrent == ENABLED) addScreen(new ScreenCurrent());
            if(workoutSettings.screenTotal == ENABLE) workoutSettings.screenTotal = ENABLED;
            if(workoutSettings.screenTotal == ENABLED && workoutData.isWorkoutTypeEle()) addScreen(new ScreenTotal());
            if(workoutSettings.screenSpeed == ENABLE) workoutSettings.screenSpeed = ENABLED;
            if(workoutSettings.screenSpeed == ENABLED) addScreen(new ScreenSpeed());
            if(workoutSettings.screenElevation == ENABLE) workoutSettings.screenElevation = ENABLED;
            if(workoutSettings.screenElevation == ENABLED) addScreen(new ScreenElevation());
            if(workoutSettings.screenTarget == ENABLE) workoutSettings.screenTarget = ENABLED;
            if(workoutSettings.screenTarget == ENABLED) addScreen(new ScreenTarget());

            if(workoutData.isWorkoutTypeEle()){
                dataTypes.add(DataType.ELEVATION_GAIN_TOTAL);
                if(workoutData.hasElevationLoss())
                    dataTypes.add(DataType.ELEVATION_LOSS_TOTAL);
                if(workoutData.hasElevation())
                    dataTypes.add(DataType.ABSOLUTE_ELEVATION);
            }
            dataTypes.add(DataType.DISTANCE_TOTAL);
            dataTypes.add(DataType.LOCATION);
            dataTypes.add(DataType.SPEED_STATS);
            configBuilder.setIsGpsEnabled(true);
        }else{
            workoutSettings.screenMap = UNAVAILABLE;
            workoutSettings.screenCurrent = UNAVAILABLE;
            workoutSettings.screenTotal = UNAVAILABLE;
            workoutSettings.screenSpeed = UNAVAILABLE;
            workoutSettings.screenElevation = UNAVAILABLE;
        }
        if(hasHRPermission){
            if(workoutSettings.screenHeartRate == ENABLE) workoutSettings.screenHeartRate = ENABLED;
            if(workoutSettings.screenHeartRate == ENABLED) addScreen(new ScreenHeartRate());
            dataTypes.add(DataType.CALORIES_TOTAL);
            dataTypes.add(DataType.HEART_RATE_BPM);
            dataTypes.add(DataType.HEART_RATE_BPM_STATS);
        }else{
            workoutSettings.screenHeartRate = UNAVAILABLE;
        }

        if(dataTypes.isEmpty()){
            Toast.makeText(this, R.string.error_no_permissions, Toast.LENGTH_LONG).show();
            return;
        }

        if(screens.size() > 1 && screens.get(0).getClass() == ScreenMap.class){
            //The first screen is a map, show the second
            showScreen(screens.get(1));
        }else if(!screens.isEmpty()){
            //The first screen isn't a map, show the first
            showScreen(screens.get(0));
        }
        configBuilder.setDataTypes(dataTypes);
        Futures.addCallback(
                exerciseClient.startExerciseAsync(configBuilder.build()),
                startCallback,
                listeningExecutorService
        );
        updateTime.run();
        update.run();
        setScreenOnFlag();

        startForegroundService(new Intent(this, RecordService.class));
    }
    void pauseResumeClicked(){
        if(exerciseState.equals(ExerciseState.ACTIVE)){
            pauseExercise();
        }else{
            resumeExercise();
        }
    }
    private void pauseExercise(){
        exerciseClient.pauseExerciseAsync();
        activeScreen.updateSpeed(0, 0.0);
    }
    private void resumeExercise(){exerciseClient.resumeExerciseAsync();}
    void endExercise(){
        handler.removeCallbacksAndMessages(null);
        exerciseClient.clearUpdateCallbackAsync(exerciseUpdateCallback);
        activeScreen = null;
        screenStop = null;
        screens.clear();

        stopService(new Intent(this, RecordService.class));

        WorkoutDataStore.storeWorkout(this, workoutData);
        Main.runInBackground(()->SettingsStore.storeLastWorkoutType(this, workoutData.workoutType));

        if(isAutoEnding()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(" ");//This makes sure the dialog shows properly for a round screen
        builder.setMessage(R.string.alert_cooldown_start);
        builder.setPositiveButton(R.string.alert_cooldown_yes, (d, w)->showCooldown());
        builder.setNegativeButton(R.string.alert_cooldown_no, (d, w)->showSummary());
        builder.create().show();
    }
    private void showCooldown(){
        fragmentManager.beginTransaction().replace(R.id.screens, new Cooldown()).commit();
    }
    void showSummary(){
        exerciseClient.endExerciseAsync();
        startActivity(new Intent(this, WorkoutActivity.class));
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        fragmentManager.getFragments().forEach(transaction::remove);
        transaction.commit();
        finishAndRemoveTask();
    }
    private boolean isAutoEnding(){
        return exerciseState.isEnding() && exerciseState != ExerciseState.ENDING;
    }
    private final Runnable updateTime = new Runnable(){@Override public void run(){
        Date date = new Date();
        currentTime = sdfTime.format(date);
        activeScreen.updateTime(currentTime);
        handler.postDelayed(updateTime, 60000-(date.getTime()%60000));
    }};

    private final Runnable update = new Runnable(){@Override public void run(){
        if(exerciseState == null || checkpointTime == 0){
            handler.postDelayed(update, 200);
            return;
        }

        long currentTime = System.currentTimeMillis();
        if(exerciseState.isPaused()){
            long pause = currentTime - checkpointTime + workoutData.pauseTotal;
            activeScreen.updatePause(pause);
            handler.postDelayed(update, 1000-(pause%1000));
            return;
        }
        long duration = currentTime - checkpointTime + checkpointDuration;
        activeScreen.updateDuration(duration);
        workoutData.durationTotal = duration;

        if(workoutData.primaryTargetType == WorkoutData.TargetType.DURATION){
            updateTargetProgress((int) ((duration/600) / workoutData.primaryTargetValue));
        }

        handler.postDelayed(update, 1000-(duration%1000));
    }};
    long timeToDuration(long time){return time - checkpointTime + checkpointDuration;}
    private void setScreenOnFlag(){
        if(SettingsStore.isSettingEnabled(workoutSettings.screenOn)){
            workoutSettings.screenOn = ENABLED;
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }else{
            workoutSettings.screenOn = DISABLED;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
    void forceScreenOn(){getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}
    private final ExerciseUpdateCallback exerciseUpdateCallback = new ExerciseUpdateCallback(){
        @Override public void onRegistrationFailed(@NonNull Throwable throwable){
            Log.e(Main.LOG_TAG, "RecordActivity.ExerciseUpdateCallback.onRegistrationFailed: " + throwable);
        }
        @Override public void onRegistered(){}
        @Override public void onLapSummaryReceived(@NonNull ExerciseLapSummary exerciseLapSummary){}
        @SuppressWarnings("unchecked") @Override public void onExerciseUpdateReceived(@NonNull ExerciseUpdate exerciseUpdate){
            if(exerciseState != exerciseUpdate.getExerciseStateInfo().getState()){
                Log.d(Main.LOG_TAG, "RecordActivity State change: " + exerciseState + " > " + exerciseUpdate.getExerciseStateInfo().getState());
                exerciseState = exerciseUpdate.getExerciseStateInfo().getState();
                if(isAutoEnding()){
                    Log.e(Main.LOG_TAG, "RecordActivity.onExerciseUpdateReceived: isAutoEnding");
                    endExercise();
                    return;
                }
                if(activeScreen != null) activeScreen.updateState(exerciseState);
            }
            if(activeScreen == null || exerciseState == ExerciseState.ENDED) return;

            if(exerciseUpdate.getActiveDurationCheckpoint() != null){
                ExerciseUpdate.ActiveDurationCheckpoint activeDurationCheckpoint = exerciseUpdate.getActiveDurationCheckpoint();
                if(activeDurationCheckpoint.getTime().toEpochMilli() > checkpointTime){
                    long checkpointTimePrev = checkpointTime;
                    long checkpointDurationPrev = checkpointDuration;
                    checkpointTime = activeDurationCheckpoint.getTime().toEpochMilli();
                    checkpointDuration = activeDurationCheckpoint.getActiveDuration().toMillis();
                    workoutData.addCheckpoint(checkpointTime, checkpointDuration);
                    if(checkpointDurationPrev == checkpointDuration && checkpointDuration > 0){
                        //resumed after a pause
                        workoutData.pauseTotal = System.currentTimeMillis() - checkpointTimePrev + workoutData.pauseTotal;
                    }
                }
            }

            DataPointContainer dataPointContainer = exerciseUpdate.getLatestMetrics();
            for(SampleDataPoint<?> dataPoint : dataPointContainer.getSampleDataPoints()){
                DataType<?, ? extends DataPoint<?>> dataType = dataPoint.getDataType();
                if(dataType.equals(DataType.HEART_RATE_BPM)){
                    SampleDataPoint<Double> dataPointHR = (SampleDataPoint<Double>) dataPoint;
                    HeartRateSample sample = new HeartRateSample();
                    sample.time = dataPointHR.getTimeDurationFromBoot().plusMillis(bootTime).toMillis();
                    sample.heartRate = dataPointHR.getValue().intValue();
                    activeScreen.updateHeartRate(sample, exerciseState.isPaused());
                    if(!exerciseState.isPaused()) workoutData.heartRateSamples.add(sample);
                }else if(dataType.equals(DataType.LOCATION)){
                    SampleDataPoint<LocationData> dataPointLocation = (SampleDataPoint<LocationData>) dataPoint;
                    LocationSample sample = new LocationSample();
                    sample.time = dataPointLocation.getTimeDurationFromBoot().plusMillis(bootTime).toMillis();
                    sample.lat = dataPointLocation.getValue().getLatitude();
                    sample.lon = dataPointLocation.getValue().getLongitude();
                    sample.ele = lastElevation;
                    workoutData.locationSamples.add(sample);
                    lastLocationTime = sample.time;
                    sample.speed = lastSpeed;
                    activeScreen.updateLocation(sample);
                }else if(dataType.equals(DataType.ABSOLUTE_ELEVATION)){
                    SampleDataPoint<Double> dataPointElevation = (SampleDataPoint<Double>) dataPoint;
                    lastElevation = dataPointElevation.getValue();
                }else{
                    Log.d(Main.LOG_TAG, "Unknown SampleDataPoint: " + dataType + " dataPoint: " + dataPoint);
                }
            }
            for(StatisticalDataPoint<?> dataPoint : dataPointContainer.getStatisticalDataPoints()){
                DataType<?, ? extends DataPoint<?>> dataType = dataPoint.getDataType();
                if(dataType.equals(DataType.HEART_RATE_BPM_STATS)){
                    StatisticalDataPoint<Double> statisticalDataPointDouble = (StatisticalDataPoint<Double>) dataPoint;
                    workoutData.heartRateAvg = statisticalDataPointDouble.getAverage();
                    workoutData.heartRateMax = statisticalDataPointDouble.getMax();
                }else if(dataType.equals(DataType.SPEED_STATS)){
                    StatisticalDataPoint<Double> statisticalDataPointDouble = (StatisticalDataPoint<Double>) dataPoint;
                    workoutData.speedAvg = statisticalDataPointDouble.getAverage();
                    activeScreen.updateSpeedAvg(workoutData.speedAvg);
                }else{
                    Log.d(Main.LOG_TAG, "Unknown StatisticalDataPoint: " + dataType + " dataPoint: " + dataPoint);
                }
            }
            for(CumulativeDataPoint<?> dataPoint : dataPointContainer.getCumulativeDataPoints()){
                DataType<?, ? extends DataPoint<?>> dataType = dataPoint.getDataType();
                if(dataType.equals(DataType.CALORIES_TOTAL)){
                    CumulativeDataPoint<Double> cdDataPoint = (CumulativeDataPoint<Double>) dataPoint;
                    workoutData.caloriesTotal = cdDataPoint.getTotal();
                    activeScreen.updateCalories(workoutData.caloriesTotal);
                }else if(dataType.equals(DataType.DISTANCE_TOTAL)){
                    CumulativeDataPoint<Double> cdDataPoint = (CumulativeDataPoint<Double>) dataPoint;
                    workoutData.distanceTotal = cdDataPoint.getTotal();
                    activeScreen.updateDistance(workoutData.distanceTotal);
                    updateLastSpeed(workoutData.distanceTotal.intValue());
                    if(workoutData.primaryTargetType == WorkoutData.TargetType.DISTANCE){
                        updateTargetProgress((int) ((workoutData.distanceTotal / 10) / workoutData.primaryTargetValue));
                    }
                }else if(dataType.equals(DataType.ELEVATION_GAIN_TOTAL)){
                    CumulativeDataPoint<Double> cdDataPoint = (CumulativeDataPoint<Double>) dataPoint;
                    workoutData.elevationGainTotal = cdDataPoint.getTotal();
                    activeScreen.updateEleGain(workoutData.elevationGainTotal);
                }else if(dataType.equals(DataType.ELEVATION_LOSS_TOTAL)){
                    CumulativeDataPoint<Double> cdDataPoint = (CumulativeDataPoint<Double>) dataPoint;
                    workoutData.elevationLossTotal = cdDataPoint.getTotal();
                    activeScreen.updateEleLoss(workoutData.elevationLossTotal);
                }else{
                    Log.d(Main.LOG_TAG, "Unknown CumulativeDataPoint: " + dataType + " dataPoint: " + dataPoint);
                }
            }
        }

        @Override public void onExerciseEventReceived(@NonNull ExerciseEvent event){ExerciseUpdateCallback.super.onExerciseEventReceived(event);}
        @Override public void onAvailabilityChanged(@NonNull DataType<?, ?> dataType, @NonNull Availability availability){
            if(dataType.equals(DataType.LOCATION)){
                if(availability.equals(LocationAvailability.ACQUIRED_TETHERED)
                        || availability.equals(LocationAvailability.ACQUIRED_UNTETHERED)
                ){
                    if(prepare != null) prepare.locationAvailable();
                }else{
                    if(prepare != null) prepare.locationUnavailable();
                }
            }else if(dataType.equals(DataType.HEART_RATE_BPM)){
                if(availability.equals(DataTypeAvailability.AVAILABLE)){
                    if(prepare != null) prepare.heartRateAvailable();
                }else{
                    if(prepare != null) prepare.heartRateUnavailable();
                }
            }
        }
    };
    private void updateLastSpeed(int distance){
        if(lastLocationTime == 0 || distance == 0) return;
        speedCache.add(new SpeedCachePoint(lastLocationTime, distance));
        if(speedCache.size() < 5) return;//wait for the speed cache to contain 5 measurements
        if(clearSpeedCache){//ignore the first 5 measurements, because GPS is unreliable in the beginning
            speedCache.clear();
            clearSpeedCache = false;
            return;
        }
        double distanceDiff = distance - speedCache.get(0).distance;//meters
        double timeDiff = lastLocationTime - speedCache.get(0).timestamp;//milliseconds
        if(distanceDiff < 4 || timeDiff < 1){
            lastSpeed = 0;
            activeScreen.updateSpeed(lastLocationTime, lastSpeed);
            return;
        }
        double newSpeed = (distanceDiff * 1000) / timeDiff;
        if(workoutData.speedMax > 0 && newSpeed > workoutData.speedMax*5){//gps error (going through tunnel for example)
            speedCache.clear();
            return;
        }
        lastSpeed = newSpeed;//meters/second
        activeScreen.updateSpeed(lastLocationTime, lastSpeed);

        if(lastSpeed > workoutData.speedMax) workoutData.speedMax = lastSpeed;
        while(speedCache.size()>5) speedCache.remove(0);
    }
    private void updateTargetProgress(int value){
        if(targetAchieved == TargetAchieved.NOT && value >= 50){
            targetAchieved = TargetAchieved._50;
            vibrator.vibrate(ve_pattern);
        }else if(targetAchieved == TargetAchieved._50 && value >= 100){
            targetAchieved = TargetAchieved._100;
            screens.forEach(Screen::setTargetAchieved);
            screenStop.setTargetAchieved();
            vibrator.vibrate(ve_pattern);
            if(workoutData.targetAutoPause) pauseExercise();
        }
        activeScreen.updateTargetProgress(value >= 100 ? value - 100 : value);
    }
    void addOnTouch(View v){v.setOnTouchListener(this::onTouch);}
    private boolean onTouch(View ignoredV, MotionEvent e){return gestureDetector.onTouchEvent(e);}
    @Override public boolean onTouchEvent(MotionEvent e){return gestureDetector.onTouchEvent(e);}
    private final GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener(){
        @Override public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float vX, float vY){
            if(Math.abs(vX) < Math.abs(vY)){
                if(vY > 0){
                    swipeUp();
                }else{
                    swipeDown();
                }
            }else{
                if(vX > 0){
                    swipeRight();
                }else{
                    swipeLeft();
                }
            }
            return true;
        }
    };
    void swipeUp(){
        if(activeScreen.getClass() == ScreenMap.class) return;
        goUp();
    }
    void goUp(){
        if(screens.size() == 1 && screens.get(0).getClass() == ScreenMap.class) showScreen(screenStop);
        int i = screens.indexOf(activeScreen);
        if(i>=0 && i<screens.size()-1) showScreen(screens.get(i+1));
    }
    void swipeDown(){
        if(activeScreen.getClass() == ScreenMap.class) return;
        int i = screens.indexOf(activeScreen);
        if(i>0) showScreen(screens.get(i-1));
    }
    private void swipeLeft(){
        if(screenStop == null || !screenStop.isVisible()) return;
        if(screens.size() > 1 && screens.get(0).getClass() == ScreenMap.class){
            //The first screen is a map, show the second screen
            showScreen(screens.get(1));
        }else if(!screens.isEmpty()){
            //There is a screen, and the first is not the map, show the first
            showScreen(screens.get(0));
        }
    }
    private void swipeRight(){
        if(prepare != null && prepare.isVisible()){
            finishAndRemoveTask();
        }else if(screenStop != null
                && !screenStop.isVisible()
                && activeScreen.getClass() != ScreenMap.class
        ){
            showScreen(screenStop);
        }
    }
    private void showScreen(Screen newScreen){
        if(activeScreen == null){
            fragmentManager.beginTransaction().show(newScreen).commit();
        }else{
            newScreen.show();
            fragmentManager.beginTransaction().show(newScreen).hide(activeScreen).commit();
            activeScreen.hide();
        }
        activeScreen = newScreen;
    }

    private float scrollDiff = 0;
    private long scrollTime = 0;
    @Override public boolean dispatchGenericMotionEvent(MotionEvent event){
        super.dispatchGenericMotionEvent(event);
        if(event.getAction() == MotionEvent.ACTION_SCROLL){
            if(scrollTime < event.getEventTime()-200) scrollDiff = 0;
            scrollTime = event.getEventTime();
            scrollDiff += event.getAxisValue(MotionEvent.AXIS_SCROLL);

            if(scrollDiff > 1){
                activeScreen.onScroll(false);
                scrollDiff = 0;
            }else if(scrollDiff < -1){
                activeScreen.onScroll(true);
                scrollDiff = 0;
            }
        }
        return true;
    }

    private final FutureCallback<? super Void> startCallback = new FutureCallback<>(){
        @Override public void onSuccess(Void result){}
        @Override public void onFailure(@NonNull Throwable throwable){
            Log.e(Main.LOG_TAG, "RecordActivity.startCallback.onFailure: " + throwable);
            runOnUiThread(()->{
                Toast.makeText(getBaseContext(), R.string.error_start_exercise, Toast.LENGTH_LONG).show();
                finishAndRemoveTask();
            });
        }
    };

    private void checkPermissions(){
        try{
            locationEnabled = ((LocationManager)getSystemService(Context.LOCATION_SERVICE))
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "RecordActivity.checkPermissions exception: " + e.getMessage());
        }

        if(Build.VERSION.SDK_INT >= 33){
            hasLocationPermission = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    && hasPermission(Manifest.permission.ACTIVITY_RECOGNITION);
            hasHRPermission = hasPermission(Manifest.permission.BODY_SENSORS);
            if(!hasLocationPermission
                    || !hasHRPermission
                    || !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
            ){
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.POST_NOTIFICATIONS
                        ,Manifest.permission.ACCESS_FINE_LOCATION
                        ,Manifest.permission.ACTIVITY_RECOGNITION
                        ,Manifest.permission.BODY_SENSORS}, 1);
            }
        }else{//30
            hasLocationPermission = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    && hasPermission(Manifest.permission.ACTIVITY_RECOGNITION);
            hasHRPermission = hasPermission(Manifest.permission.BODY_SENSORS);
            if(!hasLocationPermission || !hasHRPermission
            ){
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                        ,Manifest.permission.ACTIVITY_RECOGNITION
                        ,Manifest.permission.BODY_SENSORS}, 1);
            }
        }
    }
    private boolean hasPermission(String permission){
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int i=0; i<permissions.length; i++){
            if(permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)
                    && grantResults[i] == PackageManager.PERMISSION_GRANTED
            ){
                hasLocationPermission = true;
                prepare.locationPermissionGranted();
            }
            if(permissions[i].equals(Manifest.permission.BODY_SENSORS)
                    && grantResults[i] == PackageManager.PERMISSION_GRANTED
            ){
                hasHRPermission = true;
                prepare.heartRatePermissionGranted();
            }
        }
        if(hasLocationPermission || hasHRPermission) prepareExercise();
    }
}