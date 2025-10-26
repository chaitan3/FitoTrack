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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import de.tadris.fitness.R
import de.tadris.fitness.data.MapStore
import de.tadris.fitness.data.SettingsStore
import de.tadris.fitness.data.SettingsStore.getMapSource
import de.tadris.fitness.data.SettingsStore.storeMapSource
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.ui.ColorFOT
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.Main

@Composable
fun RecordSettings(
	modifier: Modifier,
	showTarget: Boolean,
	workoutData: WorkoutData,
	hasLocation: Boolean,
	hasHeartRate: Boolean,
	targetNotifyEnabled: Int,
	toggleSpeak: () -> Unit,
	onTargetClicked: () -> Unit
){
	val context = LocalContext.current
	val columnState = rememberTransformingLazyColumnState()
	val contentPadding = rememberResponsiveColumnPadding(
		first = ScalingLazyColumnDefaults.ItemType.MultiButton
	)
	val transformationSpec = rememberTransformationSpec()
	val workoutTypeSettings = SettingsStore.getWorkoutTypeSettings(context, workoutData.workoutType)
	ScreenScaffold(
		modifier = modifier,
		scrollState = columnState,
		contentPadding = contentPadding
	) { contentPadding ->
		TransformingLazyColumn(state = columnState, contentPadding = contentPadding) {
			if(showTarget){ item {
				OutlinedButton(
					modifier = Modifier.fillMaxWidth(),
					transformation = SurfaceTransformation(transformationSpec),
					onClick = onTargetClicked
				) {
					Column{
						Text(text = stringResource(R.string.target))
						Text(
							text = targetSubLabel(
								targetType = workoutData.primaryTargetType,
								targetValue = workoutData.primaryTargetValue,
								targetValueHigh = workoutData.primaryTargetValueHigh
							),
							color = ColorFOT
						)
					}
				}
			} }
			item {
				OutlinedButton(
					modifier = Modifier.fillMaxWidth(),
					transformation = SurfaceTransformation(transformationSpec),
					onClick = toggleSpeak
				) {
					Column {
						Text(text = stringResource(R.string.targetNotify))
						Text(
							text = stringResource(id =
								when(targetNotifyEnabled) {
									2 -> R.string.primary
									1 -> R.string.on
									0 -> R.string.off
									else -> R.string.unavailable
								}
							),
							color = ColorFOT
						)
					}
				}
			}
			if(workoutData.isWorkoutTypeGps && MapStore.getLocalMaps(context).isNotEmpty()) {
				item {
					var mapSource by remember { mutableStateOf(getMapSource(context)) }
					OutlinedButton(
						modifier = Modifier.fillMaxWidth(),
						transformation = SurfaceTransformation(transformationSpec),
						onClick = {
							mapSource = !mapSource
							storeMapSource(context, mapSource)
						}
					) {
						Column {
							Text(text = stringResource(R.string.map_source))
							Text(
								text = stringResource(id = if(mapSource) R.string.local else R.string.online),
								color = ColorFOT
							)
						}
					}
				}
			}
			item {
				Text(
					text = stringResource(
						R.string.settings_title,
						stringResource(WorkoutData.getWorkoutTypeName(workoutData.workoutType))
					),
					color = ColorFOT,
					textAlign = TextAlign.Center
				)
			}
			item {
				OutlinedButton(
					modifier = Modifier.fillMaxWidth(),
					transformation = SurfaceTransformation(transformationSpec),
					onClick = { workoutTypeSettings.toggleScreenOn(context) }
				) {
					Column{
						Text(text = stringResource(R.string.screen))
						Text(
							text = stringResource(id =
								if(workoutTypeSettings.screenOn.value) R.string.screen_keep_on else R.string.screen_auto_off
							),
							color = ColorFOT
						)
					}
				}
			}
			if(workoutData.isWorkoutTypeAutoPause) {
				item {
					OutlinedButton(
						modifier = Modifier.fillMaxWidth(),
						transformation = SurfaceTransformation(transformationSpec),
						onClick = { workoutTypeSettings.toggleAutoPause(context) }
					) {
						Column {
							Text(text = stringResource(R.string.auto_pause))
							Text(
								text = stringResource(id =
									if(workoutTypeSettings.autoPause.value) R.string.on else R.string.off
								),
								color = ColorFOT
							)
						}
					}
				}
			}
			items(workoutTypeSettings.screens){
				if((!workoutData.isWorkoutTypeGps && it.needsLocation) ||
					it.isTarget || it.isMultisport) return@items
				val unavailable = (it.needsLocation && !hasLocation) ||
						(it.needsHeartRate && !hasHeartRate)
				OutlinedButton(
					modifier = Modifier.fillMaxWidth(),
					transformation = SurfaceTransformation(transformationSpec),
					enabled = !unavailable,
					onClick = { it.toggle(context, workoutData.workoutType) }
				) {
					Column{
						Text(text = stringResource(it.name))
						Text(
							text = stringResource(id =
								if(unavailable) R.string.unavailable
								else if(it.enabled.value) R.string.on
								else R.string.off
							),
							color = ColorFOT
						)
					}
				}
			}
		}
	}
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewRecordSettings(){
	Main.GetScreenSpecs()
	val workoutData = WorkoutData()
	workoutData.workoutType = WorkoutData.WorkoutType.SWIM_OUTDOOR
	FOTTheme {
		RecordSettings(
			Modifier,
			showTarget = true,
			workoutData,
			hasLocation = true,
			hasHeartRate = true,
			targetNotifyEnabled = 1,
			toggleSpeak = {},
			onTargetClicked = {}
		)
	}
	de.tadris.fitness.ui.PreviewRoundScreen()
}
