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
import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.tadris.fitness.R
import de.tadris.fitness.data.RouteSample
import de.tadris.fitness.data.WorkoutCutter
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.map.SimpleColoringStrategy
import de.tadris.fitness.map.WorkoutLayer
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.overlay.FixedPixelCircle

@Composable
fun EditWorkoutStartEndScreen(
    id: Long,
    onBack: () -> Unit,
    onWorkoutModified: () -> Unit,
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

        is WorkoutUiState.Success -> EditWorkoutStartEndContent(
            data = s.data,
            onBack = onBack,
            onWorkoutModified = onWorkoutModified,
        )
    }
}

/** Manages the edit-preview overlay layers on the map (gray original, colored trimmed, circles). */
private class EditPreviewHolder {
    var previewLayer: WorkoutLayer? = null
    var startCircle: FixedPixelCircle? = null
    var endCircle: FixedPixelCircle? = null
    var currentStart: RouteSample? = null
    var currentEnd: RouteSample? = null

    fun update(
        mapView: MapView,
        data: WorkoutData,
        startSample: RouteSample?,
        endSample: RouteSample?,
        primaryColor: Int,
    ) {
        val hasPreview = startSample != null || endSample != null
        val mainLayer = mapView.layerManager.layers.filterIsInstance<WorkoutLayer>().firstOrNull()
        mainLayer?.setFallbackColoringStrategy(
            SimpleColoringStrategy(if (hasPreview) Color.GRAY else primaryColor)
        )

        if (currentStart == startSample && currentEnd == endSample) return
        currentStart = startSample
        currentEnd = endSample

        previewLayer?.let { mapView.layerManager.layers.remove(it) }
        startCircle?.let { mapView.layerManager.layers.remove(it) }
        endCircle?.let { mapView.layerManager.layers.remove(it) }
        previewLayer = null
        startCircle = null
        endCircle = null

        if (!hasPreview) return

        val samples = data.samples
        val drawStart = startSample ?: samples.first()
        val drawEnd = endSample ?: samples.last()
        var inside = false
        val trimmed = mutableListOf<RouteSample>()
        for (s in samples) {
            if (!inside && s.id == drawStart.id) inside = true
            if (inside) trimmed.add(s)
            if (s.id == drawEnd.id) break
        }

        val layer = WorkoutLayer(trimmed, SimpleColoringStrategy(primaryColor), null)
        previewLayer = layer
        mapView.addLayer(layer)

        if (startSample != null) {
            val p = AndroidGraphicFactory.INSTANCE.createPaint().also { it.color = Color.GREEN }
            val circle = FixedPixelCircle(startSample.toLatLong(), 12f, p, null)
            startCircle = circle
            mapView.addLayer(circle)
        }
        if (endSample != null) {
            val p = AndroidGraphicFactory.INSTANCE.createPaint().also { it.color = Color.RED }
            val circle = FixedPixelCircle(endSample.toLatLong(), 12f, p, null)
            endCircle = circle
            mapView.addLayer(circle)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun EditWorkoutStartEndContent(
    data: WorkoutData,
    onBack: () -> Unit,
    onWorkoutModified: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hazeState = rememberHazeState()
    val editHolder = remember { EditPreviewHolder() }
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()

    var selectedSample by remember { mutableStateOf<RouteSample?>(null) }
    var selectedStartSample by remember { mutableStateOf<RouteSample?>(null) }
    var selectedEndSample by remember { mutableStateOf<RouteSample?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val canCutStart = selectedSample != null &&
            (selectedEndSample == null || selectedSample!!.relativeTime <= selectedEndSample!!.relativeTime)
    val canCutEnd = selectedSample != null &&
            (selectedStartSample == null || selectedSample!!.relativeTime >= selectedStartSample!!.relativeTime)
    val hasSelection = selectedStartSample != null || selectedEndSample != null

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.editWorkoutStartEnd)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.actionBack),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { selectedStartSample = selectedSample },
                        enabled = canCutStart
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_left),
                            stringResource(R.string.moveStartOfWorkout)
                        )
                    }
                    IconButton(
                        onClick = { selectedEndSample = selectedSample },
                        enabled = canCutEnd
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_right),
                            stringResource(R.string.moveEndOfWorkout)
                        )
                    }
                    IconButton(onClick = { showConfirmDialog = true }, enabled = hasSelection) {
                        Icon(
                            painterResource(R.drawable.ic_save),
                            stringResource(R.string.actionApply)
                        )
                    }
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(painterResource(R.drawable.ic_help), stringResource(R.string.help))
                    }
                },
                modifier = Modifier.hazeEffect(hazeState, HazeMaterials.ultraThin()),
            )
        }
    ) { _ ->
        WorkoutMap(
            data = data,
            showPauses = true,
            clickable = true,
            coloring = WorkoutMapColoring.None,
            selectedSample = selectedSample,
            onSelectionChanged = { selectedSample = it },
            onMapUpdate = { mapView ->
                editHolder.update(
                    mapView,
                    data,
                    selectedStartSample,
                    selectedEndSample,
                    primaryColor
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(stringResource(R.string.editWorkoutStartEnd)) },
            text = { Text(stringResource(R.string.editStartEndHelp)) },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(stringResource(R.string.okay))
                }
            },
        )
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(stringResource(R.string.saveChangesQuestion)) },
            text = { Text(stringResource(R.string.saveChangesQuestionMessage)) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            WorkoutCutter(
                                context,
                                data,
                                selectedStartSample,
                                selectedEndSample
                            ).save()
                        }
                        onWorkoutModified()
                    }
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
