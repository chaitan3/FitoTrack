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

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.ExportTargetConfiguration
import de.tadris.fitness.data.autoexport.AutoExportPlanner
import de.tadris.fitness.data.autoexport.source.ExportSource
import de.tadris.fitness.data.autoexport.target.ExportTarget
import de.tadris.fitness.util.isUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureExportTargetsScreen(sourceId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { Instance.getInstance(context).db.exportTargetDao() }
    val scope = rememberCoroutineScope()

    var targets by remember { mutableStateOf<List<ExportTargetConfiguration>>(emptyList()) }
    var refreshKey by remember { mutableIntStateOf(0) }

    var showSelectTypeDialog by remember { mutableStateOf(false) }
    var showHttpDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ExportTargetConfiguration?>(null) }

    LaunchedEffect(refreshKey) {
        targets = withContext(Dispatchers.IO) { dao.findAllFor(sourceId).toList() }
    }

    fun onConfigurationChanged() {
        if (sourceId == ExportSource.EXPORT_SOURCE_BACKUP) {
            AutoExportPlanner(context).planAutoBackup()
        }
        refreshKey++
    }

    fun insert(type: String, data: String) {
        scope.launch(Dispatchers.IO) {
            dao.insert(ExportTargetConfiguration().also {
                it.source = sourceId
                it.type = type
                it.data = data
            })
            withContext(Dispatchers.Main) { onConfigurationChanged() }
        }
    }

    fun delete(configuration: ExportTargetConfiguration) {
        scope.launch(Dispatchers.IO) {
            dao.delete(configuration)
            withContext(Dispatchers.Main) { onConfigurationChanged() }
        }
    }

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            insert(ExportTarget.TARGET_TYPE_DIRECTORY, it.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(ExportSource.getTitle(sourceId))) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showSelectTypeDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (targets.isEmpty()) {
                Text(
                    text = stringResource(R.string.hintConfigureExportTargets),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(20.dp),
                )
            } else {
                LazyColumn {
                    item {
                        Text(
                            text = stringResource(ExportSource.getExplanation(sourceId)),
                            modifier = Modifier.padding(15.dp),
                        )
                    }
                    items(targets, key = { it.id }) { config ->
                        val targetImpl = config.targetImplementation
                        ListItem(
                            headlineContent = {
                                Text(
                                    if (targetImpl != null) stringResource(targetImpl.titleRes)
                                    else config.type
                                )
                            },
                            supportingContent = { Text(config.data) },
                            trailingContent = {
                                IconButton(onClick = { pendingDelete = config }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showSelectTypeDialog) {
        SelectTargetTypeDialog(
            onDismiss = { showSelectTypeDialog = false },
            onSelect = { target ->
                showSelectTypeDialog = false
                when (target.id) {
                    ExportTarget.TARGET_TYPE_DIRECTORY -> directoryPickerLauncher.launch(null)
                    ExportTarget.TARGET_TYPE_HTTP_POST -> showHttpDialog = true
                }
            },
        )
    }

    if (showHttpDialog) {
        HttpPostDialog(
            onDismiss = { showHttpDialog = false },
            onConfirm = { url ->
                showHttpDialog = false
                insert(ExportTarget.TARGET_TYPE_HTTP_POST, url)
            },
        )
    }

    pendingDelete?.let { config ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.deleteExportTargetConfirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    delete(config)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SelectTargetTypeDialog(
    onDismiss: () -> Unit,
    onSelect: (ExportTarget) -> Unit,
) {
    val targetTypes = remember { ExportTarget.exportTargetTypes }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.selectExportTarget)) },
        text = {
            Column {
                targetTypes.forEach { target ->
                    ListItem(
                        headlineContent = { Text(stringResource(target.titleRes)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(target) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun HttpPostDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.configureHttpPostRequest)) },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it; error = false },
                label = { Text("URL") },
                isError = error,
                supportingText = if (error) {
                    { Text(stringResource(R.string.enterValidUrl)) }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (url.isUrl()) onConfirm(url) else error = true
            }) { Text(stringResource(R.string.okay)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
