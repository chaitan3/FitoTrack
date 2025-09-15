package de.tadris.fitness.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.ui.ColorFOT
import de.tadris.fitness.ui.ColorWhite
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.Main
import de.tadris.fitness.util.prettyDistance
import de.tadris.fitness.util.prettyDuration
import de.tadris.fitness.util.prettyDurationShort
import kotlinx.coroutines.delay

@Composable
fun MultisportTransition(
	rootWorkoutData: WorkoutData,
	modifier: Modifier = Modifier,
	onNextLegClick: () -> Unit = {}
) {
	var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
	LaunchedEffect(Unit) {
		while (true) {
			currentTimeMillis = System.currentTimeMillis()
			delay(1000 - (currentTimeMillis % 1000))
		}
	}
	Column(
		modifier.fillMaxSize().padding(
				top = 25.dp, bottom = if(Main.isScreenRound) Main.vh10 else 0.dp
			),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	){
		Text(
			modifier = Modifier.weight(1f),
			text =
				if(rootWorkoutData.endTime > 0L) (rootWorkoutData.durationTotal).prettyDuration()
				else (currentTimeMillis - rootWorkoutData.id).prettyDuration(),
			color = if(rootWorkoutData.endTime > 0L) ColorWhite else ColorFOT
		)
		rootWorkoutData.legs.forEachIndexed { i, workoutData ->
			val prevLeg = if(i > 0) rootWorkoutData.legs[i-1] else null
			val nextLeg = if(i <= rootWorkoutData.legs.lastIndex) rootWorkoutData.legs[i] else null
			if(prevLeg != null && prevLeg.id > 0L && prevLeg.endTime > 0L){
				val transitionTime =
					if(workoutData.id == 0L) currentTimeMillis - prevLeg.endTime
					else workoutData.id - prevLeg.endTime
				Row(
					modifier = Modifier.weight(1f),
					verticalAlignment = Alignment.CenterVertically
				) {
					BasicText(
						text = stringResource(
							R.string.transition,
							transitionTime.prettyDurationShort()
						),
						color = {
							if(prevLeg.endTime > 0L && nextLeg?.id == 0L) ColorFOT
							else ColorWhite
						},
						maxLines = 1,
						autoSize = TextAutoSize.StepBased(maxFontSize = 16.sp)
					)
				}
			}
			Row(
				modifier = Modifier
					.weight(1f)
					.padding(
						horizontal =
							if(Main.isScreenRound && i == rootWorkoutData.legs.lastIndex) Main.vw17
							else 0.dp
					),
				verticalAlignment = Alignment.CenterVertically
			){
				Icon(
					modifier = Modifier.padding(end = 4.dp),
					imageVector = ImageVector.vectorResource(workoutData.workoutTypeIcon),
					contentDescription = "workout type",
					tint =
						if(workoutData.id > 0L && workoutData.endTime == 0L) ColorFOT
						else ColorWhite
				)
				if((prevLeg?.endTime ?: 0L) > 0L && nextLeg?.id == 0L) {
					IconButton(onClick = onNextLegClick) {
						Icon(
							imageVector = ImageVector.vectorResource(R.drawable.ic_resume),
							contentDescription = "start next leg",
						)
					}
				}
				if(workoutData.id != 0L) {
					BasicText(
						text =
							if(workoutData.distanceTotal >= 1000)
								stringResource(
									R.string.summary_km,
									workoutData.distanceTotal.prettyDistance(),
									workoutData.durationTotal.prettyDurationShort()
								)
							else stringResource(
								R.string.summary_m,
								workoutData.distanceTotal.prettyDistance(),
								workoutData.durationTotal.prettyDurationShort()
							),
						color = { if(workoutData.endTime == 0L) ColorFOT else ColorWhite },
						maxLines = 1,
						autoSize = TextAutoSize.StepBased(minFontSize = 4.sp, maxFontSize = 16.sp)
					)
				}
			}
		}
	}
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewMultisportTransition(){
	Main.GetScreenSpecs()
	val rootWorkoutData = WorkoutData()
	rootWorkoutData.id = System.currentTimeMillis() - 7266599
	val leg1 = WorkoutData()
	leg1.id = System.currentTimeMillis() - 7266600
	leg1.workoutType = WorkoutData.WorkoutType.SWIM_OUTDOOR
	leg1.distanceTotal = 1500
	leg1.durationTotal = 2091700
	leg1.endTime = System.currentTimeMillis() - 7266600 + 2091700
	rootWorkoutData.legs.add(leg1)
	val leg2 = WorkoutData()
	leg2.id = System.currentTimeMillis() - 4948500
	leg2.workoutType = WorkoutData.WorkoutType.CYCLE
	leg2.distanceTotal = 40000
	leg2.durationTotal = 4718100
	leg2.endTime = System.currentTimeMillis() - 4948500 + 4718100
	rootWorkoutData.legs.add(leg2)
	val leg3 = WorkoutData()
	//leg3.id = System.currentTimeMillis() - 0
	leg3.workoutType = WorkoutData.WorkoutType.RUN
	leg3.distanceTotal = 10000
	leg3.durationTotal = 4718100
	//leg3.isCompleted = true
	rootWorkoutData.legs.add(leg3)
	AppScaffold { FOTTheme {
		MultisportTransition(rootWorkoutData)
	} }
	de.tadris.fitness.ui.PreviewRoundScreen()
}
