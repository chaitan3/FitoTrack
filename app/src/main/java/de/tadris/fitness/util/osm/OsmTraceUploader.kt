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

package de.tadris.fitness.util.osm

import android.util.Log
import de.tadris.fitness.data.RouteSample
import de.tadris.fitness.data.Workout
import de.westnordost.osmapi.OsmConnection
import de.westnordost.osmapi.traces.GpsTraceDetails
import de.westnordost.osmapi.traces.GpsTracesApi
import de.westnordost.osmapi.traces.GpsTrackpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Collections

class OsmTraceUploader(
    private val workout: Workout,
    private val samples: List<RouteSample>,
    private val visibility: GpsTraceDetails.Visibility,
    private val oauthToken: String,
    private val cut: Boolean,
    private val description: String,
) {
    companion object {
        private const val CUT_DISTANCE = 300
    }

    suspend fun upload() = withContext(Dispatchers.IO) {
        val mutableSamples = samples.toMutableList()
        if (cut) {
            cutSamples(mutableSamples, last = false)
            cutSamples(mutableSamples, last = true)
        }

        val osm = OsmConnection("https://api.openstreetmap.org/api/0.6/", "FitoTrack", oauthToken)

        val trackpoints = mutableSamples.mapIndexed { index, sample ->
            GpsTrackpoint(
                GpsTraceLatLong(sample),
                Instant.ofEpochMilli(sample.timestamp),
                index == 0,
                0f,
                sample.elevation.toFloat(),
            )
        }

        GpsTracesApi(osm).create(
            workout.dateString,
            visibility,
            description,
            Collections.singletonList("FitoTrack"),
            trackpoints,
        )
    }

    private fun cutSamples(mutableSamples: MutableList<RouteSample>, last: Boolean) {
        var distance = 0.0
        var count = 0
        var lastSample = mutableSamples.removeAt(if (last) mutableSamples.lastIndex else 0)
        while (distance < CUT_DISTANCE) {
            val current = mutableSamples.removeAt(if (last) mutableSamples.lastIndex else 0)
            distance += lastSample.toLatLong().sphericalDistance(current.toLatLong())
            count++
            lastSample = current
        }
        Log.d("Uploader", "Cut ${if (last) "last" else "first"} $count samples ($distance meters)")
    }
}
