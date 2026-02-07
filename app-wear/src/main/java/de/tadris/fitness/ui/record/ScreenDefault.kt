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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.tooling.preview.devices.WearDevices
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.ui.FadingHorizontalDivider
import de.tadris.fitness.ui.FadingVerticalDivider
import de.tadris.fitness.ui.Field
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.Main
import de.tadris.fitness.ui.TargetProgress
import de.tadris.fitness.ui.record.RecordActivity.TargetAchieved
import de.tadris.fitness.util.pretty
import de.tadris.fitness.util.prettyDistance
import de.tadris.fitness.util.prettyDurationShort
import de.tadris.fitness.util.prettySpeed


@Composable
fun ScreenDefault(
	modifier: Modifier,
	workoutData: WorkoutData,
	speed: Double,
	heartRate: Double?,
	targetProgress: MutableFloatState,
	targetAchieved: MutableState<TargetAchieved>,
	isPaused: Boolean
) {
	Column(modifier.fillMaxSize().padding(top = 20.dp)) {
		Row(
			modifier = Modifier.fillMaxWidth().weight(1f)
				.padding(top = Main.vh5, start = Main.vw10, end = Main.vw7),
			verticalAlignment = Alignment.CenterVertically
		) {
			if(workoutData.isWorkoutTypeGps){
				Field(
					modifier = Modifier.weight(1f),
					value = speed.prettySpeed(),
					unitIcon = R.drawable.kmh,
					unitIconWeight = 1.5f,
					isActive = !isPaused
				)
			}
			if(heartRate != null){
				if(workoutData.isWorkoutTypeGps) FadingVerticalDivider()
				Field(
					modifier = Modifier.weight(1.1f),
					value = heartRate.pretty(),
					unitText = R.string.field_bpm
				)
			}
		}
		FadingHorizontalDivider()
		Row(
			modifier = Modifier.fillMaxWidth().weight(1f),
			verticalAlignment = Alignment.CenterVertically
		) {
			Field(
				modifier = Modifier.weight(1f).padding(start = Main.vw5),
				value = workoutData.durationTotal.prettyDurationShort(),
				isActive = !isPaused
			)
			if(workoutData.isWorkoutTypeGps) {
				FadingVerticalDivider()
				Field(
					modifier = Modifier.weight(1f),
					value = workoutData.distanceTotal.prettyDistance(),
					unitText =
						if(workoutData.distanceTotal >= 1000) R.string.field_km
						else R.string.field_m,
					isActive = !isPaused
				)
			}
		}
		FadingHorizontalDivider()
		Field(
			modifier = Modifier.weight(1f).padding(horizontal = Main.vw25),
			value = workoutData.caloriesTotal.toString(),
			unitText = R.string.field_cal,
			isActive = !isPaused
		)
	}
	TargetProgress(targetProgress, targetAchieved)
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewScreenDefault(){
	Main.GetScreenSpecs()
	val workoutData = WorkoutData()
	workoutData.pauseTotal = 100000
	workoutData.durationTotal = 200000
	workoutData.distanceTotal = 1100
	workoutData.caloriesTotal = 100
	AppScaffold { FOTTheme { ScreenDefault(
		Modifier,
		workoutData,
		speed = 6.6,
		heartRate = 160.0,
		targetProgress = remember { mutableFloatStateOf(0.9F) },
		targetAchieved = remember { mutableStateOf(TargetAchieved.NOT) },
		isPaused = false
	) } }
}
@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun PreviewScreenDefaultLarge(){ PreviewScreenDefault() }
