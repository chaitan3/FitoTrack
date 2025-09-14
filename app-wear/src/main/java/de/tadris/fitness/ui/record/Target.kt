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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.tooling.preview.devices.WearDevices
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.WorkoutData.TargetType
import de.tadris.fitness.ui.ColorFOT
import de.tadris.fitness.ui.ColorWhite
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.Main
import org.json.JSONObject

@Composable
fun Target(
	modifier: Modifier,
	workoutData: WorkoutData,
	onPrimaryClicked: () -> Unit,
	onSecondaryClicked: () -> Unit
){
	@Composable
	fun TargetItem(
		modifier: Modifier,
		enabled: Boolean = true,
		label: Int,
		subLabel: String,
		onClicked: () -> Unit
	){
		OutlinedButton(
			modifier = modifier.padding(vertical = 1.dp),
			enabled = enabled,
			onClick = onClicked
		) {
			Column(horizontalAlignment = Alignment.CenterHorizontally) {
				BasicText(
					text = stringResource(label),
					color = { ColorWhite },
					style = TextStyle.Default.copy(textAlign = TextAlign.Center),
					autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp)
				)
				BasicText(
					text = subLabel,
					color = { if(enabled) ColorFOT else ColorFOT.copy(alpha = 0.5f) },
					autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp)
				)
			}
		}
	}
	Column(
		modifier = modifier.fillMaxSize().padding(
			top = 25.dp,
			bottom = if(Main.isScreenRound) Main.vw10 else 0.dp
		),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		TargetItem(
			modifier = Modifier.weight(1f),
			label = R.string.record_target,
			subLabel = targetSubLabel(
				targetType = workoutData.primaryTargetType,
				targetValue = workoutData.primaryTargetValue,
				targetValueHigh = workoutData.primaryTargetValueHigh
			),
			onClicked = onPrimaryClicked
		)
		TargetItem(
			modifier = Modifier.weight(1f),
			enabled = workoutData.primaryTargetType != null,
			label = R.string.record_target_auto_pause,
			subLabel = stringResource(id = if(workoutData.targetAutoPause.value) R.string.on else R.string.off),
			onClicked = { workoutData.targetAutoPause.value = !workoutData.targetAutoPause.value }
		)
		TargetItem(
			modifier = Modifier.weight(1f),
			enabled = workoutData.primaryTargetType != null,
			label = R.string.target_secondary,
			subLabel = targetSubLabel(
				targetType = workoutData.secondaryTargetType,
				targetValue = workoutData.secondaryTargetValue,
				targetValueHigh = workoutData.secondaryTargetValueHigh
			),
			onClicked = onSecondaryClicked
		)
	}
}

@Composable
fun targetSubLabel(targetType: TargetType?, targetValue: Int, targetValueHigh: Int): String =
	when(targetType){
		TargetType.DISTANCE -> stringResource(
			R.string.distance_main,
			targetValue
		)
		TargetType.DURATION -> stringResource(
			R.string.duration_main,
			targetValue / 60,
			(targetValue % 60)/10,
			targetValue % 10
		)
		TargetType.SPEED -> stringResource(
			R.string.speed_main,
			Math.floorDiv(targetValue, 10),
			targetValue % 10
		)
		TargetType.HEART_RATE -> stringResource(
			R.string.heart_rate_main,
			targetValue,
			targetValueHigh
		)
		null -> stringResource(R.string.none)
	}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewTarget(){
	Main.GetScreenSpecs()
	val workoutData = WorkoutData(JSONObject())
	workoutData.primaryTargetType = TargetType.DURATION
	workoutData.primaryTargetValue = 125
	FOTTheme {
		Target(
			Modifier,
			workoutData = workoutData,
			onPrimaryClicked = {},
			onSecondaryClicked = {}
		)
		de.tadris.fitness.ui.PreviewRoundScreen()
	}
}
