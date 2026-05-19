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
package de.tadris.fitness.data.autoexport.source

import android.content.Context
import androidx.annotation.StringRes
import de.tadris.fitness.R
import java.io.File

interface ExportSource {

    @Throws(Exception::class)
    fun provideFile(context: Context): ExportedFile

    class ExportedFile(val file: File, val meta: Map<String, String>)

    companion object {

        const val EXPORT_SOURCE_WORKOUT_GPX = "workout-gpx"
        const val EXPORT_SOURCE_BACKUP = "backup"

        @StringRes
        fun getTitle(name: String?): Int {
            return when (name) {
                EXPORT_SOURCE_BACKUP -> R.string.autoBackupTitle
                EXPORT_SOURCE_WORKOUT_GPX -> R.string.workoutGPXExportTitle
                else -> R.string.unknown
            }
        }

        @StringRes
        fun getExplanation(name: String?): Int {
            return when (name) {
                EXPORT_SOURCE_BACKUP -> R.string.autoExportBackupExplanation
                EXPORT_SOURCE_WORKOUT_GPX -> R.string.autoExportWorkoutExplanation
                else -> R.string.unknown
            }
        }

        @JvmStatic
        fun getExportSourceByName(name: String?, data: String?): ExportSource? {
            return when (name) {
                EXPORT_SOURCE_BACKUP -> BackupExportSource(false)
                EXPORT_SOURCE_WORKOUT_GPX -> WorkoutGpxExportSource(data!!.toLong())
                else -> null
            }
        }
    }
}