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

package de.tadris.fitness.ui.record;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.data.Interval;
import de.tadris.fitness.data.IntervalSet;
import de.tadris.fitness.data.RecordingType;
import de.tadris.fitness.data.WorkoutType;
import de.tadris.fitness.data.WorkoutTypeManager;
import de.tadris.fitness.recording.autostart.AutoStartWorkout;
import de.tadris.fitness.recording.RecorderService;
import de.tadris.fitness.recording.announcement.TTSController;
import de.tadris.fitness.recording.autostart.AutoStartAnnouncements;
import de.tadris.fitness.recording.autostart.AutoStartSoundFeedback;
import de.tadris.fitness.recording.autostart.AutoStartVibratorFeedback;
import de.tadris.fitness.recording.autostart.DefaultMovementDetector;
import de.tadris.fitness.recording.autostart.MovementDetector;
import de.tadris.fitness.recording.component.AnnouncementComponent;
import de.tadris.fitness.recording.event.HeartRateConnectionChangeEvent;
import de.tadris.fitness.recording.event.TTSReadyEvent;
import de.tadris.fitness.recording.event.WorkoutAutoStopEvent;
import de.tadris.fitness.recording.information.InformationDisplay;
import de.tadris.fitness.recording.information.RecordingInformation;
import de.tadris.fitness.ui.FitoTrackActivity;
import de.tadris.fitness.ui.LauncherActivity;
import de.tadris.fitness.ui.dialog.AlertDialogWrapper;
import de.tadris.fitness.ui.dialog.ChooseAutoStartDelayDialog;
import de.tadris.fitness.ui.dialog.ChooseAutoStartModeDialog;
import de.tadris.fitness.ui.dialog.ChooseBluetoothDeviceDialog;
import de.tadris.fitness.ui.dialog.SelectIntervalSetDialog;
import de.tadris.fitness.ui.dialog.SelectWorkoutInformationDialog;
import de.tadris.fitness.util.BluetoothDevicePreferences;
import de.tadris.fitness.util.NfcAdapterHelper;
import de.tadris.fitness.util.NotificationHelper;
import de.tadris.fitness.util.PermissionUtils;
import de.tadris.fitness.util.ToneGeneratorController;
import de.tadris.fitness.util.VibratorController;
import de.tadris.fitness.util.WorkoutLogger;

public abstract class RecordWorkoutActivity extends FitoTrackActivity implements
        SelectIntervalSetDialog.IntervalSetSelectListener,
        InfoViewHolder.InfoViewClickListener, SelectWorkoutInformationDialog.WorkoutInformationSelectListener,
        ChooseBluetoothDeviceDialog.BluetoothDeviceSelectListener {

    public static final String TAG = "RecordWorkoutActivity";
    public static final String TTS_CONTROLLER_ID = TAG;

    public static final String LAUNCH_ACTION = "de.tadris.fitness.RecordWorkoutActivity.LAUNCH_ACTION";
    public static final String RESUME_ACTION = "de.tadris.fitness.RecordWorkoutActivity.RESUME_ACTION";
    public static final String WORKOUT_TYPE_EXTRA = "de.tadris.fitness.RecordWorkoutActivity.WORKOUT_TYPE_EXTRA";

    public static final int REQUEST_CODE_ENABLE_BLUETOOTH = 12;
    public static final int REQUEST_CODE_BLUETOOTH_PERMISSION = 13;

    // used to convert auto start time timebase from/to ms
    private static final int AUTO_START_DELAY_MULTIPLIER = 1_000; // s to ms

    private Button startPopupButton;
    private PopupMenu startPopupMenu = null;

    public WorkoutType activity;

    protected Instance instance;

    protected final InfoViewHolder[] infoViews = new InfoViewHolder[4];
    protected TextView timeView;
    protected ImageView hrStatusView;
    protected View waitingForGPSOverlay;
    protected Button startButton;
    protected ConstraintLayout recordStartButtonsRoot;
    protected boolean isResumed = false;
    protected final Handler mHandler = new Handler();
    protected InformationDisplay informationDisplay;

    private boolean useNfcStart;
    private long autoStartDelayMs;    // in ms
    private AutoStartWorkout.Mode autoStartMode;
    private View autoStartCountdownOverlay;
    private AlertDialogWrapper autoStartDelayDialog;
    private ChooseAutoStartModeDialog autoStartModeDialog;
    private MovementDetector movementDetector;
    private AutoStartWorkout autoStartWorkout;
    private boolean askForComment;
    private VibratorController vibratorController;
    private AutoStartVibratorFeedback autoStartVibratorFeedback;
    private ToneGeneratorController toneGeneratorController;
    private AutoStartSoundFeedback autoStartSoundFeedback;
    private AutoStartAnnouncements autoStartAnnouncements;
    private TTSController ttsController;

    private boolean voiceFeedbackAvailable = false;
    private Thread updater;
    private boolean finished;

    /**
     * This ensures that the workout is only started once. Different threads (user input, auto start)
     * might otherwise lead to nasty race conditions possibly starting a workout multiple times.
     */
    private final Semaphore startedSem = new Semaphore(1);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = Instance.getInstance(this);
        activity = WorkoutTypeManager.getInstance().getWorkoutTypeById(this, WorkoutTypeManager.WORKOUT_TYPE_ID_OTHER);

        // only use NFC when it's enabled in settings AND supported by the device
        this.useNfcStart = instance.userPreferences.getUseNfcStart() &&
                NfcAdapterHelper.isNfcPresent(this);
        Log.d(TAG, "NFC start enabled:" + this.useNfcStart);

        this.autoStartDelayMs = (long) instance.userPreferences.getAutoStartDelay() * AUTO_START_DELAY_MULTIPLIER;
        this.autoStartMode = instance.userPreferences.getAutoStartMode();
        Log.d(TAG, "auto start enabled, auto start delay: " +
                this.autoStartDelayMs + ", auto start mode: " + autoStartMode);
        this.askForComment = instance.userPreferences.getAskForComment();

        WorkoutLogger.log(TAG, "Activity created");
    }

    protected void initBeforeContent() {
        setTheme(instance.themes.getWorkoutTypeTheme(activity));
    }

    protected void initAfterContent() {
        if (useNfcStart) {
            // ask the user to enable NFC in device settings if it isn't at the moment
            if (!NfcAdapterHelper.isNfcEnabled(this)) {
                NfcAdapterHelper.createNfcEnableDialog(this).show();
            }
        }

        recordStartButtonsRoot = findViewById(R.id.recordStartButtonsRoot);
        timeView = findViewById(R.id.recordTime);
        timeView.setVisibility(View.INVISIBLE);
        hrStatusView = findViewById(R.id.recordHrStatus);
        startButton = findViewById(R.id.recordStart);

        startPopupButton = findViewById(R.id.recordStartPopup);

        // instantiate TTSController in app context to be able to completely play the auto start
        // abort announcement even when this activity has been destroyed already
        ttsController = new TTSController(getApplicationContext(), TTS_CONTROLLER_ID);
        movementDetector = new DefaultMovementDetector(this, instance.recorder.getWorkout());
        autoStartWorkout = new AutoStartWorkout(new AutoStartWorkout.Config(autoStartDelayMs,
                autoStartMode), movementDetector);
        movementDetector.registerTo(EventBus.getDefault());
        movementDetector.start();
        vibratorController = new VibratorController(this);
        autoStartVibratorFeedback = new AutoStartVibratorFeedback(vibratorController);
        toneGeneratorController = new ToneGeneratorController(this, AudioManager.STREAM_NOTIFICATION);
        autoStartSoundFeedback = new AutoStartSoundFeedback(toneGeneratorController, instance);
        autoStartVibratorFeedback.registerTo(EventBus.getDefault());
        autoStartSoundFeedback.registerTo(EventBus.getDefault());
        autoStartAnnouncements = new AutoStartAnnouncements(this, autoStartWorkout, instance, instance.recorder, ttsController);
        autoStartAnnouncements.registerTo(EventBus.getDefault());
        if (!autoStartWorkout.registerTo(EventBus.getDefault())) {
            Log.e(TAG, "onCreate: Failed to setup auto start helper, not using auto start");
            startPopupButton.setVisibility(View.GONE);
        } else {
            show(startPopupButton);
        }

        updateStartButton(false, R.string.cannotStart, null);

        informationDisplay = new InformationDisplay(RecordingType.findById(activity.recordingType), this);

        infoViews[0] = new InfoViewHolder(0, this, findViewById(R.id.recordInfo1Title), findViewById(R.id.recordInfo1Value));
        infoViews[1] = new InfoViewHolder(1, this, findViewById(R.id.recordInfo2Title), findViewById(R.id.recordInfo2Value));
        infoViews[2] = new InfoViewHolder(2, this, findViewById(R.id.recordInfo3Title), findViewById(R.id.recordInfo3Value));
        infoViews[3] = new InfoViewHolder(3, this, findViewById(R.id.recordInfo4Title), findViewById(R.id.recordInfo4Value));

        findViewById(R.id.autoStartCountdownAbort).setOnClickListener(v -> {
            onAutoStartCountdownAbortButtonClicked();
        });

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        startService();
    }

    /**
     * Cancel auto start
     */
    private void cancelAutoStart(boolean userReq) {
        // make sure auto start is cancelled
        EventBus.getDefault().post(new AutoStartWorkout.AbortEvent(userReq ?
                AutoStartWorkout.AbortEvent.Reason.USER_REQ :
                AutoStartWorkout.AbortEvent.Reason.STARTED));
    }

    private void autoStart() {
        WorkoutLogger.log(TAG, "Starting workout automatically");

        // start the workout
        start("Auto-Start");
        Toast.makeText(this, R.string.workoutAutoStarted, Toast.LENGTH_SHORT).show();
    }

    private void showAutoStartCountdownOverlay() {
        if (autoStartCountdownOverlay != null) {
            autoStartCountdownOverlay.clearAnimation();

            // if the view's not visible currently, we should start the animation from full transparency
            if (autoStartCountdownOverlay.getVisibility() != View.VISIBLE) {
                autoStartCountdownOverlay.setAlpha(0f);
            }
            // animation should take 1s max, if the view's not entirely hidden yet, it needs to be
            // proportionally shorter of course
            int durationMs = (int) ((1 - autoStartCountdownOverlay.getAlpha()) * 1000 + 0.5);

            // and finally start the animation
            autoStartCountdownOverlay.animate().alpha(1f).setDuration(durationMs).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                    // don't forget to make the view visible, needs to be done here in case hide
                    // animation is cancelled prematurely by show animation. There was a race
                    // condition when it was made visible again outside of the animation.
                    autoStartCountdownOverlay.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                }
            }).start();
        }
    }

    private void hideAutoStartCountdownOverlay() {
        if (autoStartCountdownOverlay != null &&
                autoStartCountdownOverlay.getVisibility() != View.GONE) {
            autoStartCountdownOverlay.clearAnimation();

            // animation should take 1s max, if the view's not entirely shown yet, it needs to be
            // proportionally shorter of course
            int durationMs = (int) (autoStartCountdownOverlay.getAlpha() * 1000 + 0.5);
            autoStartCountdownOverlay.animate().alpha(0f).setDuration(durationMs).setListener(new Animator.AnimatorListener() {
                private boolean cancelled = false;

                @Override
                public void onAnimationStart(Animator animator) {
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                    cancelled = true;
                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    // prevent screen flickering
                    if (!cancelled) {
                        autoStartCountdownOverlay.setVisibility(View.GONE);
                    }
                }
            }).start();
        }
    }

    private void startUpdater() {
        if (updater == null || !updater.isAlive()) {
            updater = new Thread(() -> {
                try {
                    while (instance.recorder.isActive()) {
                        Thread.sleep(1000);
                        mHandler.post(this::updateDescription);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        if (!updater.isAlive()) {
            updater.start();
        }
    }

    private void updateDescription() {
        if (isResumed) {
            timeView.setText(instance.distanceUnitUtils.getHourMinuteSecondTime(instance.recorder.getDuration()));
            for (int i = 0; i < 4; i++) {
                updateSlot(i);
            }
        }
    }

    private void updateSlot(int slot) {
        InformationDisplay.DisplaySlot data = informationDisplay.getDisplaySlot(instance.recorder, slot);
        infoViews[slot].setText(data.getTitle(), data.getValue());
    }

    private void hideStartButton() {
        hide(recordStartButtonsRoot);
        timeView.setVisibility(View.VISIBLE);
    }

    private void showStartButton() {
        show(recordStartButtonsRoot);
        timeView.setVisibility(View.INVISIBLE);
    }

    protected void hide(View view) {
        if (startPopupMenu != null
                && (view.getId() == recordStartButtonsRoot.getId()
                || view.getId() == startPopupButton.getId())) {
            startPopupMenu.dismiss();
        }
        int cx = view.getWidth() / 2;
        int cy = view.getHeight() / 2;
        float initialRadius = (float) Math.hypot(cx, cy);
        Animator anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, initialRadius, 0f);
        anim.setDuration(500);
        anim.setInterpolator(new AccelerateInterpolator());
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                view.setVisibility(View.INVISIBLE);
            }
        });

        anim.start();
    }

    protected void show(View view) {
        view.setAlpha(1);
        if (view.getVisibility() != View.VISIBLE) {
            view.clearAnimation();
            view.setAlpha(0);
            view.animate().alpha(1).setDuration(500).start();
        }
        view.setVisibility(View.VISIBLE);
    }

    protected void updateStartButton(boolean enabled, @StringRes int text, View.OnClickListener listener) {
        showStartButton();
        startButton.setEnabled(enabled);
        startButton.setText(text);
        startButton.setOnClickListener(listener);
    }

    protected void start(String reason) {
        // some nasty race conditions might occur between auto start and the user pressing the start
        // button, so better make sure we only start once
        // TODO is this really necessary or would the flag isStarted be enough
        if (startedSem.drainPermits() == 0) {   // consuming all permits just to be sure..
            Log.w(TAG, "Cannot start the workout, it has already been started.");
            return;
        }

        // take care of auto start (hide stuff and/or abort it whatever's necessary)
        cancelAutoStart(false);

        if (autoStartDelayDialog != null) {
            autoStartDelayDialog.getDialog().cancel();
            autoStartDelayDialog = null;
        }

        // show workout timer
        hideStartButton();

        // and start workout recorder
        instance.recorder.start(reason);
        invalidateOptionsMenu();
    }

    protected void stop(String reason) {
        // allow restarts after stopping
        // TODO is it save to do this right on entry and not on exit??
        startedSem.release();

        // cancel auto start if necessary
        cancelAutoStart(true);

        if (instance.recorder.getState() != GpsWorkoutRecorder.RecordingState.IDLE) { // Only Running Records can be stopped
            instance.recorder.stop(reason);
            if (instance.recorder.hasRecordedSomething()) {
                if (askForComment) {
                    showEnterDescriptionDialog();
                } else {
                    saveAndClose();
                }
            } else {
                Toast.makeText(this, R.string.workoutDiscarded, Toast.LENGTH_LONG).show();
                instance.recorder.discard();
                activityFinish();
            }
        } else {
            activityFinish();
        }
    }

    private void saveAndClose() {
        save();
        activityFinish();
    }

    private boolean save() {
        if (instance.recorder.getState() != GpsWorkoutRecorder.RecordingState.IDLE) {
            if (instance.recorder.hasRecordedSomething()) {
                instance.recorder.save();
                return true;
            } else {
                // Inform the user about not saving the workout
                Toast.makeText(this, R.string.workoutDiscarded, Toast.LENGTH_LONG).show();
                instance.recorder.discard();
                return false;
            }
        }
        // Only Started Workouts need to be discarded
        return false;
    }

    protected void saveIfNotSaved() {
        // ONLY SAVE WHEN WAS ONCE ACTIVE and Not Already Saved
        if (instance.recorder.getState() != GpsWorkoutRecorder.RecordingState.IDLE &&
                !instance.recorder.isSaved()) {
            save();
        }
    }

    private void showEnterDescriptionDialog() {
        final EditText editText = new EditText(this);
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editText.setText(instance.recorder.getWorkout().comment);
        requestKeyboard(editText);
        new AlertDialog.Builder(this).setTitle(R.string.enterComment).setPositiveButton(R.string.okay, (dialog, which) -> {
            dialog.dismiss();
            instance.recorder.setComment(editText.getText().toString());
            saveAndClose();
        }).setView(editText).setOnCancelListener(dialog -> saveAndClose()).create().show();
    }

    private void showAreYouSureToStopDialog(String reasonContext) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.stopRecordingQuestion)
                .setMessage(R.string.stopRecordingQuestionMessage)
                .setPositiveButton(R.string.stop, (dialog, which) -> stop(String.format("User requested: %s", reasonContext)))
                .setNegativeButton(R.string.continue_, null)
                .create().show();
    }

    protected boolean isServiceRunning() {
        final ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals(getServiceClass().getName())) {
                return true;
            }
        }
        return false;
    }

    protected void restartService() {
        stopService();
        startService();
    }

    protected void startService() {
        if (!isServiceRunning()) {
            if (hasServicePermission()) {
                WorkoutLogger.log(TAG, "Starting service");
                Intent locationListener = new Intent(getApplicationContext(), getServiceClass());
                NotificationHelper.requestNotificationPermissionIfNecessary(this);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(locationListener);
                } else {
                    startService(locationListener);
                }
                onListenerStart();
            } else {
                Log.i(TAG, "Not starting service, no permission");
            }
        } else {
            Log.d(TAG, "Listener Already Running");
        }
    }

    protected void stopService() {
        if (isServiceRunning()) {
            WorkoutLogger.log(TAG, "Stopping service");
            Intent locationListener = new Intent(getApplicationContext(), getServiceClass());
            stopService(locationListener);
        }
    }

    protected abstract Class<? extends RecorderService> getServiceClass();

    protected abstract boolean hasServicePermission();

    protected abstract void onListenerStart();

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onHeartRateConnectionChange(HeartRateConnectionChangeEvent e) {
        hrStatusView.setImageResource(e.state.getIconRes());
        hrStatusView.setColorFilter(getResources().getColor(e.state.getColorRes()));
    }

    @Override
    protected void onDestroy() {
        WorkoutLogger.log(TAG, "Activity onDestroy");
        // abort any ongoing auto start procedure
        cancelAutoStart(true);

        // once that's done, make sure no one stays registered to its event bus thereby creating
        // a stale process
        movementDetector.stop();
        movementDetector.unregisterFromBus();
        autoStartWorkout.unregisterFromBus();
        autoStartVibratorFeedback.unregisterFromBus();
        autoStartSoundFeedback.unregisterFromBus();
        autoStartAnnouncements.unregisterFromBus();

        // shutdown Text-to-Speech engine
        if (ttsController != null) {
            ttsController.destroyWhenDone();
        }

        if (autoStartDelayDialog != null) {
            autoStartDelayDialog.getDialog().cancel();
            autoStartDelayDialog = null;
        }

        EventBus.getDefault().unregister(this);

        // Kill Service on Finished or not Started Recording
        if (instance.recorder.getState() == GpsWorkoutRecorder.RecordingState.STOPPED ||
                instance.recorder.getState() == GpsWorkoutRecorder.RecordingState.IDLE) {
            WorkoutLogger.log(TAG, "Recorder state is " + instance.recorder.getState() + ", stopping recording");
            //ONLY SAVE WHEN STOPPED
            saveIfNotSaved();
            stopService();
            if (instance.recorder.getState() == GpsWorkoutRecorder.RecordingState.IDLE) {
                // Inform the user
                Toast.makeText(this, R.string.noWorkoutStarted, Toast.LENGTH_LONG).show();
            }
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        WorkoutLogger.log(TAG, "Activity onPause");

        // stop intercepting NFC intents
        if (useNfcStart && NfcAdapterHelper.isNfcEnabled(this)) {
            if (!NfcAdapterHelper.disableNfcForegroundDispatch(this)) {
                Log.w(TAG, "onPause: Failed to disable NFC foreground dispatch system. " +
                        "NFC is not enabled or present in this device.");
            }
        }

        isResumed = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        WorkoutLogger.log(TAG, "Activity onResume");
        finished = false;
        if (instance.userPreferences.getShowOnLockScreen()) {
            enableLockScreenVisibility();
        }
        invalidateOptionsMenu();
        isResumed = true;
        updateDescription();
        startUpdater();

        // start intercepting NFC intents
        if (useNfcStart && NfcAdapterHelper.isNfcEnabled(this)) {
            if (!NfcAdapterHelper.enableNfcForegroundDispatch(this)) {
                Log.w(TAG, "onPause: Failed to disable NFC foreground dispatch system. " +
                        "NFC is not enabled or present in this device.");
            }
        }

        // update countdown field if necessary
        if (autoStartWorkout != null
                && autoStartWorkout.getState() == AutoStartWorkout.State.COUNTDOWN) {
            onCountdownChange(new AutoStartWorkout.CountdownChangeEvent(autoStartWorkout.getCountdownMs()));
        }
    }

    public void onStartPopupButtonClicked(View v) {
        // only show if not started yet
        // => disables this button for the short time of the hide animation, when a workout has just
        // started
        if (isRecordingStarted()) {
            return;
        }
        startPopupMenu = new PopupMenu(this, v);
        startPopupMenu.setOnMenuItemClickListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.auto_start) {
                Log.d(TAG, "Auto start from popup menu selected");
                AutoStartWorkout.Config config = autoStartWorkout.getDefaultStartConfig();
                beginAutoStart(config.countdownMs, config.mode);
            } else if (itemId == R.id.auto_start_immediately) {
                start("Immediate Start-Button pressed");
            } else if (itemId == R.id.auto_start_on_move) {
                beginAutoStart(0, AutoStartWorkout.Mode.ON_MOVE);
            } else if (itemId == R.id.auto_start_wait_for_gps) {
                beginAutoStart(0, AutoStartWorkout.Mode.WAIT_FOR_GPS);
            } else if (itemId == R.id.auto_start_delay) {
                Log.d(TAG, "Auto start with custom settings from popup menu selected");
                // show the reduced delay picker dialog first (then, if selected, the custom ine)
                // and lastly the mode picker dialog
                // they'll be preloaded with either the last selected or the default value from
                // preferences
                autoStartDelayDialog = new ChooseAutoStartDelayDialog(this, delayS -> {
                    autoStartDelayDialog = new ChooseAutoStartModeDialog(this, mode -> {
                        beginAutoStart(delayS * 1_000L, mode);
                        Log.d(TAG, "Auto start from popup menu with delay of " + delayS +
                                "s and mode " + mode);
                    }, autoStartWorkout.getLastStartConfig().mode);
                    autoStartDelayDialog.show();
                }, autoStartWorkout.getLastStartConfig().countdownMs);
                autoStartDelayDialog.show();
            } else {
                return false;
            }
            return true;
        });
        startPopupMenu.inflate(R.menu.start_popup_menu);
        if (!(this instanceof RecordGpsWorkoutActivity)) {
            Menu menu = startPopupMenu.getMenu();
            menu.findItem(R.id.auto_start_immediately).setVisible(false);
            menu.findItem(R.id.auto_start_on_move).setVisible(false);
            menu.findItem(R.id.auto_start_wait_for_gps).setVisible(false);
        }
        startPopupMenu.show();
    }

    public void onAutoStartCountdownAbortButtonClicked() {
        cancelAutoStart(true);
        Toast.makeText(this, R.string.workoutAutoStartAborted, Toast.LENGTH_SHORT).show();
    }

    /**
     * Start the auto start sequence if enabled in settings.
     *
     * @param delayMs the delay in milliseconds after which the workout should be started
     * @param mode    the auto start mode with which the workout should be started
     */
    public void beginAutoStart(long delayMs, @Nullable AutoStartWorkout.Mode mode) {
        WorkoutLogger.log(TAG, "Begin autostart after " + delayMs + "ms, mode: " + mode);
        // show the countdown overlay (at least, if we're actually counting down)
        if (autoStartCountdownOverlay == null) {
            autoStartCountdownOverlay = findViewById(R.id.recorderAutoStartOverlay);
        }
        AutoStartWorkout.Config config;
        if (mode == null) {
            config = new AutoStartWorkout.Config(delayMs);
        } else if (delayMs == Long.MIN_VALUE) {
            config = new AutoStartWorkout.Config(mode);
        } else {
            config = new AutoStartWorkout.Config(delayMs, mode);
        }
        EventBus.getDefault().post(new AutoStartWorkout.BeginEvent(config));
    }

    /**
     * Start the auto start sequence in default mode.
     */
    public void beginAutoStart(long delayMs) {
        beginAutoStart(delayMs, null);
    }

    /**
     * Start the auto start sequence with default delay.
     */
    public void beginAutoStart(AutoStartWorkout.Mode mode) {
        beginAutoStart(Long.MIN_VALUE, mode);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAutoStartWorkoutStateChange(AutoStartWorkout.StateChangeEvent stateChangeEvent) {
        switch (stateChangeEvent.newState) {
            case COUNTDOWN:
                showAutoStartCountdownOverlay();
                break;
            case WAITING_FOR_GPS:
                ((TextView) findViewById(R.id.autoStartCountdownVal)).setText("");
                ((TextView) findViewById(R.id.autoStartCountdownMsg)).setText(getString(R.string.autoStartCountdownMsgGps));
                showAutoStartCountdownOverlay();
                break;
            case WAITING_FOR_MOVE:
                ((TextView) findViewById(R.id.autoStartCountdownVal)).setText("");
                ((TextView) findViewById(R.id.autoStartCountdownMsg)).setText(getString(R.string.autoStartCountdownMsgMove));
                showAutoStartCountdownOverlay();
                break;
            case AUTO_START_REQUESTED:
                autoStart();
                break;
            case ABORTED_BY_USER:
            case ABORTED_ALREADY_STARTED:
                hideAutoStartCountdownOverlay();
                break;
            default:
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCountdownChange(AutoStartWorkout.CountdownChangeEvent countdownChangeEvent) {
        ((TextView) findViewById(R.id.autoStartCountdownMsg)).setText(getString(R.string.autoStartCountdownMsg));
        String text;
        if (countdownChangeEvent.countdownS > 60) {
            // use countdownS because that is rounded properly already
            text = instance.distanceUnitUtils.getMinuteSecondTime(countdownChangeEvent.countdownS * 1_000);
        } else {
            text = String.format(getString(R.string.autoStartCountdownVal),
                    countdownChangeEvent.countdownS, getText(R.string.timeSecondsShort));
        }
        Log.d(TAG, "Updating auto start countdown: " + text + " (" +
                countdownChangeEvent.countdownMs + ")");
        ((TextView) findViewById(R.id.autoStartCountdownVal)).setText(text);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // should have only gotten here when an NFC tag has been detected
        if (useNfcStart) {
            // above check should actually not be necessary, b/c NFC should only be enabled if we
            // want to use it. But it's cheep so let's keep it just to be sure..

            // let's see if the intent contains an NFC tag and start/stop recording if it does
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                if (isRecordingStarted()) {
                    Log.i(TAG, "onNewIntent: NFC tag triggered workout end");
                    stop("NFC-Tag triggered end");
                } else {
                    Log.i(TAG, "onNewIntent: NFC tag triggered workout start");
                    start("NFC-Tag triggered start"); // start immediately, don't care about signal quality or anything
                }
            }
        }
        super.onNewIntent(intent);
    }

    private void enableLockScreenVisibility() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    private boolean isRestrictedInput() {
        KeyguardManager myKM = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
        return myKM.inKeyguardRestrictedInputMode(); // return whether phone is in locked state
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.record_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionRecordingStop:
                onPressStopButton();
                return true;
            case R.id.actionSelectIntervalSet:
                showIntervalSelection();
                return true;
            case R.id.actionEditHint:
                showEditInformationHint();
                return true;
            case R.id.actionConnectHR:
                chooseHRDevice();
                return true;
            case R.id.actionManualPause:
                onManualPauseButtonClick();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onManualPauseButtonClick() {
        if (instance.recorder.isResumed()) {
            startPopupButton.setVisibility(View.GONE);
            showStartButton();
            instance.recorder.pause();
            updateStartButton(true, R.string.actionResume, v -> {
                if (instance.recorder.isPaused()) {
                    onManualPauseButtonClick();
                }
            });
        } else if (instance.recorder.isPaused()) {
            instance.recorder.resume();
            hideStartButton();
        }
        invalidateOptionsMenu();
    }

    private void showEditInformationHint() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.editDisplayedInformation)
                .setMessage(R.string.editDisplayedInformationHint)
                .setPositiveButton(R.string.okay, null)
                .show();
    }

    private void chooseHRDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothPermissions()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, REQUEST_CODE_BLUETOOTH_PERMISSION);
            return;
        }
        try {
            new ChooseBluetoothDeviceDialog(this, this).show();
        } catch (ChooseBluetoothDeviceDialog.BluetoothNotAvailableException ignored) {
            askToActivateBluetooth();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private boolean hasBluetoothPermissions() {
        return PermissionUtils.checkPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                && PermissionUtils.checkPermission(this, Manifest.permission.BLUETOOTH_SCAN);
    }

    @SuppressLint("MissingPermission")
    // can be suppressed because method will only be called if the permission is granted
    private void askToActivateBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_CODE_ENABLE_BLUETOOTH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_CODE_ENABLE_BLUETOOTH || requestCode == REQUEST_CODE_BLUETOOTH_PERMISSION)
                && resultCode == RESULT_OK) {
            chooseHRDevice();
        }
    }

    private void onPressStopButton() {
        if (isRestrictedInput()) {
            Toast.makeText(this, R.string.unlockPhoneStopWorkout, Toast.LENGTH_LONG).show();
        } else {
            showAreYouSureToStopDialog("Stop button pressed");
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean preparationPhase = instance.recorder.getState() == GpsWorkoutRecorder.RecordingState.IDLE;
        menu.findItem(R.id.actionSelectIntervalSet).setVisible(preparationPhase && voiceFeedbackAvailable);
        menu.findItem(R.id.actionEditHint).setVisible(preparationPhase);
        menu.findItem(R.id.actionConnectHR).setVisible(isBluetoothSupported());
        MenuItem pauseResumeItem = menu.findItem(R.id.actionManualPause);
        if (!instance.recorder.isAutoPauseEnabled() && instance.recorder.isActive() && !preparationPhase) {
            pauseResumeItem.setVisible(true);
            pauseResumeItem.setIcon(instance.recorder.isResumed() ? R.drawable.ic_pause : R.drawable.ic_resume);
        } else {
            pauseResumeItem.setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        cancelAutoStart(true);
        if (instance.recorder.isActive() && instance.recorder.getState() != GpsWorkoutRecorder.RecordingState.IDLE) {
            // Still Running Workout
            showAreYouSureToStopDialog("Back button clicked");
        } else {
            // Stopped or Idle Workout
            activityFinish();
            //super.onBackPressed();
        }
    }

    protected synchronized void activityFinish() {
        if (!this.finished) {
            this.finished = true;
            this.finish();
            Intent launcherIntent = new Intent(this, LauncherActivity.class);
            launcherIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            this.startActivity(launcherIntent);
        }
    }

    private void showIntervalSelection() {
        new SelectIntervalSetDialog(this, this).show();
    }

    @Override
    public void onIntervalSetSelect(IntervalSet set) {
        Interval[] intervals = instance.db.intervalDao().getAllIntervalsOfSet(set.id);
        List<Interval> intervalList = new ArrayList<>(Arrays.asList(intervals));
        instance.recorder.setIntervalList(intervalList);
        instance.recorder.setUsedIntervalSet(set);
        Toast.makeText(this, R.string.intervalSetSelected, Toast.LENGTH_LONG).show();
    }

    @Subscribe
    public void onVoiceAnnouncementIsReady(TTSReadyEvent e) {
        // actually, we only care for the RecorderService's TTS controller here
        if (e.id.equals(AnnouncementComponent.TTS_CONTROLLER_ID)) {
            this.voiceFeedbackAvailable = e.ttsAvailable;
            invalidateOptionsMenu();
        }
    }

    @Override
    public void onInfoViewClick(int slot, boolean isLongClick) {
        if (instance.recorder.getState() == GpsWorkoutRecorder.RecordingState.IDLE || isLongClick) {
            new SelectWorkoutInformationDialog(
                    this,
                    RecordingType.findById(activity.recordingType),
                    slot,
                    this)
                    .show();
        }
    }

    @Subscribe
    public void onAutoStop(WorkoutAutoStopEvent e) {
        activityFinish();
    }

    @Override
    public void onSelectWorkoutInformation(int slot, RecordingInformation information) {
        String mode = RecordingType.findById(activity.recordingType).id;
        Instance.getInstance(this).userPreferences.setIdOfDisplayedInformation(mode, slot, information.getId());
        this.updateDescription();
    }

    @Override
    public void onSelectBluetoothDevice(BluetoothDevice device) {
        new BluetoothDevicePreferences(this).setAddress(BluetoothDevicePreferences.DEVICE_HEART_RATE, device.getAddress());
        restartService();
    }

    private boolean isBluetoothSupported() {
        // Check if device has a bluetooth adapter
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    protected void openSystemSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    /**
     * Check whether recording has already been started yet
     */
    protected boolean isRecordingStarted() {
        return startedSem.availablePermits() == 0;
    }

}
