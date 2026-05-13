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

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.RecordingType
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.data.WorkoutTypeManager
import de.tadris.fitness.recording.GpsState
import de.tadris.fitness.recording.RecordingState
import de.tadris.fitness.recording.StartReason
import de.tadris.fitness.recording.StopReason
import de.tadris.fitness.recording.autostart.AutoStartWorkout
import de.tadris.fitness.ui.MainActivity
import de.tadris.fitness.ui.dialog.ChooseBluetoothDeviceDialog
import de.tadris.fitness.ui.dialog.SelectIntervalSetDialog
import de.tadris.fitness.ui.dialog.SelectWorkoutInformationDialog
import de.tadris.fitness.ui.theme.FitoTrackTheme
import de.tadris.fitness.util.NotificationHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RecordGpsWorkoutActivity : ComponentActivity() {

    companion object {
        const val REQUEST_CODE_LOCATION_PERMISSION = 10
        private const val REQUEST_CODE_BLUETOOTH_PERMISSION = 13
    }

    private val vm: GpsRecordingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val workoutType = determineWorkoutType()
        val isLaunchAction = intent.action == RecordingConstants.LAUNCH_ACTION

        vm.init(workoutType, isLaunchAction)

        val instance = Instance.getInstance(this)
        if (instance.userPreferences.showOnLockScreen) {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        NotificationHelper.requestNotificationPermissionIfNecessary(this)

        if (!vm.hasServicePermission()) {
            showLocationPermissionConsent()
        }

        lifecycleScope.launch {
            vm.showIntervalSetDialog.collectLatest { show ->
                if (show) {
                    vm.dismissIntervalSetDialog()
                    SelectIntervalSetDialog(this@RecordGpsWorkoutActivity) { set ->
                        vm.onIntervalSetSelected(set)
                    }.show()
                }
            }
        }

        lifecycleScope.launch {
            vm.selectInfoDialogSlot.collectLatest { slot ->
                if (slot != null) {
                    val recordingType = RecordingType.findById(workoutType.recordingType)
                    SelectWorkoutInformationDialog(
                        this@RecordGpsWorkoutActivity,
                        recordingType,
                        slot
                    ) { s, info ->
                        vm.onInfoSlotSelected(s, info)
                    }.show()
                }
            }
        }

        lifecycleScope.launch {
            vm.showChooseBluetoothDevice.collectLatest { show ->
                if (show) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothPermissions()) {
                        ActivityCompat.requestPermissions(
                            this@RecordGpsWorkoutActivity,
                            arrayOf(
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                            ),
                            REQUEST_CODE_BLUETOOTH_PERMISSION
                        )
                    } else {
                        try {
                            ChooseBluetoothDeviceDialog(this@RecordGpsWorkoutActivity) { device ->
                                vm.onBtDeviceSelected(device)
                            }.show()
                        } catch (_: ChooseBluetoothDeviceDialog.BluetoothNotAvailableException) {
                            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            startActivityForResult(enableBtIntent, 12)
                        }
                    }
                    vm.dismissChooseBluetoothDevice()
                }
            }
        }

        lifecycleScope.launch {
            vm.showNoGpsDialog.collectLatest { show ->
                if (show) {
                    AlertDialog.Builder(this@RecordGpsWorkoutActivity)
                        .setTitle(R.string.noGpsTitle)
                        .setMessage(R.string.noGpsMessage)
                        .setNegativeButton(R.string.cancel) { _, _ ->
                            vm.dismissNoGpsDialog()
                            vm.finish()
                        }
                        .setPositiveButton(R.string.enable) { _, _ ->
                            vm.dismissNoGpsDialog()
                            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        }
                        .setCancelable(false)
                        .create().show()
                }
            }
        }

        setContent {
            FitoTrackTheme {
                GpsRecordingScreen(vm = vm)
            }
        }
    }

    private fun determineWorkoutType(): WorkoutType {
        val workoutType = intent.getSerializableExtra(RecordingConstants.WORKOUT_TYPE_EXTRA)
        return if (workoutType is WorkoutType) {
            workoutType
        } else {
            val workoutTypeId = intent.getStringExtra(RecordingConstants.WORKOUT_TYPE_EXTRA)
            WorkoutTypeManager.getInstance().getWorkoutTypeById(this, workoutTypeId)
        }
    }

    private fun showLocationPermissionConsent() {
        AlertDialog.Builder(this)
            .setTitle(R.string.recordingPermissionNotGrantedTitle)
            .setMessage(R.string.recordingPermissionNotGrantedMessage)
            .setPositiveButton(R.string.actionGrant) { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    REQUEST_CODE_LOCATION_PERMISSION
                )
            }
            .setNegativeButton(R.string.cancel) { _, _ -> vm.finish() }
            .setOnCancelListener { vm.finish() }
            .show()
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val instance = Instance.getInstance(this)
        if (!instance.userPreferences.zoomWithVolumeButtons) {
            return super.onKeyDown(keyCode, event)
        }
        val controller = currentMapController
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && controller != null) {
            controller.zoomIn()
            return true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && controller != null) {
            controller.zoomOut()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            val granted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                vm.restartService()
            }
        }
    }

    internal var currentMapController: RecordingMapController? = null
}

// ---- Screen ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsRecordingScreen(vm: GpsRecordingViewModel) {
    val context = LocalContext.current
    val activity = context as? RecordGpsWorkoutActivity

    val recordingState by vm.recordingState.collectAsState()
    val values by vm.values.collectAsState()
    val hrConnectionState by vm.hrConnectionState.collectAsState()
    val voiceFeedbackAvailable by vm.voiceFeedbackAvailable.collectAsState()
    val autoStartUiState by vm.autoStartUiState.collectAsState()
    val infoSlots by vm.infoSlots.collectAsState()
    val shouldFinish by vm.shouldFinish.collectAsState()
    val showCommentDialog by vm.showCommentDialog.collectAsState()
    val showStopConfirmation by vm.showStopConfirmation.collectAsState()
    val gpsState by vm.gpsState.collectAsState()
    val satelliteCount by vm.satelliteCount.collectAsState()
    val routePositions by vm.routePositions.collectAsState()
    val currentPosition by vm.currentPosition.collectAsState()
    val canStart by vm.canStart.collectAsState()

    val instance = remember { Instance.getInstance(context) }
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val showZoomControls = remember { instance.userPreferences.showWorkoutZoomControls }

    val mapController = remember { RecordingMapController() }
    var showCustomDelayDialog by remember { mutableStateOf(false) }

    LaunchedEffect(mapController) {
        activity?.currentMapController = mapController
    }

    BackHandler {
        if (recordingState == RecordingState.IDLE) vm.finish()
        else vm.requestStopConfirmation()
    }

    LaunchedEffect(shouldFinish) {
        if (shouldFinish) {
            context.startActivity(Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
            activity?.finish()
        }
    }

    if (showStopConfirmation) {
        StopConfirmDialog(
            onConfirm = { vm.dismissStopConfirmation(); vm.stop(StopReason.STOP_BUTTON_PRESSED) },
            onDismiss = { vm.dismissStopConfirmation() }
        )
    }
    if (showCommentDialog) {
        CommentDialog(
            onConfirm = { comment -> vm.onCommentEntered(comment) },
            onDismiss = { vm.onCommentDismissed() }
        )
    }
    if (showCustomDelayDialog) {
        CustomDelayDialog(
            onConfirm = { delayMs -> showCustomDelayDialog = false; vm.beginAutoStart(delayMs) },
            onDismiss = { showCustomDelayDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GpsRecordingTopBar(
            recordingState = recordingState,
            hrConnectionState = hrConnectionState,
            voiceFeedbackAvailable = voiceFeedbackAvailable,
            gpsState = gpsState,
            useAutoPause = instance.userPreferences.useAutoPause,
            onStop = { vm.requestStopConfirmation() },
            onIntervalSet = { vm.requestIntervalSetDialog() },
            onChooseBluetooth = { vm.requestChooseBluetoothDevice() },
            onTogglePause = { vm.toggleManualPause() },
        )

        Box(modifier = Modifier.weight(1f)) {
            RecordingMapView(
                routePositions = routePositions,
                currentPosition = currentPosition,
                primaryColor = primaryColor,
                controller = mapController,
                modifier = Modifier.fillMaxSize(),
            )

            GpsWaitingOverlay(
                gpsState = gpsState,
                recordingState = recordingState,
                satelliteCount = satelliteCount,
            )

            AutoStartOverlay(
                autoStartState = autoStartUiState,
                distanceUnitUtils = instance.distanceUnitUtils,
                onAbort = { vm.cancelAutoStart(true) }
            )

            Surface(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (recordingState == RecordingState.IDLE) {
                        StartButtonRow(
                            canStart = canStart,
                            isAutoStartSupported = vm.isAutoStartSupported,
                            onStart = { vm.start(StartReason.START_BUTTON) },
                            onStartImmediate = { vm.start(StartReason.START_BUTTON_IMMEDIATE) },
                            onStartOnMove = { vm.beginAutoStart(0L, AutoStartWorkout.Mode.ON_MOVE) },
                            onStartWaitForGps = { vm.beginAutoStart(0L, AutoStartWorkout.Mode.WAIT_FOR_GPS) },
                            onStartWithDelay = { showCustomDelayDialog = true },
                        )
                    } else {
                        RecordingTimeDisplay(
                            activeDuration = values.activeDuration,
                            distanceUnitUtils = instance.distanceUnitUtils,
                        )
                    }
                    InfoSlotsGrid(
                        slots = infoSlots,
                        recordingState = recordingState,
                        onSlotClick = { slot, isLongClick ->
                            if (recordingState == RecordingState.IDLE || isLongClick) {
                                vm.requestSelectInfoDialog(slot)
                            }
                        }
                    )
                }
            }

            if (showZoomControls) {
                MapZoomControls(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
                    onFocusGps = { mapController.focusGps() },
                    onZoomIn = { mapController.zoomIn() },
                    onZoomOut = { mapController.zoomOut() },
                )
            }
        }
    }
}

