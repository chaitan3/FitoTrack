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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.tooling.preview.devices.WearDevices
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.ui.Chart
import de.tadris.fitness.ui.ChartData
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.FadingHorizontalDivider
import de.tadris.fitness.ui.FadingVerticalDivider
import de.tadris.fitness.ui.Field
import de.tadris.fitness.ui.Main
import de.tadris.fitness.ui.TargetProgress
import de.tadris.fitness.ui.record.RecordActivity.TargetAchieved
import de.tadris.fitness.util.pretty
import de.tadris.fitness.util.prettyDurationShort

@Composable
fun ScreenElevation(
	modifier: Modifier,
	workoutData: WorkoutData,
	elevation: Double,
	timeToDuration: (Long) -> Long?,
	targetProgress: MutableFloatState,
	targetAchieved: MutableState<TargetAchieved>,
	isPaused: Boolean
) {
	val elevationData = remember { mutableStateListOf<ChartData>() }
	var firstElevation by remember { mutableDoubleStateOf(0.0) }
	LaunchedEffect(workoutData.locationSamplesFiltered.size) {
		if(elevationData.isEmpty() && workoutData.locationSamplesFiltered.isNotEmpty()){
			val tempList = workoutData.locationSamplesFiltered
				.filter { it.ele != 0.0 }
				.mapNotNull {
					ChartData(
						duration = timeToDuration(it.time) ?: return@mapNotNull null,
						value = it.ele
					)
				}
			elevationData.addAll(tempList)
			firstElevation = elevationData.first().value
		}else{
			workoutData.locationSamplesFiltered.lastOrNull()?.let {
				if(it.ele == 0.0) return@let
				elevationData.add(ChartData(
					duration = timeToDuration(it.time) ?: return@let,
					value = it.ele
				))
			}
		}
	}

	Column(modifier.fillMaxSize().padding(top = 20.dp)) {
		Field(
			modifier = Modifier.weight(1f),
			value = elevation.pretty(),
			unitText = R.string.field_m,
			isActive = !isPaused
		)
		Box(
			modifier = Modifier.fillMaxWidth().weight(3f).padding(
				start = if(Main.isScreenRound) Main.vw7 else 0.dp,
				end = if(Main.isScreenRound) Main.vw12 else 0.dp
			)
		){ Chart(
			data = elevationData,
			yMinInit = firstElevation-5,
			yMaxInit = firstElevation+5
		) }
		Row(
			modifier = Modifier.weight(1f).padding(
				horizontal = if(Main.isScreenRound) Main.vw10 else 0.dp
			),
			verticalAlignment = Alignment.CenterVertically
		) {
			val showGain = workoutData.elevationGainTotal >= 10 || workoutData.elevationLossTotal < 10
			val showLoss = workoutData.elevationLossTotal >= 10 || workoutData.elevationGainTotal < 10
			if(showGain) {
				Field(
					modifier = Modifier.weight(1f).padding(horizontal =
						if(!showLoss) Main.vw12 else 0.dp
					),
					value = workoutData.elevationGainTotal.toString(),
					unitText = R.string.field_m,
					unitIcon = R.drawable.ic_uphill,
					isActive = !isPaused
				)
			}
			if(showLoss) {
				if(showGain) FadingVerticalDivider()
				Field(
					modifier = Modifier.weight(1f).padding(horizontal =
						if(!showGain) Main.vw12 else 0.dp
					),
					value = workoutData.elevationLossTotal.toString(),
					unitText = R.string.field_m,
					unitIcon = R.drawable.ic_downhill,
					isActive = !isPaused
				)
			}
		}
		FadingHorizontalDivider()
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
fun PreviewScreenElevation() {
	Main.GetScreenSpecs()
	val workoutData = WorkoutData()
	workoutData.durationTotal = 8400000L
	workoutData.elevationLossTotal = 100
	workoutData.elevationGainTotal = 100
	var locationSample = WorkoutData.LocationSample()
	locationSample.time = 1000L
	locationSample.ele = 50.0
	workoutData.locationSamplesFiltered.add(locationSample)
	locationSample = WorkoutData.LocationSample()
	locationSample.time = 4400000L
	locationSample.ele = 150.0
	workoutData.locationSamplesFiltered.add(locationSample)
	locationSample = WorkoutData.LocationSample()
	locationSample.time = 8400000L
	locationSample.ele = 30.0
	workoutData.locationSamplesFiltered.add(locationSample)
	AppScaffold { FOTTheme {
		ScreenElevation(
			Modifier,
			workoutData,
			elevation = 150.0,
			timeToDuration = { it },
			targetProgress = remember { mutableFloatStateOf(0.9F) },
			targetAchieved = remember { mutableStateOf(TargetAchieved.NOT) },
			isPaused = false
		)
	} }
}
@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun PreviewScreenElevationLarge() { PreviewScreenElevation() }
