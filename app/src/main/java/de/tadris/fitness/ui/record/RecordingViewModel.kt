/*
 * Copyright (c) 2026 Jannis Scheibe <jannis@tadris.de>
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
package de.tadris.fitness.ui.record

import android.app.ActivityManager
import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.IntervalSet
import de.tadris.fitness.data.RecordingType
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.recording.RecorderService
import de.tadris.fitness.recording.RecordingState
import de.tadris.fitness.recording.StartReason
import de.tadris.fitness.recording.StopReason
import de.tadris.fitness.recording.actions.WorkoutActionDiscard
import de.tadris.fitness.recording.actions.WorkoutActionPause
import de.tadris.fitness.recording.actions.WorkoutActionResume
import de.tadris.fitness.recording.actions.WorkoutActionSelectIntervalSet
import de.tadris.fitness.recording.actions.WorkoutActionStart
import de.tadris.fitness.recording.actions.WorkoutActionStop
import de.tadris.fitness.recording.autostart.AutoStartStateEvent
import de.tadris.fitness.recording.autostart.WorkoutAutoStartedEvent
import de.tadris.fitness.recording.component.AnnouncementComponent
import de.tadris.fitness.recording.component.HeartRateComponent
import de.tadris.fitness.recording.event.AggregatedRecordingValuesEvent
import de.tadris.fitness.recording.event.HeartRateConnectionChangeEvent
import de.tadris.fitness.recording.event.ReconnectBluetoothDeviceEvent
import de.tadris.fitness.recording.event.RecordingStateChangeEvent
import de.tadris.fitness.recording.event.TTSReadyEvent
import de.tadris.fitness.recording.event.WorkoutAutoStopEvent
import de.tadris.fitness.recording.information.InformationDisplay
import de.tadris.fitness.recording.information.RecordingInformation
import de.tadris.fitness.util.BluetoothDevicePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.Semaphore

data class InfoSlot(val slot: Int, val title: String, val value: String)

sealed interface AutoStartUiState {
    data object Idle : AutoStartUiState
    data class Countdown(val remainingMs: Long, val remainingS: Int) : AutoStartUiState
    data object WaitingForGps : AutoStartUiState
    data object WaitingForMove : AutoStartUiState
}

abstract class RecordingViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "RecordingViewModel"
    }

    protected val instance: Instance = Instance.getInstance(application)

    private var initialized = false
    protected lateinit var workoutType: WorkoutType
    protected lateinit var informationDisplay: InformationDisplay

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _values = MutableStateFlow(AggregatedRecordingValuesEvent.EMPTY)
    val values: StateFlow<AggregatedRecordingValuesEvent> = _values.asStateFlow()

    private val _hrConnectionState = MutableStateFlow(HeartRateComponent.HeartRateConnectionState.DISCONNECTED)
    val hrConnectionState: StateFlow<HeartRateComponent.HeartRateConnectionState> = _hrConnectionState.asStateFlow()

    private val _voiceFeedbackAvailable = MutableStateFlow(false)
    val voiceFeedbackAvailable: StateFlow<Boolean> = _voiceFeedbackAvailable.asStateFlow()

    private val _autoStartUiState = MutableStateFlow<AutoStartUiState>(AutoStartUiState.Idle)
    val autoStartUiState: StateFlow<AutoStartUiState> = _autoStartUiState.asStateFlow()

    private val _infoSlots = MutableStateFlow<List<InfoSlot>>(emptyList())
    val infoSlots: StateFlow<List<InfoSlot>> = _infoSlots.asStateFlow()

    private val _shouldFinish = MutableStateFlow(false)
    val shouldFinish: StateFlow<Boolean> = _shouldFinish.asStateFlow()

    private val _showCommentDialog = MutableStateFlow(false)
    val showCommentDialog: StateFlow<Boolean> = _showCommentDialog.asStateFlow()

    private val _showStopConfirmation = MutableStateFlow(false)
    val showStopConfirmation: StateFlow<Boolean> = _showStopConfirmation.asStateFlow()

    private val _showIntervalSetDialog = MutableStateFlow(false)
    val showIntervalSetDialog: StateFlow<Boolean> = _showIntervalSetDialog.asStateFlow()

    private val _selectInfoDialogSlot = MutableStateFlow<Int?>(null)
    val selectInfoDialogSlot: StateFlow<Int?> = _selectInfoDialogSlot.asStateFlow()

    private val _showChooseBluetoothDevice = MutableStateFlow(false)
    val showChooseBluetoothDevice: StateFlow<Boolean> = _showChooseBluetoothDevice.asStateFlow()

    private val _currentWorkoutType = MutableStateFlow<WorkoutType?>(null)
    val currentWorkoutType: StateFlow<WorkoutType?> = _currentWorkoutType.asStateFlow()

    private val startedSem = Semaphore(1)
    private var finishGuard = false

    private val mainHandler = Handler(Looper.getMainLooper())

    fun init(workoutType: WorkoutType) {
        if (initialized) return
        initialized = true

        this.workoutType = workoutType
        _currentWorkoutType.value = workoutType
        this.informationDisplay = InformationDisplay(
            RecordingType.findById(workoutType.recordingType),
            getApplication()
        )

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }

        // Refresh current state
        val currentEvent = AggregatedRecordingValuesEvent.getCurrent()
        _values.value = currentEvent
        _recordingState.value = RecordingStateChangeEvent.getCurrent().newState
        refreshInfoSlots(currentEvent)

        startService()
    }

    protected open fun onServiceStarted() {}

    // ---- Service management ----

    private val isServiceRunning: Boolean
        get() {
            val activityManager = getApplication<Application>().getSystemService(android.content.Context.ACTIVITY_SERVICE) as ActivityManager
            val services = activityManager.getRunningServices(Int.MAX_VALUE)
            for (runningServiceInfo in services) {
                if (runningServiceInfo.service.className == RecorderService::class.java.name) {
                    return true
                }
            }
            return false
        }

    fun startService() {
        if (!isServiceRunning) {
            if (hasServicePermission()) {
                Log.d(TAG, "Starting service")
                val intent = Intent(getApplication(), RecorderService::class.java)
                intent.putExtra(RecorderService.EXTRA_WORKOUT_TYPE, workoutType.id)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getApplication<Application>().startForegroundService(intent)
                } else {
                    getApplication<Application>().startService(intent)
                }
                onServiceStarted()
            } else {
                Log.i(TAG, "Not starting service, no permission")
            }
        } else {
            Log.d(TAG, "Service already running")
            onServiceStarted()
        }
    }

    fun stopService() {
        if (isServiceRunning) {
            Log.d(TAG, "Stopping service")
            val intent = Intent(getApplication(), RecorderService::class.java)
            getApplication<Application>().stopService(intent)
        }
    }

    fun restartService() {
        stopService()
        startService()
    }

    abstract fun hasServicePermission(): Boolean

    // ---- Actions ----

    fun start(reason: StartReason) {
        if (startedSem.drainPermits() == 0) {
            Log.w(TAG, "Cannot start the workout, already started.")
            return
        }
        EventBus.getDefault().post(WorkoutActionStart(reason))
    }
    
    /**
     * Stop recording for a given reason. If nothing has been recorded immediately call finish to
     * also stop the service and leave the screen.
     */
    fun stop(reason: StopReason) {
        startedSem.release()

        val current = AggregatedRecordingValuesEvent.getCurrent()
        EventBus.getDefault().post(WorkoutActionStop(reason))
        if (current.recordedSomething) {
            if (instance.userPreferences.askForComment) {
                _showCommentDialog.value = true
            } else {
                finish()
            }
        } else {
            if (current.currentState == RecordingState.IDLE) {
                showToast(R.string.noWorkoutStarted)
            } else {
                showToast(R.string.workoutDiscarded)
            }
            EventBus.getDefault().post(WorkoutActionDiscard)
            finish()
        }
    }
    
    /**
     * Should be called only after the recording has been stopped. This stops the service and tells the UI it is okay to leave the recording screen.
     */
    fun finish() {
        Log.d(TAG, "finish() finishGuard: $finishGuard")
        if (!finishGuard) {
            finishGuard = true
            val state = RecordingStateChangeEvent.getCurrent().newState
            if (state == RecordingState.IDLE) {
                stop(StopReason.ACTIVITY_DESTROY)
            }
            stopService()
            _shouldFinish.value = true
        }
    }

    fun toggleManualPause() {
        val state = _recordingState.value
        if (state == RecordingState.RUNNING || state == RecordingState.AUTO_PAUSE) {
            EventBus.getDefault().post(WorkoutActionPause(false))
        } else if (state == RecordingState.MANUAL_PAUSE) {
            EventBus.getDefault().post(WorkoutActionResume)
        }
    }

    fun onIntervalSetSelected(set: IntervalSet) {
        val intervals = instance.db.intervalDao().getAllIntervalsOfSet(set.id).toList()
        EventBus.getDefault().post(WorkoutActionSelectIntervalSet(set, intervals))
        showToast(de.tadris.fitness.R.string.intervalSetSelected)
        _showIntervalSetDialog.value = false
    }

    fun onInfoSlotSelected(slot: Int, information: RecordingInformation) {
        val mode = RecordingType.findById(workoutType.recordingType).id
        instance.userPreferences.setIdOfDisplayedInformation(mode, slot, information.id)
        refreshInfoSlots(AggregatedRecordingValuesEvent.getCurrent())
        _selectInfoDialogSlot.value = null
    }

    fun onCommentEntered(comment: String) {
        val workout = instance.db.workoutDao().getLastWorkout()
        if (workout != null) {
            instance.db.workoutDao().updateWorkout(workout.copy(comment = comment))
        }
        _showCommentDialog.value = false
        finish()
    }

    fun onBtDeviceSelected(device: android.bluetooth.BluetoothDevice) {
        BluetoothDevicePreferences(getApplication()).setAddress(
            BluetoothDevicePreferences.DEVICE_HEART_RATE,
            device.address
        )
        EventBus.getDefault().post(ReconnectBluetoothDeviceEvent)
        _showChooseBluetoothDevice.value = false
    }

    fun requestIntervalSetDialog() {
        _showIntervalSetDialog.value = true
    }

    fun dismissIntervalSetDialog() {
        _showIntervalSetDialog.value = false
    }

    fun requestSelectInfoDialog(slot: Int) {
        _selectInfoDialogSlot.value = slot
    }

    fun dismissSelectInfoDialog() {
        _selectInfoDialogSlot.value = null
    }

    fun requestStopConfirmation() {
        _showStopConfirmation.value = true
    }

    fun dismissStopConfirmation() {
        _showStopConfirmation.value = false
    }

    fun requestChooseBluetoothDevice() {
        _showChooseBluetoothDevice.value = true
    }

    fun dismissChooseBluetoothDevice() {
        _showChooseBluetoothDevice.value = false
    }

    // ---- EventBus subscribers ----

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNewInfo(e: AggregatedRecordingValuesEvent) {
        _values.value = e
        _recordingState.value = e.currentState
        refreshInfoSlots(e)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRecordingStateChange(e: RecordingStateChangeEvent) {
        _recordingState.value = e.newState
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onHeartRateConnectionChange(e: HeartRateConnectionChangeEvent) {
        _hrConnectionState.value = e.state
    }

    @Subscribe
    fun onVoiceAnnouncementIsReady(e: TTSReadyEvent) {
        if (e.id == AnnouncementComponent.TTS_CONTROLLER_ID) {
            _voiceFeedbackAvailable.value = e.ttsAvailable
        }
    }

    @Subscribe
    fun onAutoStop(e: WorkoutAutoStopEvent) {
        mainHandler.post { finish() }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onAutoStartState(e: AutoStartStateEvent) {
        _autoStartUiState.value = when (val s = e.state) {
            is AutoStartStateEvent.State.Idle -> AutoStartUiState.Idle
            is AutoStartStateEvent.State.Countdown -> AutoStartUiState.Countdown(s.remainingMs, s.remainingS)
            is AutoStartStateEvent.State.WaitingForGps -> AutoStartUiState.WaitingForGps
            is AutoStartStateEvent.State.WaitingForMove -> AutoStartUiState.WaitingForMove
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWorkoutAutoStarted(e: WorkoutAutoStartedEvent) {
        showToast(de.tadris.fitness.R.string.workoutAutoStarted)
    }

    // ---- Info slots ----

    private fun refreshInfoSlots(e: AggregatedRecordingValuesEvent) {
        val slots = (0..3).map { slot ->
            val data = informationDisplay.getDisplaySlot(e, slot)
            InfoSlot(slot, data.title, data.value)
        }
        _infoSlots.value = slots
    }

    // ---- Helper ----

    protected fun showToast(resId: Int) {
        mainHandler.post {
            Toast.makeText(getApplication(), resId, Toast.LENGTH_SHORT).show()
        }
    }

    // ---- Lifecycle ----

    override fun onCleared() {
        super.onCleared()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }
}
