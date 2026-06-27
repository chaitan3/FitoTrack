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

package de.tadris.fitness.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * @property workoutId ID of workout
 * @property id ID
 * @property startDate Start of pause
 * @property endDate End of pause
 * @property userInitiated true if the pause was initiated manually by the user, false for automatic pauses. Will default to false if unknown
 */
@Entity(
    tableName = "workout_pause",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("workoutId")
    ]
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class WorkoutPause(
    val workoutId: Long,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startDate: Long,
    val endDate: Long,
    val userInitiated: Boolean,
){

    val duration get() = endDate - startDate

}