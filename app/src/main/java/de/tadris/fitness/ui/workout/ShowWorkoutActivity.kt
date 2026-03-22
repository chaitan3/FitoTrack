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
package de.tadris.fitness.ui.workout

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutPause
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.data.WorkoutTypeManager
import de.tadris.fitness.data.preferences.UserMeasurements
import de.tadris.fitness.osm.OAuthAuthentication
import de.tadris.fitness.osm.OAuthAuthentication.Companion.getAccessToken
import de.tadris.fitness.osm.OsmTraceUploader
import de.tadris.fitness.recording.RecorderService
import de.tadris.fitness.recording.RecordingStateHolder
import de.tadris.fitness.stats.StatsProvider
import de.tadris.fitness.ui.ShareFileActivity
import de.tadris.fitness.ui.dialog.ProgressDialogController
import de.tadris.fitness.ui.record.RecordWorkoutActivity
import de.tadris.fitness.ui.workout.diagram.FrequencyConverter
import de.tadris.fitness.ui.workout.diagram.HeartRateConverter
import de.tadris.fitness.ui.workout.diagram.HeightConverter
import de.tadris.fitness.ui.workout.diagram.IntensityConverter
import de.tadris.fitness.ui.workout.diagram.SampleConverter
import de.tadris.fitness.ui.workout.diagram.SpeedConverter
import de.tadris.fitness.util.DataManager
import de.tadris.fitness.util.DialogUtils
import de.tadris.fitness.util.DialogUtils.WorkoutDeleter
import de.tadris.fitness.util.WorkoutCalculator
import de.tadris.fitness.util.WorkoutCalculator.getSetsFromWorkout
import de.tadris.fitness.util.autoexport.source.WorkoutGpxExportSource
import de.tadris.fitness.util.charts.ChartStyles
import de.tadris.fitness.util.charts.formatter.SpeedFormatter
import de.tadris.fitness.util.charts.formatter.TimeFormatter
import de.tadris.fitness.util.charts.marker.DisplayValueMarker
import de.tadris.fitness.util.io.general.IOHelper
import de.tadris.fitness.util.sections.SectionListModel
import de.tadris.fitness.util.sections.SectionListPresenter
import de.tadris.fitness.util.sections.SectionListView
import de.tadris.fitness.util.unit.UnitUtils
import de.westnordost.osmapi.traces.GpsTraceDetails
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class ShowWorkoutActivity : MapWorkoutActivity(), WorkoutDeleter {

    private lateinit var sets: List<WorkoutCalculator.Set>
    private lateinit var commentView: TextView

    private val oauthActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result!!.resultCode == RESULT_OK) {
            showUploadOptions()
        } else {
            AlertDialog.Builder(this@ShowWorkoutActivity)
                .setTitle(R.string.error)
                .setMessage(R.string.authenticationFailed)
                .setPositiveButton(R.string.okay, null)
                .create().show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initBeforeContent()

        setContentView(R.layout.activity_show_workout)
        initRoot()

        initAfterContent()

        commentView = addText("", true)
        commentView.setOnClickListener { _: View? -> openEditCommentDialog() }
        updateCommentText()
        sets = getSetsFromWorkout(data)

        addTitle(getString(R.string.workoutTime))
        addKeyValue(getString(R.string.workoutDate), this.date)
        addKeyValue(
            getString(R.string.workoutDuration),
            distanceUnitUtils.getHourMinuteSecondTime(workout.activeDuration),
            getString(R.string.workoutPauseDuration),
            distanceUnitUtils.getHourMinuteSecondTime(workout.pauseDuration)
        )
        addKeyValue(
            getString(R.string.workoutStartTime),
            Instance.getInstance(this).userDateTimeUtils.formatTime(
                Date(workout.start)
            ),
            getString(R.string.workoutEndTime),
            Instance.getInstance(this).userDateTimeUtils.formatTime(
                Date(workout.end)
            )
        )

        if (data.repetitionSamples.isEmpty()) {
            addKeyValue(
                getString(R.string.workoutDistance),
                distanceUnitUtils.getDistance(workout.distance.toInt()),
                getString(
                    R.string.workoutPace
                ),
                distanceUnitUtils.getPace(workout.averagePaceActive)
            )
        } else {
            addKeyValue(
                getString(R.string.workoutRepetitions), workout.repetitions.toString(), getString(
                    R.string.workoutSets
                ), sets.size.toString()
            )
            if (workout.workoutTypeId == WorkoutTypeManager.WORKOUT_TYPE_ID_TREADMILL) {
                val distancePerStep = UserMeasurements.from(this).stepLength
                val estimatedDistance = (distancePerStep * workout.repetitions).roundToInt()
                val estimatedSpeed = estimatedDistance / (workout.activeDuration / 1000.0)

                addKeyValue(
                    getString(R.string.approxSymbol) + getString(R.string.workoutDistance), distanceUnitUtils.getDistance(estimatedDistance),
                    getString(R.string.approxSymbol) + getString(R.string.workoutSpeed), distanceUnitUtils.getSpeed(estimatedSpeed));
                }
        }

        if (data.hasRouteData) {
            addTitle(getString(R.string.workoutRoute))

            addMap()

            mapView.isClickable = false
            mapRoot.setOnClickListener { v: View? -> startFullscreenMapActivity() }
        }

        if (data.hasRouteData) {
            addTitle(getString(R.string.workoutSpeed))

            addKeyValue(
                getString(R.string.avgSpeedInMotion),
                distanceUnitUtils.getSpeed(workout.averageSpeedActive),
                getString(R.string.avgSpeedTotalShort),
                distanceUnitUtils.getSpeed(workout.averageSpeedTotal)
            )

            addKeyValue(
                getString(R.string.workoutTopSpeed),
                distanceUnitUtils.getSpeed(calculatedValues.topSpeed)
            )

            addDiagram(SpeedConverter(this), ShowWorkoutMapDiagramActivity.DIAGRAM_TYPE_SPEED)

            addTitle(getString(R.string.histogram))
            addSpeedHistogram()
        }

        if (data.hasRepetitions) {
            addTitle(getString(R.string.workoutFrequency))

            if (!data.repetitionSamples.isEmpty()) {
                addKeyValue(
                    getString(R.string.workoutAvgFrequencyShort),
                    UnitUtils.round(workout.averageFrequency, 2) + " " + UnitUtils.unitHertzShort,
                    getString(R.string.workoutMaxFrequency),
                    UnitUtils.round(calculatedValues.maxFrequency, 2) + " " + UnitUtils.unitHertzShort
                )

                addDiagram(FrequencyConverter(this))
            } else {
                addKeyValue(
                    getString(R.string.workoutAvgFrequencyShort),
                    UnitUtils.round(workout.averageFrequency, 2) + " " + UnitUtils.unitHertzShort
                )
            }
        }

        if (data.hasHRData) {
            addTitle(getString(R.string.workoutHeartRate))
            addKeyValue(
                getString(R.string.workoutAvgHeartRate),
                calculatedValues.averageHeartRate?.roundToInt().toString() + " " + getString(R.string.unitHeartBeatsPerMinute),
                getString(R.string.workoutMaxHeartRate),
                calculatedValues.maxHeartRate.toString() + " " + getString(R.string.unitHeartBeatsPerMinute)
            )

            addDiagram(
                HeartRateConverter(this),
                ShowWorkoutMapDiagramActivity.DIAGRAM_TYPE_HEART_RATE
            )
        }

        addTitle(getString(R.string.workoutBurnedEnergy))
        addKeyValue(
            getString(R.string.workoutTotalEnergy),
            energyUnitUtils.getEnergy(workout.energy),
            getString(R.string.workoutEnergyConsumption),
            energyUnitUtils.getRelativeEnergy(workout.energy / (workout.activeDuration.toDouble() / 1000 / 60))
        )

        if (data.hasRouteData) {
            addTitle(getString(R.string.height))

            addKeyValue(
                getString(R.string.workoutMinHeight),
                distanceUnitUtils.getElevation(calculatedValues.minElevationMSL.roundToInt()),
                getString(R.string.workoutMaxHeight),
                distanceUnitUtils.getElevation(calculatedValues.maxElevationMSL.roundToInt())
            )

            addKeyValue(
                getString(R.string.workoutAscent),
                distanceUnitUtils.getElevation(calculatedValues.ascent.roundToInt()),
                getString(R.string.workoutDescent),
                distanceUnitUtils.getElevation(calculatedValues.descent.roundToInt())
            )

            addDiagram(HeightConverter(this), ShowWorkoutMapDiagramActivity.DIAGRAM_TYPE_HEIGHT)

            addSectionList()
        }

        if (data.hasIntensityData) {
            if (!data.repetitionSamples.isEmpty()) {
                addTitle(getString(R.string.workoutIntensity))

                addKeyValue(
                    getString(R.string.workoutAvgIntensityShort), UnitUtils.round(calculatedValues.averageIntensity, 2),
                    getString(R.string.workoutMaxIntensity), UnitUtils.round(calculatedValues.maxIntensity, 2)
                )

                addDiagram(IntensityConverter(this))
            }
        }
    }

    private fun addSpeedHistogram() {
        val chart = BarChart(this)
        val data: MutableList<Double> = ArrayList()
        val weights: MutableList<Double> = ArrayList()

        // Gather the data for the histogram
        val sectionListModel = SectionListModel(this.data)
        sectionListModel.setCriterion(SectionListModel.SectionCriterion.TIME)
        val sectionTime = 9213.0
        // Yes, this is kinda random. roughly 10 seconds seem like a good timespan for the histogram,
        // and using this value makes it just a bit less obvious that we don't have more precise values.
        // The goal here is to depend not to much on outliers but apply the histogram on an averaged
        // speed diagram.
        sectionListModel.sectionLength = sectionTime
        val sections: MutableList<SectionListModel.Section> = sectionListModel.getSectionList()
        if (sections.isEmpty()) return
        for (section in sections) {
            weights.add(section.getTime(true))
            val speed = 1 / section.pace
            data.add(speed)
        }

        // create the histogram
        val nBins = 15
        val dataSet = StatsProvider(this).createWeightedHistogramData(data, weights, nBins, "")
        val barData = BarData(dataSet)
        chart.setData(barData)

        // Create the diagram
        val timeFormatter = TimeFormatter(TimeUnit.MILLISECONDS)
        val speedFormatter = SpeedFormatter(distanceUnitUtils)
        ChartStyles.defaultHistogram(chart, this, speedFormatter, timeFormatter)
        ChartStyles.setXAxisLabel(chart, distanceUnitUtils.getSpeedUnit(), this)
        ChartStyles.setYAxisLabel(chart, getString(R.string.timeMinuteShort), this)
        chart.setMarker(
            DisplayValueMarker(
                this,
                chart.getAxisLeft().getValueFormatter(),
                chart.getLegend().getEntries()[0].label,
                barData
            )
        )

        root.addView(
            chart,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getMapHeight() / 2)
        )
    }

    private fun addSectionList() {
        addTitle(getString(R.string.sections))
        val listView = SectionListView(this)
        val listModel = SectionListModel(this.data)
        SectionListPresenter(listView, listModel)
        root.addView(listView)
    }

    fun addDiagram(converter: SampleConverter?, mapDiagramActivityExtra: String?) {
        addDiagram(converter).setOnClickListener(View.OnClickListener { v: View? ->
            startDiagramActivity(
                mapDiagramActivityExtra
            )
        })
    }

    private fun startDiagramActivity(diagramType: String?) {
        val intent = Intent(this, ShowWorkoutMapDiagramActivity::class.java)
        intent.putExtra(WORKOUT_ID_EXTRA, workout.id)
        intent.putExtra(ShowWorkoutMapDiagramActivity.DIAGRAM_TYPE_EXTRA, diagramType)
        startActivity(intent)
    }

    private fun startFullscreenMapActivity() {
        val intent = Intent(this, ShowWorkoutFullscreenMapActivity::class.java)
        intent.putExtra(WORKOUT_ID_EXTRA, workout.id)
        startActivity(intent)
    }


    private fun openEditCommentDialog() {
        val editText = EditText(this)
        editText.setText(workout.comment)
        editText.setSingleLine(true)
        editText.setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        requestKeyboard(editText)
        AlertDialog.Builder(this)
            .setTitle(R.string.enterComment)
            .setPositiveButton(
                R.string.okay
            ) { _: DialogInterface?, _: Int ->
                changeComment(editText.getText().toString())
            }
            .setView(editText).create().show()
    }

    private fun changeComment(comment: String?) {
        workout = workout.copy(comment = comment)
        Instance.getInstance(this).db.workoutDao().updateWorkout(workout)
        updateCommentText()
    }

    private fun updateCommentText() {
        var str: String? = ""
        if (workout.comment != null && workout.comment!!.isNotEmpty()) {
            if (str!!.isNotEmpty()) {
                str += "\n"
            }
            str += getString(R.string.comment) + ": " + workout.comment
        }
        if (str!!.isEmpty()) {
            str = getString(R.string.noComment)
        }
        commentView.text = str
    }

    private val date: String?
        get() = Instance.getInstance(this).userDateTimeUtils.formatDate(
            Date(
                workout.start
            )
        )


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.show_workout_menu, menu)
        menu.findItem(R.id.actionResumeWorkout).isVisible = this.isLastWorkout
        menu.findItem(R.id.actionEditWorkoutStartEnd).isVisible = data.hasRouteData
        menu.findItem(R.id.actionUploadOSM).isVisible = data.hasRouteData
        menu.findItem(R.id.actionExportGpx).isVisible = data.hasRouteData
        menu.findItem(R.id.actionShareWorkout).isVisible = data.hasRouteData
        return true
    }

    private val isLastWorkout: Boolean
        get() = Instance.getInstance(this).db.workoutDao()
            .getLastWorkout()!!.id == workout.id

    override fun deleteWorkout() {
        Instance.getInstance(this).db.workoutDao().deleteWorkout(workout)
        finish()
    }

    private fun showDeleteDialog() {
        DialogUtils.showDeleteWorkoutDialog(this, this)
    }

    private fun exportToGpx() {
        if (!hasStoragePermission()) {
            requestStoragePermissions()
            return
        }
        val dialogController = ProgressDialogController(this, getString(R.string.exporting))
        dialogController.show()
        dialogController.setIndeterminate(true)
        Thread(Runnable {
            try {
                val file = WorkoutGpxExportSource(workout.id).provideFile(this).file
                val uri = DataManager.provide(this, file)
                IOHelper.GpxExporter.exportWorkout(data, file)
                mHandler.post(Runnable {
                    dialogController.cancel()
                    val intent = Intent(this, ShareFileActivity::class.java)
                    intent.putExtra(ShareFileActivity.EXTRA_FILE_URI, uri.toString())
                    startActivity(intent)
                })
            } catch (e: Exception) {
                e.printStackTrace()
                mHandler.post(Runnable {
                    dialogController.cancel()
                    showErrorDialog(e, R.string.error, R.string.errorGpxExportFailed)
                })
            }
        }).start()
    }

    private fun prepareUpload() {
        oauthActivityLauncher.launch(Intent(this, OAuthAuthentication::class.java))
    }

    private var dialog: AlertDialog? = null

    private fun showUploadOptions() {
        dialog = AlertDialog.Builder(this)
            .setTitle(R.string.actionUploadToOSM)
            .setView(R.layout.dialog_upload_osm)
            .setPositiveButton(
                R.string.upload,
                null
            ) // Listener added later so that we can control if the dialog is dismissed on click
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog!!.setOnShowListener { dialogInterface: DialogInterface? ->
            val button = dialog!!.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener { view: View? ->
                val checkBox = dialog!!.findViewById<CheckBox>(R.id.uploadCutting)
                val spinner = dialog!!.findViewById<Spinner>(R.id.uploadVisibility)
                val descriptionEdit = dialog!!.findViewById<EditText>(R.id.uploadDescription)
                val description = descriptionEdit.getText().toString().trim { it <= ' ' }
                if (description.length <= 2) {
                    descriptionEdit.setError(getString(R.string.enterDescription))
                    requestKeyboard(descriptionEdit)
                    return@setOnClickListener
                }
                val visibility: GpsTraceDetails.Visibility?
                when (spinner.getSelectedItemPosition()) {
                    0 -> visibility = GpsTraceDetails.Visibility.IDENTIFIABLE
                    1 -> visibility = GpsTraceDetails.Visibility.TRACKABLE
                    2 -> visibility = GpsTraceDetails.Visibility.PRIVATE
                    else -> visibility = GpsTraceDetails.Visibility.TRACKABLE
                }
                dialog!!.dismiss()
                uploadToOsm(checkBox.isChecked(), visibility, description)
            }
        }
        dialog!!.show()
    }

    private fun uploadToOsm(
        cut: Boolean,
        visibility: GpsTraceDetails.Visibility?,
        description: String?
    ) {
        val token = getAccessToken(this)
        OsmTraceUploader(
            this,
            mHandler,
            workout,
            data.samples,
            visibility,
            token,
            cut,
            description
        ).upload()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == EditWorkoutStartEndActivity.INTENT_RESULT_CODE_WORKOUT_MODIFIED) {
            //Restart the activity as the data has changed..
            val intent = Intent(this, ShowWorkoutActivity::class.java)
            intent.putExtra(WORKOUT_ID_EXTRA, workout.id)
            startActivity(intent)
            finish()
        }
    }

    private fun openEditWorkoutActivity() {
        val intent = Intent(this, EnterWorkoutActivity::class.java)
        intent.putExtra(EnterWorkoutActivity.WORKOUT_ID_EXTRA, workout.id)
        startActivity(intent)
        finish()
    }

    private fun editWorkoutStartEndActivity() {
        try {
            val intent = Intent(this, EditWorkoutStartEndActivity::class.java)
            intent.putExtra(WORKOUT_ID_EXTRA, workout.id)
            startActivityForResult(intent, 3)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shareWorkoutActivity() {
        try {
            val intent = Intent(this, ShareWorkoutActivity::class.java)
            intent.putExtra(WORKOUT_ID_EXTRA, workout.id)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.getItemId()
        when (id) {
            R.id.actionDeleteWorkout -> {
                showDeleteDialog()
                return true
            }

            R.id.actionEditWorkoutStartEnd -> {
                editWorkoutStartEndActivity()
                return true
            }

            R.id.actionExportGpx -> {
                exportToGpx()
                return true
            }

            R.id.actionUploadOSM -> {
                prepareUpload()
                return true
            }

            R.id.actionEditWorkout -> {
                openEditWorkoutActivity()
                return true
            }

            R.id.actionResumeWorkout -> {
                showResumeConfirmation()
                return true
            }

            R.id.actionShareWorkout -> {
                shareWorkoutActivity()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showResumeConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.resumeWorkout)
            .setMessage(R.string.resumeWorkoutConfirmation)
            .setPositiveButton(
                R.string.actionResume
            ) { dialog: DialogInterface?, which: Int -> resumeWorkout() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun resumeWorkout() {
        // Update workout to be running + insert a pause
        val dao = Instance.getInstance(this).db.workoutDao()
        dao.insertPause(
            WorkoutPause(
                workout.id,
                System.nanoTime(),
                workout.end,
                System.currentTimeMillis(),
                true
            )
        )
        dao.updateWorkout(workout.copy(end = RecordingStateHolder.RUNNING_WORKOUT_END_TIME)) // set workout to running

        // Start service
        startForegroundService(Intent(this, RecorderService::class.java))

        // Then navigate to record activity
        val workoutType: WorkoutType = workout.getWorkoutType(this)
        val recordingType = workoutType.getRecordingType()
        val recorderActivityIntent = Intent(this, recordingType.recorderActivityClass)
        recorderActivityIntent.putExtra(RecordWorkoutActivity.WORKOUT_TYPE_EXTRA, workoutType.id)
        recorderActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        recorderActivityIntent.setAction(RecordWorkoutActivity.RESUME_ACTION)
        startActivity(recorderActivityIntent)
    }

    override fun initRoot() {
        root = findViewById(R.id.showWorkoutRoot)
    }
}
