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

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.AppDatabase
import de.tadris.fitness.data.HeartRateSample
import de.tadris.fitness.data.RouteSample
import de.tadris.fitness.data.Workout
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.WorkoutPause
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.data.WorkoutTypeManager
import de.tadris.fitness.ui.wear.WearSyncActivity.Companion.LOG_TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.util.UUID
import java.util.zip.GZIPInputStream

class WearSyncWorkoutsList : Fragment() {
	lateinit var activity: Activity
	lateinit var handler: Handler
	lateinit var title: TextView
	lateinit var list: LinearLayout
	var db: AppDatabase? = null
	val dB: AppDatabase
		get() {
			db?.let { return it }
			val db = Instance.getInstance(activity).db
			this.db = db
			return db
		}

	val wearSyncWorkouts = mutableListOf<WearSyncWorkout>()
	var wearSyncWorkoutInProgress: WearSyncWorkout? = null
	override fun onAttach(context: Context) {
		super.onAttach(context)
		try {
			activity = context as Activity
			handler = Handler(Looper.getMainLooper())
		} catch(e: ClassCastException) {
			Log.e(LOG_TAG, "WearSyncWorkoutsList.onAttach exception: " + e.message)
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val rootView = inflater.inflate(R.layout.fragment_wear_sync, container, false)
		title = rootView.findViewById(R.id.wearSyncTitle)
		list = rootView.findViewById(R.id.wearSyncList)
		list.visibility = View.VISIBLE
		rootView.findViewById<View>(R.id.wearSyncIcon).visibility = View.GONE
		lifecycleScope.launch { CommsBT.status.collect { status ->
			when(CommsBT.status.value) {
				CommsBT.Status.DISCONNECTED -> title.setText(R.string.wear_disconnected)
				CommsBT.Status.ERROR -> onBTError(CommsBT.error)
				else -> {}
			}
		} }
		lifecycleScope.launch { CommsBT.bytesDone.collect { bytesDone ->
			handler.removeCallbacks(onBTTimeout)
			handler.postDelayed(onBTTimeout, 5000)
			wearSyncWorkoutInProgress?.setDownloading(CommsBT.bytesDone.value)
		} }
		lifecycleScope.launch { CommsBT.response.collect { response ->
			handler.removeCallbacks(onBTTimeout)
			if(response == null) return@collect
			try {
				if(response.getInt("version") != CommsBT.COMMS_VERSION) {
					onBTError(R.string.wear_error_update)
					return@collect
				}
				when(response.getString("requestType")) {
					"sync" -> syncResponse(response)
					"getWorkout" -> getWorkoutResponse(response)
					else -> onBTError(R.string.wear_error_tech)
				}
			} catch(e: Exception) {
				Log.e(LOG_TAG, "onBTResponse exception: $e")
				Log.e(LOG_TAG, "onBTResponse exception: " + e.message)
				onBTError(R.string.wear_error_tech)
			}
		} }
		return rootView
	}

	override fun onResume() {
		super.onResume()
		title.text = getString(R.string.wear_connected_to, CommsBT.deviceName)
		CoroutineScope(Dispatchers.Default).launch {
			handler.postDelayed(onBTTimeout, 5000)
			CommsBT.sendSyncRequest()
		}
	}

	fun syncResponse(response: JSONObject) {
		Log.d(LOG_TAG, "syncResponse")
		try {
			list.removeAllViews()
			val workouts = response.getJSONArray("workouts")
			for(i in 0..<workouts.length()) addWearSyncWorkout(workouts.getJSONObject(i))
			sync()
		} catch(e: Exception) {
			Log.e(LOG_TAG, "syncResponse exception: $e")
			Log.e(LOG_TAG, "syncResponse exception: " + e.message)
			onBTError(R.string.wear_error_tech)
		}
	}
	@Throws(JSONException::class)
	fun addWearSyncWorkout(watchWorkout: JSONObject) {
		val name = watchWorkout.getString("name") + " " + watchWorkout.getString("workoutType")
		val workoutId = watchWorkout.getLong("id")
		val duration = watchWorkout.getLong("durationTotal")
		val done = dB.workoutDao().getWorkoutById(workoutId)?.activeDuration == duration
		val wearSyncWorkout = WearSyncWorkout(activity, name, workoutId, done)
		wearSyncWorkouts.add(wearSyncWorkout)
		list.addView(wearSyncWorkout)
	}

	fun sync() {
		for(wearSyncWorkout in wearSyncWorkouts) {
			if(!wearSyncWorkout.done) {
				wearSyncWorkoutInProgress = wearSyncWorkout
				CoroutineScope(Dispatchers.Default).launch {
					handler.postDelayed(onBTTimeout, 5000)
					CommsBT.sendGetWorkoutRequest(wearSyncWorkout.workoutId)
				}
				CoroutineScope(Dispatchers.Main).launch {
					wearSyncWorkoutInProgress?.setDownloading(0)
				}
				return
			}
		}
		activity.finishAndRemoveTask()
	}

	fun getWorkoutResponse(response: JSONObject) {
		var workoutId: Long = 0
		try {
			if(response.has("error")) {
				onBTError(R.string.wear_error_tech)
				return
			}
			CoroutineScope(Dispatchers.Main).launch { wearSyncWorkoutInProgress?.setProcessing() }
			val watchWorkout = response.getJSONObject("workout")
			workoutId = watchWorkout.getLong("id")
			val watchType = watchWorkout.getString("workoutType")
			val workoutType = getWorkoutType(watchType)
			val existing = dB.workoutDao().getWorkoutById(workoutId)
			val data = toWorkoutData(watchWorkout, workoutId, workoutType, isGpsWorkout(watchType), existing)
			if(existing != null) {
				// Replace the previous import, including all child samples. Deleting the workout
				// cascades to route, heart rate, repetition and pause samples.
				dB.workoutDao().deleteWorkoutAndSamples(
					existing,
					dB.workoutDao().getAllSamplesOfWorkout(workoutId)
				)
			}
			dB.workoutDao().insertWorkoutData(data)
			doneNext()
		} catch(e: Exception) {
			Log.e(LOG_TAG, "getWorkoutResponse exception: $e")
			Log.e(LOG_TAG, "getWorkoutResponse exception: " + e.message)
			if(workoutId > 0) {
				CoroutineScope(Dispatchers.Main).launch { wearSyncWorkoutInProgress?.setFail() }
				sync()
			} else {
				onBTError(R.string.wear_error_tech)
			}
		}
	}

	fun onBTError(message: Int) {
		try {
			Log.d(LOG_TAG, "onBTError: " + getString(message))
			CoroutineScope(Dispatchers.Main).launch { title.setText(message) }
		}catch(_: Exception) {}
	}
	val onBTTimeout = Runnable { onBTError(R.string.wear_error_tech) }

	fun doneNext() {
		CoroutineScope(Dispatchers.Main).launch {
			wearSyncWorkoutInProgress?.setDone()
			CoroutineScope(Dispatchers.Default).launch { sync() }
		}
	}

	private data class Checkpoint(val time: Long, val duration: Long)

	private fun isGpsWorkout(workoutType: String): Boolean =
		workoutType != "Climbing" && workoutType != "Other (indoor)"

	private fun toWorkoutData(
		watchWorkout: JSONObject,
		workoutId: Long,
		workoutType: WorkoutType,
		isGps: Boolean,
		existing: Workout?
	): WorkoutData {
		val start = workoutId
		val durationTotal = watchWorkout.optLong("durationTotal", 0L)
		val pauseTotal = watchWorkout.optLong("pauseTotal", 0L)
		val endTime = watchWorkout.optLong("endTime", 0L)
		val end = if(endTime > 0L) endTime else start + durationTotal + pauseTotal
		val checkpoints = readCheckpoints(watchWorkout)

		val workout = Workout(
			id = workoutId,
			uuid = existing?.uuid ?: UUID.randomUUID().toString(),
			start = start,
			end = end,
			activeDuration = durationTotal,
			comment = watchWorkout.optString("desc").takeIf { it.isNotBlank() },
			distance = watchWorkout.optDouble("distanceTotal", 0.0),
			repetitions = if(isGps) 0 else (checkpoints.size + 1) / 2,
			energy = watchWorkout.optDouble("caloriesTotal", 0.0) * 4186.8,
			workoutTypeId = workoutType.id,
			intervalSetUsedId = existing?.intervalSetUsedId ?: -1L,
		)

		return WorkoutData(
			workout = workout,
			samples = if(isGps) readRouteSamples(watchWorkout, workoutId, start, checkpoints) else emptyList(),
			pauses = readPauses(workoutId, checkpoints),
			events = emptyList(),
			repetitionSamples = emptyList(),
			heartRateSamples = readHeartRateSamples(watchWorkout, workoutId, start, checkpoints),
		)
	}

	private fun readCheckpoints(watchWorkout: JSONObject): List<Checkpoint> {
		val checkpoints = watchWorkout.optJSONArray("checkpoints") ?: return emptyList()
		return (0..<checkpoints.length()).mapNotNull { i ->
			checkpoints.optJSONObject(i)?.let { Checkpoint(it.optLong("time"), it.optLong("duration")) }
		}
	}

	private fun relativeTime(time: Long, start: Long, checkpoints: List<Checkpoint>): Long {
		if(checkpoints.isEmpty()) return time - start
		var checkpoint = checkpoints.first()
		for(i in 1..<checkpoints.size) {
			if(checkpoints[i].time > time) break
			checkpoint = checkpoints[i]
		}
		return time - checkpoint.time + checkpoint.duration
	}

	private fun readRouteSamples(
		watchWorkout: JSONObject,
		workoutId: Long,
		start: Long,
		checkpoints: List<Checkpoint>
	): List<RouteSample> {
		val locations = gzipToJSONArray(watchWorkout.optString("locations"))
		if(locations.length() == 0) return emptyList()

		val startElevation = (0..<locations.length())
			.mapNotNull { locations.optJSONObject(it) }
			.firstOrNull { it.optDouble("ele", 0.0) != 0.0 }
			?.optDouble("ele", 0.0) ?: 0.0

		val samples = mutableListOf<RouteSample>()
		for(i in 0..<locations.length()) {
			val location = locations.optJSONObject(i) ?: continue
			val time = location.optLong("time")
			val elevation = location.optDouble("ele", 0.0).let {
				if(it == 0.0) startElevation else it
			}
			samples.add(
				RouteSample(
					workoutId = workoutId,
					timestamp = time,
					relativeTime = relativeTime(time, start, checkpoints),
					latitude = location.optDouble("lat", 0.0),
					longitude = location.optDouble("lon", 0.0),
					elevation = elevation,
					elevationMSL = elevation,
					speed = location.optDouble("speed", 0.0),
					pressure = 0f,
				)
			)
		}
		return samples
	}

	private fun readHeartRateSamples(
		watchWorkout: JSONObject,
		workoutId: Long,
		start: Long,
		checkpoints: List<Checkpoint>
	): List<HeartRateSample> {
		val heartRates = gzipToJSONArray(watchWorkout.optString("heartRates"))
		if(heartRates.length() == 0) return emptyList()

		val samples = mutableListOf<HeartRateSample>()
		for(i in 0..<heartRates.length()) {
			val heartRate = heartRates.optJSONObject(i) ?: continue
			val value = heartRate.optDouble("heartRate", 0.0).toInt()
			if(value <= 0) continue
			val time = heartRate.optLong("time")
			samples.add(
				HeartRateSample(
					workoutId = workoutId,
					timestamp = time,
					relativeTime = relativeTime(time, start, checkpoints),
					heartRate = value,
				)
			)
		}
		return samples
	}

	private fun readPauses(workoutId: Long, checkpoints: List<Checkpoint>): List<WorkoutPause> {
		val pauses = mutableListOf<WorkoutPause>()
		var i = 0
		while(i + 1 < checkpoints.size) {
			val start = checkpoints[i]
			val end = checkpoints[i + 1]
			if(start.duration == end.duration) {
				pauses.add(
					WorkoutPause(
						workoutId = workoutId,
						startDate = start.time,
						endDate = end.time,
						userInitiated = false,
					)
				)
				i += 2
			} else {
				i += 1
			}
		}
		return pauses
	}

	fun getWorkoutType(input: String): WorkoutType {
		val workoutTypeId = when(input) {
			"Climbing" -> WorkoutTypeManager.WORKOUT_TYPE_ID_CLIMBING
			"Cycling" -> WorkoutTypeManager.WORKOUT_TYPE_ID_CYCLING
			"Hiking" -> WorkoutTypeManager.WORKOUT_TYPE_ID_HIKING
			"Running" -> WorkoutTypeManager.WORKOUT_TYPE_ID_RUNNING
			"Skiing" -> WorkoutTypeManager.WORKOUT_TYPE_ID_SKIING
			"Snowboarding" -> WorkoutTypeManager.WORKOUT_TYPE_ID_SNOWBOARDING
			"Snowshoeing" -> WorkoutTypeManager.WORKOUT_TYPE_ID_SNOWSHOEING
			"Swim (outdoor)" -> WorkoutTypeManager.WORKOUT_TYPE_ID_SWIMMING
			"Walking" -> WorkoutTypeManager.WORKOUT_TYPE_ID_WALKING
			"Windsurfing" -> WorkoutTypeManager.WORKOUT_TYPE_ID_WINDSURFING
			// TODO Multisport
			else -> WorkoutTypeManager.WORKOUT_TYPE_ID_OTHER
		}
		return WorkoutTypeManager.getInstance().getWorkoutTypeById(activity, workoutTypeId)
	}

	private fun gzipToJSONArray(str: String?): JSONArray {
		if(str.isNullOrBlank()) return JSONArray()
		val bin = ByteArrayInputStream(Base64.decode(str, Base64.DEFAULT))
		val br = BufferedReader(InputStreamReader(GZIPInputStream(bin)))
		val line = br.readLine() ?: return JSONArray()
		return JSONArray(line)
	}
}
