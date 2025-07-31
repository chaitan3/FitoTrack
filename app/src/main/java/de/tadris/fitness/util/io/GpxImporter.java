/*
 * Copyright (c) 2022 Jannis Scheibe <jannis@tadris.de>
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

package de.tadris.fitness.util.io;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.sisyphsu.dateparser.DateParserUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.tadris.fitness.data.RouteSample;
import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.util.gpx.Gpx;
import de.tadris.fitness.util.gpx.Track;
import de.tadris.fitness.util.gpx.TrackPoint;
import de.tadris.fitness.util.gpx.TrackPointExtensions;
import de.tadris.fitness.util.gpx.TrackSegment;
import de.tadris.fitness.util.io.general.IWorkoutImporter;

public class GpxImporter implements IWorkoutImporter {

    private Gpx gpx;

    @Override
    public WorkoutImportResult readWorkouts(InputStream input) throws IOException {
        getGpx(input);

        if (gpx.getTrk().size() == 0
                || gpx.getTrk().get(0).getTrkseg().size() == 0
                || gpx.getTrk().get(0).getTrkseg().get(0).getTrkpt().size() == 0) {
            throw new IllegalArgumentException("given GPX file does not contain location data");
        }

        List<WorkoutData> workouts = new ArrayList<>();
        for (Track track : gpx.getTrk()) {
            workouts.add(getWorkoutDataFromTrack(track));
        }

        return new WorkoutImportResult(workouts);
    }

    private void getGpx(InputStream input) throws IOException {
        XmlMapper mapper = new XmlMapper();
        mapper.configure(JsonParser.Feature.IGNORE_UNDEFINED, true);
        gpx = mapper.readValue(input, Gpx.class);
    }

    private WorkoutData getWorkoutDataFromTrack(Track track) {
        TrackSegment firstSegment = track.getTrkseg().get(0);
        TrackPoint firstPoint = firstSegment.getTrkpt().get(0);

        GpsWorkout workout = new GpsWorkout();
        workout.comment = track.getName();
        if (workout.comment == null) {
            workout.comment = track.getDesc();
        }
        if (gpx.getMetadata() != null) {
            if (workout.comment == null) {
                workout.comment = gpx.getMetadata().getName();
            }
            if (workout.comment == null) {
                workout.comment = gpx.getMetadata().getDesc();
            }
        }

        String startTime = firstPoint.getTime();

        if (startTime == null || startTime.isEmpty()) {
            throw new RuntimeException("The GPX file doesn't include timestamps.");
        }

        workout.start = parseDate(startTime).getTime();

        int index = firstSegment.getTrkpt().size();
        String lastTime = firstSegment.getTrkpt().get(index - 1).getTime();
        workout.end = parseDate(lastTime).getTime();
        workout.duration = workout.end - workout.start;
        String extractedWorkoutTypeId = getTypeIdById(gpx.getTrk().get(0).getType());
        if (!extractedWorkoutTypeId.isEmpty()) {
            workout.workoutTypeId = extractedWorkoutTypeId;
        }

        List<RouteSample> samples = getSamplesFromTrack(workout.start, gpx.getTrk().get(0));

        return new WorkoutData(workout, samples);
    }

    private static List<RouteSample> getSamplesFromTrack(long startTime, Track track) {
        List<RouteSample> samples = new ArrayList<>();

        for (TrackSegment segment : track.getTrkseg()) {
            samples.addAll(getSamplesFromTrackSegment(startTime, segment));
        }

        return samples;
    }

    private static List<RouteSample> getSamplesFromTrackSegment(long startTime, TrackSegment segment) {
        List<RouteSample> samples = new ArrayList<>();
        for (TrackPoint point : segment.getTrkpt()) {
            RouteSample sample = new RouteSample();
            sample.absoluteTime = parseDate(point.getTime()).getTime();
            sample.elevation = point.getEle();
            sample.latitude = point.getLat();
            sample.longitude = point.getLon();
            sample.relativeTime = sample.absoluteTime - startTime;
            TrackPointExtensions extensions = point.getExtensions();
            if (extensions != null) {
                sample.speed = extensions.getSpeed();
                if (extensions.getGpxTpxExtension() != null) {
                    sample.heartRate = extensions.getGpxTpxExtension().getHr();
                }
            }
            samples.add(sample);
        }
        return samples;
    }

    private static Date parseDate(String str) {
        try {
            // Need parseCalendar because parseDate seems to be corrupted.
            // The hour is always one lesser then the original time.
            return DateParserUtils.parseCalendar(str).getTime();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot parse timestamps: " + e.getMessage(), e.getCause());
        }
    }

    private static String getTypeIdById(String id) {
        if (id == null) {
            id = "";
        }
        switch (id) {
            // Strava IDs
            case "1":
                return "running";
            case "2":
                return "cycling";
            case "11":
                return "walking";

            default:
                return id;
        }
    }
}