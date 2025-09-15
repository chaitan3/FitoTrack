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
package de.tadris.fitness.ui.workouts

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.WorkoutDataStore
import de.tadris.fitness.ui.ColorFOT
import de.tadris.fitness.ui.ColorWhite
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.Main
import de.tadris.fitness.ui.record.RecordActivity
import de.tadris.fitness.util.runInBackground
import de.tadris.fitness.util.toastFromBG

class WorkoutsActivity : ComponentActivity() {
	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent { FOTTheme { Workouts(
			onDelWorkout = { id ->
				WorkoutDataStore.deleteWorkout(this, id) {
					toastFromBG(R.string.error_workout_delete)
				}
			},
			onResumeClick = { id ->
				finishAndRemoveTask()
				startActivity(
					Intent(this, RecordActivity::class.java)
						.putExtra("resumeId", id)
				)
			}
		) } }
	}
}

@Composable
fun Workouts(
	onDelWorkout: (Long) -> Unit,
	onResumeClick: (Long) -> Unit
){
	val navController = rememberSwipeDismissableNavController()
	AppScaffold {
		SwipeDismissableNavHost(
			navController = navController,
			startDestination = "WorkoutsList"
		) {
			composable("WorkoutsList") { WorkoutsList{ id ->
				navController.navigate("Workout/$id")
			} }
			composable("Workout/{id}") {
				Workout(
					id = it.arguments?.getString("id")?.toLongOrNull(),
					onDelWorkout = { id ->
						onDelWorkout(id)
						navController.popBackStack()
					},
					onResumeClick
				)
			}
		}
	}
}

@Composable
fun WorkoutsList(onWorkoutClick: (Long) -> Unit){
	val context = LocalContext.current
	val columnState = rememberTransformingLazyColumnState()
	val transformationSpec = rememberTransformationSpec()
	var workouts by remember { mutableStateOf<List<WorkoutData>?>(null)}
	LaunchedEffect(Unit) {
		runInBackground {
			WorkoutDataStore.readWorkouts(context)
			workouts = WorkoutDataStore.workouts
		}
	}
	ScreenScaffold(
		modifier = Modifier,
		scrollState = columnState,
		contentPadding = rememberResponsiveColumnPadding()
	) { contentPadding ->
		TransformingLazyColumn(state = columnState, contentPadding = contentPadding) {
			item {
				Text(
					text = stringResource(R.string.workouts),
					color = ColorFOT
				)
			}
			if(workouts == null) {
				item { Text(text = stringResource(R.string.loading)) }
			} else {
				for(workoutData in workouts) {
					item {
						WorkoutsListItem(
							transformation = SurfaceTransformation(transformationSpec),
							workoutData,
							onWorkoutClick
						)
					}
				}
			}
		}
	}
}

@Composable
fun WorkoutsListItem(
	transformation: SurfaceTransformation?,
	workoutData: WorkoutData,
	onWorkoutClick: (Long) -> Unit
){
	OutlinedButton(
		modifier = Modifier.fillMaxWidth(),
		transformation = transformation,
		onClick = { onWorkoutClick(workoutData.id) }
	) {
		Row(
			modifier = Modifier.fillMaxWidth().height(45.dp),
			verticalAlignment = Alignment.CenterVertically
		){
			Icon(
				modifier = Modifier.size(40.dp),
				imageVector = ImageVector.vectorResource(workoutData.workoutTypeIcon),
				contentDescription = "workout type icon"
			)
			Column(Modifier.weight(1f)){
				BasicText(
					modifier = Modifier.fillMaxWidth().weight(8f),
					text = workoutData.name,
					color = { ColorWhite },
					maxLines = 1,
					autoSize = TextAutoSize.StepBased(maxFontSize = 20.sp)
				)
				BasicText(
					modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).weight(5f),
					text = workoutData.description,
					style = TextStyle.Default.copy(textAlign = TextAlign.Center),
					color = { ColorFOT },
					maxLines = 1,
					autoSize = TextAutoSize.StepBased(maxFontSize = 18.sp)
				)
			}
		}
	}
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewWorkoutsListItem() {
	Main.GetScreenSpecs()
	val workoutData = WorkoutData()
	workoutData.workoutType = WorkoutData.WorkoutType.CYCLE
	workoutData.name = "Mon 15 Sep 22:25"
	workoutData.description = "500m in 1:00"
	FOTTheme {
		WorkoutsListItem(
			transformation = null,
			workoutData,
			onWorkoutClick = {}
		)
	}
}
