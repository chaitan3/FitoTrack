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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.tooling.preview.devices.WearDevices
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.ui.FOTTheme

@Composable
fun TargetEntry(
	modifier: Modifier,
	targetType: WorkoutData.TargetType?,
	onBack: () -> Unit,
	onDone: (value: Int, valueMax: Int) -> Unit
){
	var targetValue by remember { mutableIntStateOf(0) }
	var targetValueMax by remember { mutableIntStateOf(0) }
	var targetValueFull by remember { mutableIntStateOf(0) }

	fun numberPressed(value: Int){
		if(targetType == WorkoutData.TargetType.HEART_RATE) {
			targetValueFull = targetValueFull  * 10 + value
			targetValue = targetValueFull / 1000
			targetValueMax = targetValueFull % 1000
		}else if(targetType == WorkoutData.TargetType.DURATION && targetValue < 99){
			targetValue = targetValue * 10 + value
			if(targetValue > 99){
				val hundreds = Math.floorDiv(targetValue, 100)
				targetValue = targetValue - hundreds * 100 + hundreds * 60
			}
		}else if(targetValue < 99) {
			targetValue = targetValue * 10 + value
		}
	}
	fun backPressed(){
		if(targetValue + targetValueMax == 0) onBack()
		when(targetType) {
			WorkoutData.TargetType.HEART_RATE -> {
				targetValueFull /= 10
				targetValue = targetValueFull / 1000
				targetValueMax = targetValueFull % 1000
			}
			WorkoutData.TargetType.DURATION -> {
				if(targetValue > 59){
					val hours = Math.floorDiv(targetValue, 60)
					targetValue = targetValue - hours * 60 + hours * 100
				}
				targetValue /= 10
			}
			else -> targetValue /= 10
		}
	}
	@Composable
	fun durationValue(): String = stringResource(
		R.string.duration_preview,
		Math.floorDiv(targetValue, 60),
		Math.floorDiv(targetValue % 60, 10),
		targetValue % 10
	)
	@Composable
	fun heartRatePreview(): String {
		val targetValueFullString = targetValueFull.toString()
		return if(targetValueFullString.length > 3){
			stringResource(
				R.string.heart_rate_preview,
				targetValueFullString.substring(0, targetValueFullString.length-3),
				targetValueFullString.substring(targetValueFullString.length-3),
			)
		}else{
			stringResource(
				R.string.heart_rate_preview,
				"0",
				targetValueFullString,
			)
		}
	}

	Column(modifier.fillMaxSize().padding(top = 24.dp)){
		Text(
			modifier = Modifier.fillMaxWidth(),
			text = when(targetType){
				WorkoutData.TargetType.DISTANCE -> stringResource(R.string.distance_preview, targetValue)
				WorkoutData.TargetType.DURATION -> durationValue()
				WorkoutData.TargetType.HEART_RATE -> heartRatePreview()
				WorkoutData.TargetType.SPEED -> stringResource(
					R.string.speed_preview,
					targetValue / 10,
					targetValue % 10
				)
				else -> targetValue.toString()
			},
			textAlign = TextAlign.Center
		)
		Row(Modifier.fillMaxWidth().weight(3f)){
			TextButton(
				modifier = Modifier.fillMaxHeight().weight(1f),
				onClick = ::backPressed
			){ Text(text = stringResource(R.string.b_back)) }
			Column(Modifier.weight(3f)){
				Row(Modifier.weight(1f)){
					for(i in 1..3){
						TextButton(
							modifier = Modifier.weight(1f),
							onClick = { numberPressed(i) }
						){ Text(i.toString()) }
					}
				}
				Row(Modifier.weight(1f)){
					for(i in 4..6){
						TextButton(
							modifier = Modifier.weight(1f),
							onClick = { numberPressed(i) }
						){ Text(i.toString()) }
					}
				}
				Row(Modifier.weight(1f)){
					for(i in 7..9){
						TextButton(
							modifier = Modifier.weight(1f),
							onClick = { numberPressed(i) }
						){ Text(i.toString()) }
					}
				}
			}
			TextButton(
				modifier = Modifier.fillMaxHeight().weight(1f),
				onClick = { onDone(targetValue, targetValueMax) }
			){ Text(text = stringResource(R.string.b_done)) }
		}
		TextButton(
			modifier = Modifier.fillMaxWidth().weight(1f),
			onClick = { numberPressed(0) }
		){ Text(text = "0") }
	}
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun PreviewTargetEntry(){
	FOTTheme { TargetEntry(
		Modifier,
		targetType = WorkoutData.TargetType.SPEED,
		{}, {_: Int, _: Int ->}
	)}
	de.tadris.fitness.ui.PreviewRoundScreen()
}
