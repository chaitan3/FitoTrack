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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.tooling.preview.devices.WearDevices
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.Field
import de.tadris.fitness.ui.Main
import de.tadris.fitness.ui.TargetProgress
import de.tadris.fitness.ui.record.RecordActivity.TargetAchieved
import de.tadris.fitness.util.pretty
import de.tadris.fitness.util.prettyDistance
import de.tadris.fitness.util.prettyDurationShort
import org.json.JSONObject

@Composable
fun ScreenStop(
	modifier: Modifier,
	workoutData: WorkoutData,
	heartRate: Double?,
	targetProgress: MutableFloatState,
	targetAchieved: MutableState<TargetAchieved>,
	isPaused: Boolean,
	onPauseResumeClick: () -> Unit,
	onEndClick: () -> Unit,
	onSettingsClick: () -> Unit
){
	Column(modifier = modifier.fillMaxSize().padding(top = 20.dp)) {
		Row(
			modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = Main.vw25),
			verticalAlignment = Alignment.CenterVertically
		){
			if(workoutData.isWorkoutTypeGps) {
				Field(
					modifier = Modifier.weight(1f),
					value = workoutData.distanceTotal.intValue.prettyDistance(),
					unitText =
						if(workoutData.distanceTotal.intValue >= 1000) R.string.field_km
						else R.string.field_m,
					isActive = !isPaused
				)
			} else if(heartRate != null) {
				Field(
					modifier = Modifier.weight(1f),
					value = heartRate.pretty(),
					unitText = R.string.field_bpm
				)
			}
		}
		Row(
			modifier = Modifier.fillMaxWidth().weight(1f),
			verticalAlignment = Alignment.CenterVertically
		){
			Field(
				modifier = Modifier.weight(1f),
				value = workoutData.pause.prettyDurationShort(),
				isActive = isPaused
			)
			Field(
				modifier = Modifier.weight(1f),
				value = workoutData.durationTotal.longValue.prettyDurationShort(),
				isActive = !isPaused
			)
		}
		Row(
			modifier = Modifier.fillMaxWidth().weight(1f),
			verticalAlignment = Alignment.CenterVertically
		){
			IconButton(
				modifier = Modifier.weight(1f),
				onClick = onPauseResumeClick
			) {
				Icon(
					imageVector = ImageVector.vectorResource(id =
						if(isPaused) R.drawable.ic_resume
						else R.drawable.ic_pause
					),
					contentDescription = "pause or resume",
				)
			}
			IconButton(
				modifier = Modifier.weight(1f),
				onClick = onEndClick
			) {
				Icon(
					imageVector = ImageVector.vectorResource(R.drawable.ic_stop),
					contentDescription = "stop",
				)
			}
		}
		Row(
			modifier = Modifier.fillMaxWidth().weight(1f),
			verticalAlignment = Alignment.CenterVertically
		) {
			IconButton(
				modifier = Modifier.weight(1f),
				onClick = onSettingsClick
			) {
				Icon(
					imageVector = ImageVector.vectorResource(R.drawable.ic_edit),
					contentDescription = "settings",
				)
			}
		}
	}
	TargetProgress(targetProgress, targetAchieved)
}
@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewScreenStop(){
	Main.GetScreenSpecs()
	val workoutData = WorkoutData(JSONObject())
	workoutData.pauseTotal.longValue = 100000
	workoutData.durationTotal.longValue = 200000
	workoutData.distanceTotal.intValue = 500
	AppScaffold { FOTTheme { ScreenStop(
		Modifier,
		workoutData,
		heartRate = 60.0,
		targetProgress = remember { mutableFloatStateOf(0.9F) },
		targetAchieved = remember { mutableStateOf(TargetAchieved.NOT) },
		isPaused = true,
		onPauseResumeClick = {},
		onEndClick = {},
		onSettingsClick = {}
	) } }
}
