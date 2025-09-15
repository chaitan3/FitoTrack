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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme.colorScheme
import androidx.wear.compose.material3.TextButton
import androidx.wear.tooling.preview.devices.WearDevices
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.ui.ColorWhite
import de.tadris.fitness.ui.record.RecordActivity.SensorStatus
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.Main

@Composable
fun Prepare(
	modifier: Modifier,
	workoutData: WorkoutData,
	onWorkoutTypeClick: () -> Unit,
	onTargetClick: () -> Unit,
	onStartClick: () -> Unit,
	locationStatus: SensorStatus,
	heartRateStatus: SensorStatus,
	onSettingsClick: () -> Unit
){
	Column(modifier.fillMaxSize().padding(top = if(Main.isScreenRound) Main.vw10 else 0.dp)) {
		Row(
			modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = Main.vw12),
			verticalAlignment = Alignment.CenterVertically
		){
			IconButton(
				modifier = Modifier.weight(1f),
				onClick = onWorkoutTypeClick,
				enabled = workoutData.id == 0L
			) {
				Icon(
					imageVector = ImageVector.vectorResource(workoutData.workoutTypeIcon),
					contentDescription = "workout type select",
				)
			}
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
		TextButton(
			modifier = Modifier.fillMaxWidth().weight(1f).padding(4.dp),
			onClick = onStartClick
		) {
			BasicText(
				text = stringResource(R.string.start),
				color = { ColorWhite },
				autoSize = TextAutoSize.StepBased(minFontSize = 10.sp),
				style = TextStyle.Default.copy(textAlign = TextAlign.Center)
			)
		}
		Row(
			modifier = Modifier.fillMaxWidth().weight(0.8f),
			verticalAlignment = Alignment.CenterVertically
		){
			Icon(
				modifier = Modifier.weight(1f),
				imageVector = ImageVector.vectorResource(id =
					if(locationStatus == SensorStatus.AVAILABLE) R.drawable.ic_gps_fixed
					else R.drawable.ic_gps_no_fix
				),
				tint =
					if(locationStatus.isDisabled) colorScheme.error
					else colorScheme.onBackground,
				contentDescription = "gps status"
			)
			Icon(
				modifier = Modifier.weight(1f),
				imageVector = ImageVector.vectorResource(id =
					if(heartRateStatus == SensorStatus.AVAILABLE) R.drawable.ic_heart_fixed
					else R.drawable.ic_heart_no_fix
				),
				tint =
					if(heartRateStatus.isDisabled) colorScheme.error
					else colorScheme.onBackground,
				contentDescription = "heart rate status",
			)
		}
		IconButton(
			modifier = Modifier.fillMaxWidth().weight(1f).padding(4.dp),
			onClick = onTargetClick
		) {
			Icon(
				imageVector = ImageVector.vectorResource(id =
					if(workoutData.primaryTargetType == null) R.drawable.ic_flag_unset
					else R.drawable.ic_flag
				),
				contentDescription = "target",
			)
		}
	}
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewPrepare(){
	Main.GetScreenSpecs()
	val workoutData = WorkoutData()
	workoutData.workoutType = WorkoutData.WorkoutType.WINDSURFING
	AppScaffold { FOTTheme {
		Prepare(
			Modifier,
			workoutData = workoutData,
			locationStatus = SensorStatus.DISABLED,
			heartRateStatus = SensorStatus.AVAILABLE,
			onWorkoutTypeClick = {},
			onTargetClick = {},
			onStartClick = {},
			onSettingsClick = {}
		)
	} }
	de.tadris.fitness.ui.PreviewRoundScreen()
}
