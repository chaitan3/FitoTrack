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

package de.tadris.fitness.ui.settings.debug

import android.hardware.SensorEvent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.recording.component.ExerciseRecognitionComponent
import de.tadris.fitness.recording.component.FitoTrackSensorOption
import de.tadris.fitness.ui.FitoTrackActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class SensorDebugActivity : FitoTrackActivity() {

    private lateinit var spinner: Spinner
    private lateinit var startStopButton: Button
    private lateinit var infoText: TextView

    private val exerciseRecognitionComponent = ExerciseRecognitionComponent()
    private var running = false

    private val sensors = FitoTrackSensorOption
        .values()
        .mapIndexed { index, sensor -> index to sensor }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_debug)
        setTitle(R.string.debugSensorsTitle)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        spinner = findViewById(R.id.debugSensorsSelection)
        startStopButton = findViewById(R.id.debugSensorsStartButton)
        infoText = findViewById(R.id.debugSensorsInfo)

        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            sensors.map { it.second.name }.toList()
        )
        startStopButton.setOnClickListener { toggleStart() }

        exerciseRecognitionComponent.init(this)

        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        stop()
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    private fun toggleStart() {
        if (running) {
            stop()
        } else {
            start()
        }
    }

    private fun start() {
        if (running) return
        running = true
        val sensor = sensors[spinner.selectedItemPosition].second
        exerciseRecognitionComponent.activateSensor(sensor)
        refreshButtonText()
    }

    private fun stop() {
        if (!running) return
        running = false
        exerciseRecognitionComponent.unregister()
        refreshButtonText()
        infoText.text = ""
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSensorEvent(event: SensorEvent) {
        val info =
            "${event.sensor.name},${event.timestamp},${event.values.joinToString(separator = ",")}"
        Instance.getInstance(this).logger.info("SensorDebug", info)
        infoText.text = info.replace(",", "\n")
    }

    private fun refreshButtonText() {
        startStopButton.setText(if (running) R.string.stop else R.string.start)
    }

}