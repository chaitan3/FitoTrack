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

package de.tadris.fitness.data;

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.mapsforge.core.model.LatLong

/**
 * @property latitude Latitude
 * @property longitude Longitude
 * @property elevation Elevation over the WGS84 ellipsoid in meters
 * @property elevationMSL Elevation over the media sea level in meters. This value should be displayed to the user.
 * @property speed Speed in m/s
 * @property pressure Pressure in hPa
 *
 */
@Entity(
    tableName = "route_sample",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workout_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RouteSample(
    @ColumnInfo(name = "workout_id")
    override val workoutId: Long,
    @PrimaryKey
    override val id: Long,
    override val timestamp: Long,
    override val relativeTime: Long,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    @ColumnInfo(name = "elevation_msl")
    val elevationMSL: Double,
    val speed: Double,
    val pressure: Float,
) : BaseSample {

    fun toLatLong() = LatLong(latitude, longitude)

    /**
     * Adds values from other sample to this one
     */
    fun add(sample: RouteSample): RouteSample {
        return this.copy(
            speed = speed + sample.speed,
            elevationMSL = elevationMSL + sample.elevationMSL,
            elevation = elevation + sample.elevation,
            relativeTime = relativeTime + sample.relativeTime,
            latitude = latitude + sample.latitude,
            longitude = longitude + sample.longitude,
        )
    }

    /**
     * Divides value by a scalar
     */
    fun divide(scalar: Int): RouteSample {
        return this.copy(
            speed = speed / scalar,
            elevationMSL = elevationMSL / scalar,
            elevation = elevation / scalar,
            relativeTime = relativeTime / scalar,
            latitude = latitude / scalar,
            longitude = longitude / scalar
        )
    }

}
