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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.tadris.fitness.ui.theme.FitoTrackTheme

@Composable
internal fun SectionHeader(title: String) {
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider(Modifier.padding(top = 4.dp))
    }
}

@Composable
internal fun KeyValueRow(
    label1: String,
    value1: String,
    label2: String? = null,
    value2: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        KeyValueText(label1, value1)
        if (label2 != null && value2 != null) {
            KeyValueText(label2, value2)
        }
    }
}

@Composable
private fun RowScope.KeyValueText(label: String, value: String) {
    Column(Modifier.weight(1f)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SectionHeaderPreview() {
    FitoTrackTheme {
        Surface {
            SectionHeader(title = "Time")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun KeyValueRowPreview() {
    FitoTrackTheme {
        Surface {
            Column {
                KeyValueRow(label1 = "Duration", value1 = "42:30")
                KeyValueRow(
                    label1 = "Distance",
                    value1 = "10.5 km",
                    label2 = "Pace",
                    value2 = "4:03 /km"
                )
            }
        }
    }
}