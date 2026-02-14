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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.LocalTextStyle
import androidx.wear.tooling.preview.devices.WearDevices
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.WorkoutData.TargetType
import de.tadris.fitness.ui.ColorInactive
import de.tadris.fitness.ui.ColorProgressAhead
import de.tadris.fitness.ui.ColorProgressBehind
import de.tadris.fitness.ui.ColorProgressBg
import de.tadris.fitness.ui.ColorWhite
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.Field
import de.tadris.fitness.ui.Main
import de.tadris.fitness.ui.TargetProgress
import de.tadris.fitness.ui.record.RecordActivity.TargetAchieved
import de.tadris.fitness.util.pretty
import de.tadris.fitness.util.prettyDistance
import de.tadris.fitness.util.prettyDurationShort
import de.tadris.fitness.util.prettySpeed

@Composable
fun ScreenTarget(
	modifier: Modifier,
	workoutData: WorkoutData,
	speed: Double,
	heartRate: Double?,
	targetProgress: MutableFloatState,
	targetAchieved: MutableState<TargetAchieved>,
	isPaused: Boolean
) {
	fun fullTargetProgress(): Float =
		if(targetAchieved.value == TargetAchieved.FULL) 1f else targetProgress.floatValue

	Column(
		modifier = modifier.fillMaxSize().padding(top = 20.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Row(
			modifier = Modifier.weight(1f)
				.padding(horizontal = if(Main.isScreenRound) Main.vw10 else 5.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			if(workoutData.primaryTargetType == TargetType.DURATION) {
				Field(
					modifier = Modifier.weight(1f),
					value = workoutData.durationTotal.prettyDurationShort(),
					isActive = !isPaused
				)
			} else if(workoutData.secondaryTargetType != TargetType.DISTANCE){
				Field(
					modifier = Modifier.weight(1f),
					value = workoutData.distanceTotal.prettyDistance(),
					unitText =
						if(workoutData.distanceTotal >= 1000) R.string.field_km
						else R.string.field_m,
					isActive = !isPaused
				)
			}
			if(heartRate != null && workoutData.primaryTargetType != TargetType.HEART_RATE  &&
				workoutData.secondaryTargetType != TargetType.HEART_RATE
			) {
				Field(
					modifier = Modifier.weight(1f),
					value = heartRate.pretty(),
					unitText = R.string.field_bpm
				)
			}
		}
		if(workoutData.primaryTargetType == TargetType.HEART_RATE){
			TargetBarHeartRate(
				Modifier.weight(.8f),
				heartRate,
				workoutData.primaryTargetValue.toInt(),
				workoutData.primaryTargetValueHigh.toInt()
			)
		}

		if(workoutData.primaryTargetType == TargetType.SPEED ||
			workoutData.secondaryTargetType == TargetType.SPEED
		){
			val target = if(workoutData.primaryTargetType == TargetType.SPEED) workoutData.primaryTargetValue.toDouble()
						else workoutData.secondaryTargetValue.toDouble()
			TargetBarSpeed(
				Modifier.weight(.8f)
					.padding(horizontal = if(Main.isScreenRound) Main.vw7 else 5.dp)
					.border(1.dp, ColorWhite),
				target,
				value = speed,
				text = stringResource(R.string.field_kmh, speed.prettySpeed()),
				isPaused
			)
			if(workoutData.primaryTargetType != TargetType.HEART_RATE) {
				TargetBarSpeed(
					Modifier.weight(.8f)
						.padding(horizontal = if(Main.isScreenRound) Main.vw7 else 5.dp)
						.border(1.dp, ColorWhite),
					target,
					value = workoutData.speedAvg,
					text = stringResource(
						R.string.field_avg_kmh,
						workoutData.speedAvg.prettySpeed()
					),
					isPaused
				)
			}
		}
		if(workoutData.secondaryTargetType == TargetType.HEART_RATE){
			TargetBarHeartRate(
				Modifier.weight(.8f),
				heartRate,
				workoutData.secondaryTargetValue.toInt(),
				workoutData.secondaryTargetValueHigh.toInt()
			)
		}
		if(workoutData.secondaryTargetType == TargetType.DISTANCE){
			Box(Modifier.weight(.8f)
				.padding(horizontal = if(Main.isScreenRound) Main.vw7 else 5.dp)
				.border(1.dp, ColorProgressBg)
			){
				val shouldBeAt = workoutData.secondaryTargetValue.toInt() * fullTargetProgress()
				val diff = (workoutData.distanceTotal - shouldBeAt).toInt()
				Canvas(Modifier.fillMaxSize()){
					val width = size.width
					var widthShould = width * fullTargetProgress()
					if(widthShould > width) widthShould = width
					var widthDiff = width * (diff / workoutData.secondaryTargetValue.toFloat())
					if(widthDiff+widthShould > width) widthDiff = width-widthShould
					drawRect(
						color = ColorProgressBg,
						size = size.copy(widthShould)
					)
					drawRect(
						color =
							if(shouldBeAt == 0f) ColorProgressBg
							else if(widthDiff < 0) ColorProgressBehind
							else ColorProgressAhead,
						topLeft = Offset(widthShould, 0f),
						size = size.copy(widthDiff)
					)
				}
				BasicText(
					modifier = Modifier.fillMaxSize().padding(4.dp),
					text = (if(diff > 0 && fullTargetProgress() > 0) "+" else "") +
						stringResource(
						if(diff in -1000..1000) R.string.field_m else R.string.field_km,
						diff.prettyDistance()
					),
					style = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
					color = { if(isPaused) ColorInactive else ColorWhite },
					maxLines = 1,
					autoSize = TextAutoSize.StepBased(minFontSize = 8.sp)
				)
			}
		} else if(workoutData.secondaryTargetType == TargetType.DURATION){
			Box(Modifier.weight(.8f)
				.padding(horizontal = if(Main.isScreenRound) Main.vw7 else 5.dp)
				.border(1.dp, ColorProgressBg)
			){
				val shouldBeAt = workoutData.secondaryTargetValue.toFloat() * fullTargetProgress()
				val diff = workoutData.durationTotal - shouldBeAt
				Canvas(Modifier.fillMaxSize()){
					val width = size.width
					var widthShould = width * fullTargetProgress()
					if(widthShould > width) widthShould = width
					var widthDiff = width * (diff / workoutData.secondaryTargetValue.toFloat())
					if(widthDiff+widthShould > width) widthDiff = width-widthShould
					drawRect(
						color = ColorProgressBg,
						size = size.copy(widthShould)
					)
					drawRect(
						color =
							if(shouldBeAt == 0f) ColorProgressBg
							else if(widthDiff > 0) ColorProgressBehind
							else ColorProgressAhead,
						topLeft = Offset(widthShould, 0f),
						size = size.copy(widthDiff)
					)
				}
				BasicText(
					modifier = Modifier.fillMaxSize().padding(4.dp),
					text =
						if(fullTargetProgress() == 0f) diff.toLong().prettyDurationShort()
						else if(diff < 0) "-" + (-diff).toLong().prettyDurationShort()
						else "+" + diff.toLong().prettyDurationShort(),
					style = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
					color = { if(isPaused) ColorInactive else ColorWhite },
					maxLines = 1,
					autoSize = TextAutoSize.StepBased(minFontSize = 8.sp)
				)
			}
		}
		if(workoutData.primaryTargetType == TargetType.DURATION){
			Field(
				modifier = Modifier.weight(1f)
					.padding(horizontal = if(Main.isScreenRound) Main.vw25 else 5.dp),
				value = workoutData.distanceTotal.prettyDistance(),
				unitText =
					if(workoutData.distanceTotal >= 1000) R.string.field_km
					else R.string.field_m,
				isActive = !isPaused
			)
		}else{
			Field(
				modifier = Modifier.weight(1f)
					.padding(horizontal = if(Main.isScreenRound) Main.vw25 else 5.dp),
				value = workoutData.durationTotal.prettyDurationShort(),
				isActive = !isPaused
			)
		}
	}
	TargetProgress(targetProgress, targetAchieved)
}

@Composable
fun TargetBarHeartRate(
	modifier: Modifier,
	heartRate: Double?,
	targetLower: Int,
	targetHigher: Int
){
	Box(modifier
		.padding(horizontal = if(Main.isScreenRound) Main.vw7 else 5.dp)
		.border(1.dp, ColorProgressBg)
	){

		val noLowerBand = targetLower < 30
		val band = targetHigher - targetLower
		val outerBand = band/2
		Canvas(Modifier.fillMaxSize()){
			val w25 = size.width/4
			drawRect(
				color = ColorProgressBg,
				topLeft = Offset(if(noLowerBand) 0f else w25, 0f),
				size = size.copy(if(noLowerBand) w25*3 else w25*2)
			)
			if(heartRate != null){
				if(heartRate < targetLower) {
					var width = (targetLower - heartRate).toFloat() / outerBand * w25
					if(width > w25) width = w25
					drawRect(
						color = ColorProgressBehind,
						topLeft = Offset(w25 - width, 0f),
						size = size.copy(width)
					)
				}else if(heartRate > targetHigher){
					var width = (heartRate - targetHigher).toFloat() / outerBand * w25
					if(width > w25) width = w25
					drawRect(
						color = ColorProgressBehind,
						topLeft = Offset(w25*3, 0f),
						size = size.copy(width)
					)
				}else{
					val x = ((heartRate - targetLower).toFloat() / band) * w25 * (if(noLowerBand) 3 else 2)
					drawLine(
						color = ColorWhite,
						start = Offset(if(noLowerBand) x else w25+x, 0f),
						end = Offset(if(noLowerBand) x else w25+x, size.height),
						strokeWidth = 2.dp.toPx()
					)
				}
			}
		}
		BasicText(
			modifier = Modifier.fillMaxSize().padding(4.dp),
			text = stringResource(R.string.field_bpm, heartRate?.pretty() ?: ""),
			style = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
			color = { ColorWhite },
			maxLines = 1,
			autoSize = TextAutoSize.StepBased(minFontSize = 8.sp)
		)
	}
}
@Composable
fun TargetBarSpeed(
	modifier: Modifier,
	target: Double,
	value: Double,
	text: String,
	isPaused: Boolean
){
	Box(modifier){
		Canvas(Modifier.fillMaxSize()){
			val width = size.width/2
			var widthDiff = (width*(value - target)/target).toFloat()
			//logD("value $value target $target width $width widthDiff $widthDiff")
			if(widthDiff > width) widthDiff = width
			if(widthDiff < -width) widthDiff = -width
			drawRect(
				color = if(widthDiff < 0) ColorProgressBehind else ColorProgressAhead,
				topLeft = Offset(size.width/2, 0f),
				size = size.copy(widthDiff)
			)
		}
		BasicText(
			modifier = Modifier.fillMaxSize().padding(4.dp),
			text = text,
			style = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
			color = { if(isPaused) ColorInactive else ColorWhite },
			maxLines = 1,
			autoSize = TextAutoSize.StepBased(minFontSize = 8.sp)
		)
	}
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewScreenTarget(){
	Main.GetScreenSpecs()
	val workoutData = WorkoutData()
	workoutData.pauseTotal = 100000
	workoutData.durationTotal = 40000
	workoutData.distanceTotal = 700
	workoutData.primaryTargetType = TargetType.DISTANCE
	workoutData.primaryTargetValue = 1
	workoutData.secondaryTargetType = TargetType.HEART_RATE
	workoutData.secondaryTargetValue = 1
	AppScaffold { FOTTheme { ScreenTarget(
		Modifier,
		workoutData,
		speed = 2.2,
		heartRate = 178.0,
		targetProgress = remember { mutableFloatStateOf(0.4F) },
		targetAchieved = remember { mutableStateOf(TargetAchieved.FULL) },
		isPaused = false
	) } }
}
