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
package de.tadris.fitness.recording

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import de.tadris.fitness.Instance
import de.tadris.fitness.data.RecordingType
import de.tadris.fitness.data.WorkoutTypeManager
import de.tadris.fitness.recording.component.AnnouncementComponent
import de.tadris.fitness.recording.component.AutoPauseComponent
import de.tadris.fitness.recording.component.AutoStopComponent
import de.tadris.fitness.recording.component.ExerciseRecognitionComponent
import de.tadris.fitness.recording.component.GnssComponent
import de.tadris.fitness.recording.component.GpsComponent
import de.tadris.fitness.recording.component.GpsSampleComponent
import de.tadris.fitness.recording.component.GpsStateComponent
import de.tadris.fitness.recording.component.HeartRateComponent
import de.tadris.fitness.recording.component.PressureComponent
import de.tadris.fitness.recording.component.RecorderServiceComponent
import de.tadris.fitness.recording.component.RepetitionSampleComponent
import de.tadris.fitness.recording.component.ServiceNotificationComponent
import de.tadris.fitness.recording.component.TemporaryAggregationComponent
import de.tadris.fitness.recording.event.AggregatedRecordingValuesEvent
import de.tadris.fitness.recording.event.RecordingStateChangeEvent
import de.tadris.fitness.util.WorkoutLogger.Companion.log
import org.greenrobot.eventbus.EventBus
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * The RecorderService is responsible for collecting data and publishing it to other app parts like
 * the WorkoutRecorder or RecorderActivity. Also it handles the notification and a watchdog.
 *
 *
 * It starts RecorderServiceComponents depending on the workout type.
 */
class RecorderService : Service() {

    companion object {
        const val EXTRA_WORKOUT_TYPE = "workoutType"

        const val TAG: String = "RecorderService"

        const val MIN_DURATION_DIFF: Int = 500 // minimum time between samples
        private const val COMPONENT_INTERVAL_FAST = 1000
        private const val COMPONENT_INTERVAL_SLOW_MULTIPLIER = 5
    }

    private var wakeLock: WakeLock? = null

    private val components = mutableListOf<RecorderServiceComponent>()
    private var notificationComponent: ServiceNotificationComponent? = null
    var actions: RecorderServiceActions = RecorderServiceActions(this)

    lateinit var instance: Instance
    lateinit var stateHolder: RecordingStateHolder

    private var mWatchdogRunner: WatchDogRunner? = null
    private var mWatchdogThread: Thread? = null

    private inner class WatchDogRunner : Runnable {
        private var count = 0
        private var running = true

        override fun run() {
            running = true
            try {
                while (running) {
                    count++
                    checkAllComponents(count % COMPONENT_INTERVAL_SLOW_MULTIPLIER == 0)
                    Thread.sleep(COMPONENT_INTERVAL_FAST.toLong())
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        fun stop() {
            running = false
        }
    }

    override fun onBind(arg0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log(TAG, "onStartCommand")
        super.onStartCommand(intent, flags, startId)

        stateHolder = RecordingStateHolder()
        stateHolder.workoutType = WorkoutTypeManager.getInstance().getWorkoutTypeById(
            this, intent?.getStringExtra(EXTRA_WORKOUT_TYPE) ?: WorkoutTypeManager.WORKOUT_TYPE_ID_OTHER
        )

        val notification = notificationComponent!!.getNotification()
        startForeground(ServiceNotificationComponent.NOTIFICATION_ID, notification)

        acquireWakelock()

        return START_STICKY
    }

    override fun onCreate() {
        this.instance = Instance.getInstance(baseContext)

        log(TAG, "Service created")
        log(TAG, "Android: " + Build.VERSION.RELEASE + " Sdk: " + Build.VERSION.SDK_INT)
        log(TAG, "Device: " + Build.PRODUCT + " / " + Build.DEVICE + " / " + Build.MODEL)

        startRelevantComponents()
        actions.register()

        initializeWatchdog()
    }

    override fun onDestroy() {
        log(TAG, "onDestroy")

        actions.unregister()
        stopAllComponents()

        // Shutdown Watchdog
        mWatchdogRunner!!.stop()

        if (wakeLock != null && wakeLock!!.isHeld) {
            wakeLock!!.release()
        }

        EventBus.getDefault().postSticky(AggregatedRecordingValuesEvent.EMPTY)
        EventBus.getDefault().postSticky(RecordingStateChangeEvent.EMPTY)

        stopForeground(true)
        super.onDestroy()
    }

    private fun startRelevantComponents() {
        val type = stateHolder.recordingType

        notificationComponent = ServiceNotificationComponent(this)
        startComponent(notificationComponent!!)

        startComponent(AutoPauseComponent())
        startComponent(AutoStopComponent())
        startComponent(TemporaryAggregationComponent())
        if (type == RecordingType.GPS) {
            startComponent(GpsComponent())
            startComponent(GpsSampleComponent())
            startComponent(GpsStateComponent())
            startComponent(PressureComponent())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                startComponent(GnssComponent())
            }
        } else {
            startComponent(ExerciseRecognitionComponent())
            startComponent(RepetitionSampleComponent())
        }
        startComponent(AnnouncementComponent())
        startComponent(HeartRateComponent())
    }

    private fun startComponent(component: RecorderServiceComponent) {
        component.register(this)
        components.add(component)
        log(TAG, "Started component " + component.javaClass.getSimpleName())
    }

    private fun checkAllComponents(alsoSlow: Boolean) {
        for (component in components) {
            component.checkFast()
            if (alsoSlow) component.checkSlow()
        }
    }

    private fun stopAllComponents() {
        for (component in components) {
            component.unregister()
            log(TAG, "Stopped component " + component.javaClass.getSimpleName())
        }
        components.clear()
    }

    private fun initializeWatchdog() {
        if (mWatchdogThread == null || !mWatchdogThread!!.isAlive) {
            mWatchdogRunner = WatchDogRunner()
            mWatchdogThread = Thread(mWatchdogRunner, "WorkoutWatchdog")
        }
        if (!mWatchdogThread!!.isAlive) {
            mWatchdogThread!!.start()
        }
    }

    private fun acquireWakelock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "de.tadris.fitotrack:workout_recorder"
        )
        wakeLock!!.acquire(TimeUnit.HOURS.toMillis(4))
    }

}
