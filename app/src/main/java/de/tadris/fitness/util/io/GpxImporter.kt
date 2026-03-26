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
package de.tadris.fitness.util.io

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.github.sisyphsu.dateparser.DateParserUtils
import de.tadris.fitness.data.HeartRateSample
import de.tadris.fitness.data.RouteSample
import de.tadris.fitness.data.Workout
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.util.gpx.Gpx
import de.tadris.fitness.util.gpx.Track
import de.tadris.fitness.util.io.general.IWorkoutImporter
import de.tadris.fitness.util.io.general.IWorkoutImporter.WorkoutImportResult
import java.io.IOException
import java.io.InputStream
import java.util.Date
import java.util.UUID

class GpxImporter : IWorkoutImporter {
    private lateinit var gpx: Gpx

    @Throws(IOException::class)
    override fun readWorkouts(input: InputStream?): WorkoutImportResult {
        getGpx(input)

        require(
            !(gpx.getTrk().isEmpty()
                    || gpx.getTrk()[0].getTrkseg().isEmpty()
                    || gpx.getTrk()[0].getTrkseg()[0].getTrkpt().isEmpty())
        ) { "given GPX file does not contain location data" }

        val workouts = mutableListOf<WorkoutData>()
        for (track in gpx.getTrk()) {
            workouts.add(getWorkoutDataFromTrack(track))
        }

        return WorkoutImportResult(workouts)
    }

    @Throws(IOException::class)
    private fun getGpx(input: InputStream?) {
        val mapper = XmlMapper()
        mapper.configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
        gpx = mapper.readValue(input, Gpx::class.java)
    }

    private fun getWorkoutDataFromTrack(track: Track): WorkoutData {
        val firstSegment = track.getTrkseg().first()
        val firstPoint = firstSegment.getTrkpt().first()

        val startTime = firstPoint.time

        if (startTime == null || startTime.isEmpty()) {
            throw RuntimeException("The GPX file doesn't include timestamps.")
        }

        val index = firstSegment.getTrkpt().size
        val lastTime = firstSegment.getTrkpt()[index - 1].time
        val start = parseDate(startTime).time
        val end = parseDate(lastTime).time

        val routeSamples = track.getRouteSamples()

        val heartRateSamples = track.getHrSamples()

        val workout = Workout(
            id = -1,
            uuid = UUID.randomUUID().toString(),
            start = start,
            end = end,
            activeDuration = end - start,
            comment = track.getComment(),
            distance = 0.0,
            repetitions = 0,
            energy = 0.0,
            workoutTypeId = track.getTypeId(),
            intervalSetUsedId = -1,
        )

        return WorkoutData(
            workout = workout,
            samples = routeSamples,
            pauses = emptyList(),
            events = emptyList(),
            repetitionSamples = emptyList(),
            heartRateSamples = heartRateSamples
        )
    }

    private fun Track.getRouteSamples() = trkseg.asSequence().flatMap { it.trkpt }.map { point ->
        RouteSample(
            workoutId = -1,
            id = -1,
            timestamp = parseDate(point.time).time,
            relativeTime = -1,
            latitude = point.lat,
            longitude = point.lon,
            elevation = point.ele,
            elevationMSL = point.ele, // MSL from GPX
            speed = point.extensions?.speed ?: 0.0,
            pressure = -1f
        )
    }.toList()

    private fun Track.getHrSamples() = trkseg.asSequence().flatMap { it.trkpt }.mapNotNull { point ->
        val hr = point.extensions?.gpxTpxExtension?.hr ?: 0
        return@mapNotNull if(hr > 0){
            HeartRateSample(
                workoutId = -1,
                id = -1,
                timestamp = parseDate(point.time).time,
                relativeTime = -1,
                heartRate = hr
            )
        } else null
    }.toList()

    private fun Track.getTypeId() = getTypeIdById(getType())

    private fun Track.getComment() = when {
        name != null -> name
        desc != null -> desc
        gpx.metadata?.name != null -> gpx.metadata.name
        gpx.metadata?.desc != null -> gpx.metadata.desc
        else -> null
    }

    companion object {

        private fun parseDate(str: String): Date {
            try {
                // Need parseCalendar because parseDate seems to be corrupted.
                // The hour is always one lesser than the original time.
                return DateParserUtils.parseCalendar(str).getTime()
            } catch (e: Exception) {
                e.printStackTrace()
                throw RuntimeException("Cannot parse timestamps: " + e.message, e.cause)
            }
        }

        private fun getTypeIdById(id: String?): String {
            return when (id) {
                null -> ""
                "1" -> "running"
                "2" -> "cycling"
                "11" -> "walking"
                else -> id
            }
        }
    }
}