package de.tadris.fitness.export

import android.content.Context
import de.tadris.fitness.export.BackupWriter.ExportStatusListener
import java.io.File

class BackupReader(
    private val context: Context,
    private val input: File,
    private val replace: Boolean,
    private val listener: ExportStatusListener
) {

    fun read() {
        // TODO implement backup reading and differentiation between old and new backups
    }

}