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
import androidx.compose.runtime.mutableStateListOf
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
import de.tadris.fitness.ui.Chart
import de.tadris.fitness.ui.ChartData
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.Field
import de.tadris.fitness.ui.Main
import de.tadris.fitness.util.pretty
import de.tadris.fitness.util.prettyDurationShort

@Composable
fun Cooldown(
	modifier: Modifier,
	duration: Long,
	heartRate: Double,
	onStopClick: () -> Unit
){
	val heartRateData = remember { mutableStateListOf<ChartData>() }
	LaunchedEffect(heartRate) { heartRateData.add(ChartData(duration, value = heartRate)) }

	Column(modifier.fillMaxSize().padding(top = 20.dp)){
		Row(
			modifier = Modifier.fillMaxWidth().weight(1f).padding(
				horizontal = if(Main.isScreenRound) Main.vw12 else 0.dp
			),
			verticalAlignment = Alignment.CenterVertically
		){
			Field(
				modifier = Modifier.weight(1f),
				value = duration.prettyDurationShort()
			)
			Field(
				modifier = Modifier.weight(1f),
				value = heartRate.pretty(),
				unitText = R.string.field_bpm
			)
		}
		Box(
			modifier = Modifier.weight(3f).padding(
				start = if(Main.isScreenRound) Main.vw5 else 0.dp,
				end = if(Main.isScreenRound) Main.vw12 else 0.dp
			)
		){ Chart(data = heartRateData, yMinInit = 50.0, yMaxInit = 100.0) }
		IconButton(
			modifier = Modifier.fillMaxWidth().weight(1f),
			onClick = onStopClick
		) {
			Icon(
				imageVector = ImageVector.vectorResource(R.drawable.ic_stop),
				contentDescription = "stop cooldown",
			)
		}
	}
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewCooldown() {
	Main.GetScreenSpecs()
	AppScaffold { FOTTheme {
		Cooldown(
			Modifier,
			duration = 100000L,
			heartRate = 70.0,
			onStopClick = {}
		)
		de.tadris.fitness.ui.PreviewRoundScreen()
	} }
}
