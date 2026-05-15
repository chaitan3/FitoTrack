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
package de.tadris.fitness.data.autoexport.target

import android.content.Context
import androidx.annotation.StringRes
import androidx.work.Constraints
import de.tadris.fitness.data.autoexport.source.ExportSource.ExportedFile

interface ExportTarget {

    @Throws(Exception::class)
    fun exportFile(context: Context, file: ExportedFile)

    val id: String

    @get:StringRes
    val titleRes: Int

    val constraints: Constraints
        get() = Constraints.Builder().build()

    companion object {
        @JvmStatic
        fun getExportTargetImplementation(type: String, data: String) = when (type) {
            TARGET_TYPE_DIRECTORY -> DirectoryTarget(data)
            TARGET_TYPE_HTTP_POST -> HTTPPostTarget(data)
            else -> null
        }

        const val TARGET_TYPE_DIRECTORY = "directory"
        const val TARGET_TYPE_HTTP_POST = "http-post"

        val exportTargetTypes = arrayOf(
            DirectoryTarget(""),
            HTTPPostTarget("")
        )
    }
}