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

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.IntervalSet
import de.tadris.fitness.ui.settings.ClickableSettingsItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageIntervalSetsScreen(
    onBack: () -> Unit,
    onEditSet: (Long) -> Unit,
) {
    val context = LocalContext.current
    val dao = remember { Instance.getInstance(context).db.intervalDao() }

    var sets by remember { mutableStateOf(dao.getVisibleSets().toList()) }
    var showCreateDialog by remember { mutableStateOf(false) }

    fun refresh() {
        sets = dao.getVisibleSets().toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manageIntervalSets)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.createIntervalSet))
            }
        },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            if (sets.isEmpty()) {
                item {
                    ClickableSettingsItem(
                        title = stringResource(R.string.hintManageIntervalSets),
                        enabled = false,
                        onClick = {},
                    )
                }
            }
            items(sets) { set ->
                ClickableSettingsItem(
                    title = set.name,
                    onClick = { onEditSet(set.id) },
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateIntervalSetDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                val set = IntervalSet().apply {
                    id = System.currentTimeMillis()
                    this.name = name
                    state = IntervalSet.STATE_VISIBLE
                }
                dao.insertIntervalSet(set)
                showCreateDialog = false
                refresh()
                onEditSet(set.id)
            },
        )
    }
}

@Composable
private fun CreateIntervalSetDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.createIntervalSet)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text(stringResource(R.string.interval_name)) },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text(stringResource(R.string.enterName)) }
                } else null,
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.trim().length <= 2) { nameError = true; return@TextButton }
                onCreate(name.trim())
            }) { Text(stringResource(R.string.create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
