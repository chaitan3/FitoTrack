/*
 * Copyright (c) 2025 Jannis Scheibe <jannis@tadris.de>
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
import androidx.room.PrimaryKey
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@Entity(
    tableName = "repetition_sample",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RepetitionSample(
    override val workoutId: Long,
    @PrimaryKey
    override val id: Long,
    override val timestamp: Long,
    val timestampEnd: Long,
    override val relativeTime: Long,
    val repetitionTypeId: Int,
    val repetitions: Int,
    val intensity: Double,
    val frequency: Double,
) : BaseSample {

    companion object {

        const val REPETITION_TYPE_UNKNOWN = 0
        const val REPETITION_TYPE_STEP = 1
        const val REPETITION_TYPE_JUMP = 2
        const val REPETITION_TYPE_PUSH_UP = 3
        const val REPETITION_TYPE_PULL_UP = 4

    }

}