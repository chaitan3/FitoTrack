/*
 * Copyright (c) 2024 Jannis Scheibe <jannis@tadris.de>
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

package de.tadris.fitness.recording;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.tadris.fitness.Instance;
import de.tadris.fitness.data.RecordingType;
import de.tadris.fitness.recording.component.AnnouncementComponent;
import de.tadris.fitness.recording.component.AutoPauseComponent;
import de.tadris.fitness.recording.component.AutoStopComponent;
import de.tadris.fitness.recording.component.ExerciseRecognitionComponent;
import de.tadris.fitness.recording.component.GnssComponent;
import de.tadris.fitness.recording.component.GpsComponent;
import de.tadris.fitness.recording.component.GpsSampleComponent;
import de.tadris.fitness.recording.component.GpsStateComponent;
import de.tadris.fitness.recording.component.HeartRateComponent;
import de.tadris.fitness.recording.component.PressureComponent;
import de.tadris.fitness.recording.component.RecorderServiceComponent;
import de.tadris.fitness.recording.component.ServiceNotificationComponent;
import de.tadris.fitness.recording.component.TemporaryAggregationComponent;
import de.tadris.fitness.util.WorkoutLogger;

/**
 * The RecorderService is responsible for collecting data and publishing it to other app parts like
 * the WorkoutRecorder or RecorderActivity. Also it handles the notification and a watchdog.
 * <p>
 * It starts RecorderServiceComponents depending on the workout type.
 */
public class RecorderService extends Service {

    protected Date serviceStartTime;

    public static final String TAG = "RecorderService";

    public static final int MIN_DURATION_DIFF = 500; // minimum time between samples
    private static final int COMPONENT_INTERVAL_FAST = 1000;
    private static final int COMPONENT_INTERVAL_SLOW_MULTIPLIER = 5;

    protected PowerManager.WakeLock wakeLock;

    public Instance instance = null;

    private final List<RecorderServiceComponent> components = new ArrayList<>();
    private ServiceNotificationComponent notificationComponent;
    public RecordingStateHolder stateHolder; // TODO needs to be initialized
    public RecorderServiceActions actions = new RecorderServiceActions(); // TODO needs to be initialized correctly

    protected WatchDogRunner mWatchdogRunner;
    protected Thread mWatchdogThread = null;

    private class WatchDogRunner implements Runnable {
        private int count = 0;
        private boolean running = true;

        @Override
        public void run() {
            running = true;
            try {
                while (running) {
                    count++;
                    checkAllComponents(count % COMPONENT_INTERVAL_SLOW_MULTIPLIER == 0);
                    Thread.sleep(COMPONENT_INTERVAL_FAST);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void stop() {
            running = false;
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        WorkoutLogger.log(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);

        serviceStartTime = new Date();
        Notification notification = notificationComponent.getNotification();

        startForeground(ServiceNotificationComponent.NOTIFICATION_ID, notification);

        acquireWakelock();

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        this.instance = Instance.getInstance(getBaseContext());

        WorkoutLogger.log(TAG, "Service created");
        WorkoutLogger.log(TAG, "Android: " + Build.VERSION.RELEASE + " Sdk: " + Build.VERSION.SDK_INT);
        WorkoutLogger.log(TAG, "Device: " + Build.PRODUCT + " / " + Build.DEVICE + " / " + Build.MODEL);

        startRelevantComponents();

        initializeWatchdog();
    }

    @Override
    public void onDestroy() {
        WorkoutLogger.log(TAG, "onDestroy");

        stopAllComponents();

        // Shutdown Watchdog
        mWatchdogRunner.stop();

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        stopForeground(true);
        super.onDestroy();
    }

    private void startRelevantComponents(){
        RecordingType type = stateHolder.getRecordingType();

        notificationComponent = new ServiceNotificationComponent(this);
        startComponent(notificationComponent);

        startComponent(new AutoPauseComponent());
        startComponent(new AutoStopComponent());
        startComponent(new TemporaryAggregationComponent());
        if(type == RecordingType.GPS) {
            startComponent(new GpsComponent());
            startComponent(new GpsSampleComponent());
            startComponent(new GpsStateComponent());
            startComponent(new PressureComponent());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                startComponent(new GnssComponent());
            }
        }else{
            startComponent(new ExerciseRecognitionComponent());
        }
        startComponent(new AnnouncementComponent());
        startComponent(new HeartRateComponent());
    }

    private void startComponent(RecorderServiceComponent component){
        component.register(this);
        components.add(component);
        WorkoutLogger.log(TAG, "Started component " + component.getClass().getSimpleName());
    }

    private void checkAllComponents(boolean alsoSlow) {
        for(RecorderServiceComponent component : components){
            component.checkFast();
            if (alsoSlow) component.checkSlow();
        }
    }

    private void stopAllComponents(){
        for(RecorderServiceComponent component : components){
            component.unregister();
            WorkoutLogger.log(TAG, "Stopped component " + component.getClass().getSimpleName());
        }
        components.clear();
    }

    private void initializeWatchdog() {
        if (mWatchdogThread == null || !mWatchdogThread.isAlive()) {
            mWatchdogRunner = new WatchDogRunner();
            mWatchdogThread = new Thread(mWatchdogRunner, "WorkoutWatchdog");
        }
        if (!mWatchdogThread.isAlive()) {
            mWatchdogThread.start();
        }
    }

    private void acquireWakelock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "de.tadris.fitotrack:workout_recorder");
        wakeLock.acquire(TimeUnit.HOURS.toMillis(4));
    }

}
