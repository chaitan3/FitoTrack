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
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import de.tadris.fitness.R
import de.tadris.fitness.data.RepetitionSample
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.data.WorkoutTypeManager
import de.tadris.fitness.recording.RecordingState
import de.tadris.fitness.recording.StartReason
import de.tadris.fitness.recording.event.AggregatedRecordingValuesEvent
import de.tadris.fitness.recording.event.RecordingStateChangeEvent
import de.tadris.fitness.recording.indoor.exercise.RepetitionRecognizedEvent
import de.tadris.fitness.util.unit.UnitUtils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class RecordIndoorWorkoutActivity : RecordWorkoutActivity() {

    companion object {
        const val REQUEST_CODE_ACTIVITY_PERMISSION = 12
    }

    private lateinit var repetitionsText: TextView
    private lateinit var exerciseText: TextView

    private lateinit var intensityDataSet: LineDataSet
    private lateinit var frequencyDataSet: LineDataSet
    private lateinit var chart: LineChart

    private val frequencyEntries = mutableListOf<Entry>()
    private val intensityEntries = mutableListOf<Entry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val wasAlreadyRunning = if (LAUNCH_ACTION != intent.action) {
            true
        } else currentState != RecordingState.IDLE

        if (wasAlreadyRunning) {
            workoutType = AggregatedRecordingValuesEvent.getCurrent().workoutType
        } else {
            val workoutType = intent.getSerializableExtra(WORKOUT_TYPE_EXTRA)
            if (workoutType is WorkoutType) {
                this@RecordIndoorWorkoutActivity.workoutType = workoutType
            } else {
                val workoutTypeId = intent.getStringExtra(WORKOUT_TYPE_EXTRA)
                this@RecordIndoorWorkoutActivity.workoutType =
                    WorkoutTypeManager.getInstance().getWorkoutTypeById(this, workoutTypeId)
            }
        }

        initBeforeContent()
        setContentView(R.layout.activity_record_indoor_workout)
        repetitionsText = findViewById(R.id.indoorRecordingReps)
        exerciseText = findViewById(R.id.indoorRecordingType)
        chart = findViewById(R.id.recordChart)
        initChart()

        initAfterContent()

        checkPermissions()

        updateStartButton(true, R.string.start) { start(StartReason.START_BUTTON) }

        setTitle(R.string.recordWorkout)

        if (wasAlreadyRunning) {
            if (currentState != RecordingState.IDLE) {
                recordStartButtonsRoot.visibility = View.INVISIBLE
                timeView.visibility = View.VISIBLE
                invalidateOptionsMenu()
            }
            val samples = instance.db.workoutDao().getAllRepetitionSamplesOfWorkout(
                RecordingStateChangeEvent.getCurrent().workoutId
            ).toList()
            if (samples.isNotEmpty()) {
                lastSampleTime = samples.first().timestamp
                samples.subList(0, samples.size - 1).forEach {
                    onSampleFinalized(it)
                }
            }
        }
    }

    private fun initChart() {
        chart.isScaleXEnabled = false
        chart.isScaleYEnabled = false

        chart.axisLeft.textColor = themeTextColor
        chart.axisRight.textColor = themeTextColor
        chart.xAxis.textColor = themeTextColor
        chart.legend.textColor = themeTextColor
        chart.description.textColor = themeTextColor

        chart.isHighlightPerDragEnabled = false
        chart.isHighlightPerTapEnabled = false

        chart.axisLeft.valueFormatter = object : DefaultValueFormatter(1) {
            override fun getFormattedValue(value: Float): String {
                return super.getFormattedValue(value) + " " + UnitUtils.unitHertzShort
            }
        }
        chart.xAxis.valueFormatter = object : DefaultValueFormatter(1) {
            override fun getFormattedValue(value: Float): String {
                return super.getFormattedValue(value) + " min"
            }
        }

        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.setDrawGridLines(false)

        chart.description.text = ""
        chart.setNoDataText("")
    }

    override fun onResume() {
        super.onResume()
        refreshRepetitions()
    }

    override fun hasServicePermission(): Boolean {
        return hasPermission()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasPermission()) {
            showActivityPermissionConsent()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showActivityPermissionConsent() {
        AlertDialog.Builder(this)
            .setTitle(R.string.recordingPermissionNotGrantedTitle)
            .setMessage(R.string.recordingActivityPermissionMessage)
            .setPositiveButton(R.string.actionGrant) { _, _ -> requestActivityPermission() }
            .setNegativeButton(R.string.cancel) { _, _ -> activityFinish() }
            .setOnCancelListener { activityFinish() }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestActivityPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACTIVITY_RECOGNITION
            ), REQUEST_CODE_ACTIVITY_PERMISSION
        )
    }

    private fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_ACTIVITY_PERMISSION) {
            if (!hasPermission()) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.recordingPermissionNotGrantedTitle)
                    .setMessage(R.string.recordingActivityPermissionMessage)
                    .setPositiveButton(R.string.settings) { _, _ -> openSystemSettings() }
                    .create().show()
            } else {
                restartService()
            }
        }
    }

    override fun onListenerStart() { }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRepetitionRecognized(event: RepetitionRecognizedEvent) {
        refreshRepetitions()
    }

    private var lastSampleTime = System.currentTimeMillis()

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSampleFinalized(sample: RepetitionSample) {
        val frequency =
            1000 * sample.repetitions.toDouble() / (sample.timestampEnd - lastSampleTime + 1)
        lastSampleTime = sample.timestampEnd
        frequencyEntries.add(Entry(sample.relativeTime.toFloat() / 1000 / 60, frequency.toFloat()))
        if (sample.intensity > 0) {
            intensityEntries.add(
                Entry(
                    sample.relativeTime.toFloat() / 1000 / 60,
                    sample.intensity.toFloat()
                )
            )
        }
        updateChart()
    }

    private fun updateChart() {
        val lineData = LineData()

        frequencyDataSet = createDataset(
            frequencyEntries,
            getString(R.string.workoutFrequency),
            resources.getColor(R.color.diagramFrequency)
        )
        intensityDataSet = createDataset(
            intensityEntries,
            getString(R.string.workoutIntensity),
            resources.getColor(R.color.diagramIntensity)
        )

        frequencyDataSet.axisDependency = YAxis.AxisDependency.LEFT
        intensityDataSet.axisDependency = YAxis.AxisDependency.RIGHT

        lineData.addDataSet(frequencyDataSet)
        lineData.addDataSet(intensityDataSet)
        lineData.setDrawValues(false)

        chart.data = lineData
        chart.invalidate()
    }

    private fun createDataset(entries: List<Entry>, name: String, color: Int): LineDataSet {
        val dataSet = LineDataSet(entries, name)
        dataSet.color = color
        dataSet.valueTextColor = color
        dataSet.setDrawCircles(false)
        dataSet.lineWidth = 4f
        dataSet.highlightLineWidth = 2.5f
        dataSet.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        return dataSet
    }

    private fun refreshRepetitions() {
        val last = AggregatedRecordingValuesEvent.getCurrent()
        repetitionsText.text = last.repetitions.toString()
        exerciseText.text = resources.getQuantityString(
            workoutType.repeatingExerciseName, last.repetitions
        )
    }

}