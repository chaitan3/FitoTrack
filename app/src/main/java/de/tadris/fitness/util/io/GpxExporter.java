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

import android.annotation.SuppressLint;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import de.tadris.fitness.data.GpsSample;
import de.tadris.fitness.data.GpsWorkout;
import de.tadris.fitness.data.GpsWorkoutData;
import de.tadris.fitness.util.gpx.Gpx;
import de.tadris.fitness.util.gpx.GpxTpxExtension;
import de.tadris.fitness.util.gpx.Metadata;
import de.tadris.fitness.util.gpx.Track;
import de.tadris.fitness.util.gpx.TrackPoint;
import de.tadris.fitness.util.gpx.TrackPointExtensions;
import de.tadris.fitness.util.gpx.TrackSegment;
import de.tadris.fitness.util.io.general.IWorkoutExporter;

public class GpxExporter implements IWorkoutExporter {

    @SuppressLint("SimpleDateFormat") // This has nothing to do with localisation
    public final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    public GpxExporter() {
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public void exportWorkout(GpsWorkoutData data, OutputStream fileStream) throws IOException {
        XmlMapper mapper = new XmlMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.enable(ToXmlGenerator.Feature.WRITE_XML_DECLARATION);
        mapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET); // don't close output stream
        mapper.writeValue(fileStream, getGpxFromWorkout(data));
    }

    private Gpx getGpxFromWorkout(GpsWorkoutData data) {
        GpsWorkout workout = data.getWorkout();
        Track track = getTrackFromWorkout(data, 0);
        ArrayList<Track> tracks = new ArrayList<>();
        tracks.add(track);
        Metadata meta = new Metadata(workout.toString(), workout.comment, getDateTime(workout.start));
        return new Gpx("1.1", "FitoTrack", meta, tracks);
    }

    private Track getTrackFromWorkout(GpsWorkoutData data, int number) {
        GpsWorkout workout = data.getWorkout();
        Track track = new Track();
        track.setNumber(number);
        track.setName(workout.toString());
        track.setCmt(workout.comment);
        track.setDesc(workout.comment);
        track.setSrc("FitoTrack");
        track.setType(workout.workoutTypeId);
        track.setTrkseg(new ArrayList<>());

        TrackSegment segment = new TrackSegment();
        ArrayList<TrackPoint> trkpt = new ArrayList<>();

        for (GpsSample sample : data.getSamples()) {
            trkpt.add(new TrackPoint(
                    sample.lat,
                    sample.lon,
                    sample.elevation,
                    getDateTime(sample.absoluteTime),
                    new TrackPointExtensions(sample.speed, new GpxTpxExtension(sample.heartRate))
            ));
        }
        segment.setTrkpt(trkpt);

        ArrayList<TrackSegment> segments = new ArrayList<>();
        segments.add(segment);
        track.setTrkseg(segments);

        return track;
    }

    private String getDateTime(long time) {
        return getDateTime(new Date(time));
    }

    private String getDateTime(Date date) {
        // Why adding a 'Z'?
        // Normally we could use the 'X' char to specify the timezone but this is only available in Android 7+
        // Since this minSdkVersion is 21 (Android 5) we cannot use it
        // Solution: add a 'Z'. This indicates a UTC-timestamp and the 'formatter' always returns UTC-timestamps (see constructor)
        return formatter.format(date) + "Z";
    }
}
