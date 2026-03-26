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

package de.tadris.fitness;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.data.WorkoutTypeManager;
import de.tadris.fitness.util.io.GpxImporter;
import de.tadris.fitness.util.io.general.IWorkoutImporter;

public class GpxImporterTest {
    GpxImporter importer = new GpxImporter();

    @Test(expected = IOException.class)
    public void testImportFail() throws IOException {
        importer.readWorkouts(new ByteArrayInputStream("".getBytes()));
    }

    @Test
    public void testImportFitoTrackGpx() throws IOException {
        IWorkoutImporter.WorkoutImportResult importResult = importer.readWorkouts(new ByteArrayInputStream(fitotrackGpx.getBytes()));
        WorkoutData workoutData = importResult.workouts.get(0);

        // Main test is that above method runs without error, additionally perform some checks:
        Assert.assertEquals("Comment", workoutData.workout.comment);
        Assert.assertEquals(WorkoutTypeManager.WORKOUT_TYPE_ID_WALKING, workoutData.workout.workoutTypeId);
        Assert.assertEquals(9, workoutData.samples.size());
        Assert.assertEquals(748.058296766, workoutData.samples.get(0).elevation, 0.001);
        Assert.assertEquals(0.72999995946, workoutData.samples.get(6).speed, 0.001);
        Assert.assertEquals(35.24767770, workoutData.samples.get(7).latitude, 0.001);
        Assert.assertEquals(24.16795774, workoutData.samples.get(8).longitude, 0.001);
        Assert.assertEquals(1, workoutData.samples.get(1).heartRate);
        Assert.assertEquals(8, workoutData.samples.get(8).heartRate);
    }

    @Test
    public void testImportOpenTracksGpx() throws IOException {
        IWorkoutImporter.WorkoutImportResult importResult = importer.readWorkouts(new ByteArrayInputStream(opentracksGpx.getBytes()));
        WorkoutData workoutData = importResult.workouts.get(0);

        // Main test is that above method runs without error, additionally perform some checks:
        Assert.assertEquals(workoutData.workout.comment, "...");
        Assert.assertEquals(workoutData.samples.size(), 10);
        Assert.assertEquals(workoutData.samples.get(0).elevation, 152.5, 0.001);
        Assert.assertEquals(workoutData.samples.get(7).latitude, 30.232106, 0.001);
        Assert.assertEquals(workoutData.samples.get(9).longitude, 10.534532, 0.001);
    }

    @Test
    public void testImportRuntasticGpx() throws IOException {
        IWorkoutImporter.WorkoutImportResult importResult = importer.readWorkouts(new ByteArrayInputStream(runtasticGpx.getBytes()));
        WorkoutData workoutData = importResult.workouts.get(0);

        Assert.assertEquals("runtastic_20160215_0843", workoutData.workout.comment);
        Assert.assertEquals("", workoutData.workout.workoutTypeId);
        Assert.assertEquals(10, workoutData.samples.size());
        //TrackSegment / samples not tested cause its similar to other imports
    }

    @Test
    public void testImportKomootGpx() throws IOException {
        IWorkoutImporter.WorkoutImportResult importResult = importer.readWorkouts(new ByteArrayInputStream(komootGpx.getBytes()));
        WorkoutData workoutData = importResult.workouts.get(0);

        Assert.assertEquals("NameOfTrack", workoutData.workout.comment);
        Assert.assertEquals("", workoutData.workout.workoutTypeId);
        Assert.assertEquals(10, workoutData.samples.size());
        //TrackSegment / samples not tested cause its similar to other imports
    }

    //region Data
    // here are example "files" from different trackers, to check if special stuff like the workout type or spee can be imported correctly

    //region FitoTrack
    private String fitotrackGpx = "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<gpx creator=\"FitoTrack\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\" xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n" +
            "  <metadata>\n" +
            "    <desc>Comment</desc>\n" +
            "    <name>Comment</name>\n" +
            "    <time>2020-07-10T09:56:24Z</time>\n" +
            "  </metadata>\n" +
            "  <trk>\n" +
            "    <cmt>Comment</cmt>\n" +
            "    <desc>Comment</desc>\n" +
            "    <name>Comment</name>\n" +
            "    <number>0</number>\n" +
            "    <src>FitoTrack</src>\n" +
            "    <trkseg>\n" +
            "      <trkpt lat=\"35.247988756746054\" lon=\"24.167601512745023\">\n" +
            "        <ele>748.0582967666053</ele>\n" +
            "        <extensions>\n" +
            "          <speed>0.0</speed>\n" +
            "          <gpxtpx:TrackPointExtension>\n" +
            "            <gpxtpx:hr>0</gpxtpx:hr>\n" +
            "          </gpxtpx:TrackPointExtension>\n" +
            "        </extensions>\n" +
            "        <time>2020-07-10T09:56:25Z</time>\n" +
            "      </trkpt>\n" +
            "      <trkpt lat=\"35.247907997108996\" lon=\"24.167666053399444\">\n" +
            "        <ele>746.8538842154519</ele>\n" +
            "        <extensions>\n" +
            "          <speed>0.0</speed>\n" +
            "          <gpxtpx:TrackPointExtension>\n" +
            "            <gpxtpx:hr>1</gpxtpx:hr>\n" +
            "          </gpxtpx:TrackPointExtension>\n" +
            "        </extensions>\n" +
            "        <time>2020-07-10T09:57:06Z</time>\n" +
            "      </trkpt>\n" +
            "      <trkpt lat=\"35.247862064279616\" lon=\"24.16768055409193\">\n" +
            "        <ele>746.302997171181</ele>\n" +
            "        <extensions>\n" +
            "          <speed>0.4599999785423279</speed>\n" +
            "          <gpxtpx:TrackPointExtension>\n" +
            "            <gpxtpx:hr>2</gpxtpx:hr>\n" +
            "          </gpxtpx:TrackPointExtension>\n" +
            "        </extensions>\n" +
            "        <time>2020-07-10T09:57:14Z</time>\n" +
            "      </trkpt>\n" +
            "      <trkpt lat=\"35.24781747255474\" lon=\"24.16771575808525\">\n" +
            "        <ele>745.7384130980276</ele>\n" +
            "        <extensions>\n" +
            "          <speed>1.1899999380111694</speed>\n" +
            "          <gpxtpx:TrackPointExtension>\n" +
            "            <gpxtpx:hr>3</gpxtpx:hr>\n" +
            "          </gpxtpx:TrackPointExtension>\n" +
            "        </extensions>\n" +
            "        <time>2020-07-10T09:57:18Z</time>\n" +
            "      </trkpt>\n" +
            "      <trkpt lat=\"35.24777309037745\" lon=\"24.167760433629155\">\n" +
            "        <ele>745.1518883658424</ele>\n" +
            "        <extensions>\n" +
            "          <speed>0.7599999904632568</speed>\n" +
            "          <gpxtpx:TrackPointExtension>\n" +
            "            <gpxtpx:hr>4</gpxtpx:hr>\n" +
            "          </gpxtpx:TrackPointExtension>\n" +
            "        </extensions>\n" +
            "        <time>2020-07-10T09:57:23Z</time>\n" +
            "      </trkpt>\n" +
            "      <trkpt lat=\"35.2477119024843\" lon=\"24.167830338701606\">\n" +
            "        <ele>744.5100716152915</ele>\n" +
            "        <extensions>\n" +
            "          <speed>1.0299999713897705</speed>\n" +
            "          <gpxtpx:TrackPointExtension>\n" +
            "            <gpxtpx:hr>5</gpxtpx:hr>\n" +
            "          </gpxtpx:TrackPointExtension>\n" +
            "        </extensions>\n" +
            "        <time>2020-07-10T09:57:28Z</time>\n" +
            "      </trkpt>\n" +
            "      <trkpt lat=\"35.24768235627562\" lon=\"24.16788716800511\">\n" +
            "        <ele>743.941824249864</ele>\n" +
            "        <extensions>\n" +
            "          <speed>0.7299999594688416</speed>\n" +
            "          <gpxtpx:TrackPointExtension>\n" +
            "            <gpxtpx:hr>6</gpxtpx:hr>\n" +
            "          </gpxtpx:TrackPointExtension>\n" +
            "        </extensions>\n" +
            "        <time>2020-07-10T09:57:35Z</time>\n" +
            "      </trkpt>\n" +
            "      <trkpt lat=\"35.24767770431936\" lon=\"24.1679463442415\">\n" +
            "        <ele>742.4461097897636</ele>\n" +
            "        <extensions>\n" +
            "          <speed>0.7999999523162842</speed>\n" +
            "          <gpxtpx:TrackPointExtension>\n" +
            "            <gpxtpx:hr>7</gpxtpx:hr>\n" +
            "          </gpxtpx:TrackPointExtension>\n" +
            "        </extensions>\n" +
            "        <time>2020-07-10T09:57:39Z</time>\n" +
            "      </trkpt>\n" +
            "      <trkpt lat=\"35.247624604962766\" lon=\"24.167957743629813\">\n" +
            "        <ele>741.2391570135359</ele>\n" +
            "        <extensions>\n" +
            "          <speed>0.9899999499320984</speed>\n" +
            "          <gpxtpx:TrackPointExtension>\n" +
            "            <gpxtpx:hr>8</gpxtpx:hr>\n" +
            "          </gpxtpx:TrackPointExtension>\n" +
            "        </extensions>\n" +
            "        <time>2020-07-10T09:57:44Z</time>\n" +
            "      </trkpt>\n" +
            "    </trkseg>\n" +
            "    <type>walking</type>\n" +
            "  </trk>\n" +
            "</gpx>";
    //endregion

    //region OpenTracks
    private String opentracksGpx ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<gpx\n" +
            "version=\"1.1\"\n" +
            "creator=\"OpenTracks\"\n" +
            "xmlns=\"http://www.topografix.com/GPX/1/1\"\n" +
            "xmlns:topografix=\"http://www.topografix.com/GPX/Private/TopoGrafix/0/1\"\n" +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.topografix.com/GPX/Private/TopoGrafix/0/1 http://www.topografix.com/GPX/Private/TopoGrafix/0/1/topografix.xsd\">\n" +
            "<metadata>\n" +  
            "<name><![CDATA[...]]></name>\n" +    
            "<desc><![CDATA[...]]></desc>\n" +   
            "</metadata>\n" +
            "<trk>\n" +
            "<name><![CDATA[...]]></name>\n" +
            "<desc><![CDATA[...]]></desc>\n" +
            "<type><![CDATA[Laufen]]></type>\n" +
            "<extensions><topografix:color>c0c0c0</topografix:color></extensions>\n" +
            "<trkseg>\n"+
            "<trkpt lat=\"30.232601\" lon=\"10.534186\">\n" +
            "<ele>152.5</ele>\n" +
            "<time>2020-03-31T15:56:34Z</time>\n" +
            "</trkpt>\n" +
            "<trkpt lat=\"30.232541\" lon=\"10.534275\">\n" +
            "<ele>152.4</ele>\n" +
            "<time>2020-03-31T15:56:37Z</time>\n" +
            "</trkpt>\n" +
            "<trkpt lat=\"30.232447\" lon=\"10.534253\">\n" +
            "<ele>152.3</ele>\n" +
            "<time>2020-03-31T15:56:40Z</time>\n" +
            "</trkpt>\n" +
            "<trkpt lat=\"30.232355\" lon=\"10.534253\">\n" +
            "<ele>152.4</ele>\n" +
            "<time>2020-03-31T15:56:43Z</time>\n" +
            "</trkpt>\n" +
            "<trkpt lat=\"30.232282\" lon=\"10.534323\">\n" +
            "<ele>152.5</ele>\n" +
            "<time>2020-03-31T15:56:46Z</time>\n" +
            "</trkpt>\n" +
            "<trkpt lat=\"30.232227\" lon=\"10.534434\">\n" +
            "<ele>152.6</ele>\n" +
            "<time>2020-03-31T15:56:49Z</time>\n" +
            "</trkpt>\n" +
            "<trkpt lat=\"30.232174\" lon=\"10.534544\">\n" +
            "<ele>152.5</ele>\n" +
            "<time>2020-03-31T15:56:52Z</time>\n" +
            "</trkpt>\n" +
            "<trkpt lat=\"30.232106\" lon=\"10.534594\">\n" +
            "<ele>152.4</ele>\n" +
            "<time>2020-03-31T15:56:55Z</time>\n" +
            "</trkpt>\n" +
            "<trkpt lat=\"30.232046\" lon=\"10.534553\">\n" +
            "<ele>152.2</ele>\n" +
            "<time>2020-03-31T15:57:00Z</time>\n" +
            "</trkpt>\n" +
            "<trkpt lat=\"30.232111\" lon=\"10.534532\">\n" +
            "<ele>152.2</ele>\n" +
            "<time>2020-03-31T15:57:18Z</time>\n" +
            "</trkpt>"+
            "\n" +
            "</trkseg>\n" +      
            "</trk>\n" +     
            "</gpx>";
    //endregion

    //region Runtastic
    //the workout type is secured in an additional json file for runtastic activities
    private String runtasticGpx ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<gpx creator=\"Runtastic: Life is short - live long, http://www.runtastic.com\" version=\"1.1\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\" xmlns:gpxx=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.garmin.com/xmlschemas/GpxExtensions/v3 http://www.garmin.com/xmlschemas/GpxExtensionsv3.xsd http://www.garmin.com/xmlschemas/TrackPointExtension/v1 http://www.garmin.com/xmlschemas/TrackPointExtensionv1.xsd\">\n" +
            "<metadata>\n" +
            "<name>runtastic_date</name>\n" +
            "<copyright author=\"www.runtastic.com\">\n" +
            "<year>2020</year>\n" +
            "<license>http://www.runtastic.com</license>\n" +
            "</copyright>\n" +
            "<link href=\"http://www.runtastic.com\"><text>runtastic</text>\n" +
            "</link>\n" +
            "<time>2016-02-15T07:43:22.000Z</time>\n" +
            "</metadata>\n" +
            "<trk>\n" +
            "<name>runtastic_20160215_0843</name>\n" +
            "<link href=\"http://www.runtastic.com/\"><text>Visit this link to view this activity on runtastic.com</text>\n" +
            "</link>\n" +
            "<trkseg>\n" +
            "<trkpt lat=\"10.533128814697266\" lon=\"3.5184867572784424\">\n" +
            "<ele>116.0</ele>\n" +
            "<time>2016-02-15T07:43:34.000Z</time>\n" +
            "</trkpt>\n" +
            "<trkpt lat=\"10.533128814697266\" lon=\"3.5184867572784424\">\n" +
            "<ele>116.0</ele>\n" +
            "<time>2016-02-15T07:43:36.000Z</time>\n" +
            "</trkpt>\n" +
            "<trkpt lat=\"10.533128814697266\" lon=\"3.5184867572784424\">\n" +
            "<ele>116.0</ele>\n" +
            "<time>2016-02-15T07:43:38.000Z</time>\n" +
            "</trkpt>\n" +
            "<trkpt lat=\"10.53315170288086\" lon=\"3.5188417625427246\">\n" +
            "<ele>113.0</ele>\n" +
            "<time>2016-02-15T07:43:42.000Z</time>\n" +
            "</trkpt>\n" +
            "<trkpt lat=\"10.53330047607422\" lon=\"3.5189917278289795\">\n" +
            "<ele>112.0</ele>\n" +
            "<time>2016-02-15T07:43:46.000Z</time>\n" +
            "</trkpt>\n" +
            "<trkpt lat=\"10.53341491699219\" lon=\"3.5190150928497314\">\n" +
            "<ele>113.0</ele>\n" +
            "<time>2016-02-15T07:43:48.000Z</time>\n" +
            "</trkpt>\n" +
            "<trkpt lat=\"10.53341491699219\" lon=\"3.5190150928497314\">\n" +
            "<ele>113.0</ele>\n" +
            "<time>2016-02-15T07:43:50.000Z</time>\n" +
            "</trkpt>\n" +
            "<trkpt lat=\"10.53341491699219\" lon=\"3.5190150928497314\">\n" +
            "<ele>113.0</ele>\n" +
            "<time>2016-02-15T07:44:02.000Z</time>\n" +
            "</trkpt>\n" +
            "<trkpt lat=\"10.53341491699219\" lon=\"3.5190150928497314\">\n" +
            "<ele>113.0</ele>\n" +
            "<time>2016-02-15T07:44:04.000Z</time>\n" +
            "</trkpt>\n" +
            "<trkpt lat=\"10.53353317260742\" lon=\"3.518856782913208\">\n" +
            "<ele>108.0</ele>\n" +
            "<time>2016-02-15T07:44:58.000Z</time>\n" +
            "</trkpt>\n" +
            "</trkseg>\n" +
            "</trk>\n" +
            "</gpx>\n";
    //endregion

    //region komoot
    private String komootGpx ="<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<gpx version=\"1.1\" creator=\"https://www.komoot.de\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n" +
            "  <metadata>\n" +
            "    <name>NameOfTrack</name>\n" +
            "    <author>\n" +
            "      <link href=\"https://www.komoot.de\">\n" +
            "        <text>komoot</text>\n" +
            "        <type>text/html</type>\n" +
            "      </link>\n" +
            "    </author>\n" +
            "  </metadata>"+
            "    <name>NameOfTrack</name>\n" +
            "    <trk>\n" +
            "    <trkseg>\n" +
            "      <trkpt lat=\"56.854918\" lon=\"12.225131\">\n" +
            "        <ele>197.022936</ele>\n" +
            "        <time>2020-04-11T08:25:57.630Z</time>\n" +
            "      </trkpt>\n" +
            "      <trkpt lat=\"56.854834\" lon=\"12.225246\">\n" +
            "        <ele>197.022936</ele>\n" +
            "        <time>2020-04-11T08:26:01.929Z</time>\n" +
            "      </trkpt>\n" +
            "      <trkpt lat=\"56.854780\" lon=\"12.225388\">\n" +
            "        <ele>197.022936</ele>\n" +
            "        <time>2020-04-11T08:26:05.919Z</time>\n" +
            "      </trkpt>\n" +
            "      <trkpt lat=\"56.854674\" lon=\"12.225327\">\n" +
            "        <ele>197.022936</ele>\n" +
            "        <time>2020-04-11T08:26:08.051Z</time>\n" +
            "      </trkpt>\n" +
            "      <trkpt lat=\"56.854570\" lon=\"12.225260\">\n" +
            "        <ele>197.022936</ele>\n" +
            "        <time>2020-04-11T08:26:10.931Z</time>\n" +
            "      </trkpt>\n" +
            "      <trkpt lat=\"56.854440\" lon=\"12.225163\">\n" +
            "        <ele>197.022936</ele>\n" +
            "        <time>2020-04-11T08:26:13.933Z</time>\n" +
            "      </trkpt>\n" +
            "      <trkpt lat=\"56.854340\" lon=\"12.225044\">\n" +
            "        <ele>197.022936</ele>\n" +
            "        <time>2020-04-11T08:26:15.937Z</time>\n" +
            "      </trkpt>\n" +
            "      <trkpt lat=\"56.854228\" lon=\"12.224942\">\n" +
            "        <ele>197.022936</ele>\n" +
            "        <time>2020-04-11T08:26:17.934Z</time>\n" +
            "      </trkpt>\n" +
            "      <trkpt lat=\"56.854090\" lon=\"12.224830\">\n" +
            "        <ele>197.073568</ele>\n" +
            "        <time>2020-04-11T08:26:19.934Z</time>\n" +
            "      </trkpt>\n" +
            "      <trkpt lat=\"56.853990\" lon=\"12.224693\">\n" +
            "        <ele>197.148469</ele>\n" +
            "        <time>2020-04-11T08:26:21.934Z</time>\n" +
            "      </trkpt>" +
            "    </trkseg>\n" +
            "  </trk>\n" +
            "</gpx>";
    //endregion

    //endregion

}
