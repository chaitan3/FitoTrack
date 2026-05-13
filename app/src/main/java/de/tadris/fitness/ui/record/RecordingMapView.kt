/*
 * Copyright (c) 2026 Jannis Scheibe <jannis@tadris.de>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     FitoTrack is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.tadris.fitness.ui.record

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.tadris.fitness.R
import de.tadris.fitness.map.MapManager
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.download.TileDownloadLayer
import org.mapsforge.map.layer.overlay.FixedPixelCircle
import org.mapsforge.map.layer.overlay.Polyline

class RecordingMapController {
    internal var mapView: MapView? = null
    internal var navigationModeHandler: NavigationModeHandler? = null

    fun zoomIn() {
        mapView?.model?.mapViewPosition?.zoomIn()
    }

    fun zoomOut() {
        mapView?.model?.mapViewPosition?.zoomOut()
    }

    fun focusGps() {
        navigationModeHandler?.onClick(null)
    }
}

@Composable
fun RecordingMapView(
    routePositions: List<LatLong>,
    currentPosition: LatLong?,
    primaryColor: Int,
    controller: RecordingMapController = remember { RecordingMapController() },
    modifier: Modifier = Modifier,
) {
    val activity = LocalActivity.current ?: return
    val lifecycleOwner = LocalLifecycleOwner.current

    val locationCircleArgb = colorResource(R.color.locationCircle).toArgb()
    val locationCircleStrokeArgb = colorResource(R.color.locationCircleStroke).toArgb()

    // Track layers across updates
    val layerHolder = remember { object {
        var polyline: Polyline? = null
        var locationCircle: FixedPixelCircle? = null
    }}

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val mv = controller.mapView ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mv.layerManager.layers.forEach { layer ->
                        if (layer is TileDownloadLayer) layer.onResume()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mv.layerManager.layers.forEach { layer ->
                        if (layer is TileDownloadLayer) layer.onPause()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { _ ->
            val mapView = MapManager(activity).setupMap()

            val navHandler = NavigationModeHandler(mapView)
            navHandler.init()

            controller.mapView = mapView
            controller.navigationModeHandler = navHandler

            mapView
        },
        update = { mapView ->
            val layers = mapView.layerManager.layers

            // Remove old route polyline
            layerHolder.polyline?.let { layers.remove(it) }
            layerHolder.polyline = null

            // Remove old location circle
            layerHolder.locationCircle?.let { layers.remove(it) }
            layerHolder.locationCircle = null

            // Add polyline if we have positions
            if (routePositions.isNotEmpty()) {
                val paint = AndroidGraphicFactory.INSTANCE.createPaint()
                paint.color = primaryColor
                paint.strokeWidth = 20f
                paint.setStyle(Style.STROKE)
                val polyline = Polyline(paint, AndroidGraphicFactory.INSTANCE)
                polyline.setPoints(routePositions)
                mapView.addLayer(polyline)
                layerHolder.polyline = polyline
            }

            // Add current position circle
            if (currentPosition != null) {
                val fillPaint = AndroidGraphicFactory.INSTANCE.createPaint()
                fillPaint.setStyle(Style.FILL)
                fillPaint.color = locationCircleArgb

                val strokePaint = AndroidGraphicFactory.INSTANCE.createPaint()
                strokePaint.color = locationCircleStrokeArgb
                strokePaint.setStyle(Style.STROKE)
                strokePaint.strokeWidth = 20f

                val circle = FixedPixelCircle(currentPosition, 15f, fillPaint, strokePaint)
                mapView.addLayer(circle)
                layerHolder.locationCircle = circle
            }
        },
        onRelease = { mapView ->
            controller.navigationModeHandler?.removeNavigationModeListener()
            controller.navigationModeHandler?.deinit()
            controller.mapView = null
            controller.navigationModeHandler = null
            mapView.destroyAll()
            AndroidGraphicFactory.clearResourceMemoryCache()
        },
        modifier = modifier.clipToBounds(),
    )
}
