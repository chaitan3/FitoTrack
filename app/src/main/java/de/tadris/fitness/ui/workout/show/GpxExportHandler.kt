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

package de.tadris.fitness.ui.workout.show

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.autoexport.source.WorkoutGpxExportSource
import de.tadris.fitness.util.DataManager
import de.tadris.fitness.util.PermissionUtils
import de.tadris.fitness.util.export.IOHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Holds all state and UI needed to trigger a GPX export from a composable. */
class GpxExportState(
    val onExport: () -> Unit,
)

@Composable
internal fun rememberGpxExportState(data: WorkoutData): GpxExportState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) runExport(
            data, context, scope,
            onLoading = { loading = it },
            onError = { errorMessage = it },
        )
    }

    fun export() {
        if (PermissionUtils.checkStoragePermissions(context)) {
            runExport(
                data, context, scope,
                onLoading = { loading = it },
                onError = { errorMessage = it },
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                )
            )
        }
    }

    if (loading) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.exporting)) },
            text = { CircularProgressIndicator() },
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

    return remember { GpxExportState(onExport = ::export) }
}

private fun runExport(
    data: WorkoutData,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onLoading: (Boolean) -> Unit,
    onError: (String) -> Unit,
) {
    scope.launch {
        onLoading(true)
        try {
            val shareIntent = withContext(Dispatchers.IO) {
                val file = WorkoutGpxExportSource(data.workout.id).provideFile(context).file
                val uri = DataManager.provide(context, file)
                IOHelper.GpxExporter.exportWorkout(data, file)
                android.content.Intent(context, de.tadris.fitness.ui.ShareFileActivity::class.java)
                    .putExtra(de.tadris.fitness.ui.ShareFileActivity.EXTRA_FILE_URI, uri.toString())
            }
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            onError(context.getString(R.string.errorGpxExportFailed))
        } finally {
            onLoading(false)
        }
    }
}
