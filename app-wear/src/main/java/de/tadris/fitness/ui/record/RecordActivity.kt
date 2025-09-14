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
package de.tadris.fitness.ui.record

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices.getClient
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseState
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.LocationAvailability
import androidx.health.services.client.data.LocationData
import androidx.health.services.client.data.WarmUpConfig
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import de.tadris.fitness.R
import de.tadris.fitness.data.SettingsStore
import de.tadris.fitness.data.SettingsStore.WorkoutSettings
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.WorkoutData.SpeedCachePoint
import de.tadris.fitness.data.WorkoutDataStore
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.record.RecordActivity.SensorStatus
import de.tadris.fitness.ui.workouts.WorkoutActivity
import de.tadris.fitness.util.logD
import de.tadris.fitness.util.logE
import de.tadris.fitness.util.runInBackground
import de.tadris.fitness.util.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

var currentWorkoutTypeIcon: Int = R.drawable.ic_run
val SensorStatus.isDisabled: Boolean
	get() = this == SensorStatus.DISABLED
class RecordActivity : ComponentActivity() {
	enum class SensorStatus { UNAVAILABLE, AVAILABLE, DISABLED }
	var locationStatus by mutableStateOf(SensorStatus.DISABLED)
	var lastLocationTime = 0L
	var heartRateStatus by mutableStateOf(SensorStatus.DISABLED)
	var heartRate by mutableDoubleStateOf(0.0)
	var lastSpeed by mutableDoubleStateOf(0.0)
	val speedCache = mutableListOf<SpeedCachePoint>()
	var lastElevation by mutableDoubleStateOf(0.0)
	enum class TargetAchieved { NOT, HALF, FULL }
	val targetAchieved = mutableStateOf(TargetAchieved.NOT)
	val targetProgress = mutableFloatStateOf(0.0F)
	lateinit var workoutData: WorkoutData
	var exerciseState by mutableStateOf(ExerciseState.Companion.PREPARING)

	lateinit var workoutSettings: WorkoutSettings

	var bootTime = 0L
	var durationJob: Job? = null
	var checkpointTime = 0L
	var checkpointDuration = 0L
	var cooldownDurationJob: Job? = null
	var cooldownDuration by mutableLongStateOf(0)

	lateinit var exerciseClient: ExerciseClient
	lateinit var vibrator: Vibrator
	val vePattern: VibrationEffect =
		VibrationEffect.createWaveform(longArrayOf(300, 500, 300, 500, 300, 500), -1)

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent { FOTTheme { RecordView(this) } }

		bootTime = System.currentTimeMillis() - SystemClock.elapsedRealtime()

		vibrator = if(Build.VERSION.SDK_INT >= 31) {
			(getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
		} else {
			getSystemService("vibrator") as Vibrator
		}

		val healthServicesClient = getClient(this)
		exerciseClient = healthServicesClient.exerciseClient
		exerciseClient.setUpdateCallback(exerciseUpdateCallback)

		workoutData = WorkoutData(exerciseClient)
		workoutData.workoutType = SettingsStore.getLastWorkoutType(this)
		workoutSettings = WorkoutSettings(this, workoutData.workoutType)

		checkPermissions()
		prepareExercise()
	}
	public override fun onDestroy() {
		super.onDestroy()
		durationJob?.cancel()
		cooldownDurationJob?.cancel()
		if(!exerciseState.isEnding && !exerciseState.isEnded)
			exerciseClient.endExerciseAsync()
	}

	fun prepareExercise() {
		try {
			if(locationStatus == SensorStatus.UNAVAILABLE &&
				!(getSystemService(LOCATION_SERVICE) as LocationManager)
					.isProviderEnabled(LocationManager.GPS_PROVIDER)
			){
				locationStatus = SensorStatus.DISABLED
				toast(R.string.record_location_off)
			}
		} catch(e: Exception) { logE("RecordActivity.checkPermissions: " + e.message) }

		if(locationStatus.isDisabled && heartRateStatus.isDisabled) return
		val dataTypes = mutableSetOf<DeltaDataType<*, *>>()
		if(!locationStatus.isDisabled) dataTypes.add(DataType.Companion.LOCATION)
		if(!heartRateStatus.isDisabled) dataTypes.add(DataType.Companion.HEART_RATE_BPM)

		Futures.addCallback(
			exerciseClient.prepareExerciseAsync(
				WarmUpConfig(workoutData.exerciseType, dataTypes)
			),
			startCallback,
			Dispatchers.IO.asExecutor()
		)
	}
	fun startExercise() {
		workoutData.id = System.currentTimeMillis()
		targetAchieved.value = TargetAchieved.NOT
		val configBuilder = ExerciseConfig.Builder(workoutData.exerciseType)
		val autoPause = workoutData.isWorkoutTypeAutoPause && workoutSettings.autoPause.value
		workoutSettings.autoPause.value = autoPause
		configBuilder.setIsAutoPauseAndResumeEnabled(autoPause)

		//https://developer.android.com/reference/androidx/health/services/client/data/DataType
		val dataTypes = mutableSetOf<DataType<*, *>>()
		if(!locationStatus.isDisabled && workoutData.isWorkoutTypeGps) {
			if(workoutData.isWorkoutTypeEle) {
				dataTypes.add(DataType.Companion.ELEVATION_GAIN_TOTAL)
				if(workoutData.hasElevationLoss) dataTypes.add(DataType.Companion.ELEVATION_LOSS_TOTAL)
				if(workoutData.hasElevation) dataTypes.add(DataType.Companion.ABSOLUTE_ELEVATION)
			}
			dataTypes.add(DataType.Companion.DISTANCE_TOTAL)
			dataTypes.add(DataType.Companion.LOCATION)
			dataTypes.add(DataType.Companion.SPEED_STATS)
			configBuilder.setIsGpsEnabled(true)
		}
		if(!heartRateStatus.isDisabled) {
			dataTypes.add(DataType.Companion.CALORIES_TOTAL)
			dataTypes.add(DataType.Companion.HEART_RATE_BPM)
			dataTypes.add(DataType.Companion.HEART_RATE_BPM_STATS)
		}

		if(dataTypes.isEmpty()) {
			toast(R.string.error_no_permissions)
			return
		}

		configBuilder.setDataTypes(dataTypes)
		val exerciseClient = exerciseClient
		exerciseClient.let {
			Futures.addCallback(
				exerciseClient.startExerciseAsync(configBuilder.build()),
				startCallback,
				Dispatchers.IO.asExecutor()
			)
		}
		startDurationJob()
		setScreenOnFlag()

		currentWorkoutTypeIcon = workoutData.workoutTypeIcon
		startForegroundService(Intent(this, RecordService::class.java))
	}
	fun pauseResumeClicked() =
		if(exerciseState.isPaused) exerciseClient.resumeExerciseAsync()
		else pauseExercise()
	fun pauseExercise() {
		exerciseClient.pauseExerciseAsync()
		speedCache.clear()
	}
	fun endExercise() {
		exerciseClient.clearUpdateCallbackAsync(exerciseUpdateCallback)
		stopService(Intent(this, RecordService::class.java))
		durationJob?.cancel()
		WorkoutDataStore.storeWorkout(this, workoutData)
		runInBackground {
			SettingsStore.storeLastWorkoutType(this, workoutData.workoutType)
		}
	}
	fun showSummary() {
		exerciseClient.endExerciseAsync()
		startActivity(Intent(this, WorkoutActivity::class.java))
		finishAndRemoveTask()
	}
	val isAutoEnding: Boolean
		get() = exerciseState.isEnding && exerciseState !== ExerciseState.Companion.ENDING

	fun timeToDuration(time: Long): Long = time - checkpointTime + checkpointDuration

	fun setScreenOnFlag() {
		if(workoutSettings.screenOn.value) {
			window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		} else {
			window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		}
	}
	fun forceScreenOn() = window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

	fun startDurationJob(){
		durationJob = CoroutineScope(Dispatchers.Main).launch {
			while(isActive){
				if(checkpointTime == 0L){
					delay(200L)
					continue
				}
				val currentTime = System.currentTimeMillis()
				if(exerciseState.isPaused){
					workoutData.pause =
						currentTime - checkpointTime + workoutData.pauseTotal.longValue
					delay(1000 - (workoutData.pauseTotal.longValue % 1000))
					continue
				}else{
					workoutData.durationTotal.longValue =
						currentTime - checkpointTime + checkpointDuration
					if(workoutData.primaryTargetType == WorkoutData.TargetType.DURATION &&
						workoutData.primaryTargetValue > 0
					) {
						updateTargetProgress((workoutData.durationTotal.longValue / 600 / workoutData.primaryTargetValue).toInt())
					}
					delay(1000 - (workoutData.durationTotal.longValue % 1000))
				}
			}
		}
	}

	val exerciseUpdateCallback = object : ExerciseUpdateCallback {
		override fun onRegistrationFailed(throwable: Throwable) {
			logE("RecordActivity.ExerciseUpdateCallback.onRegistrationFailed: $throwable")
		}
		override fun onRegistered() {}
		override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {}
		override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
			if(exerciseState != update.exerciseStateInfo.state) {
				logD("RecordActivity State change: " + exerciseState + " > " + update.exerciseStateInfo.state)
				exerciseState = update.exerciseStateInfo.state
				if(isAutoEnding) {
					logE("RecordActivity.onExerciseUpdateReceived: isAutoEnding")
					endExercise()
					return
				}
			}
			if(exerciseState == ExerciseState.Companion.ENDED) return

			val activeDurationCheckpoint = update.activeDurationCheckpoint
			if(activeDurationCheckpoint != null) {
				if(activeDurationCheckpoint.time.toEpochMilli() > checkpointTime) {
					val checkpointTimePrev = checkpointTime
					val checkpointDurationPrev = checkpointDuration
					checkpointTime = activeDurationCheckpoint.time.toEpochMilli()
					checkpointDuration = activeDurationCheckpoint.activeDuration.toMillis()
					workoutData.addCheckpoint(checkpointTime, checkpointDuration)
					if(checkpointDurationPrev == checkpointDuration && checkpointDuration > 0) {
						//resumed after a pause
						workoutData.pauseTotal.longValue =
							System.currentTimeMillis() - checkpointTimePrev + workoutData.pauseTotal.longValue
					}
				}
			}
			val dataPointContainer = update.latestMetrics
			for(dataPoint in dataPointContainer.sampleDataPoints) {
				when(dataPoint.dataType) {
					DataType.Companion.HEART_RATE_BPM -> {
						val sample = WorkoutData.HeartRateSample(
							dataPoint.timeDurationFromBoot.plusMillis(bootTime).toMillis(),
							dataPoint.value as Double
						)
						heartRate = sample.heartRate
						if(!exerciseState.isPaused) workoutData.heartRateSamples.add(sample)
					}
					DataType.Companion.LOCATION -> {
						val sample = WorkoutData.LocationSample()
						sample.time = dataPoint.timeDurationFromBoot.plusMillis(bootTime).toMillis()
						sample.lat = (dataPoint.value as LocationData).latitude
						sample.lon = (dataPoint.value as LocationData).longitude
						sample.ele = lastElevation
						workoutData.locationSamples.add(sample)
						lastLocationTime = sample.time
						sample.speed = lastSpeed
					}
					DataType.Companion.ABSOLUTE_ELEVATION ->
						lastElevation = dataPoint.value as Double
					else -> logD("Unknown SampleDataPoint: " + dataPoint.dataType + " dataPoint: $dataPoint")
				}
			}
			for(dataPoint in dataPointContainer.statisticalDataPoints) {
				when(dataPoint.dataType) {
					DataType.Companion.HEART_RATE_BPM_STATS -> {
						workoutData.heartRateAvg.intValue = (dataPoint.average as Double).toInt()
						workoutData.heartRateMax.intValue = (dataPoint.max as Double).toInt()
					}
					DataType.Companion.SPEED_STATS ->
						workoutData.speedAvg.doubleValue = dataPoint.average as Double
					else -> logD("Unknown StatisticalDataPoint: " + dataPoint.dataType + " dataPoint: $dataPoint")
				}
			}
			for(dataPoint in dataPointContainer.cumulativeDataPoints) {
				when(dataPoint.dataType) {
					DataType.Companion.CALORIES_TOTAL -> {
						workoutData.caloriesTotal.intValue = (dataPoint.total as Double).toInt()
					}
					DataType.Companion.DISTANCE_TOTAL -> {
						val distance = (dataPoint.total as Double).toInt()
						workoutData.distanceTotal.intValue = distance
						updateLastSpeed(distance)
						if(workoutData.primaryTargetType == WorkoutData.TargetType.DISTANCE &&
							workoutData.primaryTargetValue > 0
						) {
							updateTargetProgress((distance/10/workoutData.primaryTargetValue))
						}
					}
					DataType.Companion.ELEVATION_GAIN_TOTAL ->
						workoutData.elevationGainTotal.intValue = (dataPoint.total as Double).toInt()
					DataType.Companion.ELEVATION_LOSS_TOTAL ->
						workoutData.elevationLossTotal.intValue = (dataPoint.total as Double).toInt()
					else ->
						logD("Unknown CumulativeDataPoint: " + dataPoint.dataType + " dataPoint: $dataPoint")
				}
			}
		}

		override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {
			if(dataType == DataType.Companion.LOCATION) {
				locationStatus = if(
					availability == LocationAvailability.Companion.ACQUIRED_TETHERED ||
					availability == LocationAvailability.Companion.ACQUIRED_UNTETHERED
				) SensorStatus.AVAILABLE
				else SensorStatus.UNAVAILABLE
			} else if(dataType == DataType.Companion.HEART_RATE_BPM) {
				heartRateStatus = if(availability == DataTypeAvailability.Companion.AVAILABLE)
					SensorStatus.AVAILABLE
				else SensorStatus.UNAVAILABLE
			}
		}
	}

	fun updateLastSpeed(distance: Int) {
		if(lastLocationTime == 0L || distance == 0) return
		speedCache.add(SpeedCachePoint(lastLocationTime, distance))
		if(speedCache.size < 10) return  //wait for the speed cache to fill up a bit
		var distanceDiff = (distance - speedCache[0].distance).toDouble() //meters
		val timeDiff = (lastLocationTime - speedCache[0].timestamp).toDouble() //milliseconds
		if(distanceDiff < 4 || timeDiff < 1) {
			lastSpeed = 0.0
			return
		}
		lastSpeed = (distanceDiff * 1000) / timeDiff //meters/second

		if(lastSpeed > workoutData.speedMax.doubleValue)
			workoutData.speedMax.doubleValue = lastSpeed
		while(speedCache.size > 30 && distanceDiff > 200.0) {
			speedCache.removeAt(0)
			distanceDiff = (distance - speedCache[0].distance).toDouble()
		}
	}
	fun updateTargetProgress(value: Int) {
		targetProgress.floatValue = if(value >= 100) (value.toFloat() - 100) / 100 else value.toFloat() / 100
		if(targetAchieved.value == TargetAchieved.NOT && value >= 50) {
			targetAchieved.value = TargetAchieved.HALF
			vibrator.vibrate(vePattern)
		} else if(targetAchieved.value == TargetAchieved.HALF && value >= 100) {
			targetAchieved.value = TargetAchieved.FULL
			vibrator.vibrate(vePattern)
			if(workoutData.targetAutoPause.value) pauseExercise()
		}
	}
	val startCallback = object : FutureCallback<Void?> {
		override fun onSuccess(result: Void?) {}
		override fun onFailure(throwable: Throwable) {
			logE("RecordActivity.startCallback: $throwable")
			runOnUiThread {
				toast(R.string.error_start_exercise)
				finishAndRemoveTask()
			}
		}
	}

	fun startCooldown(){
		forceScreenOn()
		exerciseClient.setUpdateCallback(coolDownUpdateCallback)
		checkpointTime = System.currentTimeMillis()
		checkpointDuration = 0
		cooldownDurationJob = CoroutineScope(Dispatchers.Main).launch {
			while(isActive){
				cooldownDuration = System.currentTimeMillis() - checkpointTime + checkpointDuration
				delay(1000 - (cooldownDuration % 1000))
			}
		}
	}
	val coolDownUpdateCallback = object : ExerciseUpdateCallback {
		override fun onRegistrationFailed(throwable: Throwable) {
			logE("RecordActivity.coolDownUpdateCallback.onRegistrationFailed: $throwable")
		}
		override fun onRegistered() {}
		override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {}
		override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {}
		override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
			for(dataPoint in update.latestMetrics.sampleDataPoints) {
				when(dataPoint.dataType) {
					DataType.Companion.HEART_RATE_BPM -> {
						val sample = WorkoutData.HeartRateSample(
							dataPoint.timeDurationFromBoot.plusMillis(bootTime).toMillis(),
							dataPoint.value as Double
						)
						heartRate = sample.heartRate
					}
				}
			}
		}
	}
	fun checkPermissions() {
		locationStatus = if(
			hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
			hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)
		) SensorStatus.UNAVAILABLE
		else SensorStatus.DISABLED
		heartRateStatus = if(
			hasPermission(Manifest.permission.BODY_SENSORS)
		) SensorStatus.UNAVAILABLE
		else SensorStatus.DISABLED
		if(Build.VERSION.SDK_INT >= 33) {
			if(locationStatus.isDisabled || heartRateStatus.isDisabled ||
				!hasPermission(Manifest.permission.POST_NOTIFICATIONS)
			) {
				requestPermissions.launch(arrayOf(
					Manifest.permission.POST_NOTIFICATIONS,
					Manifest.permission.ACCESS_FINE_LOCATION,
					Manifest.permission.ACTIVITY_RECOGNITION,
					Manifest.permission.BODY_SENSORS
				))
			}
		} else { //30
			if(locationStatus.isDisabled || heartRateStatus.isDisabled) {
				requestPermissions.launch(arrayOf(
					Manifest.permission.ACCESS_FINE_LOCATION,
					Manifest.permission.ACTIVITY_RECOGNITION,
					Manifest.permission.BODY_SENSORS
				))
			}
		}
	}
	fun hasPermission(permission: String): Boolean =
		checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
	val requestPermissions =
		registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
			permissions.entries.forEach {
				if (it.value && it.key == Manifest.permission.ACCESS_FINE_LOCATION)
					locationStatus = SensorStatus.UNAVAILABLE
				if (it.value && it.key == Manifest.permission.BODY_SENSORS)
					heartRateStatus = SensorStatus.UNAVAILABLE
			}
			if (locationStatus == SensorStatus.UNAVAILABLE ||
				heartRateStatus == SensorStatus.UNAVAILABLE
			) prepareExercise()
		}
}
