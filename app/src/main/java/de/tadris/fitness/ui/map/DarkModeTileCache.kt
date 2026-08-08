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
package de.tadris.fitness.ui.map

import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import org.mapsforge.core.graphics.TileBitmap
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.queue.Job

/**
 * A [TileCache] decorator that darkens every tile stored in it so the map
 * matches the app's dark theme. Both downloaded raster tiles and locally
 * rendered offline tiles pass through [put], while overlays (track, markers)
 * are drawn on top of the map and keep their original colors.
 */
class DarkModeTileCache(private val delegate: TileCache) : TileCache by delegate {

    override fun put(key: Job?, bitmap: TileBitmap?) {
        delegate.put(key, bitmap?.let { darkened(it) })
    }

    private fun darkened(source: TileBitmap): TileBitmap {
        val result = AndroidGraphicFactory.INSTANCE.createTileBitmap(source.width, true)
        result.timestamp = source.timestamp
        Canvas(AndroidGraphicFactory.getBitmap(result))
            .drawBitmap(AndroidGraphicFactory.getBitmap(source), 0f, 0f, darkModePaint)
        return result
    }

    companion object {
        // Inverts brightness while keeping hues intact (inversion followed by
        // a 180° hue rotation) so water and parks stay blue and green, the
        // same transformation as CSS "invert(100%) hue-rotate(180deg)".
        private val darkModePaint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(
                ColorMatrix(
                    floatArrayOf(
                        0.574f, -1.430f, -0.144f, 0f, 255f,
                        -0.426f, -0.430f, -0.144f, 0f, 255f,
                        -0.426f, -1.430f, 0.856f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
    }
}
