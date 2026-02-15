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
package de.tadris.fitness.ui.workouts

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import de.tadris.fitness.R
import de.tadris.fitness.data.SettingsStore
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.WorkoutData.TargetType
import de.tadris.fitness.data.WorkoutData.WorkoutType
import de.tadris.fitness.data.WorkoutDataStore
import de.tadris.fitness.ui.Chart
import de.tadris.fitness.ui.ChartData
import de.tadris.fitness.ui.ColorFOT
import de.tadris.fitness.ui.ColorLocationCircle
import de.tadris.fitness.ui.ColorProgressDone
import de.tadris.fitness.ui.ConfirmDialog
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.FullscreenMapActivity
import de.tadris.fitness.ui.Main
import de.tadris.fitness.ui.getEndMarker
import de.tadris.fitness.ui.getMapPathLine
import de.tadris.fitness.ui.getStartMarker
import de.tadris.fitness.ui.getTileLayerLocal
import de.tadris.fitness.ui.getTileLayerOnline
import de.tadris.fitness.ui.record.MultisportTransition
import de.tadris.fitness.ui.record.RecordActivity
import de.tadris.fitness.util.logE
import de.tadris.fitness.util.prettyDistance
import de.tadris.fitness.util.prettyDurationShort
import de.tadris.fitness.util.prettySpeed
import de.tadris.fitness.util.runInBackground
import de.tadris.fitness.util.toast
import de.tadris.fitness.util.toastFromBG
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.Dimension
import org.mapsforge.core.util.LatLongUtils
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.view.MapView

class WorkoutActivity : ComponentActivity() {
	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val workoutData = WorkoutDataStore.workouts.firstOrNull()
		if(workoutData == null) {
			toast(R.string.failed)
			finishAndRemoveTask()
			return
		}
		if(workoutData.workoutType == WorkoutType.MULTISPORT){
			setContent {
				FOTTheme {
					MultisportTransition(rootWorkoutData = workoutData)
				}
			}
		} else {
			setContent {
				FOTTheme {
					Workout(
						workoutData,
						onDelWorkout = { id ->
							WorkoutDataStore.deleteWorkout(this, id) {
								toastFromBG(R.string.error_workout_delete)
							}
							finishAndRemoveTask()
						},
						onResumeClick = { resumeId ->
							finishAndRemoveTask()
							startActivity(
								Intent(this, RecordActivity::class.java)
									.putExtra("resumeId", resumeId)
							)
						},
						onFullscreenMap = {
							startActivity(
								Intent(this, FullscreenMapActivity::class.java)
									.putExtra("workoutId", workoutData.id)
							)
						}
					)
				}
			}
		}
	}
}

@Composable
fun Workout(
	workoutData: WorkoutData,
	onDelWorkout: (Long) -> Unit,
	onResumeClick: (Long) -> Unit,
	onFullscreenMap: () -> Unit
){
	val context = LocalContext.current
	val columnState = rememberTransformingLazyColumnState()
	var locationSamplesAvailable by remember { mutableStateOf(false) }
	var showMap by remember { mutableStateOf(false) }
	var showSpeedChart by remember { mutableStateOf(false) }
	var showElevationChart by remember { mutableStateOf(false) }
	var showHeartRateChart by remember { mutableStateOf<Boolean?>(null) }
	var delWorkout by remember { mutableStateOf(false) }
	var resume = false
	fun Long.timeToDuration(): Long {
		if(workoutData.checkpoints.isEmpty()) return 0
		var checkpoint = workoutData.checkpoints[0]
		for(i in 1..<workoutData.checkpoints.size) {
			if(workoutData.checkpoints[i].time > this) {
				if(workoutData.checkpoints[i].duration == checkpoint.duration) { return checkpoint.duration }//during a break
				else { break }
			}
			checkpoint = workoutData.checkpoints[i]
		}
		return this - checkpoint.time + checkpoint.duration
	}
	DisposableEffect(Unit) {
		onDispose {
			if(!resume) {
				workoutData.locationSamples.clear()
				workoutData.heartRateSamples.clear()
			}
		}
	}

	ScreenScaffold(
		modifier = Modifier,
		scrollState = columnState,
		contentPadding = rememberResponsiveColumnPadding()
	) { contentPadding ->
		TransformingLazyColumn(state = columnState, contentPadding = contentPadding) {
			item { Text(text = stringResource(R.string.summary), fontSize = 22.sp) }
			item {
				Icon(
					modifier = Modifier.size(40.dp),
					imageVector = ImageVector.vectorResource(workoutData.workoutTypeIcon),
					contentDescription = "workout type icon"
				)
			}
			if(workoutData.id == WorkoutDataStore.workouts.firstOrNull()?.id &&
				workoutData.endTime > System.currentTimeMillis() - 14400000 //ended less than 4 hours ago
			) {
				item {
					TextButton(
						modifier = Modifier.fillMaxWidth(),
						onClick = {
							resume = true
							onResumeClick(workoutData.id)
						}
					) {
						Text(text = stringResource(R.string.resume))
					}
				}
			}
			item { Text(text = stringResource(R.string.duration), color = ColorFOT) }
			if(workoutData.pauseTotal == 0L) {
				item { Text(text = workoutData.durationTotal.prettyDurationShort()) }
			} else {
				item { Text(text = stringResource(R.string.summary_duration_label)) }
				item {
					Text(
						text = stringResource(
							R.string.summary_duration,
							workoutData.durationTotal.prettyDurationShort(),
							workoutData.pauseTotal.prettyDurationShort(),
							workoutData.durationTotal
								.plus(workoutData.pauseTotal)
								.prettyDurationShort()
						)
					)
				}
			}
			if(workoutData.isWorkoutTypeGps) {
				fun readLocations(onDone: () -> Unit) = runInBackground {
					WorkoutDataStore.readLocations(context, workoutData, onDone, filter = true)
				}
				item { Text(text = stringResource(R.string.distance), color = ColorFOT) }
				item {
					Text(
						text = workoutData.distanceTotal.prettyDistance() +
							if(workoutData.distanceTotal >= 1000) " km" else " m"
					)
				}
				if(!showMap){
					item {
						TextButton(
							modifier = Modifier.fillMaxWidth(),
							onClick = { showMap = true }
						) { Text(text = stringResource(R.string.show_map)) }
					}
				}else if(!locationSamplesAvailable){
					readLocations { locationSamplesAvailable = true }
					item { Text(text = stringResource(R.string.loading)) }
				}else{
					item {
						Box(modifier = Modifier.clickable { onFullscreenMap() }) {
							WorkoutMap(locationSamplesSet =
								if(workoutData.legs.isEmpty()) setOf(workoutData.locationSamples)
								else workoutData.legs
									.filter { it.locationSamples.size > 10 }
									.map { it.locationSamples }
									.toSet()
							)
						}
					}
				}
				item { Text(text = stringResource(R.string.speed), color = ColorFOT) }
				item { Text(text =
					stringResource(
						R.string.summary_kmh,
						workoutData.speedAvg.prettySpeed()
						,workoutData.speedMax.prettySpeed()
					)
				) }

				if(!showSpeedChart){
					item {
						if(workoutData.workoutType == WorkoutType.MULTISPORT) return@item
						TextButton(
							modifier = Modifier.fillMaxWidth(),
							onClick = { showSpeedChart = true }
						) { Text(text = stringResource(R.string.show_chart)) }
					}
				}else if(!locationSamplesAvailable){
					readLocations { locationSamplesAvailable = true }
					item { Text(text = stringResource(R.string.loading)) }
				}else{
					if(workoutData.primaryTargetType == TargetType.SPEED) {
						item { Text(text = stringResource(R.string.target_kmh,
							workoutData.primaryTargetValue.toDouble().prettySpeed()
						)) }
					}
					if(workoutData.secondaryTargetType == TargetType.SPEED) {
						item { Text(text = stringResource(R.string.target_kmh,
							workoutData.secondaryTargetValue.toDouble().prettySpeed()
						)) }
					}
					item {
						val skipFrames = remember(workoutData.locationSamples.size) {
							maxOf(5, workoutData.locationSamples.size / Main.vw80px)
						}
						val chartData = remember(workoutData.locationSamples.size) {
							workoutData.locationSamples
								.filterIndexed { i, it -> i % skipFrames == 0 && it.speed > 0.0 }
								.map { ChartData(it.time.timeToDuration(), it.speed) }
						}
						Row(Modifier.size(width = Main.vw80, height = Main.vh50)) {
							Chart(
								data = chartData,
								target1 =
									if(workoutData.primaryTargetType == TargetType.SPEED)
										workoutData.primaryTargetValue.toDouble()
									else null,
								target2 =
									if(workoutData.secondaryTargetType == TargetType.SPEED)
										workoutData.secondaryTargetValue.toDouble()
									else null,
								yMinInit = 0.0,
								yMaxInit = 1.11,
								isSpeed = true
							)
						}
					}
				}
				if(workoutData.isWorkoutTypeEle &&
					workoutData.elevationGainTotal + workoutData.elevationLossTotal > 10
				){
					item { Text(text = stringResource(R.string.elevation), color = ColorFOT) }
					item { Text(text =
						stringResource(
							R.string.summary_ele,
							workoutData.elevationGainTotal
							,workoutData.elevationLossTotal
						)
					) }
					if(!showElevationChart){
						item {
							if(workoutData.workoutType == WorkoutType.MULTISPORT) return@item
							TextButton(
								modifier = Modifier.fillMaxWidth(),
								onClick = {
									showElevationChart = workoutData.locationSamples.isNotEmpty()
								}
							) { Text(text = stringResource(R.string.show_chart)) }
						}
					}else if(!locationSamplesAvailable){
						readLocations { showElevationChart = true }
						item { Text(text = stringResource(R.string.loading)) }
					}else{
						item {
							val skipFrames = remember(workoutData.locationSamples.size) {
								maxOf(5, workoutData.locationSamples.size / Main.vw80px)
							}
							val chartData = remember(workoutData.locationSamples.size) {
								workoutData.locationSamples
									.filterIndexed { i, it -> i % skipFrames == 0 && it.ele != 0.0 }
									.map { ChartData(it.time.timeToDuration(), it.ele) }
							}
							Row(Modifier.size(width = Main.vw80, height = Main.vh50)) {
								Chart(
									data = chartData,
									yMinInit = chartData.firstOrNull()?.value ?: 0.0,
									yMaxInit = chartData.firstOrNull()?.value ?: 10.0
								)
							}
						}
					}
				}
			}
			item { Text(text = stringResource(R.string.heart_rate), color = ColorFOT) }
			item { Text(text =
				stringResource(
					R.string.summary_bpm,
					workoutData.heartRateAvg
					,workoutData.heartRateMax
				)
			) }
			when(showHeartRateChart) {
				null -> item {
					if(workoutData.workoutType == WorkoutType.MULTISPORT) return@item
					TextButton(
						modifier = Modifier.fillMaxWidth(),
						onClick = { showHeartRateChart = false }
					) { Text(text = stringResource(R.string.show_chart)) }
				}
				false -> {
					runInBackground {
						workoutData.heartRateSamples.clear()
						WorkoutDataStore.readHeartRates(
							context,
							workoutData,
							onDone = { showHeartRateChart = true }
						)
					}
					item { Text(text = stringResource(R.string.loading)) }
				}
				true -> {
					val skipFrames = maxOf(5, workoutData.heartRateSamples.size / Main.vw80px)
					val chartData = workoutData.heartRateSamples
						.filterIndexed { i, it -> i % skipFrames == 0 && it.heartRate > 30 }
						.map { ChartData(it.time.timeToDuration(), it.heartRate) }
					var target1: Double? = null
					var target2: Double? = null
					if(workoutData.primaryTargetType == TargetType.HEART_RATE){
						target1 = workoutData.primaryTargetValue.toDouble()
						target2 = workoutData.primaryTargetValueHigh.toDouble()
					}else if(workoutData.secondaryTargetType == TargetType.HEART_RATE){
						target1 = workoutData.secondaryTargetValue.toDouble()
						target2 = workoutData.secondaryTargetValueHigh.toDouble()
					}
					if(target1 != null && target2 != null) {
						item { Text(text = stringResource(R.string.target_bpm,
							target1.toInt(), target2.toInt()
						)) }
					}
					item {
						Row(Modifier.size(width = Main.vw80, height = Main.vh50)) {
							Chart(
								data = chartData,
								target1,
								target2,
								yMinInit = workoutData.heartRateAvg.toDouble(),
								yMaxInit = workoutData.heartRateMax.toDouble()
							)
						}
					}
				}
			}
			item { Text(text = stringResource(R.string.calories), color = ColorFOT) }
			item { Text(text =
				stringResource(
					R.string.summary_kcal,
					workoutData.caloriesTotal
				)
			) }
			item {
				IconButton(onClick = { delWorkout = true }) {
					Icon(
						modifier = Modifier.size(40.dp),
						imageVector = ImageVector.vectorResource(R.drawable.ic_delete),
						contentDescription = "delete"
					)
				}
			}
		}
	}
	if(delWorkout) {
		ConfirmDialog(
			text = R.string.delete_workout,
			confirmText = R.string.delete,
			dismissText = R.string.cancel,
			onConfirm = { onDelWorkout(workoutData.id) },
			onDismiss = { delWorkout = false }
		)
	}
}

@Composable
fun WorkoutMap(locationSamplesSet: Set<List<WorkoutData.LocationSample>>){
	AndroidView(
		modifier = Modifier.size(Main.vw80, Main.vh80),
		factory = { context ->
			MapView(context).apply {
				clipToOutline = true
				AndroidGraphicFactory.createInstance(context)
				isClickable = false
				if(SettingsStore.getMapSource(context)) {
					layerManager.layers.add(getTileLayerLocal(context, model), false)
				} else {
					val tileLayerOnline = getTileLayerOnline(context, model)
					layerManager.layers.add(tileLayerOnline, false)
					tileLayerOnline.onResume()
				}
				mapScaleBar = null
				setBuiltInZoomControls(false)

				try {
					var box: BoundingBox? = null
					locationSamplesSet.forEachIndexed { legIndex, locationSamples ->
						val latLongs = locationSamples.map { it.toLatLong() }
						layerManager.layers.add(getStartMarker(latLongs.first()), false)
						val pathLine = when(legIndex) {
							0 -> getMapPathLine()
							1 -> getMapPathLine(ColorLocationCircle)
							2 -> getMapPathLine(ColorProgressDone)
							else -> getMapPathLine()
						}
						latLongs.forEach { pathLine.addPoint(it) }
						layerManager.layers.add(pathLine, false)
						getEndMarker(context, latLongs.last())?.let{ layerManager.layers.add(it, false) }
						box = if(box == null) BoundingBox(latLongs)
						else box.extendBoundingBox(BoundingBox(latLongs))
					}

					box?.let {
						setCenter(box.centerPoint)
						setZoomLevel(
							LatLongUtils.zoomForBounds(
								Dimension((Main.vw80px*.85).toInt(), (Main.vh80px*.85).toInt()),
								box,
								model.displayModel.tileSize
							).coerceIn(
								minimumValue = 2,
								maximumValue = 18
							)
						)
					}
				}catch (e: Exception){
					logE("WorkoutMap: ${e.message}\n$e")
					context.toast(R.string.failed)
				}
			}
		}
	)
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewWorkout() {
	Main.GetScreenSpecs()
	val workoutData = WorkoutData()
	workoutData.id = 0
	workoutData.pauseTotal = 100000
	workoutData.durationTotal = 200000
	workoutData.distanceTotal = 1100
	workoutData.workoutType = WorkoutType.CYCLE
	AppScaffold { FOTTheme { Workout(
		workoutData,
		onDelWorkout = {},
		onResumeClick = {},
		onFullscreenMap = {}
	)} }
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun PreviewWorkoutLarge() { PreviewWorkout() }
