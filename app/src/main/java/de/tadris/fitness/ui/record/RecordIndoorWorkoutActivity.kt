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
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                IndoorRecordingScreen(vm = vm)
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

@Composable
fun IndoorRecordingScreen(vm: IndoorRecordingViewModel) {
    val chartEntries by vm.chartEntries.collectAsState()
    val repetitions by vm.repetitions.collectAsState()
    val workoutType by vm.currentWorkoutType.collectAsState()

    val onBackgroundColor = MaterialTheme.colorScheme.onBackground.toArgb()
    val frequencyColor = colorResource(R.color.diagramFrequency).toArgb()
    val intensityColor = colorResource(R.color.diagramIntensity).toArgb()
    val frequencyLabel = stringResource(R.string.workoutFrequency)
    val intensityLabel = stringResource(R.string.workoutIntensity)

    RecordingScaffold(
        vm = vm,
        mainContent = {
            IndoorChartContent(
                title = workoutType?.title.orEmpty(),
                repetitions = repetitions,
                chartEntries = chartEntries,
                onBackgroundColor = onBackgroundColor,
                frequencyColor = frequencyColor,
                intensityColor = intensityColor,
                frequencyLabel = frequencyLabel,
                intensityLabel = intensityLabel,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

@Composable
private fun IndoorChartContent(
    title: String,
    repetitions: Int,
    chartEntries: List<ChartEntry>,
    onBackgroundColor: Int,
    frequencyColor: Int,
    intensityColor: Int,
    frequencyLabel: String,
    intensityLabel: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 80.dp), // clear the top overlay row (back + status + HR pills)
    ) {
        // Chart fills the padded area
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
                        override fun getFormattedValue(value: Float): String =
                            super.getFormattedValue(value) + " " + UnitUtils.unitHertzShort
                    }
                    xAxis.valueFormatter = object : DefaultValueFormatter(1) {
                        override fun getFormattedValue(value: Float): String =
                            super.getFormattedValue(value) + " min"
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

                fun makeDataset(entries: List<Entry>, name: String, color: Int) =
                    LineDataSet(entries, name).apply {
                        this.color = color
                        valueTextColor = color
                        setDrawCircles(false)
                        lineWidth = 4f
                        highlightLineWidth = 2.5f
                        mode = LineDataSet.Mode.HORIZONTAL_BEZIER
                    }

                val lineData = LineData().apply {
                    addDataSet(makeDataset(freqEntries, frequencyLabel, frequencyColor).apply {
                        axisDependency = YAxis.AxisDependency.LEFT
                    })
                    addDataSet(makeDataset(intEntries, intensityLabel, intensityColor).apply {
                        axisDependency = YAxis.AxisDependency.RIGHT
                    })
                    setDrawValues(false)
                }
                chart.data = lineData
                chart.invalidate()
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Repetition count + label centered over the chart
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = repetitions.toString(),
                fontSize = 120.sp,
                fontWeight = FontWeight.Light,
                lineHeight = 120.sp,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
