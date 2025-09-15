/*
 * Copyright (c) 2025 Jannis Scheibe <jannis@tadris.de>
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
package de.tadris.fitness.ui.wear

import android.content.Context
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import de.tadris.fitness.R

class WearSyncWorkout(
	context: Context,
	name: String,
	val workoutId: Long,
	var done: Boolean
) : LinearLayout(context) {
	val wearSyncWorkoutStatus: TextView
	init {
		val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		inflater.inflate(R.layout.view_wear_sync_workout, this, true)
		findViewById<TextView>(R.id.wearSyncWorkoutName).text = name
		wearSyncWorkoutStatus = findViewById(R.id.wearSyncWorkoutStatus)
		if(done) wearSyncWorkoutStatus.setText(R.string.wear_status_done)
	}
	fun setDownloading(length: Int) {
		wearSyncWorkoutStatus.text = context
			.getString(R.string.wear_status_downloading, (length / 1000).toString())
	}
	fun setProcessing() { wearSyncWorkoutStatus.setText(R.string.wear_status_processing) }
	fun setDone() {
		done = true
		wearSyncWorkoutStatus.setText(R.string.wear_status_done)
	}
	fun setFail() {
		done = true
		wearSyncWorkoutStatus.setText(R.string.wear_status_fail)
	}
}
