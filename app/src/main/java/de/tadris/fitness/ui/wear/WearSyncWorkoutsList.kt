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
import de.tadris.fitness.data.GpsSample
import de.tadris.fitness.data.GpsWorkout
import de.tadris.fitness.data.IndoorSample
import de.tadris.fitness.data.IndoorWorkout
import de.tadris.fitness.data.RecordingType
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

	var minHeight = 0.0
	var maxHeight = 0.0
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
			try {
				if(response?.getInt("version") != CommsBT.COMMS_VERSION) {
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
		val exists = checkWorkoutExists(workoutId)
		val wearSyncWorkout = WearSyncWorkout(activity, name, workoutId, exists)
		wearSyncWorkouts.add(wearSyncWorkout)
		list.addView(wearSyncWorkout)
	}

	fun checkWorkoutExists(workoutId: Long): Boolean =
		dB.gpsWorkoutDao().getWorkoutById(workoutId) != null ||
		dB.indoorWorkoutDao().getWorkoutById(workoutId) != null

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
			if(checkWorkoutExists(workoutId)) {
				Log.e(LOG_TAG, "getWorkoutResponse somehow already in db, skipping")
				doneNext()
				return
			}
			val type = watchWorkout.getString("workoutType")
			if(getWorkoutType(type).getRecordingType() == RecordingType.GPS) {
				val samples = getGpsSamples(watchWorkout, workoutId)
				val gpsWorkout = toGpsWorkout(watchWorkout, workoutId)
				dB.gpsWorkoutDao()
					.insertWorkoutAndSamples(gpsWorkout, samples.toTypedArray<GpsSample?>())
			} else {
				val samples = getIndoorSamples(watchWorkout, workoutId)
				val indoorWorkout = toIndoorWorkout(watchWorkout, workoutId)
				dB.indoorWorkoutDao()
					.insertWorkoutAndSamples(indoorWorkout, samples.toTypedArray<IndoorSample?>())
			}
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

	@Throws(JSONException::class)
	fun toIndoorWorkout(watchWorkout: JSONObject, workoutId: Long): IndoorWorkout {
		val indoorWorkout = IndoorWorkout()
		indoorWorkout.edited = false
		indoorWorkout.setWorkoutType(getWorkoutType(watchWorkout.getString("workoutType")))
		indoorWorkout.id = workoutId
		indoorWorkout.start = workoutId
		indoorWorkout.duration = watchWorkout.getLong("durationTotal")
		indoorWorkout.repetitions = (watchWorkout.getJSONArray("checkpoints").length() + 1) / 2
		if(watchWorkout.has("pauseTotal")) indoorWorkout.pauseDuration =
			watchWorkout.getLong("pauseTotal")
		indoorWorkout.end =
			indoorWorkout.start + indoorWorkout.duration + indoorWorkout.pauseDuration
		if(watchWorkout.has("heartRateAvg")) indoorWorkout.avgHeartRate =
			watchWorkout.getDouble("heartRateAvg").toInt()
		if(watchWorkout.has("heartRateMax")) indoorWorkout.maxHeartRate =
			watchWorkout.getInt("heartRateMax")
		if(watchWorkout.has("caloriesTotal")) indoorWorkout.calorie =
			watchWorkout.getDouble("caloriesTotal").toInt()
		return indoorWorkout
	}
	@Throws(Exception::class)
	fun getIndoorSamples(watchWorkout: JSONObject, workoutId: Long): MutableList<IndoorSample> {
		val samples = mutableListOf<IndoorSample>()
		//get checkpoints
		val checkpoints = watchWorkout.getJSONArray("checkpoints")
		var icp = 0
		var checkpoint = checkpoints.getJSONObject(icp)
		var icpTime = checkpoint.getLong("time")
		var icpDuration = checkpoint.getInt("duration")
		var ncpTime =
			if(checkpoints.length() <= 1) 0
			else checkpoints.getJSONObject(1).getLong("time")
		//get heart rates
		val heartRates = gzipToJSONArray(watchWorkout.getString("heartRates"))
		for(ihr in 0..<heartRates.length()) {
			val heartRate = heartRates.getJSONObject(ihr)
			val ihrTime = heartRate.getLong("time")
			val ihrHeartRate = heartRate.getInt("heartRate")

			while(ncpTime > 0 && ncpTime < ihrTime) {
				icp++
				checkpoint = checkpoints.getJSONObject(icp)
				icpTime = checkpoint.getLong("time")
				icpDuration = checkpoint.getInt("duration")
				ncpTime = if(checkpoints.length() > icp + 1) checkpoints.getJSONObject(icp + 1)
					.getLong("time") else 0
			}
			val sample = IndoorSample()
			sample.absoluteTime = ihrTime
			sample.relativeTime = ihrTime - icpTime + icpDuration
			sample.heartRate = ihrHeartRate
			//sample.repetitions
			//sample.intensity
			//sample.frequency
			sample.id = workoutId + ihr + 1
			sample.workoutId = workoutId
			samples.add(sample)
		}
		return samples
	}
	@Throws(JSONException::class)
	fun toGpsWorkout(watchWorkout: JSONObject, workoutId: Long): GpsWorkout {
		val gpsWorkout = GpsWorkout()
		gpsWorkout.edited = false
		gpsWorkout.setWorkoutType(getWorkoutType(watchWorkout.getString("workoutType")))
		gpsWorkout.id = workoutId
		gpsWorkout.start = workoutId
		gpsWorkout.duration = watchWorkout.getLong("durationTotal")
		if(watchWorkout.has("pauseTotal")) gpsWorkout.pauseDuration =
			watchWorkout.getLong("pauseTotal")
		gpsWorkout.end = gpsWorkout.start + gpsWorkout.duration + gpsWorkout.pauseDuration
		if(watchWorkout.has("heartRateAvg")) gpsWorkout.avgHeartRate =
			watchWorkout.getDouble("heartRateAvg").toInt()
		if(watchWorkout.has("heartRateMax")) gpsWorkout.maxHeartRate =
			watchWorkout.getInt("heartRateMax")
		if(watchWorkout.has("caloriesTotal")) gpsWorkout.calorie =
			watchWorkout.getDouble("caloriesTotal").toInt()
		if(watchWorkout.has("distanceTotal")) gpsWorkout.length =
			watchWorkout.getDouble("distanceTotal").toInt()
		if(watchWorkout.has("elevationGainTotal")) gpsWorkout.ascent =
			watchWorkout.getInt("elevationGainTotal").toFloat()
		if(watchWorkout.has("elevationLossTotal")) gpsWorkout.descent =
			watchWorkout.getInt("elevationLossTotal").toFloat()
		if(watchWorkout.has("speedAvg")) {
			gpsWorkout.avgSpeed = watchWorkout.getDouble("speedAvg")
			gpsWorkout.avgPace = 1 / (gpsWorkout.avgSpeed / 1000 * 60)
		}
		if(watchWorkout.has("speedMax")) gpsWorkout.topSpeed = watchWorkout.getDouble("speedMax")
		gpsWorkout.minElevationMSL = minHeight.toFloat()
		gpsWorkout.maxElevationMSL = maxHeight.toFloat()
		return gpsWorkout
	}
	@Throws(Exception::class)
	fun getGpsSamples(watchWorkout: JSONObject, workoutId: Long): MutableList<GpsSample> {
		val samples = mutableListOf<GpsSample>()
		//get checkpoints
		val checkpoints = watchWorkout.getJSONArray("checkpoints")
		var icp = 0
		var checkpoint = checkpoints.getJSONObject(icp)
		var icpTime = checkpoint.getLong("time")
		var icpDuration = checkpoint.getInt("duration")
		var ncpTime =
			if(checkpoints.length() > 1) checkpoints.getJSONObject(1).getLong("time") else 0
		//get heart rates
		val heartRates = gzipToJSONArray(watchWorkout.getString("heartRates"))
		var ihr = 0
		var ihrHeartRate = 0
		if(heartRates.length() > 0) {
			val heartRate = heartRates.getJSONObject(ihr)
			ihrHeartRate = heartRate.getInt("heartRate")
		}
		var nhrTime = if(heartRates.length() > 1) heartRates.getJSONObject(1).getLong("time") else 0
		//get locations
		val locations = gzipToJSONArray(watchWorkout.getString("locations"))
		minHeight = 100000.0
		maxHeight = 0.0
		var startElevation = 0.0
		for(il in 0..<locations.length()) {
			val location = locations.getJSONObject(il)
			if(location.getDouble("ele") != 0.0) {
				startElevation = location.getDouble("ele")
				break
			}
		}
		for(il in 0..<locations.length()) {
			val location = locations.getJSONObject(il)
			val ilTime = location.getLong("time")
			while(ncpTime > 0 && ncpTime < ilTime) {
				icp++
				checkpoint = checkpoints.getJSONObject(icp)
				icpTime = checkpoint.getLong("time")
				icpDuration = checkpoint.getInt("duration")
				ncpTime = if(checkpoints.length() > icp + 1) checkpoints.getJSONObject(icp + 1)
					.getLong("time") else 0
			}
			while(nhrTime > 0 && nhrTime < ilTime) {
				ihr++
				val heartRate = heartRates.getJSONObject(ihr)
				ihrHeartRate = heartRate.getInt("heartRate")
				nhrTime = if(heartRates.length() > ihr + 1) heartRates.getJSONObject(ihr + 1)
					.getLong("time") else 0
			}
			val sample = GpsSample()
			sample.lat = location.getDouble("lat")
			sample.lon = location.getDouble("lon")
			sample.elevation = location.getDouble("ele")
			if(sample.elevation == 0.0) {
				sample.elevation = startElevation
			} else {
				if(sample.elevation < minHeight) minHeight = sample.elevation
				if(sample.elevation > maxHeight) maxHeight = sample.elevation
			}
			sample.elevationMSL = sample.elevation
			sample.speed = location.getDouble("speed")
			sample.absoluteTime = ilTime
			sample.relativeTime = ilTime - icpTime + icpDuration
			//sample.pressure
			sample.heartRate = ihrHeartRate
			//sample.intervalTriggered
			sample.id = workoutId + il + 1
			sample.workoutId = workoutId
			samples.add(sample)
		}
		if(minHeight == 100000.0) minHeight = maxHeight
		return samples
	}

	fun getWorkoutType(input: String): WorkoutType {
		var workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_OTHER
		when(input) {
			"Climbing" -> workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_CLIMBING
			"Cycling" -> workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_CYCLING
			"Hiking" -> workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_HIKING
			"Running" -> workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_RUNNING
			"Snowboarding" -> workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_SNOWBOARDING
			"Snowshoeing" -> workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_SNOWSHOEING
			"Swim (outdoor)" -> workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_SWIMMING
			"Walking" -> workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_WALKING
			"Windsurfing" -> workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_WINDSURFING
			//TODO Multisport
		}
		val workoutType =
			WorkoutTypeManager.getInstance().getWorkoutTypeById(activity, workoutTypeId)
		if(workoutTypeId == WorkoutTypeManager.WORKOUT_TYPE_ID_OTHER) {
			workoutType.title = input
			workoutType.recordingType = RecordingType.GPS.id
		}
		return workoutType
	}
	@Throws(Exception::class)
	fun gzipToJSONArray(str: String): JSONArray {
		val bin = ByteArrayInputStream(Base64.decode(str, Base64.DEFAULT))
		val br = BufferedReader(InputStreamReader(GZIPInputStream(bin)))
		return JSONArray(br.readLine())
	}
}
