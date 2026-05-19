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

package de.tadris.fitness.util.export.gpx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackPoint {

    @JacksonXmlProperty(isAttribute = true)
    private double lat;

    @JacksonXmlProperty(isAttribute = true)
    private double lon;

    /**
     * Elevation over the WGS84 ellipsoid in meters
     * according to http://www.topografix.com/GPX/1/1/gpx.xsd
     * "all coordinates are relative to the WGS84 datum"
     */
    private double ele;

    private String time;

    private TrackPointExtensions extensions;

    public TrackPoint() {
    }

    public TrackPoint(double lat, double lon, double ele, String time, TrackPointExtensions extensions) {
        this.lat = lat;
        this.lon = lon;
        this.ele = ele;
        this.time = time;
        this.extensions = extensions;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getEle() {
        return ele;
    }

    public void setEle(double ele) {
        this.ele = ele;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public TrackPointExtensions getExtensions() {
        return extensions;
    }

    public void setExtensions(TrackPointExtensions extensions) {
        this.extensions = extensions;
    }
}
