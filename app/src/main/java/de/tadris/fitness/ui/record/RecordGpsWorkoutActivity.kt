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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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

        // Check location permission
        if (!vm.hasServicePermission()) {
            showLocationPermissionConsent()
        }

        // Observe dialog triggers that need legacy View-based dialogs
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
        // The controller is accessed through the composable; we post to the VM instead via state
        // For simplicity we let the composable handle this via a side-effect triggered by VM state
        // However the controller is held in the composable's remember block, so we use a companion
        // object to store the current controller reference.
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

    // Holds reference to the current map controller from the composable
    internal var currentMapController: RecordingMapController? = null
}

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

    // Give the activity a reference to the controller for volume key handling
    LaunchedEffect(mapController) {
        activity?.currentMapController = mapController
    }

    BackHandler {
        if (recordingState == RecordingState.IDLE) {
            vm.finish()
        } else {
            vm.requestStopConfirmation()
        }
    }

    LaunchedEffect(shouldFinish) {
        if (shouldFinish) {
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
            activity?.finish()
        }
    }

    if (showStopConfirmation) {
        StopConfirmDialog(
            onConfirm = {
                vm.dismissStopConfirmation()
                vm.stop(StopReason.STOP_BUTTON_PRESSED)
            },
            onDismiss = { vm.dismissStopConfirmation() }
        )
    }

    if (showCommentDialog) {
        CommentDialog(
            onConfirm = { comment -> vm.onCommentEntered(comment) },
            onDismiss = { vm.onCommentDismissed() }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map
        RecordingMapView(
            routePositions = routePositions,
            currentPosition = currentPosition,
            primaryColor = primaryColor,
            controller = mapController,
            modifier = Modifier.fillMaxSize(),
        )

        // Top bar
        Column(modifier = Modifier.fillMaxWidth()) {
            TopAppBar(
                title = { Text(stringResource(R.string.recordWorkout)) },
                actions = {
                    // Stop button (only when recording)
                    if (recordingState != RecordingState.IDLE) {
                        IconButton(onClick = { vm.requestStopConfirmation() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.stop))
                        }
                    }
                    // Interval set
                    if (recordingState == RecordingState.IDLE && voiceFeedbackAvailable) {
                        IconButton(onClick = { vm.requestIntervalSetDialog() }) {
                            Icon(
                                painterResource(R.drawable.ic_list),
                                contentDescription = stringResource(R.string.intervalSet)
                            )
                        }
                    }
                    // Edit hint
                    if (recordingState == RecordingState.IDLE) {
                        IconButton(onClick = { /* show edit hint */ }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.editDisplayedInformation))
                        }
                    }
                    // HR
                    IconButton(onClick = { vm.requestChooseBluetoothDevice() }) {
                        HrStatusIcon(hrConnectionState)
                    }
                    // Manual pause
                    if (!instance.userPreferences.useAutoPause && recordingState != RecordingState.IDLE) {
                        IconButton(onClick = { vm.toggleManualPause() }) {
                            Icon(
                                painterResource(
                                    if (recordingState == RecordingState.RUNNING)
                                        R.drawable.ic_pause
                                    else
                                        R.drawable.ic_resume
                                ),
                                contentDescription = null
                            )
                        }
                    }
                    // GPS status indicator (colored dot)
                    val gpsColor = Color(gpsState.color)
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(gpsColor, shape = CircleShape)
                            .padding(horizontal = 8.dp)
                    )
                }
            )
        }

        // GPS waiting overlay
        AnimatedVisibility(
            visible = gpsState == GpsState.SIGNAL_LOST && recordingState == RecordingState.IDLE,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painterResource(R.drawable.ic_gps_no_fix),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = stringResource(R.string.waiting_gps),
                        color = Color.White,
                    )
                    Text(
                        text = pluralStringResource(R.plurals.satelliteCount, satelliteCount, satelliteCount),
                        color = Color.White,
                    )
                }
            }
        }

        // AutoStart overlay
        AutoStartOverlay(
            autoStartState = autoStartUiState,
            distanceUnitUtils = instance.distanceUnitUtils,
            onAbort = {
                vm.cancelAutoStart(true)
            }
        )

        // Bottom panel
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (recordingState == RecordingState.IDLE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = { vm.start(StartReason.START_BUTTON) },
                            enabled = canStart,
                        ) {
                            Text(stringResource(R.string.start))
                        }

                        if (vm.isAutoStartSupported) {
                            Spacer(modifier = Modifier.width(8.dp))
                            var showPopup by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showPopup = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = showPopup,
                                    onDismissRequest = { showPopup = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.popup_auto_start_immediately)) },
                                        onClick = {
                                            showPopup = false
                                            vm.start(StartReason.START_BUTTON_IMMEDIATE)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.popup_auto_start_on_move)) },
                                        onClick = {
                                            showPopup = false
                                            vm.beginAutoStart(Long.MIN_VALUE, AutoStartWorkout.Mode.ON_MOVE)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.popup_auto_start_wait_for_gps)) },
                                        onClick = {
                                            showPopup = false
                                            vm.beginAutoStart(Long.MIN_VALUE, AutoStartWorkout.Mode.WAIT_FOR_GPS)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.popup_auto_start_delay)) },
                                        onClick = {
                                            showPopup = false
                                            vm.beginAutoStart(AutoStartWorkout.DEFAULT_DELAY_S * 1000L)
                                        }
                                    )
                                }
                            }
                        }
                    }
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

        // Map zoom controls
        if (showZoomControls) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FloatingActionButton(
                    onClick = { mapController.focusGps() },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_gps_fixed), contentDescription = null)
                }
                FloatingActionButton(
                    onClick = { mapController.zoomIn() },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_map_zoomin), contentDescription = "+")
                }
                FloatingActionButton(
                    onClick = { mapController.zoomOut() },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_map_zoomout), contentDescription = "-")
                }
            }
        }
    }
}
