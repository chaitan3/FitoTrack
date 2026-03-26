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

package de.tadris.fitness.ui.settings

import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import de.tadris.fitness.R
import de.tadris.fitness.recording.component.PressureComponent
import de.tadris.fitness.recording.event.PressureChangeEvent
import de.tadris.fitness.ui.FitoTrackActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class DebugPressureActivity : FitoTrackActivity() {

    companion object {
        const val tickInterval = 100L
        const val smoothSpeed = 1.5f * tickInterval / 1000f
    }

    val handler = Handler()
    private var running = false

    private lateinit var valueText: TextView
    private lateinit var heightDiffText: TextView

    private val pressureComponent = PressureComponent()
    private var lastResume = 0L

    private var lastPressure = -1f
    private var currentPressure = -1f
    private var nullPressure = -1f

    private var timer = Runnable { tick() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_pressure)
        setTitle(R.string.debugPressureSensor)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        valueText = findViewById(R.id.debugPressureValue)
        heightDiffText = findViewById(R.id.debugPressureHeightDiff)
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()
        running = true
        pressureComponent.register(this)
        lastResume = System.currentTimeMillis()
        tick()
    }

    override fun onPause() {
        super.onPause()
        running = false
        pressureComponent.unregister()
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    private fun tick() {
        if (!running) return
        currentPressure = smoothSpeed * lastPressure + (1 - smoothSpeed) * currentPressure

        valueText.text = String.format("%.2f", currentPressure) + " hPa"
        if (nullPressure == -1f && System.currentTimeMillis() - lastResume > 3000) {
            nullPressure = lastPressure
        }
        val heightDiff = getHeightDiff()
        if (heightDiff == -1f) {
            heightDiffText.setText(R.string.calibrating)
            currentPressure = lastPressure
        } else {
            heightDiffText.text = String.format("%.2f", heightDiff) + " m"
        }
        handler.postDelayed(timer, tickInterval)
    }

    @Subscribe
    fun onPressureChanged(event: PressureChangeEvent) {
        lastPressure = event.pressure
    }

    private fun getHeightDiff(): Float {
        if (nullPressure == -1f) return -1f
        return SensorManager.getAltitude(
            SensorManager.PRESSURE_STANDARD_ATMOSPHERE,
            currentPressure
        ) -
                SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, nullPressure)
    }

}