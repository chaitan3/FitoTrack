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
package de.tadris.fitness.util.autoexport.source

import android.content.Context
import de.tadris.fitness.util.DataManager
import de.tadris.fitness.util.autoexport.source.ExportSource.ExportedFile
import java.io.File

class BackupExportSource(private val includeTimestamp: Boolean) : ExportSource {

    @Throws(Exception::class)
    override fun provideFile(context: Context): ExportedFile {
        return ExportedFile(
            provideFile(context, ExportStatusListener.DUMMY), mapOf(
                "FitoTrack-Type" to ExportSource.EXPORT_SOURCE_BACKUP,
                "FitoTrack-Timestamp" to System.currentTimeMillis().toString(),
            )
        )
    }

    @Throws(Exception::class)
    fun provideFile(context: Context?, listener: ExportStatusListener?): File {
        val file = DataManager.createSharableFile(context, fileName)
        val backupController = LegacyBackupReader(context, file, listener)
        backupController.exportData()
        return file
    }

    private val fileName: String
        get() = if (includeTimestamp) {
            "fitotrack-backup" + System.currentTimeMillis() + ".ftb"
        } else {
            "fitotrack-backup.ftb"
        }
}