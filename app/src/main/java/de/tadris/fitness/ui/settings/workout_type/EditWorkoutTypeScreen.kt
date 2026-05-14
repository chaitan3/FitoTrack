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

package de.tadris.fitness.ui.settings.workout_type

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.RecordingType
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.data.WorkoutTypeManager
import de.tadris.fitness.util.Icon as WorkoutIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkoutTypeScreen(
    typeId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isNewType = typeId.isEmpty()
    val primaryColor = MaterialTheme.colorScheme.primary

    var id by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var minDistance by remember { mutableStateOf("5") }
    var met by remember { mutableStateOf("0") }
    var iconName by remember { mutableStateOf(WorkoutIcon.RUNNING.name) }
    var typeColor by remember { mutableStateOf(primaryColor) }
    var recordingType by remember { mutableStateOf(RecordingType.GPS.id) }

    var idError by remember { mutableStateOf<String?>(null) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var minDistanceError by remember { mutableStateOf<String?>(null) }
    var metError by remember { mutableStateOf<String?>(null) }

    var showIconPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val distanceUnit = remember {
        Instance.getInstance(context).distanceUnitUtils.getDistanceUnitSystem().getShortDistanceUnit()
    }

    LaunchedEffect(typeId) {
        if (!isNewType) {
            withContext(Dispatchers.IO) {
                val type = Instance.getInstance(context).db.workoutTypeDao().findById(typeId)
                if (type != null) {
                    id = type.id
                    title = type.title
                    minDistance = type.minDistance.toString()
                    met = type.MET.toString()
                    iconName = type.icon
                    typeColor = Color(type.color)
                    recordingType = type.recordingType
                }
            }
        } else {
            typeColor = primaryColor
        }
    }

    fun validate(): WorkoutType? {
        idError = null
        titleError = null
        minDistanceError = null
        metError = null

        val trimmedId = id.trim()
        val trimmedTitle = title.trim()
        val parsedMinDistance = minDistance.toIntOrNull()
        val parsedMet = met.toIntOrNull()

        if (trimmedId.isEmpty()) {
            idError = context.getString(R.string.workoutTypeEditIdErrorEmpty)
            return null
        }
        if (!trimmedId.matches(Regex("[a-zA-Z0-9_-]*"))) {
            idError = context.getString(R.string.workoutTypeEditIdErrorCharacters)
            return null
        }
        if (isNewType) {
            val existing = WorkoutTypeManager.getInstance().getWorkoutTypeById(context, trimmedId)
            if (existing != null && existing.id != WorkoutTypeManager.WORKOUT_TYPE_ID_OTHER) {
                idError = context.getString(R.string.workoutTypeEditIdErrorUnique)
                return null
            }
        }
        if (trimmedTitle.isEmpty()) {
            titleError = context.getString(R.string.workoutTypeEditNameError)
            return null
        }
        if (parsedMinDistance == null) {
            minDistanceError = context.getString(R.string.errorEnterValidNumber)
            return null
        }
        if (parsedMet == null) {
            metError = context.getString(R.string.errorEnterValidNumber)
            return null
        }
        return WorkoutType(
            trimmedId,
            trimmedTitle,
            parsedMinDistance,
            typeColor.toArgb(),
            iconName,
            parsedMet,
            recordingType,
        )
    }

    fun save() {
        val type = validate() ?: return
        scope.launch(Dispatchers.IO) {
            if (isNewType) {
                Instance.getInstance(context).db.workoutTypeDao().insert(type)
            } else {
                Instance.getInstance(context).db.workoutTypeDao().update(type)
            }
            withContext(Dispatchers.Main) { onBack() }
        }
    }

    fun delete() {
        scope.launch(Dispatchers.IO) {
            val type = Instance.getInstance(context).db.workoutTypeDao().findById(typeId)
            if (type != null) {
                Instance.getInstance(context).db.workoutTypeDao().delete(type)
            }
            withContext(Dispatchers.Main) { onBack() }
        }
    }

    if (showIconPicker) {
        IconPickerDialog(
            onDismiss = { showIconPicker = false },
            onSelectIcon = { icon ->
                iconName = icon.name
                showIconPicker = false
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.deleteWorkoutType)) },
            text = { Text(stringResource(R.string.deleteWorkoutTypeConfirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    delete()
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.editWorkoutType)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (!isNewType) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.deleteWorkoutType))
                        }
                    }
                    IconButton(onClick = { save() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_save),
                            contentDescription = stringResource(if (isNewType) R.string.add else R.string.save),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = id,
                onValueChange = { id = it; idError = null },
                label = { Text(stringResource(R.string.workoutTypeEditId)) },
                isError = idError != null,
                supportingText = idError?.let { { Text(it) } },
                enabled = isNewType,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; titleError = null },
                label = { Text(stringResource(R.string.workoutTypeEditName)) },
                isError = titleError != null,
                supportingText = titleError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = minDistance,
                onValueChange = { minDistance = it; minDistanceError = null },
                label = { Text(stringResource(R.string.workoutTypeEditMinDistance)) },
                isError = minDistanceError != null,
                supportingText = minDistanceError?.let { { Text(it) } },
                suffix = { Text(distanceUnit) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = met,
                onValueChange = { met = it; metError = null },
                label = { Text(stringResource(R.string.workoutTypeEditMET)) },
                isError = metError != null,
                supportingText = metError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.workoutTypeEditIcon)) },
                trailingContent = {
                    Icon(
                        painter = painterResource(WorkoutIcon.getIcon(iconName)),
                        contentDescription = null,
                        tint = typeColor,
                    )
                },
                modifier = Modifier.clickable { showIconPicker = true },
            )
        }
    }
}

@Composable
private fun IconPickerDialog(
    onDismiss: () -> Unit,
    onSelectIcon: (WorkoutIcon) -> Unit,
) {
    val icons = remember { WorkoutIcon.values().toList() }
    val primary = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.workoutTypeEditIcon)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(4.dp),
            ) {
                items(icons) { icon ->
                    IconButton(onClick = { onSelectIcon(icon) }) {
                        Icon(
                            painter = painterResource(icon.iconRes),
                            contentDescription = icon.name,
                            tint = primary,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
