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
import de.tadris.fitness.data.autoexport.source.ExportSource.ExportedFile
import de.tadris.fitness.data.backup.BackupWriter
import de.tadris.fitness.util.DataManager
import java.io.File

class BackupExportSource(private val includeTimestamp: Boolean) : ExportSource {

    @Throws(Exception::class)
    override fun provideFile(context: Context): ExportedFile {
        return ExportedFile(
            exportToFile(context) { _, _ -> }, mapOf(
                "FitoTrack-Type" to ExportSource.EXPORT_SOURCE_BACKUP,
                "FitoTrack-Timestamp" to System.currentTimeMillis().toString(),
            )
        )
    }

    fun exportToFile(context: Context, listener: BackupWriter.ExportStatusListener): File {
        val file = DataManager.createSharableFile(context, fileName)
        val backupController = BackupWriter(context, file, listener)
        backupController.write()
        return file
    }

    private val fileName: String
        get() = if (includeTimestamp) {
            "fitotrack-backup" + System.currentTimeMillis() + ".ftb"
        } else {
            "fitotrack-backup.ftb"
        }
}