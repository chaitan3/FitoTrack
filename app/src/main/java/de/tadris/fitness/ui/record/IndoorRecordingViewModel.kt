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
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewModelScope
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.recording.RecordingState
import de.tadris.fitness.recording.event.AggregatedRecordingValuesEvent
import de.tadris.fitness.recording.event.RecordingStateChangeEvent
import de.tadris.fitness.recording.event.RepetitionSampleEvent
import de.tadris.fitness.recording.indoor.exercise.RepetitionRecognizedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

data class ChartEntry(val timeMin: Float, val frequency: Float, val intensity: Float)

class IndoorRecordingViewModel(application: Application) : RecordingViewModel(application) {

    private val _chartEntries = MutableStateFlow<List<ChartEntry>>(emptyList())
    val chartEntries: StateFlow<List<ChartEntry>> = _chartEntries.asStateFlow()

    private val _repetitions = MutableStateFlow(0)
    val repetitions: StateFlow<Int> = _repetitions.asStateFlow()

    private var lastSampleTime = System.currentTimeMillis()

    override fun onServiceStarted() {
        if (wasAlreadyRunning) {
            loadExistingChartData()
        }
    }

    override fun hasServicePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(
                getApplication(), Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun loadExistingChartData() {
        viewModelScope.launch(Dispatchers.IO) {
            val workoutId = RecordingStateChangeEvent.getCurrent().workoutId
            val samples = instance.db.workoutDao().getAllRepetitionSamplesOfWorkout(workoutId).toList()
            if (samples.isNotEmpty()) {
                lastSampleTime = samples.first().timestamp
                val entries = mutableListOf<ChartEntry>()
                // Process all but the last (the last is still "open")
                samples.subList(0, samples.size - 1).forEach { sample ->
                    val frequency = 1000 * sample.repetitions.toDouble() / (sample.timestampEnd - lastSampleTime + 1)
                    lastSampleTime = sample.timestampEnd
                    entries.add(
                        ChartEntry(
                            timeMin = sample.relativeTime.toFloat() / 1000 / 60,
                            frequency = frequency.toFloat(),
                            intensity = sample.intensity.toFloat()
                        )
                    )
                }
                withContext(Dispatchers.Main) {
                    _chartEntries.value = entries
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRepetitionRecognized(event: RepetitionRecognizedEvent) {
        val last = AggregatedRecordingValuesEvent.getCurrent()
        _repetitions.value = last.repetitions
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSampleFinalized(e: RepetitionSampleEvent) {
        val sample = e.sample
        val frequency = 1000 * sample.repetitions.toDouble() / (sample.timestampEnd - lastSampleTime + 1)
        lastSampleTime = sample.timestampEnd
        val entry = ChartEntry(
            timeMin = sample.relativeTime.toFloat() / 1000 / 60,
            frequency = frequency.toFloat(),
            intensity = sample.intensity.toFloat()
        )
        _chartEntries.value = _chartEntries.value + entry
    }
}
