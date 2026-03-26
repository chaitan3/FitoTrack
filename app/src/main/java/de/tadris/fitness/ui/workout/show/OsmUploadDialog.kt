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

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.tadris.fitness.R
import de.tadris.fitness.data.RouteSample
import de.tadris.fitness.data.Workout
import de.tadris.fitness.osm.OAuthAuthentication
import de.tadris.fitness.osm.OsmTraceUploader
import de.tadris.fitness.ui.theme.spacing
import de.westnordost.osmapi.traces.GpsTraceDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OsmUploadDialog(
    workout: Workout,
    samples: List<RouteSample>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val visibilityOptions = listOf(
        stringResource(R.string.identifiable_visibility) to GpsTraceDetails.Visibility.IDENTIFIABLE,
        stringResource(R.string.trackable_visibility) to GpsTraceDetails.Visibility.TRACKABLE,
        stringResource(R.string.private_visibility) to GpsTraceDetails.Visibility.PRIVATE,
    )

    var description by rememberSaveable { mutableStateOf("") }
    var descriptionError by rememberSaveable { mutableStateOf(false) }
    var cut by rememberSaveable { mutableStateOf(true) }
    var visibilityIndex by rememberSaveable { mutableIntStateOf(0) }
    var visibilityExpanded by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.actionUploadToOSM)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.defaultFull)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it; descriptionError = false },
                    label = { Text(stringResource(R.string.description)) },
                    placeholder = { Text(stringResource(R.string.enterDescription)) },
                    isError = descriptionError,
                    supportingText = if (descriptionError) {
                        { Text(stringResource(R.string.enterDescription)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                ExposedDropdownMenuBox(
                    expanded = visibilityExpanded,
                    onExpandedChange = { visibilityExpanded = it },
                ) {
                    OutlinedTextField(
                        value = visibilityOptions[visibilityIndex].first,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.trackVisibilityPref)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(visibilityExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = visibilityExpanded,
                        onDismissRequest = { visibilityExpanded = false },
                    ) {
                        visibilityOptions.forEachIndexed { index, (label, _) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { visibilityIndex = index; visibilityExpanded = false },
                            )
                        }
                    }
                }

                CheckboxRow(
                    checked = cut,
                    onCheckedChange = { cut = it },
                    label = stringResource(R.string.cut),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (description.trim().length <= 2) {
                    descriptionError = true
                    return@TextButton
                }
                val token = OAuthAuthentication.getAccessToken(context)
                if (activity != null) {
                    OsmTraceUploader(
                        activity,
                        Handler(Looper.getMainLooper()),
                        workout,
                        samples,
                        visibilityOptions[visibilityIndex].second,
                        token,
                        cut,
                        description.trim(),
                    ).upload()
                }
                onDismiss()
            }) { Text(stringResource(R.string.upload)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun CheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}
