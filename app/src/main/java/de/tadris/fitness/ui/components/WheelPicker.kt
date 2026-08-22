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
package de.tadris.fitness.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * A scrollable, snapping wheel picker for choosing an integer from [range].
 * The value centered in the visible window is the selected one; it settles there via snapping
 * and is reported through [onValueChange] once scrolling stops.
 */
@Composable
fun WheelPicker(
    range: IntRange,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 40.dp,
    visibleItemCount: Int = 3,
    labelFormatter: (Int) -> String = { it.toString().padStart(2, '0') },
) {
    val values = remember(range) { range.toList() }
    val initialIndex = remember(range) { (value - range.first).coerceIn(0, values.lastIndex) }
    val listState = rememberLazyListState(initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(listState)
    val coroutineScope = rememberCoroutineScope()

    val selectedIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@derivedStateOf initialIndex
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            visibleItems.minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }?.index
                ?: initialIndex
        }
    }

    val isScrollInProgress = listState.isScrollInProgress
    LaunchedEffect(isScrollInProgress, selectedIndex) {
        if (!isScrollInProgress) {
            val newValue = values.getOrNull(selectedIndex) ?: return@LaunchedEffect
            if (newValue != value) onValueChange(newValue)
        }
    }

    Box(
        modifier = modifier
            .width(64.dp)
            .height(itemHeight * visibleItemCount),
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItemCount / 2)),
            modifier = Modifier.fillMaxHeight(),
        ) {
            itemsIndexed(values) { index, itemValue ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable {
                            coroutineScope.launch { listState.animateScrollToItem(index) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = labelFormatter(itemValue),
                        style = if (selected) MaterialTheme.typography.titleLarge
                        else MaterialTheme.typography.bodyLarge,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalDivider(Modifier
            .align(Alignment.Center)
            .offset(y = -itemHeight / 2))
        HorizontalDivider(Modifier
            .align(Alignment.Center)
            .offset(y = itemHeight / 2))
    }
}
