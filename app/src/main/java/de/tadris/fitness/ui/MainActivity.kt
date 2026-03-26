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
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.documentfile.provider.DocumentFile
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.osm.OAuthAuthentication
import de.tadris.fitness.ui.dialog.SelectWorkoutTypeDialog
import de.tadris.fitness.ui.dialog.ThreadSafeProgressDialogController
import de.tadris.fitness.ui.navigation.AppNavigation
import de.tadris.fitness.ui.record.RecordWorkoutActivity
import de.tadris.fitness.ui.theme.FitoTrackTheme
import de.tadris.fitness.ui.workout.EnterWorkoutActivity
import de.tadris.fitness.util.PermissionUtils
import de.tadris.fitness.util.io.general.IOHelper

class MainActivity : ComponentActivity() {

    private val mHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FitoTrackTheme {
                AppNavigation(
                    onRecord = { showWorkoutSelection() },
                    onRecordLast = { workoutType -> startRecording(workoutType) },
                    onEnterWorkout = { startEnterWorkoutActivity() },
                    onImport = { showImportDialog() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        OAuthAuthentication.resetCustomTabState(this)
    }

    private fun showWorkoutSelection() {
        SelectWorkoutTypeDialog(this) { workoutType: WorkoutType -> startRecording(workoutType) }.show()
    }

    private fun startRecording(workoutType: WorkoutType) {
        val intent = Intent(this, workoutType.getRecordingType().recorderActivityClass)
        intent.action = RecordWorkoutActivity.LAUNCH_ACTION
        intent.putExtra(RecordWorkoutActivity.WORKOUT_TYPE_EXTRA, workoutType)
        startActivity(intent)
    }

    private fun startEnterWorkoutActivity() {
        val intent = Intent(this, EnterWorkoutActivity::class.java)
        Handler().postDelayed({ startActivity(intent) }, 300)
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
        } catch (_: ActivityNotFoundException) {
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && resultCode == RESULT_OK) {
            when (requestCode) {
                FILE_IMPORT_SELECT_CODE -> importFile(data.data!!)
                FOLDER_IMPORT_SELECT_CODE -> massImportGpx(data.data!!)
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
                            if (imported == 0 && i == files.size - 1) throw e
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

    private fun hasPermission() = PermissionUtils.checkStoragePermissions(this, false)

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            10
        )
    }

    companion object {
        private const val FILE_IMPORT_SELECT_CODE = 21
        private const val FOLDER_IMPORT_SELECT_CODE = 23
    }
}
