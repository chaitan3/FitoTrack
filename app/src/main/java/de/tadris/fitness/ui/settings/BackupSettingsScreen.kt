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

package de.tadris.fitness.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.export.BackupReader
import de.tadris.fitness.export.BackupWriter
import de.tadris.fitness.ui.ShareFileActivity
import de.tadris.fitness.ui.settings.export.ConfigureExportTargetsActivity
import de.tadris.fitness.util.DataManager
import de.tadris.fitness.util.PermissionUtils
import de.tadris.fitness.util.autoexport.AutoExportPlanner
import de.tadris.fitness.util.autoexport.source.BackupExportSource
import de.tadris.fitness.util.autoexport.source.ExportSource
import de.tadris.fitness.util.io.GpxExporter
import de.tadris.fitness.util.io.MassExporter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val handler = remember { Handler(Looper.getMainLooper()) }
    val sharedPrefs = remember { Instance.getInstance(context).userPreferences.preferences }

    var showExportConfirmDialog by remember { mutableStateOf(false) }
    var showImportChoiceDialog by remember { mutableStateOf(false) }
    var showReplaceConfirmDialog by remember { mutableStateOf(false) }

    var progressActive by remember { mutableStateOf(false) }
    var progressValue by remember { mutableFloatStateOf(0f) }
    var progressAction by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    var backupInterval by remember {
        mutableStateOf(sharedPrefs.getString("autoBackupInterval", "168") ?: "168")
    }

    fun startExportTask(task: (listener: BackupWriter.ExportStatusListener) -> File) {
        progressActive = true
        progressValue = 0f
        progressAction = ""
        Thread {
            try {
                val file = task { progress, action ->
                    handler.post {
                        progressValue = progress / 100f
                        progressAction = action ?: ""
                    }
                }
                val uri = DataManager.provide(context, file)
                handler.post {
                    progressActive = false
                    context.startActivity(
                        Intent(context, ShareFileActivity::class.java)
                            .putExtra(ShareFileActivity.EXTRA_FILE_URI, uri.toString())
                    )
                }
            } catch (e: Exception) {
                handler.post {
                    progressActive = false
                    errorMessage = e.message ?: context.getString(R.string.errorExportFailed)
                }
            }
        }.start()
    }

    fun startImportTask(uri: Uri, replace: Boolean) {
        progressActive = true
        progressValue = 0f
        progressAction = ""
        Thread {
            try {
                BackupReader(context, uri, replace) { progress, action ->
                    handler.post {
                        progressValue = progress / 100f
                        progressAction = action ?: ""
                    }
                }.read()
                handler.post { progressActive = false }
            } catch (e: Exception) {
                handler.post {
                    progressActive = false
                    errorMessage = e.message ?: context.getString(R.string.errorImportFailed)
                }
            }
        }.start()
    }

    val replaceImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { startImportTask(it, replace = true) }
    }
    val mergeImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { startImportTask(it, replace = false) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* handled by hasPermission check on next interaction */ }

    fun hasPermission() = PermissionUtils.checkStoragePermissions(context, true)
    fun requestPermissions() {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preferencesBackupTitle)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item { SettingsCategory(stringResource(R.string.backup)) }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.importBackup),
                    supporting = stringResource(R.string.importBackupSummary),
                    onClick = {
                        if (hasPermission()) showImportChoiceDialog = true
                        else requestPermissions()
                    },
                )
            }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.exportData),
                    supporting = stringResource(R.string.exportDataSummary),
                    onClick = {
                        if (hasPermission()) showExportConfirmDialog = true
                        else requestPermissions()
                    },
                )
            }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.massExportGpxTitle),
                    supporting = stringResource(R.string.massExportGpxSummary),
                    onClick = {
                        startExportTask { listener ->
                            val file = DataManager.createSharableFile(context, "workouts.zip")
                            MassExporter(
                                context,
                                Instance.getInstance(context).db.workoutDao(),
                                GpxExporter(),
                                file,
                            ) { progress -> listener.onStatusChanged(progress, null) }.export()
                            file
                        }
                    },
                )
            }
            item { SettingsCategory(stringResource(R.string.autoBackupTitle)) }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.workoutGPXExportTitle),
                    supporting = stringResource(R.string.autoExportWorkoutsSummary),
                    onClick = {
                        context.startActivity(
                            Intent(context, ConfigureExportTargetsActivity::class.java)
                                .putExtra(ConfigureExportTargetsActivity.EXTRA_SOURCE, ExportSource.EXPORT_SOURCE_WORKOUT_GPX)
                        )
                    },
                )
            }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.autoExportBackupTitle),
                    supporting = stringResource(R.string.autoExportBackupSummary),
                    onClick = {
                        context.startActivity(
                            Intent(context, ConfigureExportTargetsActivity::class.java)
                                .putExtra(ConfigureExportTargetsActivity.EXTRA_SOURCE, ExportSource.EXPORT_SOURCE_BACKUP)
                        )
                    },
                )
            }
            item {
                ListPreferenceItem(
                    title = stringResource(R.string.exportIntervalTitle),
                    entries = stringArrayResource(R.array.pref_backup_interval_entries),
                    entryValues = stringArrayResource(R.array.pref_backup_interval_values),
                    selectedValue = backupInterval,
                    onValueChange = { value ->
                        backupInterval = value
                        sharedPrefs.edit().putString("autoBackupInterval", value).apply()
                        AutoExportPlanner(context).planAutoBackup()
                    },
                )
            }
        }
    }

    if (showExportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExportConfirmDialog = false },
            title = { Text(stringResource(R.string.exportData)) },
            text = { Text(stringResource(R.string.exportDataSummary)) },
            confirmButton = {
                TextButton(onClick = {
                    showExportConfirmDialog = false
                    startExportTask { listener ->
                        BackupExportSource(true).exportToFile(context, listener)
                    }
                }) { Text(stringResource(R.string.backup)) }
            },
            dismissButton = {
                TextButton(onClick = { showExportConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showImportChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showImportChoiceDialog = false },
            title = { Text(stringResource(R.string.importBackup)) },
            text = { Text(stringResource(R.string.replaceOrMergeMessage)) },
            confirmButton = {
                TextButton(onClick = {
                    showImportChoiceDialog = false
                    showReplaceConfirmDialog = true
                }) { Text(stringResource(R.string.replace)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportChoiceDialog = false
                    mergeImportLauncher.launch("*/*")
                }) { Text(stringResource(R.string.merge)) }
            },
        )
    }

    if (showReplaceConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showReplaceConfirmDialog = false },
            title = { Text(stringResource(R.string.importBackup)) },
            text = { Text(stringResource(R.string.importBackupMessage)) },
            confirmButton = {
                TextButton(onClick = {
                    showReplaceConfirmDialog = false
                    replaceImportLauncher.launch("*/*")
                }) { Text(stringResource(R.string.restore)) }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (progressActive) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.backup)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (progressAction.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(progressAction)
                    }
                }
            },
            confirmButton = {},
        )
    }

    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text(stringResource(R.string.error)) },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text(stringResource(R.string.okay))
                }
            },
        )
    }
}
