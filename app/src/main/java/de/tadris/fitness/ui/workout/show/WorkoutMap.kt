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

package de.tadris.fitness.ui.workout.show

import android.graphics.Color
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView
import de.tadris.fitness.Instance
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.preferences.UserPreferences
import de.tadris.fitness.map.GradientColoringStrategy
import de.tadris.fitness.map.MapManager
import de.tadris.fitness.map.SimpleColoringStrategy
import de.tadris.fitness.map.WorkoutLayer
import de.tadris.fitness.ui.workout.diagram.SampleConverter
import de.tadris.fitness.ui.workout.diagram.SpeedConverter
import de.tadris.fitness.util.ThemeUtils
import de.tadris.fitness.util.WorkoutCalculator
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.MapPosition
import org.mapsforge.core.util.LatLongUtils
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.layer.overlay.FixedPixelCircle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal sealed class WorkoutMapColoring {
    data object FollowPreference : WorkoutMapColoring()
    data object None : WorkoutMapColoring()
    data class Custom(val converter: SampleConverter) : WorkoutMapColoring()
}

@Composable
internal fun WorkoutMap(
    data: WorkoutData,
    modifier: Modifier = Modifier,
    showPauses: Boolean = false,
    clickable: Boolean = false,
    coloring: WorkoutMapColoring = WorkoutMapColoring.FollowPreference,
) {
    val activity = LocalActivity.current ?: return
    val holder = androidx.compose.runtime.remember {
        object {
            var layer: WorkoutLayer? = null
        }
    }

    AndroidView(
        factory = { ctx ->
            val mapView = MapManager(activity).setupMap()

            val instance = Instance.getInstance(ctx)
            val trackStyle = instance.userPreferences.trackStyle

            val coloringStrategy = when (trackStyle) {
                "purple_rain" -> GradientColoringStrategy.fromPattern(
                    GradientColoringStrategy.PATTERN_PURPLE,
                    true
                )

                "pink_mist" -> GradientColoringStrategy.fromPattern(
                    GradientColoringStrategy.PATTERN_PINK,
                    false
                )

                "rainbow_warrior" -> GradientColoringStrategy.fromPattern(
                    GradientColoringStrategy.PATTERN_MAP,
                    true
                )

                "height_map" -> GradientColoringStrategy.fromPattern(
                    GradientColoringStrategy.PATTERN_HEIGHT_MAP,
                    true
                )

                "bright_night" -> GradientColoringStrategy.fromPattern(
                    GradientColoringStrategy.PATTERN_BRIGHT,
                    false
                )

                "mondriaan" -> GradientColoringStrategy.fromPattern(
                    GradientColoringStrategy.PATTERN_YELLOW_RED_BLUE,
                    false
                )

                else -> SimpleColoringStrategy(
                    ThemeUtils.resolveThemeColor(
                        ctx,
                        android.R.attr.colorPrimary
                    )
                )
            }

            val primaryColor = ThemeUtils.resolveThemeColor(ctx, android.R.attr.colorPrimary)
            val workoutLayer =
                WorkoutLayer(data.samples, SimpleColoringStrategy(primaryColor), coloringStrategy)
            holder.layer = workoutLayer

            if (coloring == WorkoutMapColoring.FollowPreference &&
                instance.userPreferences.trackStyleMode == UserPreferences.STYLE_USAGE_ALWAYS
            ) {
                workoutLayer.setSampleConverter(data, SpeedConverter(ctx))
            }

            mapView.addLayer(workoutLayer)

            val bounds: BoundingBox = workoutLayer.boundingBox.extendMeters(50)
            mapView.postDelayed({
                mapView.model.mapViewPosition.mapPosition = MapPosition(
                    bounds.centerPoint,
                    LatLongUtils.zoomForBounds(
                        mapView.dimension, bounds,
                        mapView.model.displayModel.tileSize
                    )
                )
                mapView.animate().alpha(1f).setDuration(1000).start()
            }, 500)

            mapView.alpha = 0f
            mapView.isClickable = clickable

            if (showPauses) {
                val pBlue =
                    AndroidGraphicFactory.INSTANCE.createPaint().also { it.color = Color.BLUE }
                for (pauseLocation in WorkoutCalculator.getPausesFromWorkout(data)) {
                    val radius =
                        min(10f, max(2f, sqrt(pauseLocation.pause.duration / 1000.0).toFloat()))
                    mapView.addLayer(
                        FixedPixelCircle(pauseLocation.location, radius, pBlue, null)
                    )
                }
            }

            val pGreen =
                AndroidGraphicFactory.INSTANCE.createPaint().also { it.color = Color.GREEN }
            mapView.addLayer(FixedPixelCircle(data.samples.first().toLatLong(), 10f, pGreen, null))

            val pRed = AndroidGraphicFactory.INSTANCE.createPaint().also { it.color = Color.RED }
            mapView.addLayer(FixedPixelCircle(data.samples.last().toLatLong(), 10f, pRed, null))

            mapView
        },
        update = { _ ->
            val layer = holder.layer ?: return@AndroidView
            when (coloring) {
                WorkoutMapColoring.FollowPreference -> {}
                WorkoutMapColoring.None -> layer.setSampleConverter(data, null)
                is WorkoutMapColoring.Custom -> layer.setSampleConverter(data, coloring.converter)
            }
        },
        onRelease = { mapView ->
            mapView.destroyAll()
            AndroidGraphicFactory.clearResourceMemoryCache()
        },
        modifier = modifier.clipToBounds(),
    )
}
