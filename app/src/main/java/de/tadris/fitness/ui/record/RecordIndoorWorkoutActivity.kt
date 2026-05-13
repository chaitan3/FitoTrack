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
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.RecordingType
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.data.WorkoutTypeManager
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
import de.tadris.fitness.util.unit.UnitUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RecordIndoorWorkoutActivity : ComponentActivity() {

    companion object {
        const val REQUEST_CODE_ACTIVITY_PERMISSION = 12
    }

    private val vm: IndoorRecordingViewModel by viewModels()

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasActivityPermission()) {
            showActivityPermissionConsent()
        }

        lifecycleScope.launch {
            vm.showIntervalSetDialog.collectLatest { show ->
                if (show) {
                    vm.dismissIntervalSetDialog()
                    SelectIntervalSetDialog(this@RecordIndoorWorkoutActivity) { set ->
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
                        this@RecordIndoorWorkoutActivity,
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
                    try {
                        ChooseBluetoothDeviceDialog(this@RecordIndoorWorkoutActivity) { device ->
                            vm.onBtDeviceSelected(device)
                        }.show()
                    } catch (_: ChooseBluetoothDeviceDialog.BluetoothNotAvailableException) {
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        startActivityForResult(enableBtIntent, 13)
                    }
                    vm.dismissChooseBluetoothDevice()
                }
            }
        }

        setContent {
            FitoTrackTheme {
                IndoorRecordingScreen(vm = vm, workoutType = workoutType)
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

    private fun hasActivityPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun showActivityPermissionConsent() {
        AlertDialog.Builder(this)
            .setTitle(R.string.recordingPermissionNotGrantedTitle)
            .setMessage(R.string.recordingActivityPermissionMessage)
            .setPositiveButton(R.string.actionGrant) { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                        REQUEST_CODE_ACTIVITY_PERMISSION
                    )
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> vm.finish() }
            .setOnCancelListener { vm.finish() }
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_ACTIVITY_PERMISSION) {
            if (hasActivityPermission()) {
                vm.restartService()
            } else {
                AlertDialog.Builder(this)
                    .setTitle(R.string.recordingPermissionNotGrantedTitle)
                    .setMessage(R.string.recordingActivityPermissionMessage)
                    .setPositiveButton(R.string.settings) { _, _ ->
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = android.net.Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }
                    .create().show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndoorRecordingScreen(vm: IndoorRecordingViewModel, workoutType: WorkoutType) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    val recordingState by vm.recordingState.collectAsState()
    val values by vm.values.collectAsState()
    val hrConnectionState by vm.hrConnectionState.collectAsState()
    val voiceFeedbackAvailable by vm.voiceFeedbackAvailable.collectAsState()
    val autoStartUiState by vm.autoStartUiState.collectAsState()
    val infoSlots by vm.infoSlots.collectAsState()
    val shouldFinish by vm.shouldFinish.collectAsState()
    val showCommentDialog by vm.showCommentDialog.collectAsState()
    val showStopConfirmation by vm.showStopConfirmation.collectAsState()
    val chartEntries by vm.chartEntries.collectAsState()
    val repetitions by vm.repetitions.collectAsState()

    val instance = remember { Instance.getInstance(context) }
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground.toArgb()
    val frequencyColor = androidx.compose.ui.res.colorResource(R.color.diagramFrequency).toArgb()
    val intensityColor = androidx.compose.ui.res.colorResource(R.color.diagramIntensity).toArgb()
    val frequencyLabel = stringResource(R.string.workoutFrequency)
    val intensityLabel = stringResource(R.string.workoutIntensity)

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recordWorkout)) },
                actions = {
                    if (recordingState != RecordingState.IDLE) {
                        IconButton(onClick = { vm.requestStopConfirmation() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.stop))
                        }
                    }
                    if (recordingState == RecordingState.IDLE && voiceFeedbackAvailable) {
                        IconButton(onClick = { vm.requestIntervalSetDialog() }) {
                            Icon(painterResource(R.drawable.ic_list), contentDescription = stringResource(R.string.intervalSet))
                        }
                    }
                    if (recordingState == RecordingState.IDLE) {
                        IconButton(onClick = { /* edit info hint */ }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.editDisplayedInformation))
                        }
                    }
                    IconButton(onClick = { vm.requestChooseBluetoothDevice() }) {
                        HrStatusIcon(hrConnectionState)
                    }
                    if (!instance.userPreferences.useAutoPause && recordingState != RecordingState.IDLE) {
                        IconButton(onClick = { vm.toggleManualPause() }) {
                            Icon(
                                painterResource(
                                    if (recordingState == RecordingState.RUNNING) R.drawable.ic_pause else R.drawable.ic_resume
                                ),
                                contentDescription = null
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Exercise name and reps
                Text(
                    text = workoutType.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = repetitions.toString(),
                    style = MaterialTheme.typography.displayMedium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Chart
                AndroidView(
                    factory = { ctx ->
                        LineChart(ctx).apply {
                            isScaleXEnabled = false
                            isScaleYEnabled = false
                            axisLeft.textColor = onBackgroundColor
                            axisRight.textColor = onBackgroundColor
                            xAxis.textColor = onBackgroundColor
                            legend.textColor = onBackgroundColor
                            description.textColor = onBackgroundColor
                            isHighlightPerDragEnabled = false
                            isHighlightPerTapEnabled = false
                            axisLeft.valueFormatter = object : DefaultValueFormatter(1) {
                                override fun getFormattedValue(value: Float): String {
                                    return super.getFormattedValue(value) + " " + UnitUtils.unitHertzShort
                                }
                            }
                            xAxis.valueFormatter = object : DefaultValueFormatter(1) {
                                override fun getFormattedValue(value: Float): String {
                                    return super.getFormattedValue(value) + " min"
                                }
                            }
                            axisLeft.setDrawGridLines(false)
                            axisRight.setDrawGridLines(false)
                            description.text = ""
                            setNoDataText("")
                        }
                    },
                    update = { chart ->
                        if (chartEntries.isEmpty()) {
                            chart.clear()
                            return@AndroidView
                        }

                        val freqEntries = chartEntries.map { Entry(it.timeMin, it.frequency) }
                        val intEntries = chartEntries.filter { it.intensity > 0 }
                            .map { Entry(it.timeMin, it.intensity) }

                        fun makeDataset(entries: List<Entry>, name: String, color: Int): LineDataSet {
                            return LineDataSet(entries, name).apply {
                                this.color = color
                                valueTextColor = color
                                setDrawCircles(false)
                                lineWidth = 4f
                                highlightLineWidth = 2.5f
                                mode = LineDataSet.Mode.HORIZONTAL_BEZIER
                            }
                        }

                        val freqDataset = makeDataset(freqEntries, frequencyLabel, frequencyColor).apply {
                            axisDependency = YAxis.AxisDependency.LEFT
                        }
                        val intDataset = makeDataset(intEntries, intensityLabel, intensityColor).apply {
                            axisDependency = YAxis.AxisDependency.RIGHT
                        }

                        val lineData = LineData().apply {
                            addDataSet(freqDataset)
                            addDataSet(intDataset)
                            setDrawValues(false)
                        }

                        chart.data = lineData
                        chart.invalidate()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Start button or time display
                if (recordingState == RecordingState.IDLE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Button(onClick = { vm.start(StartReason.START_BUTTON) }) {
                            Text(stringResource(R.string.start))
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

            // AutoStart overlay
            AutoStartOverlay(
                autoStartState = autoStartUiState,
                distanceUnitUtils = instance.distanceUnitUtils,
                onAbort = { vm.cancelAutoStart(true) }
            )
        }
    }
}
