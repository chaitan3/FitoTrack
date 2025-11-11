package de.tadris.fitness.data

import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseCapabilities
import androidx.health.services.client.data.ExerciseType
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import de.tadris.fitness.util.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

object WorkoutCapabilities {
	var capabilities: ExerciseCapabilities? = null
	fun init(client: ExerciseClient) {
		Futures.addCallback(
			client.getCapabilitiesAsync(),
			object : FutureCallback<ExerciseCapabilities> {
				override fun onSuccess(result: ExerciseCapabilities) { capabilities = result }
				override fun onFailure(t: Throwable) { logE("WorkoutData.getCapabilitiesAsync: $t") }
			},
			Dispatchers.IO.asExecutor()
		)
	}
	fun isWorkoutTypeAutoPause(exerciseType: ExerciseType) =
		capabilities?.autoPauseAndResumeEnabledExercises?.contains(exerciseType) == true
	fun hasElevation(exerciseType: ExerciseType) =
		capabilities?.getExerciseTypeCapabilities(exerciseType)?.supportedDataTypes?.contains(
			DataType.ABSOLUTE_ELEVATION
		) == true
	fun hasElevationLoss(exerciseType: ExerciseType) =
		capabilities?.getExerciseTypeCapabilities(exerciseType)?.supportedDataTypes?.contains(
			DataType.ELEVATION_LOSS_TOTAL
		) == true
}
