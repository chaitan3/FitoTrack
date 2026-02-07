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
import de.tadris.fitness.util.pretty
import de.tadris.fitness.util.prettyDurationShort

@Composable
fun ScreenHeartRate(
	modifier: Modifier,
	workoutData: WorkoutData,
	heartRate: Double,
	timeToDuration: (Long) -> Long?,
	targetProgress: MutableFloatState,
	targetAchieved: MutableState<TargetAchieved>,
	isPaused: Boolean
) {
	val heartRateData = remember { mutableStateListOf<ChartData>() }
	LaunchedEffect(workoutData.heartRateSamplesFiltered.size) {
		if(heartRateData.isEmpty() && workoutData.heartRateSamplesFiltered.isNotEmpty()){
			val tempList = workoutData.heartRateSamplesFiltered
				.filter { it.heartRate > 30 }
				.mapNotNull {
					ChartData(
						duration = timeToDuration(it.time) ?: return@mapNotNull null,
						value = it.heartRate
					)
				}
			heartRateData.addAll(tempList)
		}else{
			workoutData.heartRateSamplesFiltered.lastOrNull()?.let {
				if(it.heartRate < 30) return@let
				heartRateData.add(ChartData(
					duration = timeToDuration(it.time) ?: return@let,
					value = it.heartRate
				))
			}
		}
	}
	Column(modifier.fillMaxSize().padding(top = 20.dp, bottom =
			if(Main.isScreenRound) 5.dp else 0.dp
	)) {
		Field(
			modifier = Modifier.fillMaxWidth().weight(1f),
			value = heartRate.pretty(),
			unitText = R.string.field_bpm
		)
		Box(
			modifier = Modifier.fillMaxWidth().weight(3f).padding(
					start = if(Main.isScreenRound) Main.vw7 else 0.dp,
					end = if(Main.isScreenRound) Main.vw12 else 0.dp
				)
		){
			val target1 =
				if(workoutData.primaryTargetType == TargetType.HEART_RATE) workoutData.primaryTargetValue.toDouble()
				else if(workoutData.secondaryTargetType == TargetType.HEART_RATE) workoutData.secondaryTargetValue.toDouble()
				else null
			val target2 =
				if(workoutData.primaryTargetType == TargetType.HEART_RATE) workoutData.primaryTargetValueHigh.toDouble()
				else if(workoutData.secondaryTargetType == TargetType.HEART_RATE) workoutData.secondaryTargetValueHigh.toDouble()
				else null
			Chart(
				data = heartRateData,
				target1,
				target2,
				yMinInit = 50.0,
				yMaxInit = 100.0
			)
		}
		Field(
			modifier = Modifier.weight(1f).padding(
				bottom = if(Main.isScreenRound) Main.vh3 else 0.dp
			),
			value = workoutData.durationTotal.prettyDurationShort(),
			isActive = !isPaused
		)
	}
	TargetProgress(targetProgress, targetAchieved)
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewScreenHeartRate() {
	Main.GetScreenSpecs()
	val workoutData = WorkoutData()
	workoutData.durationTotal = 8400000L
	var heartRateSample = WorkoutData.HeartRateSample(1000L, 70.0)
	workoutData.heartRateSamplesFiltered.add(heartRateSample)
	heartRateSample = WorkoutData.HeartRateSample(4400000L, 100.0)
	workoutData.heartRateSamplesFiltered.add(heartRateSample)
	heartRateSample = WorkoutData.HeartRateSample(8400000L, 90.0)
	workoutData.heartRateSamplesFiltered.add(heartRateSample)
	AppScaffold { FOTTheme {
		ScreenHeartRate(
			Modifier,
			workoutData,
			heartRate = 70.0,
			timeToDuration = { it },
			targetProgress = remember { mutableFloatStateOf(0.9F) },
			targetAchieved = remember { mutableStateOf(TargetAchieved.NOT) },
			isPaused = false
		)
	} }
}
@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun PreviewScreenHeartRateLarge() { PreviewScreenHeartRate() }
