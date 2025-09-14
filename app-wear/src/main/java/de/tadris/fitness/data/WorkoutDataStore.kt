/*
 * Copyright (c) 2025 Bart Vullings <dev@windkracht8.com>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU General Public License as published by
 * 	the Free Software Foundation, either version 3 of the License, or
 * 	(at your option) any later version.
 *
 * 	FitoTrack is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 *
 * 	You should have received a copy of the GNU General Public License
 * 	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.tadris.fitness.data

import android.app.Activity
import android.content.Context
import de.tadris.fitness.R
import de.tadris.fitness.ui.Main.Companion.runInBackgroundJAVA
import de.tadris.fitness.ui.workouts.WorkoutActivity
import de.tadris.fitness.ui.workouts.WorkoutsActivity
import de.tadris.fitness.util.logE
import de.tadris.fitness.util.logW
import de.tadris.fitness.util.toast
import de.tadris.fitness.util.toastFromBG
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.util.Arrays

object WorkoutDataStore {
	@JvmField
    val workouts = mutableListOf<WorkoutData>()
	@JvmField
    var lastWorkoutID = 0L
	@JvmStatic
    fun getWorkout(id: Long): WorkoutData? =//TODO replace inline where used
		workouts.firstOrNull{ it.id == id }

	@JvmStatic
    fun deleteWorkout(activity: Activity, id: Long) { //Thread: BG
		workouts.removeIf{ it.id == id }
		if(!activity.deleteFile(idToWorkoutFilename(id))) {
			logE("WorkoutDataStore.deleteWorkout failed, filename: " + idToWorkoutFilename(id))
			activity.toastFromBG(R.string.error_workout_delete)
		}
		if(!activity.deleteFile(idToLocationFilename(id))) {
			logW("WorkoutDataStore.deleteWorkout failed, filename: " + idToLocationFilename(id))
		}
		if(!activity.deleteFile(idToHeartRateFilename(id))) {
			logW("WorkoutDataStore.deleteWorkout failed, filename: " + idToHeartRateFilename(id))
		}
	}

	@JvmStatic
    fun readWorkouts(workoutsActivity: WorkoutsActivity) { //Thread: BG
		readWorkouts(workoutsActivity.baseContext)
		workoutsActivity.loadItems()//TODO replace this when workoutsActivity is in Kotlin
	}

	@JvmStatic
    fun readWorkouts(context: Context) { //Thread: BG
		val files = context.filesDir.listFiles()
		if(files == null) return
		Arrays.sort(files)
		workouts.clear()
		for(i in files.indices.reversed()) {
			val filename = files[i].getName()
			if(filename.startsWith("workout_") && filename.endsWith(".json")) {
				val workoutData = readWorkout(context, filename)
				if(workoutData != null) workouts.add(workoutData)
			}
		}
	}

	@JvmStatic
    fun getFullWorkout(context: Context, id: Long): WorkoutData? { //Thread: BG
		try {
			val workoutData = readWorkout(context, idToWorkoutFilename(id))
			if(workoutData == null) return null
			if(workoutData.isWorkoutTypeGps) {
				val locations = readLocations(context, id)
				if(locations != null) workoutData.locationsFromJson(locations)
			}
			val heartRates = readHeartRates(context, id)
			if(heartRates != null) workoutData.heartRatesFromJson(heartRates)
			return workoutData
		} catch(e: Exception) {
			logE("WorkoutDataStore.getFullWorkout: " + e.message)
		}
		return null
	}

	fun readWorkout(context: Context, filename: String): WorkoutData? { //Thread: BG
		try {
			val text = context.openFileInput(filename).bufferedReader().use { it.readText() }
			return WorkoutData(JSONObject(text))
		} catch(e: Exception) {
			logE("WorkoutDataStore.readWorkout: " + e.message)
		}
		return null
	}

	@JvmStatic
    fun readLocations(workoutActivity: WorkoutActivity, workoutData: WorkoutData) { //Thread: BG
		try {
			val locations = readLocations(workoutActivity, workoutData.id)
			if(locations == null) return
			workoutData.locationsFromJson(locations)
			workoutActivity.locationsRead(workoutData)
		} catch(e: Exception) {
			logW("WorkoutDataStore.readLocations: " + e.message)
		}
	}

	fun readLocations(context: Context, workoutDataId: Long): JSONArray? { //Thread: BG
		try {
			val filename = idToLocationFilename(workoutDataId)
			val text = context.openFileInput(filename).bufferedReader().use { it.readText() }
			return JSONArray(text)
		} catch(e: Exception) {
			logW("WorkoutDataStore.readLocations: " + e.message)
		}
		return null
	}

	@JvmStatic
    fun readHeartRates(workoutActivity: WorkoutActivity, workoutData: WorkoutData) { //Thread: BG
		try {
			val heartRates = readHeartRates(workoutActivity, workoutData.id)
			if(heartRates == null) return
			workoutData.heartRatesFromJson(heartRates)
			workoutActivity.heartRatesRead(workoutData)
		} catch(e: Exception) {
			logW("WorkoutDataStore.readHeartRates: " + e.message)
		}
	}

	private fun readHeartRates(context: Context, workoutDataId: Long): JSONArray? { //Thread: BG
		try {
			val filename = idToHeartRateFilename(workoutDataId)
			val text = context.openFileInput(filename).bufferedReader().use { it.readText() }
			return JSONArray(text)
		} catch(e: Exception) {
			logW("WorkoutDataStore.readHeartRates: " + e.message)
		}
		return null
	}

	fun storeWorkout(activity: Activity, workoutData: WorkoutData) { //Thread: UI
		workoutData.setDescName()
		workouts.add(0, workoutData)
		lastWorkoutID = workoutData.id
		runInBackgroundJAVA { writeWorkout(activity, workoutData) }
	}

	fun writeWorkout(activity: Activity, workoutData: WorkoutData) { //Thread: BG
		try {
			val workoutDataJson = workoutData.toJsonString(activity)
			if(workoutDataJson.length < 10) {
				activity.toastFromBG(R.string.error_workout_write)
				return
			}
			val filename = idToWorkoutFilename(workoutData.id)
			var fos = activity.openFileOutput(filename, Context.MODE_PRIVATE)
			var osr = OutputStreamWriter(fos)
			osr.write(workoutDataJson)
			osr.flush()
			fos.close()
			val locationJson = workoutData.locationsToJsonString()
			if(locationJson.length > 10) {
				val filename = idToLocationFilename(workoutData.id)
				fos = activity.openFileOutput(filename, Context.MODE_PRIVATE)
				osr = OutputStreamWriter(fos)
				osr.write(locationJson)
				osr.flush()
				fos.close()
			}
			val heartRateJson = workoutData.heartRatesToJsonString()
			if(heartRateJson.length > 10) {
				val filename = idToHeartRateFilename(workoutData.id)
				fos = activity.openFileOutput(filename, Context.MODE_PRIVATE)
				osr = OutputStreamWriter(fos)
				osr.write(heartRateJson)
				osr.flush()
				fos.close()
			}
		} catch(e: Exception) {
			logE("WorkoutDataStore.writeWorkout: " + e.message)
			activity.toastFromBG(R.string.error_workout_write)
		}
	}

	private fun idToWorkoutFilename(id: Long): String = "workout_$id.json"
	private fun idToLocationFilename(id: Long): String = "location_$id.json"
	private fun idToHeartRateFilename(id: Long): String = "heart_$id.json"
}