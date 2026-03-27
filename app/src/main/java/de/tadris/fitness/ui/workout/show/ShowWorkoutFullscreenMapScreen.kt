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

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.preferences.UserPreferences
import de.tadris.fitness.ui.workout.diagram.ConverterManager
import de.tadris.fitness.ui.workout.diagram.SpeedConverter
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun ShowWorkoutFullscreenMapScreen(
    id: Long,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val vm: ShowWorkoutViewModel = viewModel(
        key = "workout_$id",
        factory = viewModelFactory {
            initializer {
                ShowWorkoutViewModel(
                    context.applicationContext as android.app.Application,
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

        is WorkoutUiState.Success -> ShowWorkoutFullscreenMapContent(
            data = s.data,
            onBack = onBack,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
private fun ShowWorkoutFullscreenMapContent(
    data: WorkoutData,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val instance = remember { Instance.getInstance(context) }
    val converterManager = remember(data) { ConverterManager(context, data) }

    val initialColoring = remember(data) {
        if (instance.userPreferences.trackStyleMode == UserPreferences.STYLE_USAGE_ALWAYS) {
            WorkoutMapColoring.Custom(SpeedConverter(context))
        } else {
            WorkoutMapColoring.None
        }
    }
    var coloring by remember { mutableStateOf(initialColoring) }

    var showColoringMenu by remember { mutableStateOf(false) }
    val hazeState = rememberHazeState()

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
                            contentDescription = stringResource(R.string.actionBack)
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showColoringMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.selectColoring)
                            )
                        }
                        DropdownMenu(
                            expanded = showColoringMenu,
                            onDismissRequest = { showColoringMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.auto)) },
                                onClick = {
                                    showColoringMenu = false
                                    val autoConverter = converterManager.availableConverters
                                        .firstOrNull()
                                    coloring = if (autoConverter != null) {
                                        WorkoutMapColoring.Custom(autoConverter)
                                    } else {
                                        WorkoutMapColoring.None
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.noColoring)) },
                                onClick = {
                                    showColoringMenu = false
                                    coloring = WorkoutMapColoring.None
                                }
                            )
                            for (converter in converterManager.availableConverters) {
                                DropdownMenuItem(
                                    text = { Text(converter.name) },
                                    onClick = {
                                        showColoringMenu = false
                                        coloring = WorkoutMapColoring.Custom(converter)
                                    }
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.hazeEffect(hazeState, HazeMaterials.ultraThin())
            )
        }
    ) { _ ->
        WorkoutMap(
            data = data,
            showPauses = true,
            clickable = true,
            coloring = coloring,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
        )
    }
}
