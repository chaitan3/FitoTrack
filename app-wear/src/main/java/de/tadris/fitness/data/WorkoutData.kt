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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.health.services.client.data.ExerciseType
import de.tadris.fitness.R
import de.tadris.fitness.util.prettyDistance
import de.tadris.fitness.util.prettyDurationShort
import de.tadris.fitness.util.tryIgnore
import org.json.JSONArray
import org.json.JSONObject
import org.mapsforge.core.model.LatLong
import java.text.SimpleDateFormat
import java.util.Locale

class WorkoutData {
	val sdfGPXName = SimpleDateFormat("E dd MMM HH:mm", Locale.getDefault())
    var id = 0L
		set(value) {
			field = value
			for(i in legs.indices -1) legs[i].nextLeg = legs[i+1]
		}
    var description = ""
    var name = ""

	enum class TargetType { DISTANCE, DURATION, SPEED, HEART_RATE }
	var primaryTargetType: TargetType? = null
	var primaryTargetValue: Number = 0
	var primaryTargetValueHigh: Number = 0
	var targetAutoPause by mutableStateOf(false)
	var secondaryTargetType: TargetType? = null
	var secondaryTargetValue: Number = 0
	var secondaryTargetValueHigh: Number = 0

	enum class WorkoutType {
		CLIMB, CYCLE, HIKE, RUN, SNOWBOARDING, SNOWSHOEING, SWIM_OUTDOOR, WALK, WINDSURFING,
		OTHER_INDOOR, OTHER_OUTDOOR, MULTISPORT
	}
	var workoutType = WorkoutType.OTHER_OUTDOOR
	val exerciseType: ExerciseType
		get() = when(workoutType) {
			WorkoutType.CLIMB -> ExerciseType.Companion.ROCK_CLIMBING
			WorkoutType.CYCLE -> ExerciseType.Companion.BIKING
			WorkoutType.HIKE -> ExerciseType.Companion.HIKING
			WorkoutType.RUN -> ExerciseType.Companion.RUNNING
			WorkoutType.SNOWBOARDING -> ExerciseType.Companion.SNOWBOARDING
			WorkoutType.SNOWSHOEING -> ExerciseType.Companion.SNOWSHOEING
			WorkoutType.SWIM_OUTDOOR -> ExerciseType.Companion.SWIMMING_OPEN_WATER
			WorkoutType.WINDSURFING -> ExerciseType.Companion.SAILING
			else -> ExerciseType.Companion.WALKING
		}
    var calories by mutableIntStateOf(0)
    var caloriesTotal by mutableIntStateOf(0)
    var distance by mutableIntStateOf(0)
    var distanceTotal by mutableIntStateOf(0)
    val checkpoints = mutableListOf<Checkpoint>()
    var duration by mutableLongStateOf(0L)
    var durationTotal by mutableLongStateOf(0L)
    var pauseTotal by mutableLongStateOf(0L)
	var pause by mutableLongStateOf(0L)
	var endTime = 0L
    var elevationGain by mutableIntStateOf(0)
    var elevationGainTotal by mutableIntStateOf(0)
    var elevationLoss by mutableIntStateOf(0)
    var elevationLossTotal by mutableIntStateOf(0)
    var heartRateAvg by mutableIntStateOf(0)
    var heartRateMax by mutableIntStateOf(0)
    var speedAvg by mutableDoubleStateOf(0.0)
    var speedMax by mutableDoubleStateOf(0.0)
    val locationSamples = mutableListOf<LocationSample>()
    val locationSamplesFiltered = mutableStateListOf<LocationSample>()
    val heartRateSamples = mutableListOf<HeartRateSample>()
	val heartRateSamplesFiltered = mutableStateListOf<HeartRateSample>()

	var rootWorkoutId: Long? = null
	var rootWorkout: WorkoutData? = null
	val legs = SnapshotStateList<WorkoutData>()
	var nextLeg: WorkoutData? = null

	constructor()
	constructor(workoutDataJson: JSONObject) {
		id = workoutDataJson.optLong("id")
		description = workoutDataJson.optString("desc")
		name = workoutDataJson.optString("name")
		setWorkoutTypeString(workoutDataJson.optString("workoutType"))
		durationTotal = workoutDataJson.optLong("durationTotal", 0)
		pauseTotal = workoutDataJson.optLong("pauseTotal", 0)
		endTime = workoutDataJson.optLong("endTime", 0)
		distanceTotal = workoutDataJson.optInt("distanceTotal", 0)
		elevationGainTotal = workoutDataJson.optInt("elevationGainTotal", 0)
		elevationLossTotal = workoutDataJson.optInt("elevationLossTotal", 0)
		speedAvg = workoutDataJson.optDouble("speedAvg", 0.0)
		speedMax = workoutDataJson.optDouble("speedMax", 0.0)
		heartRateAvg = workoutDataJson.optInt("heartRateAvg", 0)
		heartRateMax = workoutDataJson.optInt("heartRateMax", 0)
		caloriesTotal = workoutDataJson.optInt("caloriesTotal", 0)

		workoutDataJson.optJSONArray("checkpoints")?.let{ checkpoints ->
			for(i in 0..<checkpoints.length()) {
				checkpoints.optJSONObject(i)?.let { checkpoint ->
					addCheckpoint(
						checkpoint.optLong("time"),
						checkpoint.optLong("duration")
					)
				}
			}
		}
		tryIgnore {
			primaryTargetType = TargetType.valueOf(workoutDataJson.optString("primaryTargetType"))
			primaryTargetValue = convertTargetValue(workoutDataJson.optInt("primaryTargetValue", 0), primaryTargetType)
			primaryTargetValueHigh = convertTargetValue(workoutDataJson.optInt("primaryTargetValueHigh", 0), primaryTargetType)
		}
		tryIgnore {
			secondaryTargetType = TargetType.valueOf(workoutDataJson.optString("secondaryTargetType"))
			secondaryTargetValue = convertTargetValue(workoutDataJson.optInt("primaryTargetValue", 0), secondaryTargetType)
			secondaryTargetValueHigh = convertTargetValue(workoutDataJson.optInt("secondaryTargetValueHigh", 0), secondaryTargetType)
		}
		rootWorkoutId = workoutDataJson.optLong("rootWorkoutId")
		if(rootWorkoutId == 0L) rootWorkoutId = null
	}

	class SpeedCachePoint(val timestamp: Long, val distance: Int)
	class Checkpoint(val time: Long, val duration: Long) {
		fun toJson(): JSONObject = JSONObject()
			.put("time", time)
			.put("duration", duration)
	}
	fun addCheckpoint(time: Long, duration: Long) = checkpoints.add(Checkpoint(time, duration))

	class LocationSample {
        var time = 0L
		var lat = 0.0
		var lon = 0.0
        var ele = 0.0
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
        var time: Long
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

	val workoutTypeIcon
		get() = getWorkoutTypeIcon(workoutType)
	val isWorkoutTypeAutoPause
		get() = WorkoutCapabilities.isWorkoutTypeAutoPause(exerciseType)
	val hasElevation
		get() = WorkoutCapabilities.hasElevation(exerciseType)
	val hasElevationLoss
		get() = WorkoutCapabilities.hasElevationLoss(exerciseType)
	val isWorkoutTypeGps
		get() = workoutType != WorkoutType.CLIMB && workoutType != WorkoutType.OTHER_INDOOR
	val isWorkoutTypeEle
		get() = workoutType != WorkoutType.WINDSURFING && workoutType != WorkoutType.SWIM_OUTDOOR

	fun setWorkoutTypeString(workoutTypeString: String) {
		workoutType = when(workoutTypeString) {
			"Climbing" -> WorkoutType.CLIMB
			"Cycling" -> WorkoutType.CYCLE
			"Hiking" -> WorkoutType.HIKE
			"Running" -> WorkoutType.RUN
			"Snowboarding" -> WorkoutType.SNOWBOARDING
			"Snowshoeing" -> WorkoutType.SNOWSHOEING
			"Swim (outdoor)" -> WorkoutType.SWIM_OUTDOOR
			"Walking" -> WorkoutType.WALK
			"Windsurfing" -> WorkoutType.WINDSURFING
			"Other (indoor)" -> WorkoutType.OTHER_INDOOR
			"Multisport" -> WorkoutType.MULTISPORT
			else -> WorkoutType.OTHER_OUTDOOR
		}
	}

	fun setDescName() {
		if(distanceTotal >= 1000) {
			description = distanceTotal.prettyDistance() + "km in " +
						durationTotal.prettyDurationShort()
		} else if(distanceTotal > 10) {
			description = distanceTotal.prettyDistance() + "m in " +
					durationTotal.prettyDurationShort()
		} else {
			val count = (checkpoints.size + 1) / 2
			description = 
				if(count == 1) durationTotal.prettyDurationShort()
				else count.toString() + " sessions in " + durationTotal.prettyDurationShort()
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
		workoutDataJson.put("durationTotal", durationTotal)
		workoutDataJson.put("pauseTotal", pauseTotal)
		workoutDataJson.put("endTime", endTime)
		if(distanceTotal > 0) workoutDataJson.put("distanceTotal", distanceTotal)
		if(elevationGainTotal > 0) workoutDataJson.put("elevationGainTotal", elevationGainTotal)
		if(elevationLossTotal > 0) workoutDataJson.put("elevationLossTotal", elevationLossTotal)
		if(speedAvg > 0) workoutDataJson.put("speedAvg", speedAvg)
		if(speedMax > 0) workoutDataJson.put("speedMax", speedMax)
		if(heartRateAvg > 0) workoutDataJson.put("heartRateAvg", heartRateAvg)
		if(heartRateMax > 0) workoutDataJson.put("heartRateMax", heartRateMax)
		if(caloriesTotal > 0) workoutDataJson.put("caloriesTotal", caloriesTotal)
		val checkpointsJson = JSONArray()
		checkpoints.forEach { checkpointsJson.put(it.toJson()) }
		workoutDataJson.put("checkpoints", checkpointsJson)
		if(primaryTargetType != null){
			workoutDataJson.put("primaryTargetType", primaryTargetType.toString())
			workoutDataJson.put("primaryTargetValue", convertTargetValue(primaryTargetValue, primaryTargetType))
			workoutDataJson.put("primaryTargetValueHigh", convertTargetValue(primaryTargetValueHigh, primaryTargetType))
		}
		if(secondaryTargetType != null){
			workoutDataJson.put("secondaryTargetType", secondaryTargetType.toString())
			workoutDataJson.put("secondaryTargetValue", convertTargetValue(secondaryTargetValue, secondaryTargetType))
			workoutDataJson.put("secondaryTargetValueHigh", convertTargetValue(secondaryTargetValueHigh, secondaryTargetType))
		}
		if(rootWorkoutId != null) workoutDataJson.put("rootWorkoutId", rootWorkoutId)
		else if(rootWorkout != null) workoutDataJson.put("rootWorkoutId", rootWorkout?.id)
		return workoutDataJson
	}
	fun locationsToJsonString(): String =
		locationSamples.toList().joinToString(prefix = "[", postfix = "]", separator = ",") {
			it.toJson().toString()
		}
	fun locationsFromJson(locations: JSONArray, filter: Boolean = false) {
		(0..<locations.length())
			.asSequence()
			.filter { !filter || it % 10 == 0 }
			.map { LocationSample(locations.getJSONObject(it)) }
			.toList()
			.let { locationSamples.addAll(it) }
	}
	fun heartRatesToJsonString(): String =
		heartRateSamples.joinToString(prefix = "[", postfix = "]", separator = ",") {
			it.toJson().toString()
		}
	fun heartRatesFromJson(heartRates: JSONArray, filter: Boolean = false) {
		(0..<heartRates.length())
			.asSequence()
			.filter { !filter || it % 5 == 0 }
			.map { HeartRateSample(heartRates.getJSONObject(it)) }
			.toList()
			.let { heartRateSamples.addAll(it) }
	}

	companion object {
        fun getWorkoutTypeName(workoutType: WorkoutType) =
			when(workoutType) {
				WorkoutType.CLIMB -> R.string.WorkoutType_CLIMB
				WorkoutType.CYCLE -> R.string.WorkoutType_CYCLE
				WorkoutType.HIKE -> R.string.WorkoutType_HIKE
				WorkoutType.RUN -> R.string.WorkoutType_RUN
				WorkoutType.SNOWBOARDING -> R.string.WorkoutType_SNOWBOARDING
				WorkoutType.SNOWSHOEING -> R.string.WorkoutType_SNOWSHOEING
				WorkoutType.SWIM_OUTDOOR -> R.string.WorkoutType_SWIM_OUTDOOR
				WorkoutType.WALK -> R.string.WorkoutType_WALK
				WorkoutType.WINDSURFING -> R.string.WorkoutType_WINDSURFING
				WorkoutType.OTHER_INDOOR -> R.string.WorkoutType_OTHER_INDOOR
				WorkoutType.MULTISPORT -> R.string.WorkoutType_MULTISPORT
				else -> R.string.WorkoutType_OTHER_OUTDOOR
			}

        fun getWorkoutTypeIcon(workoutType: WorkoutType) =
			when(workoutType) {
				WorkoutType.CLIMB -> R.drawable.ic_climb
				WorkoutType.CYCLE -> R.drawable.ic_cycle
				WorkoutType.HIKE -> R.drawable.ic_hike
				WorkoutType.RUN -> R.drawable.ic_run
				WorkoutType.SNOWBOARDING -> R.drawable.ic_snowboarding
				WorkoutType.SNOWSHOEING -> R.drawable.ic_snowshoeing
				WorkoutType.SWIM_OUTDOOR -> R.drawable.ic_swim
				WorkoutType.WALK -> R.drawable.ic_walk
				WorkoutType.WINDSURFING -> R.drawable.ic_windsurfing
				WorkoutType.MULTISPORT -> R.drawable.ic_multisport
				else -> R.drawable.ic_other
			}
        fun getWorkoutTypeFromInt(workoutTypeInt: Int): WorkoutType? =
			WorkoutType.entries[workoutTypeInt]
		fun convertTargetValue(value: Int, targetType: TargetType?): Number =
			when(targetType) {
				TargetType.DISTANCE -> value * 1000
				TargetType.DURATION -> value * 60000L
				TargetType.SPEED -> value / 36.0
				else -> value
			}
		fun convertTargetValue(value: Number, targetType: TargetType?): Int =
			when(targetType) {
				TargetType.DISTANCE -> (value.toInt() / 1000)
				TargetType.DURATION -> (value.toLong() / 60000L).toInt()
				TargetType.SPEED -> (value.toDouble() * 36.0).toInt()
				else -> value.toInt()
			}
	}
}
