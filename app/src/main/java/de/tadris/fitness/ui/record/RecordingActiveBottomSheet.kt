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
package de.tadris.fitness.ui.record

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.tadris.fitness.R
import de.tadris.fitness.recording.RecordingState
import de.tadris.fitness.util.unit.DistanceUnitUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RecordingActiveBottomSheet(
    activeDuration: Long,
    infoSlots: List<InfoSlot>,
    recordingState: RecordingState,
    distanceUnitUtils: DistanceUnitUtils,
    onPauseResume: () -> Unit,
    onStop: () -> Unit,
    onSlotClick: (slot: Int, isLongClick: Boolean) -> Unit,
) {
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Elapsed label
        Text(
            text = stringResource(R.string.recording_elapsed).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        // Time display
        Text(
            text = distanceUnitUtils.getHourMinuteSecondTime(activeDuration),
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Info slots grid
        InfoSlotsGrid(
            slots = infoSlots,
            recordingState = recordingState,
            onSlotClick = onSlotClick,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Action row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = navBottomPadding + 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left placeholder for symmetry
            Box(modifier = Modifier.size(64.dp))

            // Center: Pause/Resume FAB
            val haptic = LocalHapticFeedback.current
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingActionButton(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onPauseResume() },
                    modifier = Modifier.size(72.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        painter = if (recordingState == RecordingState.MANUAL_PAUSE) {
                            painterResource(R.drawable.ic_resume)
                        } else {
                            painterResource(R.drawable.ic_pause)
                        },
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (recordingState == RecordingState.MANUAL_PAUSE) stringResource(R.string.recording_resume) else stringResource(
                        R.string.recording_pause
                    ),
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            // Right: Hold to stop button
            HoldToStopButton(onStop = onStop)
        }
    }
}

@Composable
fun HoldToStopButton(onStop: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val currentOnStop by rememberUpdatedState(onStop)
    val haptic = LocalHapticFeedback.current
    var pressing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val errorColor = MaterialTheme.colorScheme.error
    val errorContainerColor = MaterialTheme.colorScheme.errorContainer
    val onErrorContainerColor = MaterialTheme.colorScheme.onErrorContainer

    LaunchedEffect(pressing) {
        if (pressing) {
            while (true) {
                delay(100)
                haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressing = true
                            val job = scope.launch {
                                progress.animateTo(
                                    1f,
                                    animationSpec = tween(2000, easing = LinearEasing),
                                )
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                currentOnStop()
                            }
                            tryAwaitRelease()
                            pressing = false
                            if (job.isActive) {
                                job.cancel()
                                if (progress.value < 0.3f) {
                                    Toast.makeText(
                                        context,
                                        R.string.recording_hold_to_stop,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                scope.launch {
                                    progress.animateTo(0f, animationSpec = tween(200))
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            // Background circle
            Surface(
                shape = CircleShape,
                color = errorContainerColor,
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Stop square icon
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(onErrorContainerColor, RoundedCornerShape(2.dp)),
                    )
                }
            }

            // Progress arc ring
            val p = progress.value
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (p > 0f) {
                    val strokeWidth = 4.dp.toPx()
                    drawArc(
                        color = errorColor,
                        startAngle = -90f,
                        sweepAngle = 360f * p,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.stop),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
