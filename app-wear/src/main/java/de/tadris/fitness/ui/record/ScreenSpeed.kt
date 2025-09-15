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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.tooling.preview.devices.WearDevices
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.WorkoutData.TargetType
import de.tadris.fitness.ui.Chart
import de.tadris.fitness.ui.ChartData
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.Field
import de.tadris.fitness.ui.Main
import de.tadris.fitness.ui.TargetProgress
import de.tadris.fitness.ui.record.RecordActivity.TargetAchieved
import de.tadris.fitness.ui.secsToKeep
import de.tadris.fitness.util.isSeconds
import de.tadris.fitness.util.prettyDurationShort
import de.tadris.fitness.util.prettySpeed

@Composable
fun ScreenSpeed(
	modifier: Modifier,
	workoutData: WorkoutData,
	speed: Double,
	timeToDuration: (Long) -> Long?,
	targetProgress: MutableFloatState,
	targetAchieved: MutableState<TargetAchieved>,
	isPaused: Boolean
) {
	val speedData = remember { mutableStateListOf<ChartData>() }
	LaunchedEffect(workoutData.locationSamples.isNotEmpty()) {
		if(speedData.isEmpty() && workoutData.locationSamples.isNotEmpty()){
			val locationSamples = workoutData.locationSamples
			val secsToKeep = secsToKeep(workoutData.durationTotal)
			val tempList = locationSamples
				.filter { it.speed > 0 }
				.mapNotNull {
					val duration = timeToDuration(it.time)
					if(duration?.isSeconds(secsToKeep) != true) null
					else ChartData(duration, it.speed)
				}
			speedData.addAll(tempList)
		}
	}
	LaunchedEffect(speed) {
		if(speedData.size > 1000){
			val tempList = speedData.toMutableList()
			val secsToKeep = secsToKeep(workoutData.durationTotal)
			tempList.removeIf { !it.duration.isSeconds(secsToKeep) }
			speedData.retainAll(tempList)
		}
		if(speed > 0){
			speedData.add(ChartData(
				duration = workoutData.durationTotal,
				value = speed
			))
		}
	}
	Column(modifier.fillMaxSize().padding(top = 20.dp)) {
		Field(
			modifier = Modifier.fillMaxWidth().weight(1f),
			value = speed.prettySpeed(),
			unitText = R.string.field_kmh,
			isActive = !isPaused
		)
		Box(
			modifier = Modifier.fillMaxWidth().weight(3f).padding(
				start = if(Main.isScreenRound) Main.vw10 else 0.dp,
				end = if(Main.isScreenRound) Main.vw12 else 0.dp
			)
		){
			Chart(
				data = speedData,
				target1 =
					if(workoutData.primaryTargetType == TargetType.SPEED)
						workoutData.primaryTargetValue.toDouble() / 36
					else null,
				target2 =
					if(workoutData.secondaryTargetType == TargetType.SPEED)
						workoutData.secondaryTargetValue.toDouble() / 36
					else null,
				yMinInit = 0.0,
				yMaxInit = 1.11,
				isSpeed = true
			)
		}
		Field(
			modifier = Modifier.weight(1f).padding(horizontal = Main.vw25),
			value = workoutData.durationTotal.prettyDurationShort(),
			isActive = !isPaused
		)
	}
	TargetProgress(targetProgress, targetAchieved)
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun PreviewScreenSpeed() {
	Main.GetScreenSpecs()
	val workoutData = WorkoutData()
	val speedData = mutableListOf<ChartData>()
	speedData.add(ChartData(0, 2.2))
	speedData.add(ChartData(10000, 2.2))
	speedData.add(ChartData(30000, 2.2))
	AppScaffold { FOTTheme {
		ScreenSpeed(
			Modifier,
			workoutData,
			speed = 2.2,
			timeToDuration = { it },
			targetProgress = remember { mutableFloatStateOf(0.9F) },
			targetAchieved = remember { mutableStateOf(TargetAchieved.NOT) },
			isPaused = false
		)
	} }
}
