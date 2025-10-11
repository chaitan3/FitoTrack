package de.tadris.fitness.export

import android.content.Context
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.json.JsonMapper
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.export.model.DataContainer
import de.tadris.fitness.export.model.IntervalSetContainer
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupWriter(
    private val context: Context,
    private val output: File,
    private val listener: ExportStatusListener
) {

    companion object {
        private const val VERSION = 4
        private const val FILE_VERSION = "version.txt"
        private const val FILE_DATA = "data.json"
        private const val FILE_WORKOUT = "workout_%d.json"
    }

    private val db = Instance.getInstance(context).db
    private val fileStream = FileOutputStream(output)
    private val zipStream = ZipOutputStream(fileStream)
    private val mapper = JsonMapper.builder()
        .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
        .build()

    fun write(){
        listener.onStatusChanged(0, context.getString(R.string.initialising))
        writeVersionFile()
        listener.onStatusChanged(0, context.getString(R.string.customWorkoutTypesTitle))
        writeExtraData()
        listener.onStatusChanged(0, context.getString(R.string.workouts))
        writeWorkouts()
        listener.onStatusChanged(100, context.getString(R.string.finished))
        zipStream.close()
        fileStream.close()
    }

    fun writeVersionFile(){
        zipStream.putNextEntry(ZipEntry(FILE_VERSION))
        zipStream.write(VERSION.toString().toByteArray())
        zipStream.closeEntry()
    }

    fun writeExtraData(){
        val intervalSets = db.intervalDao().allSets.map {
            IntervalSetContainer(it, db.intervalDao().getAllIntervalsOfSet(it.id).toList())
        }
        val container = DataContainer(
            intervalSets,
            db.workoutTypeDao().findAll().toList(),
        )
        zipStream.putNextEntry(ZipEntry(FILE_DATA))
        mapper.writeValue(zipStream, container)
        zipStream.closeEntry()
    }

    fun writeWorkouts(){
        val dao = db.workoutDao()
        val workouts = dao.getWorkouts()
        workouts.forEachIndexed { index, workout ->
            zipStream.putNextEntry(ZipEntry(FILE_WORKOUT.format(index)))
            val data = WorkoutData.fromWorkout(dao, workout)
            mapper.writeValue(zipStream, data)
            zipStream.closeEntry()
            listener.onStatusChanged(100 * index / workouts.size, context.getString(R.string.workouts))
        }
    }

    fun interface ExportStatusListener {

        fun onStatusChanged(progress: Int, action: String?)

    }

}