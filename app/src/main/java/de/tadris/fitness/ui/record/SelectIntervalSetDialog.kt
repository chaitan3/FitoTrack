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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.IntervalSet

@Composable
fun SelectIntervalSetDialog(
    onSelect: (IntervalSet) -> Unit,
    onDismiss: () -> Unit,
    onManage: () -> Unit,
) {
    val context = LocalContext.current
    val sets = remember { Instance.getInstance(context).db.intervalDao().getVisibleSets().toList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.selectIntervalSet)) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(sets) { set ->
                    ListItem(
                        headlineContent = { Text(set.name) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onSelect(set) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onManage) {
                Text(stringResource(R.string.manageIntervalSets))
            }
        },
    )
}
