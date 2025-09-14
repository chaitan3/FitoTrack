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

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseCapabilities
import androidx.health.services.client.data.ExerciseType
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import de.tadris.fitness.R
import de.tadris.fitness.ui.ChartData
import de.tadris.fitness.util.Util
import de.tadris.fitness.util.logE
import org.json.JSONArray
import org.json.JSONObject
import org.mapsforge.core.model.LatLong
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

class WorkoutData {
	val sdfGPXName = SimpleDateFormat("E dd MMM HH:mm", Locale.ENGLISH)
	var capabilities: ExerciseCapabilities? = null
	@JvmField
    var id = 0L//TODO late init
	@JvmField
    var description = ""
	@JvmField
    var name = ""

	enum class TargetType { DISTANCE, DURATION, SPEED, HEART_RATE }
	var primaryTargetType: TargetType? = null
	var primaryTargetValue = 0
	var primaryTargetValueHigh = 0
	var targetAutoPause = mutableStateOf(false)
	var secondaryTargetType: TargetType? = null
	var secondaryTargetValue = 0
	var secondaryTargetValueHigh = 0

	enum class WorkoutType {
		BIKE, CLIMB, HIKE, RUN, SNOWBOARDING, SNOWSHOEING, SWIM_OUTDOOR, WALK, WINDSURFING,
		OTHER_INDOOR, OTHER_OUTDOOR
	}
	var workoutType = WorkoutType.OTHER_OUTDOOR
	val exerciseType: ExerciseType
		get() = when(workoutType) {
			WorkoutType.BIKE -> ExerciseType.Companion.BIKING
			WorkoutType.CLIMB -> ExerciseType.Companion.ROCK_CLIMBING
			WorkoutType.HIKE -> ExerciseType.Companion.HIKING
			WorkoutType.RUN -> ExerciseType.Companion.RUNNING
			WorkoutType.SNOWBOARDING -> ExerciseType.Companion.SNOWBOARDING
			WorkoutType.SNOWSHOEING -> ExerciseType.Companion.SNOWSHOEING
			WorkoutType.SWIM_OUTDOOR -> ExerciseType.Companion.SWIMMING_OPEN_WATER
			WorkoutType.WINDSURFING -> ExerciseType.Companion.SAILING
			else -> ExerciseType.Companion.WALKING
		}
	@JvmField
    var caloriesTotal = mutableIntStateOf(0)
	@JvmField
    var distanceTotal = mutableIntStateOf(0)
	@JvmField
    val checkpoints = mutableListOf<Checkpoint>()
	@JvmField
    var durationTotal = mutableLongStateOf(0L)
	@JvmField//TODO after full move to Kotlin, can be "by mutableX"
    var pauseTotal = mutableLongStateOf(0L)
	var pause by mutableLongStateOf(0L)
	@JvmField
    var elevationGainTotal = mutableIntStateOf(0)
	@JvmField
    var elevationLossTotal = mutableIntStateOf(0)
	@JvmField
    var heartRateAvg = mutableIntStateOf(0)
	@JvmField
    var heartRateMax = mutableIntStateOf(0)
	@JvmField
    var speedAvg = mutableDoubleStateOf(0.0)
	@JvmField
    var speedMax = mutableDoubleStateOf(0.0)
	@JvmField
    val locationSamples = SnapshotStateList<LocationSample>()
	@JvmField
    val heartRateSamples = mutableListOf<HeartRateSample>()

	constructor(client: ExerciseClient) {
		Futures.addCallback(
			client.getCapabilitiesAsync(),
			object : FutureCallback<ExerciseCapabilities> {
				override fun onSuccess(result: ExerciseCapabilities) { capabilities = result }
				override fun onFailure(t: Throwable) { logE("WorkoutData.getCapabilitiesAsync: $t") }
			},
			Executors.newSingleThreadExecutor()
		)
	}

	constructor(workoutDataJson: JSONObject) {
		id = workoutDataJson.optLong("id")
		description = workoutDataJson.optString("desc")
		name = workoutDataJson.optString("name")
		setWorkoutTypeString(workoutDataJson.optString("workoutType"))
		durationTotal.longValue = workoutDataJson.optLong("durationTotal", 0)
		distanceTotal.intValue = workoutDataJson.optInt("distanceTotal", 0)
		elevationGainTotal.intValue = workoutDataJson.optInt("elevationGainTotal", 0)
		elevationLossTotal.intValue = workoutDataJson.optInt("elevationLossTotal", 0)
		pauseTotal.longValue = workoutDataJson.optLong("pauseTotal", 0)
		speedAvg.doubleValue = workoutDataJson.optDouble("speedAvg", 0.0)
		speedMax.doubleValue = workoutDataJson.optDouble("speedMax", 0.0)
		heartRateAvg.intValue = workoutDataJson.optInt("heartRateAvg", 0)
		heartRateMax.intValue = workoutDataJson.optInt("heartRateMax", 0)
		caloriesTotal.intValue = workoutDataJson.optInt("caloriesTotal", 0)

		if(workoutDataJson.has("checkpoints")) {
			val checkpointsJson = workoutDataJson.optJSONArray("checkpoints")
			if(checkpointsJson != null) {
				for(i in 0..<checkpointsJson.length()) {
					checkpointsJson.optJSONObject(i)?.let {
						addCheckpoint(
							it.optLong("time"),
							it.optLong("duration")
						)
					}
				}
			}
		}
	}

	class SpeedCachePoint(val timestamp: Long, val distance: Int)
	class Checkpoint(@JvmField val time: Long, @JvmField val duration: Long) {
		fun toJson(): JSONObject = JSONObject()
			.put("time", time)
			.put("duration", duration)
	}
	fun addCheckpoint(time: Long, duration: Long) = checkpoints.add(Checkpoint(time, duration))

	class LocationSample {
		@JvmField
        var time = 0L
		var lat = 0.0
		var lon = 0.0
		@JvmField
        var ele = 0.0
		@JvmField
        var speed = 0.0

		constructor()
		constructor(locationSampleJson: JSONObject) {
			time = locationSampleJson.optLong("time")
			lat = locationSampleJson.optDouble("lat", 0.0)
			lon = locationSampleJson.optDouble("lon", 0.0)
			ele = locationSampleJson.optDouble("ele", 0.0)
			speed = locationSampleJson.optDouble("speed", 0.0)
		}
		fun toLatLong(): LatLong = LatLong(lat, lon)
		fun toJson(): JSONObject = JSONObject()
			.put("time", time)
			.put("lat", lat)
			.put("lon", lon)
			.put("ele", ele)
			.put("speed", speed)
	}

	class HeartRateSample {
		@JvmField
        var time: Long//TODO late init
		@JvmField
        var heartRate: Double
		constructor(time: Long, heartRate: Double) {
			this.time = time
			this.heartRate = heartRate
		}
		constructor(heartRateSampleJson: JSONObject) {
			time = heartRateSampleJson.optLong("time")
			heartRate = heartRateSampleJson.optDouble("heartRate", 0.0)
		}
		fun toJson(): JSONObject = JSONObject()
			.put("time", time)
			.put("heartRate", heartRate)
	}

	val workoutTypeName
		get() = getWorkoutTypeName(workoutType)
	val workoutTypeIcon
		get() = getWorkoutTypeIcon(workoutType)
	val isWorkoutTypeAutoPause
		get() = capabilities?.autoPauseAndResumeEnabledExercises?.contains(exerciseType) == true
	val hasElevation
		get() = capabilities?.getExerciseTypeCapabilities(exerciseType)?.supportedDataTypes?.contains(
			DataType.Companion.ABSOLUTE_ELEVATION
		) == true
	val hasElevationLoss
		get() = capabilities?.getExerciseTypeCapabilities(exerciseType)?.supportedDataTypes?.contains(
			DataType.Companion.ELEVATION_LOSS_TOTAL
		) == true
	val isWorkoutTypeGps
		get() = workoutType != WorkoutType.CLIMB && workoutType != WorkoutType.OTHER_INDOOR
	val isWorkoutTypeEle
		get() = workoutType != WorkoutType.WINDSURFING && workoutType != WorkoutType.SWIM_OUTDOOR

	fun setWorkoutTypeString(workoutTypeString: String) {
		workoutType = when(workoutTypeString) {
			"Biking" -> WorkoutType.BIKE
			"Climbing" -> WorkoutType.CLIMB
			"Hiking" -> WorkoutType.HIKE
			"Running" -> WorkoutType.RUN
			"Snowboarding" -> WorkoutType.SNOWBOARDING
			"Snowshoeing" -> WorkoutType.SNOWSHOEING
			"Swim (outdoor)" -> WorkoutType.SWIM_OUTDOOR
			"Walking" -> WorkoutType.WALK
			"Windsurfing" -> WorkoutType.WINDSURFING
			"Other (indoor)" -> WorkoutType.OTHER_INDOOR
			else -> WorkoutType.OTHER_OUTDOOR
		}
	}

	fun setDescName() {
		if(distanceTotal.intValue > 1000) {
			description =
				Util.distanceDoubleToString(distanceTotal.intValue.toDouble() / 1000) + "km in " + Util.prettyDurationShort(
					durationTotal.longValue
				)
		} else if(distanceTotal.intValue > 10) {
			description = "" + distanceTotal.intValue + "m in " + Util.prettyDurationShort(durationTotal.longValue)
		} else {
			val count = (checkpoints.size + 1) / 2
			description = 
				if(count == 1) { Util.prettyDurationShort(durationTotal.longValue) }
				else {
					count.toString() + " sessions in " + Util.prettyDurationShort(durationTotal.longValue)
				}
		}
		name = sdfGPXName.format(id)
	}
	fun toJsonString(context: Context): String = toJson(context).toString()
	fun toJson(context: Context): JSONObject {
		val workoutDataJson = JSONObject()
		workoutDataJson.put("id", id)
		workoutDataJson.put("name", name)
		workoutDataJson.put("desc", description)
		workoutDataJson.put("workoutType", context.getString(getWorkoutTypeName(workoutType)))
		workoutDataJson.put("durationTotal", durationTotal.longValue)
		if(distanceTotal.intValue > 0) workoutDataJson.put("distanceTotal", distanceTotal.intValue)
		if(elevationGainTotal.intValue > 0) workoutDataJson.put("elevationGainTotal", elevationGainTotal.intValue)
		if(elevationLossTotal.intValue > 0) workoutDataJson.put("elevationLossTotal", elevationLossTotal.intValue)
		if(pauseTotal.longValue > 0) workoutDataJson.put("pauseTotal", pauseTotal.longValue)
		if(speedAvg.doubleValue > 0) workoutDataJson.put("speedAvg", speedAvg.doubleValue)
		if(speedMax.doubleValue > 0) workoutDataJson.put("speedMax", speedMax.doubleValue)
		if(heartRateAvg.intValue > 0) workoutDataJson.put("heartRateAvg", heartRateAvg.intValue)
		if(heartRateMax.intValue > 0) workoutDataJson.put("heartRateMax", heartRateMax.intValue)
		if(caloriesTotal.intValue > 0) workoutDataJson.put("caloriesTotal", caloriesTotal.intValue)
		if(!checkpoints.isEmpty()) {
			val checkpointsJson = JSONArray()
			for(i in checkpoints.indices)
				checkpoints[i].let { checkpointsJson.put(it.toJson()) }
			workoutDataJson.put("checkpoints", checkpointsJson)
		}
		return workoutDataJson
	}
	fun locationsToJsonString(): String {
		val locations = JSONArray()
		for(i in locationSamples.indices)
			locationSamples[i].let { locations.put(it.toJson()) }
		return locations.toString()
	}
	fun locationsFromJson(locations: JSONArray) {
		for(i in 0..<locations.length()) {
			locationSamples.add(LocationSample(locations.getJSONObject(i)))
		}
	}
	fun heartRatesToJsonString(): String {
		val heartRates = JSONArray()
		for(i in heartRateSamples.indices)
			heartRateSamples[i].let { heartRates.put(it.toJson()) }
		return heartRates.toString()
	}
	fun heartRatesFromJson(heartRates: JSONArray) {
		for(i in 0..<heartRates.length()) {
			heartRateSamples.add(HeartRateSample(heartRates.getJSONObject(i)))
		}
	}

	companion object {
		@JvmStatic
        fun getWorkoutTypeName(workoutType: WorkoutType) =
			when(workoutType) {
				WorkoutType.BIKE -> R.string.WorkoutType_BIKE
				WorkoutType.CLIMB -> R.string.WorkoutType_CLIMB
				WorkoutType.HIKE -> R.string.WorkoutType_HIKE
				WorkoutType.RUN -> R.string.WorkoutType_RUN
				WorkoutType.SNOWBOARDING -> R.string.WorkoutType_SNOWBOARDING
				WorkoutType.SNOWSHOEING -> R.string.WorkoutType_SNOWSHOEING
				WorkoutType.SWIM_OUTDOOR -> R.string.WorkoutType_SWIM_OUTDOOR
				WorkoutType.WALK -> R.string.WorkoutType_WALK
				WorkoutType.WINDSURFING -> R.string.WorkoutType_WINDSURFING
				WorkoutType.OTHER_INDOOR -> R.string.WorkoutType_OTHER_INDOOR
				else -> R.string.WorkoutType_OTHER_OUTDOOR
			}

		@JvmStatic
        fun getWorkoutTypeIcon(workoutType: WorkoutType) =
			when(workoutType) {
				WorkoutType.BIKE -> R.drawable.ic_bike
				WorkoutType.CLIMB -> R.drawable.ic_climb
				WorkoutType.HIKE -> R.drawable.ic_hike
				WorkoutType.RUN -> R.drawable.ic_run
				WorkoutType.SNOWBOARDING -> R.drawable.ic_snowboarding
				WorkoutType.SNOWSHOEING -> R.drawable.ic_snowshoeing
				WorkoutType.SWIM_OUTDOOR -> R.drawable.ic_swim
				WorkoutType.WALK -> R.drawable.ic_walk
				WorkoutType.WINDSURFING -> R.drawable.ic_windsurfing
				else -> R.drawable.ic_other
			}
		@JvmStatic
        fun getWorkoutTypeFromInt(workoutTypeInt: Int): WorkoutType? =
			WorkoutType.entries[workoutTypeInt]
	}
}
