package de.tadris.fitness.ui.record

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.ui.ColorFOT
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.Main

@Composable
fun Multisport(
	modifier: Modifier,
	workoutData: WorkoutData,
	navController: NavHostController
) {
	val columnState = rememberTransformingLazyColumnState()
	val contentPadding = rememberResponsiveColumnPadding()
	ScreenScaffold(
		modifier = modifier,
		scrollState = columnState,
		contentPadding = contentPadding
	) { contentPadding ->
		TransformingLazyColumn(state = columnState, contentPadding = contentPadding) {
			item {
				Text(
					text = stringResource(R.string.WorkoutType_MULTISPORT),
					color = ColorFOT
				)
			}
			workoutData.legs.forEachIndexed { index, leg ->
				item {
					MultisportLeg(
						index,
						workoutData = leg,
						onTypeClick = { navController.navigate("TypeSelect/$index/true") },
						onSettingsClick = { navController.navigate("RecordSettings/$index") },
						onDelClick = { workoutData.legs.removeAt(index) },
						onPrimaryTargetClick = {
							navController.navigate("TargetTypeSelect/true/$index")
						},
						onSecondaryTargetClick = {
							navController.navigate("TargetTypeSelect/false/$index")
						}
					)
				}
			}
			item {
				TextButton(
					modifier = Modifier.fillMaxWidth(),
					onClick = {
						val leg = WorkoutData()
						leg.rootWorkout = workoutData
						workoutData.legs.add(leg)
						navController.navigate("TypeSelect/" + workoutData.legs.lastIndex + "/true")
					}
				){ Text(text = stringResource(R.string.add_leg)) }
			}
		}
	}
}

@Composable
fun MultisportLeg(
	index: Int,
	workoutData: WorkoutData,
	onTypeClick: () -> Unit,
	onSettingsClick: () -> Unit,
	onDelClick: () -> Unit,
	onPrimaryTargetClick: () -> Unit,
	onSecondaryTargetClick: () -> Unit
) {
	Column(
		modifier = Modifier.fillMaxWidth(),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(text = stringResource(R.string.leg, index+1))
		Row {
			IconButton(onClick = onTypeClick) {
				Icon(
					imageVector = ImageVector.vectorResource(workoutData.workoutTypeIcon),
					contentDescription = "workout type select"
				)
			}
			IconButton(onClick = onSettingsClick) {
				Icon(
					modifier = Modifier.size(40.dp),
					imageVector = ImageVector.vectorResource(R.drawable.ic_edit),
					contentDescription = "settings"
				)
			}
			IconButton(onClick = onDelClick) {
				Icon(
					modifier = Modifier.size(40.dp),
					imageVector = ImageVector.vectorResource(R.drawable.ic_delete),
					contentDescription = "delete"
				)
			}
		}
		OutlinedButton(
			modifier = Modifier.fillMaxWidth(),
			onClick = onPrimaryTargetClick
		) {
			Column {
				Text(text = stringResource(R.string.target))
				BasicText(
					text = targetSubLabel(
						targetType = workoutData.primaryTargetType,
						targetValue = workoutData.primaryTargetValue,
						targetValueHigh = workoutData.primaryTargetValueHigh
					),
					color = { ColorFOT },
					autoSize = TextAutoSize.StepBased(maxFontSize = 14.sp)
				)
			}
		}
		if(workoutData.primaryTargetType != null) {
			OutlinedButton(
				modifier = Modifier.fillMaxWidth(),
				onClick = onSecondaryTargetClick
			) {
				Column {
					Text(text = stringResource(R.string.target_secondary))
					BasicText(
						text = targetSubLabel(
							targetType = workoutData.secondaryTargetType,
							targetValue = workoutData.secondaryTargetValue,
							targetValueHigh = workoutData.secondaryTargetValueHigh
						),
						color = { ColorFOT },
						autoSize = TextAutoSize.StepBased(maxFontSize = 14.sp)
					)
				}
			}
		}
	}
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewMultisport(){
	Main.GetScreenSpecs()
	AppScaffold { FOTTheme {
		Multisport(
			Modifier,
			WorkoutData(),
			navController = rememberSwipeDismissableNavController()
		)
	} }
	de.tadris.fitness.ui.PreviewRoundScreen()
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewMultisportLeg(){
	Main.GetScreenSpecs()
	val workoutData = WorkoutData()
	workoutData.workoutType = WorkoutData.WorkoutType.SWIM_OUTDOOR
	workoutData.primaryTargetType = WorkoutData.TargetType.DISTANCE
	workoutData.primaryTargetValue = 2
	AppScaffold { FOTTheme {
		MultisportLeg(
			index = 1,
			workoutData,
			onTypeClick = {},
			onSettingsClick = {},
			onDelClick = {},
			onPrimaryTargetClick = {},
			onSecondaryTargetClick = {}
		)
	} }
	de.tadris.fitness.ui.PreviewRoundScreen()
}
