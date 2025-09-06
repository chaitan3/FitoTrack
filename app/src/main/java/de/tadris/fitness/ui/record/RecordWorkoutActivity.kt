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
package de.tadris.fitness.ui.record

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.KeyguardManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.DialogInterface
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.IntervalSet
import de.tadris.fitness.data.RecordingType
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.data.WorkoutTypeManager
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
import de.tadris.fitness.recording.announcement.TTSController
import de.tadris.fitness.recording.autostart.AutoStartAnnouncements
import de.tadris.fitness.recording.autostart.AutoStartSoundFeedback
import de.tadris.fitness.recording.autostart.AutoStartVibratorFeedback
import de.tadris.fitness.recording.autostart.AutoStartWorkout
import de.tadris.fitness.recording.autostart.AutoStartWorkout.AbortEvent
import de.tadris.fitness.recording.autostart.AutoStartWorkout.BeginEvent
import de.tadris.fitness.recording.autostart.AutoStartWorkout.CountdownChangeEvent
import de.tadris.fitness.recording.autostart.AutoStartWorkout.StateChangeEvent
import de.tadris.fitness.recording.autostart.DefaultMovementDetector
import de.tadris.fitness.recording.autostart.MovementDetector
import de.tadris.fitness.recording.component.AnnouncementComponent
import de.tadris.fitness.recording.event.AggregatedRecordingValuesEvent
import de.tadris.fitness.recording.event.AggregatedRecordingValuesEvent.Companion.getCurrent
import de.tadris.fitness.recording.event.HeartRateConnectionChangeEvent
import de.tadris.fitness.recording.event.TTSReadyEvent
import de.tadris.fitness.recording.event.WorkoutAutoStopEvent
import de.tadris.fitness.recording.information.InformationDisplay
import de.tadris.fitness.recording.information.RecordingInformation
import de.tadris.fitness.ui.FitoTrackActivity
import de.tadris.fitness.ui.LauncherActivity
import de.tadris.fitness.ui.dialog.AlertDialogWrapper
import de.tadris.fitness.ui.dialog.ChooseAutoStartDelayDialog
import de.tadris.fitness.ui.dialog.ChooseAutoStartDelayDialog.AutoStartDelaySelectListener
import de.tadris.fitness.ui.dialog.ChooseAutoStartModeDialog
import de.tadris.fitness.ui.dialog.ChooseAutoStartModeDialog.AutoStartModeSelectListener
import de.tadris.fitness.ui.dialog.ChooseBluetoothDeviceDialog
import de.tadris.fitness.ui.dialog.ChooseBluetoothDeviceDialog.BluetoothDeviceSelectListener
import de.tadris.fitness.ui.dialog.ChooseBluetoothDeviceDialog.BluetoothNotAvailableException
import de.tadris.fitness.ui.dialog.SelectIntervalSetDialog
import de.tadris.fitness.ui.dialog.SelectIntervalSetDialog.IntervalSetSelectListener
import de.tadris.fitness.ui.dialog.SelectWorkoutInformationDialog
import de.tadris.fitness.ui.dialog.SelectWorkoutInformationDialog.WorkoutInformationSelectListener
import de.tadris.fitness.ui.record.InfoViewHolder.InfoViewClickListener
import de.tadris.fitness.util.AbstractAnimatorListener
import de.tadris.fitness.util.BluetoothDevicePreferences
import de.tadris.fitness.util.NfcAdapterHelper
import de.tadris.fitness.util.NotificationHelper
import de.tadris.fitness.util.PermissionUtils.checkPermission
import de.tadris.fitness.util.ToneGeneratorController
import de.tadris.fitness.util.VibratorController
import de.tadris.fitness.util.WorkoutLogger.Companion.log
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.Semaphore
import kotlin.math.hypot

abstract class RecordWorkoutActivity : FitoTrackActivity(), IntervalSetSelectListener,
    InfoViewClickListener, WorkoutInformationSelectListener, BluetoothDeviceSelectListener {

    companion object {
        const val TAG: String = "RecordWorkoutActivity"

        const val LAUNCH_ACTION: String = "de.tadris.fitness.RecordWorkoutActivity.LAUNCH_ACTION"
        const val RESUME_ACTION: String = "de.tadris.fitness.RecordWorkoutActivity.RESUME_ACTION"
        const val WORKOUT_TYPE_EXTRA: String =
            "de.tadris.fitness.RecordWorkoutActivity.WORKOUT_TYPE_EXTRA"

        const val REQUEST_CODE_ENABLE_BLUETOOTH: Int = 12
        const val REQUEST_CODE_BLUETOOTH_PERMISSION: Int = 13

        // used to convert auto start time timebase from/to ms
        private const val AUTO_START_DELAY_MULTIPLIER = 1000 // s to ms
    }

    protected lateinit var instance: Instance
    protected val infoViews: Array<InfoViewHolder?> = arrayOfNulls(4)

    protected var isResumed2: Boolean = false


    private lateinit var startPopupButton: Button
    private var startPopupMenu: PopupMenu? = null
    protected lateinit var timeView: TextView
    protected lateinit var hrStatusView: ImageView
    protected lateinit var waitingForGPSOverlay: View
    protected lateinit var startButton: Button
    protected lateinit var recordStartButtonsRoot: ConstraintLayout
    protected lateinit var informationDisplay: InformationDisplay

    private var useNfcStart = false
    private var autoStartDelayMs: Long = 0 // in ms
    private var autoStartMode: AutoStartWorkout.Mode? = null
    private var autoStartCountdownOverlay: View? = null
    private var autoStartDelayDialog: AlertDialogWrapper? = null
    private lateinit var movementDetector: MovementDetector
    private lateinit var autoStartWorkout: AutoStartWorkout
    private var askForComment = false
    private var vibratorController: VibratorController? = null
    private lateinit var autoStartVibratorFeedback: AutoStartVibratorFeedback
    private var toneGeneratorController: ToneGeneratorController? = null
    private lateinit var autoStartSoundFeedback: AutoStartSoundFeedback
    private var autoStartAnnouncements: AutoStartAnnouncements? = null
    private var ttsController: TTSController? = null

    private var voiceFeedbackAvailable = false
    private var finished = false
    protected lateinit var activity: WorkoutType

    /**
     * This ensures that the workout is only started once. Different threads (user input, auto start)
     * might otherwise lead to nasty race conditions possibly starting a workout multiple times.
     */
    private val startedSem = Semaphore(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = Instance.getInstance(this)
        activity = WorkoutTypeManager.getInstance()
            .getWorkoutTypeById(this, WorkoutTypeManager.WORKOUT_TYPE_ID_OTHER)

        // only use NFC when it's enabled in settings AND supported by the device
        this.useNfcStart = instance.userPreferences.useNfcStart &&
                NfcAdapterHelper.isNfcPresent(this)
        Log.d(TAG, "NFC start enabled:" + this.useNfcStart)

        this.autoStartDelayMs =
            instance.userPreferences.autoStartDelay.toLong() * AUTO_START_DELAY_MULTIPLIER
        this.autoStartMode = instance.userPreferences.autoStartMode
        Log.d(
            TAG, "auto start enabled, auto start delay: " +
                    this.autoStartDelayMs + ", auto start mode: " + autoStartMode
        )
        this.askForComment = instance.userPreferences.askForComment

        log(TAG, "Activity created")
    }

    protected fun initBeforeContent() {
        setTheme(instance.themes.getWorkoutTypeTheme(activity))
    }

    protected fun initAfterContent() {
        if (useNfcStart) {
            // ask the user to enable NFC in device settings if it isn't at the moment
            if (!NfcAdapterHelper.isNfcEnabled(this)) {
                NfcAdapterHelper.createNfcEnableDialog(this).show()
            }
        }

        recordStartButtonsRoot = findViewById<ConstraintLayout>(R.id.recordStartButtonsRoot)
        timeView = findViewById(R.id.recordTime)
        timeView.visibility = View.INVISIBLE
        hrStatusView = findViewById<ImageView>(R.id.recordHrStatus)
        startButton = findViewById<Button>(R.id.recordStart)

        startPopupButton = findViewById<Button>(R.id.recordStartPopup)

        // instantiate TTSController in app context to be able to completely play the auto start
        // abort announcement even when this activity has been destroyed already
        ttsController = TTSController(applicationContext, TAG)
        movementDetector = DefaultMovementDetector()
        autoStartWorkout = AutoStartWorkout(
            AutoStartWorkout.Config(
                autoStartDelayMs,
                autoStartMode
            ), movementDetector
        )
        movementDetector.registerTo(EventBus.getDefault())
        movementDetector.start()
        vibratorController = VibratorController(this)
        autoStartVibratorFeedback = AutoStartVibratorFeedback(vibratorController)
        autoStartVibratorFeedback!!.registerTo(EventBus.getDefault())
        toneGeneratorController = ToneGeneratorController(this, AudioManager.STREAM_NOTIFICATION)
        autoStartSoundFeedback = AutoStartSoundFeedback(toneGeneratorController, instance)
        autoStartSoundFeedback.registerTo(EventBus.getDefault())
        autoStartAnnouncements = AutoStartAnnouncements(this, instance, ttsController)
        autoStartAnnouncements!!.registerTo(EventBus.getDefault())
        if (!autoStartWorkout.registerTo(EventBus.getDefault())) {
            Log.e(TAG, "onCreate: Failed to setup auto start helper, not using auto start")
            startPopupButton.visibility = View.GONE
        } else {
            show(startPopupButton)
        }

        updateStartButton(false, R.string.cannotStart, null)

        informationDisplay =
            InformationDisplay(RecordingType.findById(activity!!.recordingType), this)

        infoViews[0] = InfoViewHolder(
            0, this, findViewById<TextView?>(R.id.recordInfo1Title), findViewById<TextView?>(
                R.id.recordInfo1Value
            )
        )
        infoViews[1] = InfoViewHolder(
            1, this, findViewById<TextView?>(R.id.recordInfo2Title), findViewById<TextView?>(
                R.id.recordInfo2Value
            )
        )
        infoViews[2] = InfoViewHolder(
            2, this, findViewById<TextView?>(R.id.recordInfo3Title), findViewById<TextView?>(
                R.id.recordInfo3Value
            )
        )
        infoViews[3] = InfoViewHolder(
            3, this, findViewById<TextView?>(R.id.recordInfo4Title), findViewById<TextView?>(
                R.id.recordInfo4Value
            )
        )

        findViewById<View?>(R.id.autoStartCountdownAbort).setOnClickListener(View.OnClickListener { v: View? ->
            onAutoStartCountdownAbortButtonClicked()
        })

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }

        startService()
    }

    /**
     * Cancel auto start
     */
    private fun cancelAutoStart(userReq: Boolean) {
        // make sure auto start is cancelled
        EventBus.getDefault()
            .post(AbortEvent(if (userReq) AbortEvent.Reason.USER_REQ else AbortEvent.Reason.STARTED))
    }

    private fun autoStart() {
        log(TAG, "Starting workout automatically")

        // start the workout
        start(StartReason.AUTO_START)
        Toast.makeText(this, R.string.workoutAutoStarted, Toast.LENGTH_SHORT).show()
    }

    private fun showAutoStartCountdownOverlay() {
        if (autoStartCountdownOverlay != null) {
            autoStartCountdownOverlay!!.clearAnimation()

            // if the view's not visible currently, we should start the animation from full transparency
            if (autoStartCountdownOverlay!!.visibility != View.VISIBLE) {
                autoStartCountdownOverlay!!.setAlpha(0f)
            }
            // animation should take 1s max, if the view's not entirely hidden yet, it needs to be
            // proportionally shorter of course
            val durationMs = ((1 - autoStartCountdownOverlay!!.alpha) * 1000 + 0.5).toInt()

            // and finally start the animation
            autoStartCountdownOverlay!!.animate().alpha(1f).setDuration(durationMs.toLong())
                .setListener(object : AbstractAnimatorListener() {
                    override fun onAnimationStart(animator: Animator) {
                        // don't forget to make the view visible, needs to be done here in case hide
                        // animation is cancelled prematurely by show animation. There was a race
                        // condition when it was made visible again outside of the animation.
                        autoStartCountdownOverlay!!.visibility = View.VISIBLE
                    }
                }).start()
        }
    }

    private fun hideAutoStartCountdownOverlay() {
        if (autoStartCountdownOverlay != null &&
            autoStartCountdownOverlay!!.visibility != View.GONE
        ) {
            autoStartCountdownOverlay!!.clearAnimation()

            // animation should take 1s max, if the view's not entirely shown yet, it needs to be
            // proportionally shorter of course
            val durationMs = (autoStartCountdownOverlay!!.alpha * 1000 + 0.5).toInt()
            autoStartCountdownOverlay!!.animate().alpha(0f).setDuration(durationMs.toLong())
                .setListener(object : AbstractAnimatorListener() {
                    private var cancelled = false

                    override fun onAnimationCancel(animator: Animator) {
                        cancelled = true
                    }

                    override fun onAnimationEnd(animator: Animator) {
                        // prevent screen flickering
                        if (!cancelled) {
                            autoStartCountdownOverlay!!.visibility = View.GONE
                        }
                    }
                }).start()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNewInfo(e: AggregatedRecordingValuesEvent) {
        updateDescription(e)
    }

    private fun updateDescription(e: AggregatedRecordingValuesEvent) {
        if (isResumed2) {
            timeView!!.text = instance!!.distanceUnitUtils.getHourMinuteSecondTime(e.activeDuration)
            for (i in 0..3) {
                updateSlot(e, i)
            }
        }
    }

    private fun updateSlot(e: AggregatedRecordingValuesEvent?, slot: Int) {
        val data = informationDisplay.getDisplaySlot(e, slot)
        infoViews[slot]!!.setText(data.title, data.value)
    }

    private fun hideStartButton() {
        hide(recordStartButtonsRoot)
        timeView.visibility = View.VISIBLE
    }

    private fun showStartButton() {
        show(recordStartButtonsRoot)
        timeView.visibility = View.INVISIBLE
    }

    protected fun hide(view: View) {
        if (startPopupMenu != null
            && (view.id == recordStartButtonsRoot!!.id
                    || view.id == startPopupButton!!.id)
        ) {
            startPopupMenu!!.dismiss()
        }
        val cx = view.width / 2
        val cy = view.height / 2
        val initialRadius = hypot(cx.toDouble(), cy.toDouble()).toFloat()
        val anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, initialRadius, 0f)
        anim.duration = 500
        anim.interpolator = AccelerateInterpolator()
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                view.visibility = View.INVISIBLE
            }
        })

        anim.start()
    }

    protected fun show(view: View) {
        view.setAlpha(1f)
        if (view.visibility != View.VISIBLE) {
            view.clearAnimation()
            view.setAlpha(0f)
            view.animate().alpha(1f).setDuration(500).start()
        }
        view.visibility = View.VISIBLE
    }

    protected fun updateStartButton(
        enabled: Boolean,
        @StringRes text: Int,
        listener: View.OnClickListener?
    ) {
        showStartButton()
        startButton!!.setEnabled(enabled)
        startButton!!.setText(text)
        startButton!!.setOnClickListener(listener)
    }

    protected fun start(reason: StartReason) {
        // some nasty race conditions might occur between auto start and the user pressing the start
        // button, so better make sure we only start once
        // TODO is this really necessary or would the flag isStarted be enough
        if (startedSem.drainPermits() == 0) {   // consuming all permits just to be sure..
            Log.w(TAG, "Cannot start the workout, it has already been started.")
            return
        }

        // take care of auto start (hide stuff and/or abort it whatever's necessary)
        cancelAutoStart(false)

        if (autoStartDelayDialog != null) {
            autoStartDelayDialog!!.getDialog().cancel()
            autoStartDelayDialog = null
        }

        // show workout timer
        hideStartButton()

        // and start workout recorder
        EventBus.getDefault().post(WorkoutActionStart(reason))
        invalidateOptionsMenu()
    }

    protected fun stop(reason: StopReason) {
        // allow restarts after stopping
        // TODO is it save to do this right on entry and not on exit??
        startedSem.release()

        // cancel auto start if necessary
        cancelAutoStart(true)

        val current = getCurrent()
        if (current.currentState != RecordingState.IDLE) { // Only Running Records can be stopped
            EventBus.getDefault().post(WorkoutActionStop(reason))
            if (current.recordedSomething) {
                if (askForComment) {
                    showEnterDescriptionDialog()
                } else {
                    activityFinish()
                }
            } else {
                Toast.makeText(this, R.string.workoutDiscarded, Toast.LENGTH_LONG).show()
                EventBus.getDefault().post(WorkoutActionDiscard)
                activityFinish()
            }
        } else {
            activityFinish()
        }
    }

    private fun showEnterDescriptionDialog() {
        val editText = EditText(this)
        editText.setSingleLine(true)
        editText.setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        requestKeyboard(editText)
        AlertDialog.Builder(this).setTitle(R.string.enterComment).setPositiveButton(
            R.string.okay
        ) { dialog: DialogInterface?, which: Int ->
            dialog!!.dismiss()
            val workout = instance.db.workoutDao().getLastWorkout()!!
            instance.db.workoutDao().updateWorkout(workout.copy(
                comment = editText.text.toString()
            ))
            activityFinish()
        }.setView(editText)
            .setOnCancelListener { dialog: DialogInterface? -> activityFinish() }
            .create().show()
    }

    private fun showAreYouSureToStopDialog(stopReason: StopReason) {
        AlertDialog.Builder(this)
            .setTitle(R.string.stopRecordingQuestion)
            .setMessage(R.string.stopRecordingQuestionMessage)
            .setPositiveButton(R.string.stop) { _: DialogInterface?, _: Int -> stop(stopReason) }
            .setNegativeButton(R.string.continue_, null)
            .create().show()
    }

    protected val isServiceRunning: Boolean
        get() {
            val activityManager =
                this.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val services =
                activityManager.getRunningServices(Int.Companion.MAX_VALUE)

            for (runningServiceInfo in services) {
                if (runningServiceInfo.service.className == RecorderService::class.java.name) {
                    return true
                }
            }
            return false
        }

    protected fun restartService() {
        stopService()
        startService()
    }

    protected fun startService() {
        if (!this.isServiceRunning) {
            if (hasServicePermission()) {
                log(TAG, "Starting service")
                val locationListener = Intent(applicationContext, RecorderService::class.java)
                NotificationHelper.requestNotificationPermissionIfNecessary(this)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(locationListener)
                } else {
                    startService(locationListener)
                }
                onListenerStart()
            } else {
                Log.i(TAG, "Not starting service, no permission")
            }
        } else {
            Log.d(TAG, "Listener Already Running")
        }
    }

    protected fun stopService() {
        if (this.isServiceRunning) {
            log(TAG, "Stopping service")
            val locationListener = Intent(applicationContext, RecorderService::class.java)
            stopService(locationListener)
        }
    }

    protected abstract fun hasServicePermission(): Boolean

    protected abstract fun onListenerStart()

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onHeartRateConnectionChange(e: HeartRateConnectionChangeEvent) {
        hrStatusView.setImageResource(e.state.iconRes)
        hrStatusView.setColorFilter(getResources().getColor(e.state.colorRes))
    }

    override fun onDestroy() {
        log(TAG, "Activity onDestroy")
        // abort any ongoing auto start procedure
        cancelAutoStart(true)

        // once that's done, make sure no one stays registered to its event bus thereby creating
        // a stale process
        movementDetector!!.stop()
        movementDetector!!.unregisterFromBus()
        autoStartWorkout!!.unregisterFromBus()
        autoStartVibratorFeedback!!.unregisterFromBus()
        autoStartSoundFeedback!!.unregisterFromBus()
        autoStartAnnouncements!!.unregisterFromBus()

        // shutdown Text-to-Speech engine
        if (ttsController != null) {
            ttsController!!.destroyWhenDone()
        }

        if (autoStartDelayDialog != null) {
            autoStartDelayDialog!!.getDialog().cancel()
            autoStartDelayDialog = null
        }

        EventBus.getDefault().unregister(this)

        // Kill Service on Finished or not Started Recording
        val state = getCurrent().currentState
        if (state == RecordingState.STOPPED || state == RecordingState.IDLE) {
            log(TAG, "Recorder state is " + state + ", stopping recording")
            stop(StopReason.ACTIVITY_DESTROY)
            stopService()
            if (state == RecordingState.IDLE) {
                // Inform the user
                Toast.makeText(this, R.string.noWorkoutStarted, Toast.LENGTH_LONG).show()
            }
        }
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        log(TAG, "Activity onPause")

        // stop intercepting NFC intents
        if (useNfcStart && NfcAdapterHelper.isNfcEnabled(this)) {
            if (!NfcAdapterHelper.disableNfcForegroundDispatch(this)) {
                Log.w(
                    TAG, "onPause: Failed to disable NFC foreground dispatch system. " +
                            "NFC is not enabled or present in this device."
                )
            }
        }

        isResumed2 = false
    }

    override fun onResume() {
        super.onResume()
        log(TAG, "Activity onResume")
        finished = false
        if (instance!!.userPreferences.showOnLockScreen) {
            enableLockScreenVisibility()
        }
        invalidateOptionsMenu()
        isResumed2 = true
        updateDescription(getCurrent())

        // start intercepting NFC intents
        if (useNfcStart && NfcAdapterHelper.isNfcEnabled(this)) {
            if (!NfcAdapterHelper.enableNfcForegroundDispatch(this)) {
                Log.w(
                    TAG, "onPause: Failed to disable NFC foreground dispatch system. " +
                            "NFC is not enabled or present in this device."
                )
            }
        }

        // update countdown field if necessary
        if (autoStartWorkout != null
            && autoStartWorkout!!.state == AutoStartWorkout.State.COUNTDOWN
        ) {
            onCountdownChange(CountdownChangeEvent(autoStartWorkout!!.countdownMs))
        }
    }

    fun onStartPopupButtonClicked(v: View?) {
        // only show if not started yet
        // => disables this button for the short time of the hide animation, when a workout has just
        // started
        if (this.isRecordingStarted) {
            return
        }
        startPopupMenu = PopupMenu(this, v)
        startPopupMenu!!.setOnMenuItemClickListener { menuItem: MenuItem? ->
            val itemId = menuItem!!.itemId
            if (itemId == R.id.auto_start) {
                Log.d(TAG, "Auto start from popup menu selected")
                val config = autoStartWorkout!!.defaultStartConfig
                beginAutoStart(config.countdownMs, config.mode)
            } else if (itemId == R.id.auto_start_immediately) {
                start(StartReason.START_BUTTON_IMMEDIATE)
            } else if (itemId == R.id.auto_start_on_move) {
                beginAutoStart(0, AutoStartWorkout.Mode.ON_MOVE)
            } else if (itemId == R.id.auto_start_wait_for_gps) {
                beginAutoStart(0, AutoStartWorkout.Mode.WAIT_FOR_GPS)
            } else if (itemId == R.id.auto_start_delay) {
                Log.d(TAG, "Auto start with custom settings from popup menu selected")
                // show the reduced delay picker dialog first (then, if selected, the custom ine)
                // and lastly the mode picker dialog
                // they'll be preloaded with either the last selected or the default value from
                // preferences
                autoStartDelayDialog =
                    ChooseAutoStartDelayDialog(this, AutoStartDelaySelectListener { delayS: Int ->
                        autoStartDelayDialog = ChooseAutoStartModeDialog(
                            this,
                            AutoStartModeSelectListener { mode: AutoStartWorkout.Mode? ->
                                beginAutoStart(delayS * 1000L, mode)
                                Log.d(
                                    TAG, "Auto start from popup menu with delay of " + delayS +
                                            "s and mode " + mode
                                )
                            },
                            autoStartWorkout!!.lastStartConfig.mode
                        )
                        autoStartDelayDialog!!.show()
                    }, autoStartWorkout!!.lastStartConfig.countdownMs)
                autoStartDelayDialog!!.show()
            } else {
                return@setOnMenuItemClickListener false
            }
            true
        }
        startPopupMenu!!.inflate(R.menu.start_popup_menu)
        if (this !is RecordGpsWorkoutActivity) {
            val menu = startPopupMenu!!.menu
            menu.findItem(R.id.auto_start_immediately).isVisible = false
            menu.findItem(R.id.auto_start_on_move).isVisible = false
            menu.findItem(R.id.auto_start_wait_for_gps).isVisible = false
        }
        startPopupMenu!!.show()
    }

    fun onAutoStartCountdownAbortButtonClicked() {
        cancelAutoStart(true)
        Toast.makeText(this, R.string.workoutAutoStartAborted, Toast.LENGTH_SHORT).show()
    }

    /**
     * Start the auto start sequence if enabled in settings.
     *
     * @param delayMs the delay in milliseconds after which the workout should be started
     * @param mode    the auto start mode with which the workout should be started
     */
    /**
     * Start the auto start sequence in default mode.
     */
    @JvmOverloads
    fun beginAutoStart(delayMs: Long, mode: AutoStartWorkout.Mode? = null) {
        log(TAG, "Begin autostart after " + delayMs + "ms, mode: " + mode)
        // show the countdown overlay (at least, if we're actually counting down)
        if (autoStartCountdownOverlay == null) {
            autoStartCountdownOverlay = findViewById(R.id.recorderAutoStartOverlay)
        }
        val config = when {
            mode == null -> AutoStartWorkout.Config(delayMs)
            delayMs == Long.Companion.MIN_VALUE -> AutoStartWorkout.Config(mode)
            else -> AutoStartWorkout.Config(delayMs, mode)
        }
        EventBus.getDefault().post(BeginEvent(config))
    }

    /**
     * Start the auto start sequence with default delay.
     */
    fun beginAutoStart(mode: AutoStartWorkout.Mode?) {
        beginAutoStart(Long.Companion.MIN_VALUE, mode)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAutoStartWorkoutStateChange(stateChangeEvent: StateChangeEvent) {
        when (stateChangeEvent.newState) {
            AutoStartWorkout.State.COUNTDOWN -> showAutoStartCountdownOverlay()
            AutoStartWorkout.State.WAITING_FOR_GPS -> {
                (findViewById<View?>(R.id.autoStartCountdownVal) as TextView).text = ""
                (findViewById<View?>(R.id.autoStartCountdownMsg) as TextView).text = getString(R.string.autoStartCountdownMsgGps)
                showAutoStartCountdownOverlay()
            }

            AutoStartWorkout.State.WAITING_FOR_MOVE -> {
                (findViewById<View?>(R.id.autoStartCountdownVal) as TextView).text = ""
                (findViewById<View?>(R.id.autoStartCountdownMsg) as TextView).text = getString(R.string.autoStartCountdownMsgMove)
                showAutoStartCountdownOverlay()
            }

            AutoStartWorkout.State.AUTO_START_REQUESTED -> autoStart()
            AutoStartWorkout.State.ABORTED_BY_USER, AutoStartWorkout.State.ABORTED_ALREADY_STARTED -> hideAutoStartCountdownOverlay()
            else -> {}
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCountdownChange(countdownChangeEvent: CountdownChangeEvent) {
        (findViewById<View?>(R.id.autoStartCountdownMsg) as TextView).text = getString(R.string.autoStartCountdownMsg)
        val text: String?
        text = if (countdownChangeEvent.countdownS > 60) {
            // use countdownS because that is rounded properly already
            instance.distanceUnitUtils.getMinuteSecondTime(countdownChangeEvent.countdownS * 1000L)
        } else {
            String.format(
                getString(R.string.autoStartCountdownVal),
                countdownChangeEvent.countdownS, getText(R.string.timeSecondsShort)
            )
        }
        Log.d(
            TAG, "Updating auto start countdown: " + text + " (" +
                    countdownChangeEvent.countdownMs + ")"
        )
        (findViewById<View?>(R.id.autoStartCountdownVal) as TextView).text = text
    }

    override fun onNewIntent(intent: Intent) {
        // should have only gotten here when an NFC tag has been detected
        if (useNfcStart) {
            // above check should actually not be necessary, b/c NFC should only be enabled if we
            // want to use it. But it's cheep so let's keep it just to be sure..

            // let's see if the intent contains an NFC tag and start/stop recording if it does

            val tag = intent.getParcelableExtra<Tag?>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                if (this.isRecordingStarted) {
                    Log.i(TAG, "onNewIntent: NFC tag triggered workout end")
                    stop(StopReason.NFC)
                } else {
                    Log.i(TAG, "onNewIntent: NFC tag triggered workout start")
                    start(StartReason.NFC) // start immediately, don't care about signal quality or anything
                }
            }
        }
        super.onNewIntent(intent)
    }

    private fun enableLockScreenVisibility() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }

    private val isRestrictedInput: Boolean
        get() {
            val myKM =
                this.getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            return myKM.inKeyguardRestrictedInputMode() // return whether phone is in locked state
        }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.record_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionRecordingStop -> {
                onPressStopButton()
                return true
            }

            R.id.actionSelectIntervalSet -> {
                showIntervalSelection()
                return true
            }

            R.id.actionEditHint -> {
                showEditInformationHint()
                return true
            }

            R.id.actionConnectHR -> {
                chooseHRDevice()
                return true
            }

            R.id.actionManualPause -> {
                onManualPauseButtonClick()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onManualPauseButtonClick() {
        val state = getCurrent().currentState
        if (state == RecordingState.RUNNING || state == RecordingState.AUTO_PAUSE) {
            startPopupButton!!.visibility = View.GONE
            showStartButton()
            EventBus.getDefault().post(WorkoutActionPause(false))
            updateStartButton(
                true,
                R.string.actionResume,
                View.OnClickListener { v: View? -> onManualPauseButtonClick() })
        } else if (state == RecordingState.MANUAL_PAUSE) {
            EventBus.getDefault().post(WorkoutActionResume)
            hideStartButton()
        }
        invalidateOptionsMenu()
    }

    private fun showEditInformationHint() {
        AlertDialog.Builder(this)
            .setTitle(R.string.editDisplayedInformation)
            .setMessage(R.string.editDisplayedInformationHint)
            .setPositiveButton(R.string.okay, null)
            .show()
    }

    private fun chooseHRDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothPermissions()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                REQUEST_CODE_BLUETOOTH_PERMISSION
            )
            return
        }
        try {
            ChooseBluetoothDeviceDialog(this, this).show()
        } catch (ignored: BluetoothNotAvailableException) {
            askToActivateBluetooth()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private fun hasBluetoothPermissions(): Boolean {
        return checkPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                && checkPermission(this, Manifest.permission.BLUETOOTH_SCAN)
    }

    @SuppressLint("MissingPermission") // can be suppressed because method will only be called if the permission is granted
    private fun askToActivateBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_CODE_ENABLE_BLUETOOTH)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == REQUEST_CODE_ENABLE_BLUETOOTH || requestCode == REQUEST_CODE_BLUETOOTH_PERMISSION)
            && resultCode == RESULT_OK
        ) {
            chooseHRDevice()
        }
    }

    private fun onPressStopButton() {
        if (this.isRestrictedInput) {
            Toast.makeText(this, R.string.unlockPhoneStopWorkout, Toast.LENGTH_LONG).show()
        } else {
            showAreYouSureToStopDialog(StopReason.STOP_BUTTON_PRESSED)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val state = this.currentState
        val preparationPhase = state == RecordingState.IDLE
        menu.findItem(R.id.actionSelectIntervalSet).isVisible = preparationPhase && voiceFeedbackAvailable
        menu.findItem(R.id.actionEditHint).isVisible = preparationPhase
        menu.findItem(R.id.actionConnectHR).isVisible = this.isBluetoothSupported
        val pauseResumeItem = menu.findItem(R.id.actionManualPause)
        if (!instance.userPreferences.useAutoPause && !preparationPhase) {
            pauseResumeItem.isVisible = true
            pauseResumeItem.setIcon(if (state == RecordingState.RUNNING) R.drawable.ic_pause else R.drawable.ic_resume)
        } else {
            pauseResumeItem.isVisible = false
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onBackPressed() {
        cancelAutoStart(true)
        if (this.currentState != RecordingState.IDLE) {
            // Still Running Workout
            showAreYouSureToStopDialog(StopReason.BACK_BUTTON_PRESSED)
        } else {
            // Stopped or Idle Workout
            activityFinish()
            //super.onBackPressed();
        }
    }

    @Synchronized
    protected fun activityFinish() {
        if (!this.finished) {
            this.finished = true
            this.finish()
            val launcherIntent = Intent(this, LauncherActivity::class.java)
            launcherIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            this.startActivity(launcherIntent)
        }
    }

    private fun showIntervalSelection() {
        SelectIntervalSetDialog(this, this).show()
    }

    override fun onIntervalSetSelect(set: IntervalSet) {
        val intervals = instance.db.intervalDao().getAllIntervalsOfSet(set.id).toList()
        EventBus.getDefault().post(WorkoutActionSelectIntervalSet(set, intervals))
        Toast.makeText(this, R.string.intervalSetSelected, Toast.LENGTH_LONG).show()
    }

    @Subscribe
    fun onVoiceAnnouncementIsReady(e: TTSReadyEvent) {
        // actually, we only care for the RecorderService's TTS controller here
        if (e.id == AnnouncementComponent.TTS_CONTROLLER_ID) {
            this.voiceFeedbackAvailable = e.ttsAvailable
            invalidateOptionsMenu()
        }
    }

    override fun onInfoViewClick(slot: Int, isLongClick: Boolean) {
        if (this.currentState == RecordingState.IDLE || isLongClick) {
            SelectWorkoutInformationDialog(
                this,
                RecordingType.findById(activity!!.recordingType),
                slot,
                this
            )
                .show()
        }
    }

    @Subscribe
    fun onAutoStop(e: WorkoutAutoStopEvent?) {
        activityFinish()
    }

    override fun onSelectWorkoutInformation(slot: Int, information: RecordingInformation) {
        val mode = RecordingType.findById(activity!!.recordingType).id
        Instance.getInstance(this).userPreferences.setIdOfDisplayedInformation(
            mode,
            slot,
            information.id
        )
        this.updateDescription(getCurrent())
    }

    override fun onSelectBluetoothDevice(device: BluetoothDevice) {
        BluetoothDevicePreferences(this).setAddress(
            BluetoothDevicePreferences.DEVICE_HEART_RATE,
            device.getAddress()
        )
        restartService()
    }

    private val isBluetoothSupported: Boolean
        get() =// Check if device has a bluetooth adapter
            BluetoothAdapter.getDefaultAdapter() != null

    protected fun openSystemSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.setData(uri)
        startActivity(intent)
    }

    protected val isRecordingStarted: Boolean
        /**
         * Check whether recording has already been started yet
         */
        get() = startedSem.availablePermits() == 0

    protected val currentState: RecordingState
        get() = getCurrent().currentState

}
