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
	LaunchedEffect(workoutData.locationSamplesFiltered.size) {
		if(speedData.isEmpty() && workoutData.locationSamplesFiltered.isNotEmpty()){
			val tempList = workoutData.locationSamplesFiltered
				.filter { it.speed > 0 }
				.mapNotNull {
					ChartData(
						duration = timeToDuration(it.time) ?: return@mapNotNull null,
						value = it.speed
					)
				}
			speedData.addAll(tempList)
		}else{
			workoutData.locationSamplesFiltered.lastOrNull()?.let {
				if(it.speed == 0.0) return@let
				speedData.add(ChartData(
					duration = timeToDuration(it.time) ?: return@let,
					value = it.speed
				))
			}
		}
	}

	Column(modifier.fillMaxSize().padding(top = 20.dp)) {
		Field(
			modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = Main.vw25),
			value = speed.prettySpeed(),
			unitIcon = R.drawable.kmh,
			unitIconWeight = 1.3f,
			isActive = !isPaused
		)
		Box(
			modifier = Modifier.fillMaxWidth().weight(3f).padding(
				start = if(Main.isScreenRound) Main.vw7 else 0.dp,
				end = if(Main.isScreenRound) Main.vw12 else 0.dp
			)
		){
			Chart(
				data = speedData,
				target1 =
					if(workoutData.primaryTargetType == TargetType.SPEED)
						workoutData.primaryTargetValue.toDouble()
					else null,
				target2 =
					if(workoutData.secondaryTargetType == TargetType.SPEED)
						workoutData.secondaryTargetValue.toDouble()
					else null,
				yMinInit = 0.0,
				yMaxInit = 1.11,
				isSpeed = true
			)
		}
		Field(
			modifier = Modifier.weight(1f).padding(
				bottom = if(Main.isScreenRound) Main.vw5 else 0.dp
			),
			value = workoutData.durationTotal.prettyDurationShort(),
			isActive = !isPaused
		)
	}
	TargetProgress(targetProgress, targetAchieved)
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewScreenSpeed() {
	Main.GetScreenSpecs()
	val workoutData = WorkoutData()
	workoutData.durationTotal = 8400000L
	var locationSample = WorkoutData.LocationSample()
	locationSample.time = 1000L
	locationSample.speed = 2.0
	workoutData.locationSamplesFiltered.add(locationSample)
	locationSample = WorkoutData.LocationSample()
	locationSample.time = 4400000L
	locationSample.speed = 3.0
	workoutData.locationSamplesFiltered.add(locationSample)
	locationSample = WorkoutData.LocationSample()
	locationSample.time = 8400000L
	locationSample.speed = 2.0
	workoutData.locationSamplesFiltered.add(locationSample)
	AppScaffold { FOTTheme {
		ScreenSpeed(
			Modifier,
			workoutData,
			speed = 6.2,
			timeToDuration = { it },
			targetProgress = remember { mutableFloatStateOf(0.9F) },
			targetAchieved = remember { mutableStateOf(TargetAchieved.NOT) },
			isPaused = false
		)
	} }
}
@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun PreviewScreenSpeedLarge() { PreviewScreenSpeed() }
