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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import de.tadris.fitness.R

@Composable
fun MapZoomControls(
    onFocusGps: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FloatingActionButton(onClick = onFocusGps, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(R.drawable.ic_gps_fixed), contentDescription = null)
        }
        FloatingActionButton(onClick = onZoomIn, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(R.drawable.ic_map_zoomin), contentDescription = "+")
        }
        FloatingActionButton(onClick = onZoomOut, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(R.drawable.ic_map_zoomout), contentDescription = "-")
        }
    }
}
