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

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme.colorScheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.curvedText
import androidx.wear.tooling.preview.devices.WearDevices
import de.tadris.fitness.R
import de.tadris.fitness.data.SettingsStore
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.ui.ColorBlack
import de.tadris.fitness.ui.ColorInactive
import de.tadris.fitness.ui.ColorLocationCircle
import de.tadris.fitness.ui.ColorWhite
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.Main
import de.tadris.fitness.ui.TargetProgress
import de.tadris.fitness.ui.getMapPathLine
import de.tadris.fitness.ui.getTileLayerLocal
import de.tadris.fitness.ui.getTileLayerOnline
import de.tadris.fitness.ui.record.RecordActivity.TargetAchieved
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.Dimension
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.util.LatLongUtils
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.download.TileDownloadLayer
import org.mapsforge.map.layer.overlay.FixedPixelCircle
import org.mapsforge.map.scalebar.DefaultMapScaleBar
import org.mapsforge.map.scalebar.MapScaleBar
import org.mapsforge.map.view.InputListener

enum class MapCenter {NOT, POSITION, ROUTE, FINISH}
const val zoomLevelMin: Byte = 10
const val zoomLevelMax: Byte = 18
@Composable
fun ScreenMap(
	workoutData: WorkoutData,
	targetProgress: MutableFloatState,
	targetAchieved: MutableState<TargetAchieved>,
	isPaused: Boolean,
	hasOtherScreens: Boolean,
	mapCenter: MutableState<MapCenter>,
	mapZoomLevel: MutableState<Byte>,
	mapPosition: MutableState<LatLong?>,
	goBack: () -> Unit
){
	val context = LocalContext.current
	val initLatLong = workoutData.locationSamplesFiltered.lastOrNull()?.toLatLong()
		?: LatLong(0.0, 0.0)
	var mapView by remember { mutableStateOf(MapView(context)) }
	val latLongs = remember { mutableStateListOf<LatLong>() }
	val locationMarker = remember { locationMarker(initLatLong) }
	val pathLine = remember { getMapPathLine() }
	val hasRotary = remember { hasRotary(context) }
	var mapLoaded by remember { mutableStateOf(false) }
	var tileLayerOnline: TileDownloadLayer? = null
	var scrollDiff = 0f
	var scrollTime = 0L
	fun centerLatLongs(latLongs: List<LatLong>) {
		val box = (BoundingBox(latLongs)).extendMeters(50)
		mapView.setCenter(box.centerPoint)
		val calcZoomLevel = LatLongUtils.zoomForBounds(
			Dimension(Main.vw80px, Main.vh80px),
			box,
			mapView.model.displayModel.tileSize
		)
		mapZoomLevel.value = calcZoomLevel.coerceIn(zoomLevelMin, zoomLevelMax)
		mapView.setZoomLevel(mapZoomLevel.value)
	}
	fun centerMap(){
		val lastLatLong = latLongs.last()
		locationMarker.setLatLong(lastLatLong)
		when(mapCenter.value) {
			MapCenter.NOT -> locationMarker.requestRedraw()
			MapCenter.POSITION -> mapView.setCenter(lastLatLong)
			MapCenter.ROUTE -> centerLatLongs(latLongs)
			MapCenter.FINISH -> centerLatLongs(listOf(latLongs.first(), lastLatLong))
		}
	}
	fun zoomIn(){
		if(mapZoomLevel.value >= zoomLevelMax) return
		mapZoomLevel.value++
		mapView.setZoomLevel(mapZoomLevel.value)
		if(mapCenter.value in setOf(MapCenter.ROUTE, MapCenter.FINISH))
			mapCenter.value = MapCenter.NOT
	}
	fun zoomOut(){
		if(mapZoomLevel.value <= zoomLevelMin) return
		mapZoomLevel.value--
		mapView.setZoomLevel(mapZoomLevel.value)
		if(mapCenter.value in setOf(MapCenter.ROUTE, MapCenter.FINISH))
			mapCenter.value = MapCenter.NOT
	}
	LaunchedEffect(workoutData.locationSamplesFiltered.size) {
		if(latLongs.isEmpty() && workoutData.locationSamplesFiltered.isNotEmpty()){
			workoutData.locationSamplesFiltered
				.map { it.toLatLong() }
				.let {
					latLongs.addAll(it)
					pathLine.addPoints(it)
				}
		}else {
			workoutData.locationSamplesFiltered.lastOrNull()?.let {
				latLongs.add(it.toLatLong())
				pathLine.addPoint(it.toLatLong())
			}
		}
		if(latLongs.isNotEmpty()) centerMap()
	}
	LaunchedEffect(isPaused) {
		locationMarker.paintStroke.color = if(isPaused) ColorInactive.toArgb() else ColorWhite.toArgb()
	}
	DisposableEffect(Unit) {
		onDispose {
			mapPosition.value = mapView.model.mapViewPosition.center
			tileLayerOnline?.onPause()
		}
	}
	val mapInputListener = remember { object: InputListener {
		override fun onMoveEvent() {
			if(mapCenter.value != MapCenter.NOT) mapCenter.value = MapCenter.NOT
		}
		override fun onZoomEvent() {
			if(mapCenter.value in setOf(MapCenter.ROUTE, MapCenter.FINISH))
				mapCenter.value = MapCenter.NOT
		}
	} }
	if(latLongs.isNotEmpty()) {
		AndroidView(
			modifier = Modifier
				.fillMaxSize()
				.onRotaryScrollEvent {
					if(scrollTime < it.uptimeMillis - 200) scrollDiff = 0f
					scrollTime = it.uptimeMillis
					scrollDiff += it.verticalScrollPixels
					if(scrollDiff > 200) {
						zoomIn()
						scrollDiff = 0f
					} else if(scrollDiff < -200) {
						zoomOut()
						scrollDiff = 0f
					}
					true
				},
			factory = { context ->
				mapView = MapView(context).apply {
					AndroidGraphicFactory.createInstance(context)
					isClickable = true

					if(SettingsStore.getMapSource(context)) {
						layerManager.layers.add(getTileLayerLocal(context, model))
					} else {
						tileLayerOnline = getTileLayerOnline(context, model)
						layerManager.layers.add(tileLayerOnline)
						tileLayerOnline.onResume()
					}
					val scaleBar = DefaultMapScaleBar(
						model.mapViewPosition,
						model.mapViewDimension,
						AndroidGraphicFactory.INSTANCE,
						model.displayModel
					)
					scaleBar.setScaleBarPosition(MapScaleBar.ScaleBarPosition.BOTTOM_CENTER)
					scaleBar.scaleBarMode = DefaultMapScaleBar.ScaleBarMode.SINGLE
					mapScaleBar = scaleBar

					setZoomLevelMin(zoomLevelMin)
					setZoomLevelMax(zoomLevelMax)
					setZoomLevel(mapZoomLevel.value)
					setBuiltInZoomControls(false)

					isFocusable = true
					isFocusableInTouchMode = true

					layerManager.layers.add(pathLine)
					layerManager.layers.add(locationMarker)
					addInputListener(mapInputListener)

					post {
						requestFocus()
						mapPosition.value?.let { da -> mapView.setCenter(da) }
						centerMap()
						mapLoaded = true
					}
				}
				mapView
			}
		)
	}
	if(Main.isScreenRound){
		CurvedLayout(modifier = Modifier.fillMaxSize(), anchor = 35f) {
			curvedText(
				text = context.getString(R.string.osm_copyright),
				color = ColorBlack,
				fontSize = 8.sp,
				angularDirection = CurvedDirection.Angular.CounterClockwise
			)
		}
	} else {
		Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
			Text(
				text = stringResource(R.string.osm_copyright),
				color = ColorBlack,
				fontSize = 8.sp
			)
		}
	}
	Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
		IconButton(
			onClick = {
				mapCenter.value = when(mapCenter.value) {
					MapCenter.NOT, MapCenter.FINISH -> MapCenter.POSITION
					MapCenter.POSITION -> MapCenter.ROUTE
					MapCenter.ROUTE -> MapCenter.FINISH
				}
				centerMap()
			}
		) {
			Icon(
				modifier = Modifier
					.size(48.dp)
					.padding(4.dp),
				imageVector = ImageVector.vectorResource(
					when(mapCenter.value){
						MapCenter.POSITION -> R.drawable.ic_map_center_pos
						MapCenter.ROUTE -> R.drawable.ic_map_center_route
						MapCenter.FINISH -> R.drawable.ic_map_center_finish
						else -> R.drawable.ic_map_center_not
					}
				),
				tint = Color.Unspecified,
				contentDescription = "map center type"
			)
		}
	}
	if(!hasRotary){
		Column(Modifier
			.fillMaxHeight()
			.padding(
				horizontal = 5.dp, vertical =
					if(Main.isScreenRound) Main.vh25 else 0.dp
			)) {
			IconButton(
				onClick = ::zoomOut,
				modifier = Modifier.weight(1f)
			) {
				Icon(
					modifier = Modifier.size(48.dp).padding(4.dp),
					imageVector = ImageVector.vectorResource(R.drawable.ic_zoom_out),
					tint = if(mapZoomLevel.value > zoomLevelMin) ColorWhite else ColorInactive,
					contentDescription = "zoom out"
				)
			}
			IconButton(
				onClick = ::zoomIn,
				modifier = Modifier.weight(1f)
			) {
				Icon(
					modifier = Modifier.size(48.dp).padding(4.dp),
					imageVector = ImageVector.vectorResource(R.drawable.ic_zoom_in),
					tint = if(mapZoomLevel.value < zoomLevelMax) ColorWhite else ColorInactive,
					contentDescription = "zoom in"
				)
			}
		}
	}
	if(!mapLoaded){
		Box(
			modifier = Modifier.fillMaxSize().background(colorScheme.background),
			contentAlignment = Alignment.Center
		){ Text(text = stringResource(R.string.map_waiting)) }
	}
	Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
		IconButton(onClick = goBack) {
			Icon(
				modifier = Modifier.size(48.dp).padding(top = 15.dp)
					.then(if(!hasOtherScreens) Modifier.rotate(-90f) else Modifier),
				imageVector = ImageVector.vectorResource(R.drawable.ic_map_up),
				contentDescription = "go back"
			)
		}
	}
	TargetProgress(targetProgress, targetAchieved)
}

fun locationMarker(initLatLong: LatLong): FixedPixelCircle{
	val paintStroke = AndroidGraphicFactory.INSTANCE.createPaint()
	paintStroke.setStyle(Style.STROKE)
	paintStroke.strokeWidth = 8f
	val paintFill = AndroidGraphicFactory.INSTANCE.createPaint()
	paintFill.setStyle(Style.FILL)
	paintFill.color = ColorLocationCircle.toArgb()
	return FixedPixelCircle(initLatLong, 4f, paintFill, paintStroke)
}

fun hasRotary(context: Context): Boolean {
	val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
	return inputManager.inputDeviceIds.any {
		inputManager.getInputDevice(it)
			?.supportsSource(InputDevice.SOURCE_ROTARY_ENCODER) == true
	}
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewScreenMap() {
	Main.GetScreenSpecs()
	val workoutData = WorkoutData()
	AppScaffold { FOTTheme {
		ScreenMap(
			workoutData,
			targetProgress = remember { mutableFloatStateOf(0.9F) },
			targetAchieved = remember { mutableStateOf(TargetAchieved.NOT) },
			isPaused = false,
			hasOtherScreens = true,
			mapCenter = remember { mutableStateOf(MapCenter.NOT) },
			mapZoomLevel = remember { mutableStateOf(15) },
			mapPosition = remember { mutableStateOf(null) },
			goBack = {}
		)
	} }
}
