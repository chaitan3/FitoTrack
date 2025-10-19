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
package de.tadris.fitness.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.curvedText
import de.tadris.fitness.R
import de.tadris.fitness.data.SettingsStore
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.WorkoutDataStore
import de.tadris.fitness.ui.record.hasRotary
import de.tadris.fitness.ui.record.zoomLevelMax
import de.tadris.fitness.ui.record.zoomLevelMin
import de.tadris.fitness.util.logE
import de.tadris.fitness.util.toast
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.Dimension
import org.mapsforge.core.util.LatLongUtils
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.scalebar.DefaultMapScaleBar
import org.mapsforge.map.scalebar.MapScaleBar

class FullscreenMapActivity : ComponentActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var workoutData: WorkoutData? = null
        if(intent.hasExtra("workoutId")) {
            val workoutId = intent.getLongExtra("workoutId", 0)
            workoutData = WorkoutDataStore.workouts.firstOrNull { it.id == workoutId }
        }
        setContent {
            FOTTheme {
                FullscreenMap(
                    goBack = { finishAndRemoveTask() },
                    locationSamplesSet =
                        if(workoutData == null) null
                        else if (workoutData.legs.isEmpty()) setOf(workoutData.locationSamples)
                        else workoutData.legs
                            .filter { it.locationSamples.size > 10 }
                            .map { it.locationSamples }
                            .toSet()
                )
            }
        }
    }
}

@Composable
fun FullscreenMap(
    goBack: () -> Unit,
    locationSamplesSet: Set<List<WorkoutData.LocationSample>>?
){
    val context = LocalContext.current
    var mapView by remember { mutableStateOf(MapView(context)) }
    var mapZoomLevel by remember { mutableStateOf<Byte>(15) }
    var scrollDiff = 0f
    var scrollTime = 0L
    fun zoomIn(){
        if(mapZoomLevel >= zoomLevelMax) return
        mapZoomLevel++
        mapView.setZoomLevel(mapZoomLevel)
    }
    fun zoomOut(){
        if(mapZoomLevel <= zoomLevelMin) return
        mapZoomLevel--
        mapView.setZoomLevel(mapZoomLevel)
    }
    AndroidView(
        modifier = Modifier.fillMaxSize().onRotaryScrollEvent {
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
                    val tileLayerOnline = getTileLayerOnline(context, model)
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
                setBuiltInZoomControls(false)

                isFocusable = true
                isFocusableInTouchMode = true

                try {
                    var box: BoundingBox? = null
                    locationSamplesSet?.forEachIndexed { legIndex, locationSamples ->
                        val latLongs = locationSamples.map { it.toLatLong() }
                        val pathLine = when(legIndex) {
                            0 -> getMapPathLine()
                            1 -> getMapPathLine(ColorLocationCircle)
                            2 -> getMapPathLine(ColorProgressDone)
                            else -> getMapPathLine()
                        }
                        latLongs.forEach { pathLine.addPoint(it) }
                        layerManager.layers.add(pathLine)
                        box = if(box == null) BoundingBox(latLongs)
                        else box.extendBoundingBox(BoundingBox(latLongs))
                    }

                    box?.let {
                        box.extendMeters(50)
                        setCenter(box.centerPoint)
                        val calcZoomLevel = LatLongUtils.zoomForBounds(
                            Dimension(Main.vw80px, Main.vh80px),
                            box,
                            mapView.model.displayModel.tileSize
                        )
                        mapZoomLevel = calcZoomLevel.coerceIn(zoomLevelMin, zoomLevelMax)
                        setZoomLevel(mapZoomLevel)
                    }
                    post {
                        requestFocus()
                    }
                }catch (e: Exception){
                    logE("MapScreen: ${e.message}\n$e")
                    context.toast(R.string.failed)
                }
            }
            mapView
        }
    )
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
    if(!hasRotary(context)){
        Column(Modifier
            .fillMaxHeight()
            .padding(horizontal = 5.dp, vertical = if(Main.isScreenRound) Main.vh25 else 0.dp)
        ) {
            IconButton(
                onClick = ::zoomOut,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    modifier = Modifier.size(48.dp).padding(4.dp),
                    imageVector = ImageVector.vectorResource(R.drawable.ic_zoom_out),
                    tint = if(mapZoomLevel > zoomLevelMin) ColorWhite else ColorInactive,
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
                    tint = if(mapZoomLevel < zoomLevelMax) ColorWhite else ColorInactive,
                    contentDescription = "zoom in"
                )
            }
        }
    }
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        IconButton(onClick = goBack) {
            Icon(
                modifier = Modifier.size(48.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_map_up),
                contentDescription = "go back"
            )
        }
    }
}
