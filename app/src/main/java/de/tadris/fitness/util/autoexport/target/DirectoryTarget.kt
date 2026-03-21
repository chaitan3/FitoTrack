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
package de.tadris.fitness.util.autoexport.target

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import de.tadris.fitness.R
import de.tadris.fitness.util.autoexport.source.ExportSource.ExportedFile
import org.apache.commons.io.IOUtils
import java.io.IOException

class DirectoryTarget(private val directoryUri: String) : ExportTarget {

    override val id get() = ExportTarget.TARGET_TYPE_DIRECTORY

    override val titleRes get() = R.string.exportTargetDirectory

    override fun exportFile(context: Context, file: ExportedFile) {
        val input = context.contentResolver.openInputStream(Uri.fromFile(file.file))
            ?: throw IOException("Source file not found")

        val targetFolder = Uri.parse(directoryUri)
        val directoryFile = DocumentFile.fromTreeUri(context, targetFolder)

        val existingFile = directoryFile!!.findFile(file.file.name)
        existingFile?.delete()

        val targetFile = directoryFile.createFile("application/*", file.file.name)
            ?: throw IOException("Cannot create target file")
        if (!targetFile.canWrite()) {
            throw IOException("Cannot write to target file.")
        }

        val output = context.contentResolver.openOutputStream(targetFile.uri)
            ?: throw IOException("Target file not found")

        IOUtils.copy(input, output)
        input.close()
        output.flush()
        output.close()
    }

}