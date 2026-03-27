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

package de.tadris.fitness.ui.workout

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.tadris.fitness.R
import de.tadris.fitness.aggregation.WorkoutInformation
import de.tadris.fitness.ui.workout.show.WorkoutMap
import de.tadris.fitness.ui.workout.show.WorkoutMapColoring
import de.tadris.fitness.util.Icon as WorkoutIcon

private val gradientDark = Color(0xAF03031B)

@Composable
fun ShareWorkoutScreen(
    id: Long,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val vm: ShareWorkoutViewModel = viewModel(
        key = "share_workout_$id",
        factory = viewModelFactory {
            initializer {
                ShareWorkoutViewModel(context.applicationContext as Application, id)
            }
        }
    )
    val state by vm.uiState.collectAsState()

    when (val s = state) {
        is ShareWorkoutUiState.Loading -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }

        is ShareWorkoutUiState.Error -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { Text(s.message) }

        is ShareWorkoutUiState.Ready -> ShareWorkoutContent(state = s, vm = vm, onBack = onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareWorkoutContent(
    state: ShareWorkoutUiState.Ready,
    vm: ShareWorkoutViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as Activity
    val form = state.form

    var cardBoundsInWindow by remember { mutableStateOf<android.graphics.Rect?>(null) }
    var editingSlot by remember { mutableStateOf<Int?>(null) }

    fun captureAndShare() {
        val rect = cardBoundsInWindow ?: return
        val bitmap = createBitmap(rect.width(), rect.height())
        PixelCopy.request(
            activity.window,
            rect,
            bitmap,
            { result -> if (result == PixelCopy.SUCCESS) vm.shareBitmap(bitmap, context) },
            Handler(Looper.getMainLooper()),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.share)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { captureAndShare() }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = stringResource(R.string.share)
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .onGloballyPositioned { coords ->
                    val b = coords.boundsInWindow()
                    cardBoundsInWindow = android.graphics.Rect(
                        b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt()
                    )
                },
        ) {
            WorkoutMap(
                data = form.data,
                modifier = Modifier.fillMaxSize(),
                coloring = WorkoutMapColoring.FollowPreference,
                clickable = true,
            )

            // Top overlay: activity icon + FitoTrack branding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(gradientDark, Color.Transparent)))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val icon = form.workoutType.icon
                if (icon != null && WorkoutIcon.getIcon(icon) != WorkoutIcon.OTHER.iconRes) {
                    Icon(
                        painter = painterResource(WorkoutIcon.getIcon(icon)),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(45.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.logo_small),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(35.dp, 45.dp),
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 35.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        shadow = Shadow(blurRadius = 10f),
                    ),
                    modifier = Modifier.padding(start = 8.dp, end = 12.dp),
                )
            }

            // Bottom overlay: 3 metric slots + attribution
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, gradientDark)))
                    .padding(horizontal = 5.dp, vertical = 8.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    form.slots.forEachIndexed { index, slot ->
                        MetricSlotItem(
                            slot = slot,
                            onClick = { editingSlot = index },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.OpenStreetMapAttribution),
                    color = Color(0xFFDCDCDC),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 4.dp),
                )
            }
        }
    }

    val slot = editingSlot
    if (slot != null) {
        MetricPickerDialog(
            availableInfo = form.availableInfo,
            onDismiss = { editingSlot = null },
            onSelect = { info ->
                vm.selectMetric(slot, info.id)
                editingSlot = null
            },
        )
    }
}

@Composable
private fun MetricSlotItem(
    slot: ShareMetricSlot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(2.dp),
    ) {
        Text(
            text = slot.label,
            style = TextStyle(
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(blurRadius = 7f),
            ),
        )
        Text(
            text = slot.value,
            style = TextStyle(
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(blurRadius = 8f),
            ),
        )
    }
}

@Composable
private fun MetricPickerDialog(
    availableInfo: List<WorkoutInformation>,
    onDismiss: () -> Unit,
    onSelect: (WorkoutInformation) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            LazyColumn {
                items(availableInfo) { info ->
                    ListItem(
                        headlineContent = { Text(stringResource(info.titleRes)) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onSelect(info) },
                    )
                }
            }
        },
        confirmButton = {},
    )
}
