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

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.tadris.fitness.R

@Composable
fun ImportHandler(vm: ImportViewModel) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current

    val fileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) vm.importFile(uri) else vm.dismissChoice()
        }
    val folderLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) vm.importFolder(uri) else vm.dismissMassImportConfirm()
        }

    when (val s = state) {
        is ImportUiState.ShowingChoice -> AlertDialog(
            onDismissRequest = { vm.dismissChoice() },
            title = { Text(stringResource(R.string.importWorkout)) },
            text = { Text(stringResource(R.string.importWorkoutMultipleQuestion)) },
            confirmButton = {
                TextButton(onClick = { fileLauncher.launch("*/*") }) {
                    Text(stringResource(R.string.actionImport))
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.requestMassImportConfirm() }) {
                    Text(stringResource(R.string.actionImportMultiple))
                }
            },
        )

        is ImportUiState.ShowingMassImportConfirm -> AlertDialog(
            onDismissRequest = { vm.dismissMassImportConfirm() },
            title = { Text(stringResource(R.string.importMultipleGpxFiles)) },
            text = { Text(stringResource(R.string.importMultipleMessageSelectFolder)) },
            confirmButton = {
                TextButton(onClick = { folderLauncher.launch(null) }) {
                    Text(stringResource(R.string.okay))
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissMassImportConfirm() }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )

        is ImportUiState.Importing -> Dialog(onDismissRequest = {}) {
            Card {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        stringResource(R.string.importWorkout),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (s.progress != null) {
                        LinearProgressIndicator(
                            progress = { s.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        is ImportUiState.Done -> {
            val message = if (s.count == 1) {
                stringResource(R.string.workoutImported)
            } else {
                pluralStringResource(R.plurals.importedWorkouts, s.count, s.count)
            }
            LaunchedEffect(Unit) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                vm.resetState()
            }
        }

        is ImportUiState.Error -> AlertDialog(
            onDismissRequest = { vm.resetState() },
            title = { Text(stringResource(R.string.error)) },
            text = { Text(s.message) },
            confirmButton = {
                TextButton(onClick = { vm.resetState() }) {
                    Text(stringResource(R.string.okay))
                }
            },
        )

        is ImportUiState.Idle -> {}
    }
}
