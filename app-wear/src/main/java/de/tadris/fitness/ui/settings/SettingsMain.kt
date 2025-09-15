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
package de.tadris.fitness.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import de.tadris.fitness.R
import de.tadris.fitness.ui.ColorFOT
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.Main

@Composable
fun SettingsMain(
	btEnabled: Boolean?,
	onBTClick: () -> Unit,
	onMapsClick: () -> Unit
){
	val columnState = rememberTransformingLazyColumnState()
	val transformationSpec = rememberTransformationSpec()
	ScreenScaffold(
		modifier = Modifier,
		scrollState = columnState,
		contentPadding = rememberResponsiveColumnPadding()
	) { contentPadding ->
		TransformingLazyColumn(state = columnState, contentPadding = contentPadding) {
			item { Text(
				text = stringResource(R.string.settings),
				color = ColorFOT
			) }
			item {
				OutlinedButton(
					modifier = Modifier.fillMaxWidth(),
					transformation = SurfaceTransformation(transformationSpec),
					onClick = onBTClick
				) {
					Column {
						Text(text = stringResource(R.string.BT))
						Text(
							text =
								if(btEnabled == null) stringResource(R.string.permission_denied)
								else if(btEnabled) stringResource(R.string.enabled)
								else stringResource(R.string.disabled),
							color = ColorFOT
						)
					}
				}
			}
			item {
				OutlinedButton(
					modifier = Modifier.fillMaxWidth(),
					transformation = SurfaceTransformation(transformationSpec),
					onClick = onMapsClick
				) { Text(stringResource(R.string.maps)) }
			}
		}
	}
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewSettingsMain(){
	Main.GetScreenSpecs()
	FOTTheme { SettingsMain(
		btEnabled = true,
		onBTClick = {},
		onMapsClick = {}
	) }
}
