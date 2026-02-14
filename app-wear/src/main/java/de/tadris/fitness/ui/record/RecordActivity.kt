/*
 * Copyright (c) 2026 Bart Vullings <dev@windkracht8.com>
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
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.health.services.client.data.ExerciseState.Companion.ACTIVE
import androidx.health.services.client.data.ExerciseState.Companion.ENDED
import androidx.health.services.client.data.ExerciseState.Companion.ENDING
import androidx.health.services.client.data.ExerciseState.Companion.PREPARING
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.LocationAccuracy
import androidx.health.services.client.data.LocationData
import androidx.health.services.client.data.WarmUpConfig
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import de.tadris.fitness.R
import de.tadris.fitness.data.SettingsStore
import de.tadris.fitness.data.SettingsStore.WorkoutTypeSettings
import de.tadris.fitness.data.WorkoutCapabilities
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.WorkoutData.SpeedCachePoint
import de.tadris.fitness.data.WorkoutData.TargetType
import de.tadris.fitness.data.WorkoutDataStore
import de.tadris.fitness.data.WorkoutDataStore.writeWorkout
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.record.RecordActivity.SensorStatus
import de.tadris.fitness.ui.workouts.WorkoutActivity
import de.tadris.fitness.util.logD
import de.tadris.fitness.util.logE
import de.tadris.fitness.util.runInBackground
import de.tadris.fitness.util.toast
import de.tadris.fitness.util.toastFromBG
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
	var locationAccuracy by mutableDoubleStateOf(100.0)
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
	var exerciseState by mutableStateOf(PREPARING)

	lateinit var workoutTypeSettings: WorkoutTypeSettings

	var bootTime = 0L
	var durationJob: Job? = null
	var targetCheckJob: Job? = null
	var targetCheckType: TargetType? = null
	var targetCheckValue: Number = 0
	var targetCheckValueHigh: Number = 0
	var checkpointTime = 0L
	var checkpointDuration = 0L
	var cooldownDurationJob: Job? = null
	var cooldownDuration by mutableLongStateOf(0)

	lateinit var exerciseClient: ExerciseClient
	lateinit var vibrator: Vibrator
	val veTargetBelow: VibrationEffect =
		VibrationEffect.createWaveform(longArrayOf(0, 500, 300, 500), -1)
	val veTargetHalf: VibrationEffect =
		VibrationEffect.createWaveform(longArrayOf(0, 500, 300, 800), -1)
	val veTargetFull: VibrationEffect =
		VibrationEffect.createWaveform(longArrayOf(0, 500, 300, 500, 300, 800), -1)
	var targetNotifyEnabled by mutableIntStateOf(0)
	var textToSpeech: TextToSpeech? = null
	lateinit var audioManager: AudioManager
	lateinit var audioFocusRequest: AudioFocusRequest

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
		if(WorkoutCapabilities.capabilities == null) WorkoutCapabilities.init(exerciseClient)

		workoutData = WorkoutData()
		workoutData.workoutType = SettingsStore.getLastWorkoutType(this)
		if(intent.hasExtra("resumeId")) {
			val resumeId = intent.getLongExtra("resumeId", 0)
			WorkoutDataStore.workouts.find{ it.id == resumeId }?.let {
				workoutData = it
				workoutData.calories = workoutData.caloriesTotal
				workoutData.distance = workoutData.distanceTotal
				workoutData.duration = workoutData.durationTotal
				workoutData.elevationGain = workoutData.elevationGainTotal
				workoutData.elevationLoss = workoutData.elevationLossTotal
				checkpointTime = workoutData.checkpoints.lastOrNull()?.time ?: 0
				checkpointDuration = workoutData.checkpoints.lastOrNull()?.duration ?: 0
				runInBackground {
					workoutData.locationSamples.clear()
					workoutData.locationSamplesFiltered.clear()
					WorkoutDataStore.readLocations(context = this, workoutData, filter = true)
					workoutData.locationSamplesFiltered.addAll(workoutData.locationSamples)
					workoutData.locationSamples.clear()

					workoutData.heartRateSamples.clear()
					workoutData.heartRateSamplesFiltered.clear()
					WorkoutDataStore.readHeartRates(context = this, workoutData, filter = true)
					workoutData.heartRateSamplesFiltered.addAll(workoutData.heartRateSamples)
					workoutData.heartRateSamples.clear()
				}
			}
		}
		workoutTypeSettings = SettingsStore.getWorkoutTypeSettings(this, workoutData.workoutType)
		setScreenOnFlag()
		targetNotifyEnabled = SettingsStore.getTargetNotifyEnabled(this)
		if(targetNotifyEnabled == 1) initTextToSpeech()

		checkPermissions()
		prepareExercise()
	}
	public override fun onDestroy() {
		super.onDestroy()
		durationJob?.cancel()
		targetCheckJob?.cancel()
		cooldownDurationJob?.cancel()
		if(!exerciseState.isEnding && !exerciseState.isEnded)
			exerciseClient.endExerciseAsync()
		textToSpeech?.shutdown()
	}

	fun prepareExercise() {
		try {
			if(locationStatus == SensorStatus.UNAVAILABLE &&
				!(getSystemService(LOCATION_SERVICE) as LocationManager)
					.isProviderEnabled(LocationManager.GPS_PROVIDER)
			){
				locationStatus = SensorStatus.DISABLED
				toast(R.string.location_off)
			}
		} catch(e: Exception) { logE("RecordActivity.checkPermissions: " + e.message) }

		if(locationStatus.isDisabled && heartRateStatus.isDisabled) return
		val dataTypes = mutableSetOf<DeltaDataType<*, *>>()
		if(!locationStatus.isDisabled) dataTypes.add(DataType.LOCATION)
		if(!heartRateStatus.isDisabled) dataTypes.add(DataType.HEART_RATE_BPM)

		Futures.addCallback(
			exerciseClient.prepareExerciseAsync(
				WarmUpConfig(workoutData.exerciseType, dataTypes)
			),
			startCallback,
			Dispatchers.IO.asExecutor()
		)
	}
	fun startExercise() {
		if(workoutData.legs.isNotEmpty()){
			workoutData.id = System.currentTimeMillis()-1
			workoutData = workoutData.legs[0]
		}
		setTargetCheck()

		exerciseClient.setUpdateCallback(exerciseUpdateCallback)
		targetAchieved.value = TargetAchieved.NOT
		val configBuilder = ExerciseConfig.Builder(workoutData.exerciseType)
		workoutTypeSettings = SettingsStore.getWorkoutTypeSettings(this, workoutData.workoutType)
		setScreenOnFlag()
		val autoPause = workoutData.isWorkoutTypeAutoPause && workoutTypeSettings.autoPause.value
		workoutTypeSettings.autoPause.value = autoPause
		configBuilder.setIsAutoPauseAndResumeEnabled(autoPause)

		//https://developer.android.com/reference/androidx/health/services/client/data/DataType
		val dataTypes = mutableSetOf<DataType<*, *>>()
		if(!locationStatus.isDisabled && workoutData.isWorkoutTypeGps) {
			if(workoutData.isWorkoutTypeEle) {
				dataTypes.add(DataType.ELEVATION_GAIN_TOTAL)
				if(workoutData.hasElevationLoss) dataTypes.add(DataType.ELEVATION_LOSS_TOTAL)
				if(workoutData.hasElevation) dataTypes.add(DataType.ABSOLUTE_ELEVATION)
			}
			dataTypes.add(DataType.DISTANCE_TOTAL)
			dataTypes.add(DataType.LOCATION)
			dataTypes.add(DataType.SPEED_STATS)
			configBuilder.setIsGpsEnabled(true)
		}
		if(!heartRateStatus.isDisabled) {
			dataTypes.add(DataType.CALORIES_TOTAL)
			dataTypes.add(DataType.HEART_RATE_BPM)
			dataTypes.add(DataType.HEART_RATE_BPM_STATS)
		}

		if(dataTypes.isEmpty()) {
			toast(R.string.error_no_permissions)
			return
		}

		startDurationJob()
		setScreenOnFlag()

		configBuilder.setDataTypes(dataTypes)
		Futures.addCallback(
			exerciseClient.startExerciseAsync(configBuilder.build()),
			startCallback,
			Dispatchers.IO.asExecutor()
		)

		currentWorkoutTypeIcon = workoutData.workoutTypeIcon
		startForegroundService(Intent(this, RecordService::class.java))
		runInBackground {
			SettingsStore.storeLastWorkoutType(this, workoutData.workoutType)
		}
	}
	fun pauseResumeClicked() =
		if(exerciseState.isPaused) exerciseClient.resumeExerciseAsync()
		else pauseExercise()
	fun pauseExercise() {
		exerciseClient.pauseExerciseAsync()
		speedCache.clear()
	}
	fun endExercise() {
		val endTimestamp = System.currentTimeMillis()
		exerciseClient.clearUpdateCallbackAsync(exerciseUpdateCallback)
		stopService(Intent(this, RecordService::class.java))
		durationJob?.cancel()
		targetCheckJob?.cancel()
		WorkoutDataStore.storeWorkout(this, workoutData) {
			toastFromBG(R.string.error_workout_write)
		}
		workoutData.endTime = endTimestamp
		workoutData.rootWorkout?.let { rootWorkout ->
			if(workoutData.nextLeg == null) {
				rootWorkout.endTime = endTimestamp
				rootWorkout.durationTotal = System.currentTimeMillis() - rootWorkout.id
				rootWorkout.distanceTotal = rootWorkout.legs.sumOf { it.distanceTotal }
				rootWorkout.speedAvg = rootWorkout.distanceTotal * 1000.0 / rootWorkout.durationTotal
				rootWorkout.speedMax = rootWorkout.legs.maxOf { it.speedMax }
				rootWorkout.heartRateAvg = rootWorkout.legs.sumOf { it.heartRateAvg } / rootWorkout.legs.size
				rootWorkout.heartRateMax = rootWorkout.legs.maxOf { it.heartRateMax }
				rootWorkout.caloriesTotal = rootWorkout.legs.sumOf { it.caloriesTotal }
				WorkoutDataStore.storeWorkout(this, rootWorkout){
					toastFromBG(R.string.error_workout_write)
				}
			} else {
				Futures.addCallback(
					exerciseClient.endExerciseAsync(),
					restartCallback,
					Dispatchers.IO.asExecutor()
				)
				speedCache.clear()
				checkpointTime = 0L
				checkpointDuration = 0L
			}
		}
	}
	val restartCallback = object : FutureCallback<Void?> {
		override fun onSuccess(result: Void?) {
			runInBackground {
				delay(2000)
				prepareExercise()
			}
		}
		override fun onFailure(throwable: Throwable) {
			logE("RecordActivity.restartCallback: $throwable")
		}
	}
	fun showSummary() {
		exerciseClient.endExerciseAsync()
		startActivity(Intent(this, WorkoutActivity::class.java))
		finishAndRemoveTask()
	}
	val isAutoEnding: Boolean
		get() = exerciseState.isEnding && exerciseState !== ENDING

	fun timeToDuration(time: Long): Long? {
		if(time > checkpointTime) return time - checkpointTime + checkpointDuration
		if(workoutData.checkpoints.isEmpty()) return null
		var checkpoint = workoutData.checkpoints[0]
		for(i in 1..<workoutData.checkpoints.size) {
			if(workoutData.checkpoints[i].time > time) {
				if(workoutData.checkpoints[i].duration == checkpoint.duration) { return null }//during a break, don't show
				else { break }
			}
			checkpoint = workoutData.checkpoints[i]
		}
		return time - checkpoint.time + checkpoint.duration
	}

	fun setScreenOnFlag() {
		if(workoutTypeSettings.screenOn.value) {
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
					workoutData.pause = currentTime - checkpointTime + workoutData.pauseTotal
					delay(1000 - (workoutData.pauseTotal % 1000))
					continue
				}else{
					workoutData.durationTotal = currentTime - checkpointTime + checkpointDuration
					if(workoutData.primaryTargetType == TargetType.DURATION && workoutData.primaryTargetValue.toLong() > 0) {
						updateTargetProgress(workoutData.durationTotal.toFloat()/workoutData.primaryTargetValue.toLong())
					} else if(targetCheckType == TargetType.DURATION && workoutData.durationTotal > 60000) {
						if(workoutData.durationTotal <= targetCheckValue.toLong() * targetProgress.floatValue) {
							targetCheckJob?.cancel()
						} else if(targetCheckJob?.isActive != true) {
							setTargetCheckJob()
						}
					}
					if((workoutData.durationTotal/1000) % 300 == 0L) {
						workoutData.setDescName()
						runInBackground { writeWorkout(
							context = this@RecordActivity,
							workoutData,
							onError = { toastFromBG(R.string.error_workout_write) })
						}
					}
					delay(1000 - (workoutData.durationTotal % 1000))
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
			if(exerciseState == ENDED) return

			val activeDurationCheckpoint = update.activeDurationCheckpoint
			if(activeDurationCheckpoint != null) {
				if(activeDurationCheckpoint.time.toEpochMilli() > checkpointTime) {
					val checkpointTimePrev = checkpointTime
					val checkpointDurationPrev = checkpointDuration
					checkpointTime = activeDurationCheckpoint.time.toEpochMilli()
					checkpointDuration = activeDurationCheckpoint.activeDuration.toMillis() +
							workoutData.duration
					workoutData.addCheckpoint(checkpointTime, checkpointDuration)
					if(checkpointDurationPrev == checkpointDuration && checkpointDuration > 0) {
						//resumed after a pause
						workoutData.pauseTotal += System.currentTimeMillis() - checkpointTimePrev
					}
					if(workoutData.id == 0L) workoutData.id = checkpointTime
					if(workoutData.endTime > 0L) {
						workoutData.pauseTotal += checkpointTime - workoutData.endTime
						workoutData.pause = workoutData.pauseTotal
						workoutData.endTime = 0L
					}
				}
			}
			val dataPointContainer = update.latestMetrics
			for(dataPoint in dataPointContainer.sampleDataPoints) {
				when(dataPoint.dataType) {
					DataType.HEART_RATE_BPM -> {
						val sample = WorkoutData.HeartRateSample(
							dataPoint.timeDurationFromBoot.plusMillis(bootTime).toMillis(),
							dataPoint.value as Double
						)
						heartRate = sample.heartRate
						//logD("Got heartRate update: $exerciseState $heartRate $exerciseState")
						if(exerciseState == ACTIVE){
							workoutData.heartRateSamples.add(sample)
							if((workoutData.heartRateSamplesFiltered.lastOrNull()?.time ?: 0) < sample.time - 10000) {
								workoutData.heartRateSamplesFiltered.add(sample)
							}
							if(targetCheckType == TargetType.HEART_RATE && workoutData.durationTotal > 60000){
								if(heartRate >= targetCheckValue.toDouble() &&
									heartRate <= targetCheckValueHigh.toDouble()
								) {
									targetCheckJob?.cancel()
								} else if(targetCheckJob?.isActive != true) {
									setTargetCheckJob()
								}
							}
						}
					}
					DataType.LOCATION -> {
						val sample = WorkoutData.LocationSample()
						sample.time = dataPoint.timeDurationFromBoot.plusMillis(bootTime).toMillis()
						sample.lat = (dataPoint.value as LocationData).latitude
						sample.lon = (dataPoint.value as LocationData).longitude
						sample.ele = lastElevation

						//logD("Got location update: $exerciseState " + sample.toLatLong() + dataPoint.accuracy + dataPoint.metadata)
						if(locationStatus == SensorStatus.UNAVAILABLE){
							locationAccuracy = (dataPoint.accuracy as LocationAccuracy).horizontalPositionErrorMeters
							if(locationAccuracy < 10) locationStatus = SensorStatus.AVAILABLE
						}
						if(exerciseState == ACTIVE) {
							workoutData.locationSamples.add(sample)
							lastLocationTime = sample.time
							sample.speed = lastSpeed
							if((workoutData.locationSamplesFiltered.lastOrNull()?.time ?: 0) < sample.time - 10000) {
								workoutData.locationSamplesFiltered.add(sample)
							}
						}
					}
					DataType.ABSOLUTE_ELEVATION -> lastElevation = dataPoint.value as Double
					else -> logD("Unknown SampleDataPoint: " + dataPoint.dataType + " dataPoint: $dataPoint")
				}
			}
			for(dataPoint in dataPointContainer.statisticalDataPoints) {
				when(dataPoint.dataType) {
					DataType.HEART_RATE_BPM_STATS -> {
						workoutData.heartRateAvg = (dataPoint.average as Double).toInt()
						workoutData.heartRateMax = (dataPoint.max as Double).toInt()
					}
					DataType.SPEED_STATS ->
						workoutData.speedAvg = dataPoint.average as Double
					else -> logD("Unknown StatisticalDataPoint: " + dataPoint.dataType + " dataPoint: $dataPoint")
				}
			}
			for(dataPoint in dataPointContainer.cumulativeDataPoints) {
				when(dataPoint.dataType) {
					DataType.CALORIES_TOTAL -> {
						workoutData.caloriesTotal = (dataPoint.total as Double).toInt() +
								workoutData.calories
					}
					DataType.DISTANCE_TOTAL -> {
						val distance = (dataPoint.total as Double).toInt()
						workoutData.distanceTotal = distance + workoutData.distance
						updateLastSpeed()
						if(workoutData.primaryTargetType == TargetType.DISTANCE) {
							updateTargetProgress(workoutData.distanceTotal.toFloat()/workoutData.primaryTargetValue.toInt())
						}
						if(targetCheckType == TargetType.DISTANCE && workoutData.durationTotal > 60000){
							if(workoutData.distanceTotal >= targetCheckValue.toInt() * targetProgress.floatValue) {
								targetCheckJob?.cancel()
							} else if(targetCheckJob?.isActive != true) {
								setTargetCheckJob()
							}
						}
					}
					DataType.ELEVATION_GAIN_TOTAL ->
						workoutData.elevationGainTotal = (dataPoint.total as Double).toInt() +
								workoutData.elevationGain
					DataType.ELEVATION_LOSS_TOTAL ->
						workoutData.elevationLossTotal = (dataPoint.total as Double).toInt() +
								workoutData.elevationLoss
					else ->
						logD("Unknown CumulativeDataPoint: " + dataPoint.dataType + " dataPoint: $dataPoint")
				}
			}
		}

		override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {
			if(dataType == DataType.HEART_RATE_BPM) {
				heartRateStatus = if(availability == DataTypeAvailability.AVAILABLE)
					SensorStatus.AVAILABLE
				else SensorStatus.UNAVAILABLE
			}
		}
	}

	fun updateLastSpeed() {
		if(lastLocationTime == 0L || workoutData.distanceTotal == 0) return
		speedCache.add(SpeedCachePoint(lastLocationTime, workoutData.distanceTotal))
		if(speedCache.size < 10) return  //wait for the speed cache to fill up a bit
		var distanceDiff = (workoutData.distanceTotal - speedCache[0].distance).toDouble() //meters
		val timeDiff = (lastLocationTime - speedCache[0].timestamp).toDouble() //milliseconds
		if(distanceDiff < 4 || timeDiff < 1) {
			lastSpeed = 0.0
			return
		}
		lastSpeed = (distanceDiff * 1000) / timeDiff //meters/second

		if(lastSpeed > workoutData.speedMax) workoutData.speedMax = lastSpeed
		while (speedCache.size > 30 && distanceDiff > 200.0) {
			speedCache.removeAt(0)
			distanceDiff = (workoutData.distanceTotal - speedCache[0].distance).toDouble()
		}
		if(targetCheckType != TargetType.SPEED || workoutData.durationTotal < 60000) return
		if(lastSpeed >= targetCheckValue.toDouble()) {
			targetCheckJob?.cancel()
		} else if(targetCheckJob?.isActive != true) {
			setTargetCheckJob()
		}
	}
	fun setTargetCheckJob() {
		if(targetNotifyEnabled < 1 ||
			(workoutData.primaryTargetType == targetCheckType && targetNotifyEnabled < 2)
		) return
		targetCheckJob = CoroutineScope(Dispatchers.Main).launch {
			delay(5000)
			var delay = 30000L
			while(isActive){
				logD("RecordActivity.setTargetCheckJob notify below")
				vibrator.vibrate(veTargetBelow)
				if(targetCheckType == TargetType.HEART_RATE && heartRate > targetCheckValueHigh.toInt()) {
					speak(R.string.speak_target_above)
				} else {
					speak(R.string.speak_target_below)
				}
				delay(delay)
				delay *= 2
			}
		}
	}

	fun setTargetCheck() {
		targetCheckJob?.cancel()
		targetCheckType = null
		if(workoutData.primaryTargetType == TargetType.SPEED) {
			targetCheckType = TargetType.SPEED
			targetCheckValue = workoutData.primaryTargetValue
		} else if(workoutData.primaryTargetType == TargetType.HEART_RATE) {
			targetCheckType = TargetType.HEART_RATE
			targetCheckValue = workoutData.primaryTargetValue
			targetCheckValueHigh = workoutData.primaryTargetValueHigh
		} else if(workoutData.secondaryTargetType == TargetType.SPEED) {
			targetCheckType = TargetType.SPEED
			targetCheckValue = workoutData.secondaryTargetValue
		} else if(workoutData.secondaryTargetType == TargetType.HEART_RATE) {
			targetCheckType = TargetType.HEART_RATE
			targetCheckValue = workoutData.secondaryTargetValue
			targetCheckValueHigh = workoutData.secondaryTargetValueHigh
		} else if(workoutData.secondaryTargetType == TargetType.DURATION) {
			targetCheckType = TargetType.DURATION
			targetCheckValue = workoutData.secondaryTargetValue
		} else if(workoutData.secondaryTargetType == TargetType.DISTANCE) {
			targetCheckType = TargetType.DISTANCE
			targetCheckValue = workoutData.secondaryTargetValue
		}
	}
	fun updateTargetProgress(value: Float) {
		targetProgress.floatValue = value.coerceIn(0f, 1f)
		if(targetAchieved.value == TargetAchieved.NOT && value >= 0.5) {
			targetAchieved.value = TargetAchieved.HALF
			if(targetNotifyEnabled >= 1) {
				logD("RecordActivity.updateTargetProgress notify half")
				vibrator.vibrate(veTargetHalf)
				speak(R.string.speak_target_half)
			}
		} else if(targetAchieved.value == TargetAchieved.HALF && value >= 1) {
			targetAchieved.value = TargetAchieved.FULL
			if(workoutData.targetAutoPause) pauseExercise()
			if(targetNotifyEnabled >= 1) {
				logD("RecordActivity.updateTargetProgress notify full")
				vibrator.vibrate(veTargetFull)
				speak(R.string.speak_target_full)
			}
		}
	}
	val startCallback = object : FutureCallback<Void?> {
		override fun onSuccess(result: Void?) {}
		override fun onFailure(throwable: Throwable) {
			logE("RecordActivity.startCallback: $throwable")
			CoroutineScope(Dispatchers.Main).launch {
				toast(R.string.error_start_exercise)
				finishAndRemoveTask()
			}
		}
	}

	fun toggleTargetNotify(){
        when (targetNotifyEnabled) {
            1 -> {
				targetNotifyEnabled = 2
				SettingsStore.storeTargetNotify(this, targetNotifyEnabled)
            }
            2 -> {
				targetNotifyEnabled = 0
				SettingsStore.storeTargetNotify(this, targetNotifyEnabled)
				textToSpeech?.stop()
				textToSpeech?.shutdown()
				textToSpeech = null
            }
            else -> {
                targetNotifyEnabled = 1
                SettingsStore.storeTargetNotify(this, targetNotifyEnabled)
                initTextToSpeech()
            }
        }
	}
	fun initTextToSpeech(){
		textToSpeech = TextToSpeech(this) { status ->
			if(status == TextToSpeech.SUCCESS) {
				textToSpeech?.setOnUtteranceProgressListener(object: UtteranceProgressListener() {
					override fun onStart(utteranceId: String?) {}
					override fun onError(utteranceId: String?) {//Deprecated, but cannot be removed...
						logE("RecordActivity.initTextToSpeech.onError")
						audioManager.abandonAudioFocusRequest(audioFocusRequest)
					}
					override fun onError(utteranceId: String?, errorCode: Int) {
						logE("RecordActivity.initTextToSpeech.onError: $errorCode")
						audioManager.abandonAudioFocusRequest(audioFocusRequest)
					}
					override fun onDone(utteranceId: String?) {
						audioManager.abandonAudioFocusRequest(audioFocusRequest)
					}
				})
			}
		}
		audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
		audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
			.setAudioAttributes(
				AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
					.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
					.build()
			)
			.build()
	}
	fun speak(text: Int){
		logD("speak: ${getString(text)}")
		audioManager.requestAudioFocus(audioFocusRequest)
		textToSpeech?.speak(
			getString(text),
			TextToSpeech.QUEUE_FLUSH,
			Bundle(),
			this.hashCode().toString() + System.currentTimeMillis()
		)
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
					DataType.HEART_RATE_BPM -> {
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
				if(it.value && it.key == Manifest.permission.ACCESS_FINE_LOCATION)
					locationStatus = SensorStatus.UNAVAILABLE
				if(it.value && it.key == Manifest.permission.BODY_SENSORS)
					heartRateStatus = SensorStatus.UNAVAILABLE
			}
			if(locationStatus == SensorStatus.UNAVAILABLE ||
				heartRateStatus == SensorStatus.UNAVAILABLE
			) prepareExercise()
		}
}
