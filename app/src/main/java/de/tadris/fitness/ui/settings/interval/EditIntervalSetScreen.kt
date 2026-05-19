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

package de.tadris.fitness.ui.settings.interval

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.Interval
import de.tadris.fitness.data.IntervalSet
import de.tadris.fitness.ui.settings.ClickableSettingsItem
import java.util.concurrent.TimeUnit
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditIntervalSetScreen(
    setId: Long,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val dao = remember { Instance.getInstance(context).db.intervalDao() }
    val intervalSet = remember { dao.getSet(setId) }

    var intervals by remember { mutableStateOf(dao.getAllIntervalsOfSet(setId).toList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingInterval by remember { mutableStateOf<Interval?>(null) }
    var showDeleteSetDialog by remember { mutableStateOf(false) }

    fun refresh() {
        intervals = dao.getAllIntervalsOfSet(setId).toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(intervalSet?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteSetDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.deleteIntervalSet))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_interval))
            }
        },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            if (intervals.isEmpty()) {
                item {
                    ClickableSettingsItem(
                        title = stringResource(R.string.hintEditIntervalSet),
                        enabled = false,
                        onClick = {},
                    )
                }
            }
            items(intervals, key = { it.id }) { interval ->
                val minutes = interval.delayMillis / TimeUnit.MINUTES.toMillis(1).toFloat()
                val minutesStr = if (minutes == round(minutes)) minutes.toInt().toString() else "%.1f".format(minutes)
                ListItem(
                    headlineContent = { Text(interval.name) },
                    supportingContent = { Text("$minutesStr min") },
                    trailingContent = {
                        IconButton(onClick = {
                            dao.deleteInterval(interval)
                            refresh()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    },
                    modifier = Modifier.clickable { editingInterval = interval },
                )
            }
        }
    }

    if (showAddDialog) {
        IntervalDialog(
            title = stringResource(R.string.add_interval),
            confirmLabel = stringResource(R.string.add),
            onDismiss = { showAddDialog = false },
            onConfirm = { name, lengthMinutes ->
                val interval = Interval().apply {
                    id = System.currentTimeMillis()
                    this.setId = setId
                    this.name = name
                    delayMillis = (TimeUnit.MINUTES.toMillis(1) * lengthMinutes).toLong()
                }
                dao.insertInterval(interval)
                showAddDialog = false
                refresh()
            },
        )
    }

    editingInterval?.let { interval ->
        val defaultLength = (interval.delayMillis / TimeUnit.MINUTES.toMillis(1).toFloat()).let { m ->
            if (m == round(m)) m.toInt().toString() else "%.1f".format(m)
        }
        IntervalDialog(
            title = stringResource(R.string.edit_interval),
            confirmLabel = stringResource(R.string.save),
            defaultName = interval.name,
            defaultLength = defaultLength,
            onDismiss = { editingInterval = null },
            onConfirm = { name, lengthMinutes ->
                interval.name = name
                interval.delayMillis = (TimeUnit.MINUTES.toMillis(1) * lengthMinutes).toLong()
                dao.updateInterval(interval)
                editingInterval = null
                refresh()
            },
        )
    }

    if (showDeleteSetDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSetDialog = false },
            title = { Text(stringResource(R.string.deleteIntervalSet)) },
            text = { Text(stringResource(R.string.deleteIntervalSetMessage)) },
            confirmButton = {
                TextButton(onClick = {
                    intervalSet?.state = IntervalSet.STATE_DELETED
                    intervalSet?.let { dao.updateIntervalSet(it) }
                    onBack()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSetDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun IntervalDialog(
    title: String,
    confirmLabel: String,
    defaultName: String = "",
    defaultLength: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, lengthMinutes: Double) -> Unit,
) {
    var name by remember { mutableStateOf(defaultName) }
    var length by remember { mutableStateOf(defaultLength) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var lengthError by remember { mutableStateOf<String?>(null) }

    val enterNameMsg = stringResource(R.string.enterName)
    val invalidNumberMsg = stringResource(R.string.errorEnterValidNumber)
    val invalidDurationMsg = stringResource(R.string.errorEnterValidDuration)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = { Text(stringResource(R.string.interval_name)) },
                    isError = nameError != null,
                    supportingText = nameError?.let { e -> { Text(e) } },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = length,
                    onValueChange = { length = it; lengthError = null },
                    label = { Text(stringResource(R.string.length_in_minutes)) },
                    isError = lengthError != null,
                    supportingText = lengthError?.let { e -> { Text(e) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.trim().length <= 2) { nameError = enterNameMsg; return@TextButton }
                val minutes = length.toDoubleOrNull()
                if (minutes == null) { lengthError = invalidNumberMsg; return@TextButton }
                if (minutes < 0.1 || minutes > 300) { lengthError = invalidDurationMsg; return@TextButton }
                onConfirm(name.trim(), minutes)
            }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
