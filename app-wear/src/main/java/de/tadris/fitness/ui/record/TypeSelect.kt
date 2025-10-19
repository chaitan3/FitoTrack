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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.WorkoutData.Companion.getWorkoutTypeIcon
import de.tadris.fitness.data.WorkoutData.Companion.getWorkoutTypeName
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.Main

@Composable
fun TypeSelect(
	modifier: Modifier,
	onItemClick: (WorkoutData.WorkoutType) -> Unit,
	noMultisport: Boolean
){
	val columnState = rememberTransformingLazyColumnState()
	val contentPadding = rememberResponsiveColumnPadding()
	val transformationSpec = rememberTransformationSpec()
	ScreenScaffold(
		modifier = modifier,
		scrollState = columnState,
		contentPadding = contentPadding
	) { contentPadding ->
		TransformingLazyColumn(state = columnState, contentPadding = contentPadding) {
			items(WorkoutData.WorkoutType.entries) { workoutType ->
			if(workoutType == WorkoutData.WorkoutType.MULTISPORT && noMultisport) return@items
				Type(
					transformation = SurfaceTransformation(transformationSpec),
					workoutType,
					onItemClick
				)
			}
		}
	}
}

@Composable
fun Type(
	transformation: SurfaceTransformation,
	workoutType: WorkoutData.WorkoutType,
	onItemClick: (WorkoutData.WorkoutType) -> Unit
) {
	OutlinedButton(
		modifier = Modifier.fillMaxWidth(),
		transformation = transformation,
		onClick = { onItemClick(workoutType) }
	) {
		Row(verticalAlignment = Alignment.CenterVertically){
			Icon(
				modifier = Modifier.size(24.dp).padding(end = 4.dp),
				imageVector = ImageVector.vectorResource(getWorkoutTypeIcon(workoutType)),
				contentDescription = "Workout type icon"
			)
			Text(stringResource(getWorkoutTypeName(workoutType)))
		}
	}
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewTypeSelect(){
	Main.GetScreenSpecs()
	AppScaffold { FOTTheme { TypeSelect(Modifier, onItemClick = {}, noMultisport = false) } }
}
