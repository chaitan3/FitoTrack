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

package de.tadris.fitness.ui.map

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.RouteSample
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.preferences.UserPreferences
import de.tadris.fitness.ui.workout.diagram.ConverterManager
import de.tadris.fitness.ui.workout.show.ShowWorkoutViewModel
import de.tadris.fitness.ui.workout.show.WorkoutUiState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi

/** State passed into the optional [bottomContent] slot of [WorkoutMapScreen]. */
internal class WorkoutMapContentState(
    val data: WorkoutData,
    val onSampleSelected: (RouteSample?) -> Unit,
    val onColoringChange: (WorkoutMapColoring) -> Unit,
    val converterManager: ConverterManager,
)

/**
 * Shared screen for all fullscreen map views.
 *
 * When [bottomContent] is null the map fills the screen (fullscreen map).
 * When [bottomContent] is provided the screen splits: map 55% top, content 45% bottom
 * (map+diagram layout). The [WorkoutMapContentState] gives the slot access to loaded
 * data, the current cross-selection state and coloring controls.
 *
 * @param isDiagramScreen Affects the initial auto-coloring: diagram screens additionally
 *   trigger auto-coloring when [UserPreferences.STYLE_USAGE_DIAGRAM] is set.
 */
@Composable
internal fun WorkoutMapScreen(
    id: Long,
    onBack: () -> Unit,
    isDiagramScreen: Boolean = false,
    bottomContent: (@Composable (WorkoutMapContentState) -> Unit)? = null,
) {
    val context = LocalContext.current
    val vm: ShowWorkoutViewModel = viewModel(
        key = "workout_$id",
        factory = viewModelFactory {
            initializer {
                ShowWorkoutViewModel(
                    context.applicationContext as Application,
                    id,
                )
            }
        }
    )
    val state by vm.uiState.collectAsState()

    when (val s = state) {
        is WorkoutUiState.Loading -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        is WorkoutUiState.Error -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(s.message)
        }

        is WorkoutUiState.Success -> WorkoutMapContent(
            data = s.data,
            onBack = onBack,
            isDiagramScreen = isDiagramScreen,
            bottomContent = bottomContent,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
private fun WorkoutMapContent(
    data: WorkoutData,
    onBack: () -> Unit,
    isDiagramScreen: Boolean,
    bottomContent: (@Composable (WorkoutMapContentState) -> Unit)?,
) {
    val context = LocalContext.current
    val instance = remember { Instance.getInstance(context) }
    val converterManager = remember(data) { ConverterManager(context, data) }

    val initialColoring = remember(data) {
        val mode = instance.userPreferences.trackStyleMode
        val useColoring = mode == UserPreferences.STYLE_USAGE_ALWAYS ||
                (isDiagramScreen && mode == UserPreferences.STYLE_USAGE_DIAGRAM)
        if (useColoring) {
            converterManager.availableConverters.firstOrNull()
                ?.let { WorkoutMapColoring.Custom(it) }
                ?: WorkoutMapColoring.None
        } else {
            WorkoutMapColoring.None
        }
    }
    var coloring by remember { mutableStateOf(initialColoring) }
    var selectedSample by remember { mutableStateOf<RouteSample?>(null) }
    var showColoringMenu by remember { mutableStateOf(false) }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.actionBack)
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showColoringMenu = true }) {
                            Icon(Icons.Default.MoreVert, stringResource(R.string.selectColoring))
                        }
                        DropdownMenu(
                            expanded = showColoringMenu,
                            onDismissRequest = { showColoringMenu = false },
                        ) {
                            ColoringMenuItems(
                                converterManager = converterManager,
                                onSelected = { coloring = it; showColoringMenu = false },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (bottomContent == null) {
            WorkoutMap(
                data = data,
                showPauses = true,
                clickable = true,
                coloring = coloring,
                selectedSample = selectedSample,
                onSelectionChanged = { selectedSample = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding()),
            )
        } else {
            val contentState = WorkoutMapContentState(
                data = data,
                onSampleSelected = { selectedSample = it },
                onColoringChange = { coloring = it },
                converterManager = converterManager,
            )
            Column(Modifier.fillMaxSize()) {
                WorkoutMap(
                    data = data,
                    showPauses = false,
                    clickable = true,
                    coloring = coloring,
                    selectedSample = selectedSample,
                    onSelectionChanged = { selectedSample = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = padding.calculateTopPadding())
                        .weight(0.55f),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(0.45f)
                        .padding(bottom = padding.calculateBottomPadding())
                ) {
                    bottomContent(contentState)
                }
            }
        }
    }
}

@Composable
internal fun ColoringMenuItems(
    converterManager: ConverterManager,
    onSelected: (WorkoutMapColoring) -> Unit,
) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.auto)) },
        onClick = {
            val converter = converterManager.availableConverters.firstOrNull()
            onSelected(if (converter != null) WorkoutMapColoring.Custom(converter) else WorkoutMapColoring.None)
        },
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.noColoring)) },
        onClick = { onSelected(WorkoutMapColoring.None) },
    )
    for (converter in converterManager.availableConverters) {
        DropdownMenuItem(
            text = { Text(converter.name) },
            onClick = { onSelected(WorkoutMapColoring.Custom(converter)) },
        )
    }
}
