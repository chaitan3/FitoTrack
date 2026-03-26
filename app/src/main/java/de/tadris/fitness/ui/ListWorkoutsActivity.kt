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
package de.tadris.fitness.ui

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnLongClickListener
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.Workout
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.osm.OAuthAuthentication
import de.tadris.fitness.ui.adapter.WorkoutAdapter
import de.tadris.fitness.ui.adapter.WorkoutAdapter.WorkoutAdapterListener
import de.tadris.fitness.ui.dialog.SelectWorkoutTypeDialog
import de.tadris.fitness.ui.dialog.ThreadSafeProgressDialogController
import de.tadris.fitness.ui.record.RecordWorkoutActivity
import de.tadris.fitness.ui.settings.FitoTrackSettingsActivity
import de.tadris.fitness.ui.statistics.ShortStatsView
import de.tadris.fitness.ui.statistics.StatisticsActivity
import de.tadris.fitness.ui.workout.EnterWorkoutActivity
import de.tadris.fitness.ui.workout.ShowWorkoutActivity
import de.tadris.fitness.ui.workout.WorkoutActivity
import de.tadris.fitness.util.DialogUtils
import de.tadris.fitness.util.Icon
import de.tadris.fitness.util.PermissionUtils
import de.tadris.fitness.util.io.general.IOHelper

class ListWorkoutsActivity : FitoTrackActivity(), WorkoutAdapterListener {

    private lateinit var listView: RecyclerView
    private lateinit var shortStatsView: ShortStatsView
    private lateinit var adapter: WorkoutAdapter
    private lateinit var layoutManager: RecyclerView.LayoutManager
    private lateinit var menu: FloatingActionMenu
    private lateinit var hintText: TextView

    private var workouts: Array<Workout> = emptyArray()

    private var listSize = 0
    private var lastClickedIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_workouts)

        listView = findViewById(R.id.workoutList)
        listView.setHasFixedSize(true)
        layoutManager = LinearLayoutManager(this)
        listView.layoutManager = layoutManager
        adapter = WorkoutAdapter(workouts, this)
        listView.adapter = adapter
        shortStatsView = findViewById(R.id.short_stats_view)

        menu = findViewById(R.id.workoutListMenu)
        menu.setOnMenuButtonLongClickListener(OnLongClickListener {
            if (workouts.isNotEmpty()) {
                startRecording(workouts[0].getWorkoutType(this))
                return@OnLongClickListener true
            } else {
                return@OnLongClickListener false
            }
        })

        hintText = findViewById(R.id.hintAddWorkout)

        findViewById<View>(R.id.workoutListRecord).setOnClickListener { showWorkoutSelection() }
        findViewById<View>(R.id.workoutListEnter).setOnClickListener { startEnterWorkoutActivity() }
        findViewById<View>(R.id.workoutListImport).setOnClickListener { showImportDialog() }

        checkFirstStart()
        refresh()
    }

    private val mHandler = Handler()

    private fun hasPermission(): Boolean {
        return PermissionUtils.checkStoragePermissions(this, false)
    }

    private fun requestPermissions() {
        if (!hasPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                10
            )
        }
    }

    private fun showImportDialog() {
        if (!hasPermission()) {
            requestPermissions()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.importWorkout)
            .setMessage(R.string.importWorkoutMultipleQuestion)
            .setPositiveButton(R.string.actionImport) { _: DialogInterface?, _: Int -> importWorkout() }
            .setNeutralButton(R.string.actionImportMultiple) { _: DialogInterface?, _: Int -> showMassImportGpx() }
            .show()
        refresh()
        menu.close(true)
    }

    private fun importWorkout() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        try {
            startActivityForResult(
                Intent.createChooser(intent, getString(R.string.importWorkout)),
                FILE_IMPORT_SELECT_CODE
            )
        } catch (_: ActivityNotFoundException) { }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null) {
            if (resultCode == RESULT_OK) {
                if (requestCode == FILE_IMPORT_SELECT_CODE) {
                    importFile(data.data!!)
                } else if (requestCode == FOLDER_IMPORT_SELECT_CODE) {
                    massImportGpx(data.data!!)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun importFile(uri: Uri) {
        val intent = Intent(this, ImportGpxActivity::class.java)
        intent.data = uri
        startActivity(intent)
    }

    private fun showMassImportGpx() {
        AlertDialog.Builder(this)
            .setTitle(R.string.importMultipleGpxFiles)
            .setMessage(R.string.importMultipleMessageSelectFolder)
            .setPositiveButton(R.string.okay) { _: DialogInterface?, _: Int -> openMassImportFolderSelector() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openMassImportFolderSelector() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, FOLDER_IMPORT_SELECT_CODE)
    }

    private fun massImportGpx(dirUri: Uri) {
        Log.d("MassImport", dirUri.toString())
        val dialog = ThreadSafeProgressDialogController(this, getString(R.string.importingFiles))
        dialog.show()
        Thread {
            try {
                var imported = 0
                val documentFile = DocumentFile.fromTreeUri(this, dirUri)
                val files = documentFile!!.listFiles()
                for (i in files.indices) {
                    dialog.setProgress(100 * i / files.size)
                    val file = files[i]
                    if (file.isFile && file.canRead()) {
                        try {
                            val fileUri = file.uri
                            Log.d("MassImport", "Importing $fileUri")
                            IOHelper.GpxImporter.importWorkout(
                                this,
                                contentResolver.openInputStream(fileUri)
                            )
                            imported++
                        } catch (e: Exception) {
                            e.printStackTrace()
                            if (imported == 0 && i == files.size - 1) {
                                // If all workouts failed throw exception so it is shown to the user
                                throw e
                            }
                        }
                    }
                }
                dialog.setProgress(100)
                mHandler.post {
                    dialog.cancel()
                    Toast.makeText(
                            this,
                            resources.getQuantityString(R.plurals.importedWorkouts, imported, imported),
                            Toast.LENGTH_LONG
                    ).show()
                    refresh()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mHandler.post {
                    dialog.cancel()
                    Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun checkFirstStart() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (preferences.getBoolean("firstStart", true)) {
            preferences.edit().putBoolean("firstStart", false).apply()
            AlertDialog.Builder(this)
                .setTitle(R.string.setPreferencesTitle)
                .setMessage(R.string.setPreferencesMessage)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.settings) { _: DialogInterface?, _: Int ->
                    startActivity(
                        Intent(this@ListWorkoutsActivity, FitoTrackSettingsActivity::class.java)
                    )
                }
                .create().show()
        }
    }

    private fun startEnterWorkoutActivity() {
        menu.close(true)
        val intent = Intent(this, EnterWorkoutActivity::class.java)
        Handler().postDelayed({ startActivity(intent) }, 300)
    }

    private fun showWorkoutSelection() {
        menu.close(true)
        SelectWorkoutTypeDialog(this) { activity: WorkoutType -> startRecording(activity) }.show()
    }

    private fun startRecording(activity: WorkoutType) {
        menu.close(true)
        val intent = Intent(this, activity.getRecordingType().recorderActivityClass)
        intent.action = RecordWorkoutActivity.LAUNCH_ACTION
        intent.putExtra(RecordWorkoutActivity.WORKOUT_TYPE_EXTRA, activity)
        startActivity(intent)
    }

    public override fun onResume() {
        super.onResume()
        
        // In case the user closes the app while the OAuth CustomTab is open, it is needed to reset the state of the CustomTab.
        // Otherwise, an error will be shown without starting the CustomTab.
        OAuthAuthentication.resetCustomTabState(this)

        refresh()
    }

    override fun onPause() {
        super.onPause()
        menu.close(true)
    }

    override fun onItemClick(pos: Int, workout: Workout) {
        val intent = Intent(this, ShowWorkoutActivity::class.java)
        intent.putExtra(WorkoutActivity.WORKOUT_ID_EXTRA, workout.id)
        startActivity(intent)
        lastClickedIndex = pos
    }

    override fun onItemLongClick(pos: Int, workout: Workout) {
        DialogUtils.showDeleteWorkoutDialog(this) {
            Instance.getInstance(this).db.workoutDao().deleteWorkout(workout)
            refresh()
        }
    }

    private fun refresh() {
        shortStatsView.refresh()
        loadData()
        if (workouts.size > lastClickedIndex) {
            adapter.notifyItemChanged(lastClickedIndex, workouts[lastClickedIndex])
        }
        if (listSize != workouts.size) {
            adapter.notifyDataSetChanged()
        }
        listSize = workouts.size
        refreshFABMenu()
    }

    private fun loadData() {
        this.workouts = Instance.getInstance(this).db.workoutDao().getWorkouts().toTypedArray()

        hintText.visibility = if (workouts.isEmpty()) View.VISIBLE else View.INVISIBLE
        adapter.setWorkouts(this.workouts)
    }

    private fun refreshFABMenu() {
        val lastFab = findViewById<FloatingActionButton>(R.id.workoutListRecordLast)
        if (workouts.isNotEmpty()) {
            val lastType = workouts[0].getWorkoutType(this)
            lastFab.labelText = lastType.title
            lastFab.setImageResource(Icon.getIcon(lastType.icon))
            lastFab.colorNormal = lastType.color
            lastFab.colorPressed = lastFab.colorNormal
            lastFab.setOnClickListener {
                menu.close(true)
                Handler().postDelayed({ startRecording(lastType) }, 300)
            }
        } else {
            lastFab.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.list_workout_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.actionOpenSettings) {
            startActivity(Intent(this, FitoTrackSettingsActivity::class.java))
            return true
        }
        if (id == R.id.actionOpenStatisticss) {
            startActivity(Intent(this, StatisticsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val FILE_IMPORT_SELECT_CODE = 21
        private const val FOLDER_IMPORT_SELECT_CODE = 23
    }
}