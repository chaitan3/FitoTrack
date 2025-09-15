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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutData.TargetType
import de.tadris.fitness.ui.ColorBlack
import de.tadris.fitness.ui.ColorFOT
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.Main

@Composable
fun TargetTypeSelect(
	modifier: Modifier,
	targetType: TargetType?,
	disabledTargetType: TargetType?,
	onTargetSelected: (TargetType?) -> Unit
){
	Column(modifier.fillMaxSize().padding(
		top = 25.dp,
		bottom = if(Main.isScreenRound) Main.vh5 else 0.dp
	)) {
		Row(
			modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 1.dp),
			horizontalArrangement = Arrangement.Center
		) {
			Button(
				onClick = { onTargetSelected(null) }
			) { Text(
				text = stringResource(R.string.none),
				color = if(targetType == null) ColorFOT else ColorBlack
			) }
		}
		Row(
			modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 1.dp),
			horizontalArrangement = Arrangement.Center
		){
			Button(
				modifier = Modifier.padding(horizontal = 1.dp),
				onClick = { onTargetSelected(TargetType.DISTANCE) },
				enabled = disabledTargetType != TargetType.DISTANCE
			) {
				Text(
					text = stringResource(R.string.distance),
					color = if(targetType == TargetType.DISTANCE) ColorFOT else ColorBlack
				)
			}
			Button(
				modifier = Modifier.padding(horizontal = 1.dp),
				onClick = { onTargetSelected(TargetType.DURATION) },
				enabled = disabledTargetType != TargetType.DURATION
			) {
				Text(
					text = stringResource(R.string.duration),
					color = if(targetType == TargetType.DURATION) ColorFOT else ColorBlack
				)
			}
		}
		Row(
			modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 1.dp),
			horizontalArrangement = Arrangement.Center
		) {
			Button(
				onClick = { onTargetSelected(TargetType.HEART_RATE) },
				enabled = disabledTargetType != TargetType.HEART_RATE
			) { Text(
				text = stringResource(R.string.heart_rate),
				color = if(targetType == TargetType.HEART_RATE) ColorFOT else ColorBlack
			) }
		}
		Row(
			modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 1.dp),
			horizontalArrangement = Arrangement.Center
		) {
			Button(
				onClick = { onTargetSelected(TargetType.SPEED) },
				enabled = disabledTargetType != TargetType.SPEED
			) { Text(
				text = stringResource(R.string.speed),
				color = if(targetType == TargetType.SPEED) ColorFOT else ColorBlack
			) }
		}
	}
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun PreviewTargetTypeSelect(){
	Main.GetScreenSpecs()
	FOTTheme {
		TargetTypeSelect(
			Modifier,
			targetType = TargetType.DURATION,
			disabledTargetType = TargetType.DISTANCE,
			onTargetSelected = {}
		)
		de.tadris.fitness.ui.PreviewRoundScreen()
	}
}
