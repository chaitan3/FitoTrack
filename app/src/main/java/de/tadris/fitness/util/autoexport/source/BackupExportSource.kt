package de.tadris.fitness.util.autoexport.source

import android.content.Context
import de.tadris.fitness.export.BackupWriter
import de.tadris.fitness.util.DataManager
import de.tadris.fitness.util.autoexport.source.ExportSource.ExportedFile
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