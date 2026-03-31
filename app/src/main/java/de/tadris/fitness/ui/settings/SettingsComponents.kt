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

package de.tadris.fitness.ui.settings

import kotlin.math.exp
import kotlin.math.ln
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import de.tadris.fitness.R

@Composable
fun ClickableSettingsItem(
    title: String,
    supporting: String? = null,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = supporting?.let { { Text(it) } },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
fun ListPreferenceItem(
    title: String,
    entries: Array<String>,
    entryValues: Array<String>,
    selectedValue: String,
    onValueChange: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    val selectedIndex = entryValues.indexOf(selectedValue)
    val currentLabel = if (selectedIndex >= 0) entries[selectedIndex] else ""

    ClickableSettingsItem(
        title = title,
        supporting = currentLabel,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .verticalScroll(rememberScrollState())
                ) {
                    entries.forEachIndexed { index, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = entryValues[index] == selectedValue,
                                    onClick = {
                                        onValueChange(entryValues[index])
                                        showDialog = false
                                    },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = entryValues[index] == selectedValue,
                                onClick = null,
                            )
                            Text(
                                text = entry,
                                modifier = Modifier.padding(start = 16.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

/**
 * A dialog with a logarithmic slider and an editable text field that stay in sync.
 * [initialValue], [valueRange], and the [onConfirm] callback all work in display units —
 * callers are responsible for converting to/from stored units.
 *
 * The slider position is mapped logarithmically so that low values have finer control.
 */
@Composable
fun SliderPickerDialog(
    title: String,
    initialValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit,
) {
    val lnMin = ln(valueRange.start)
    val lnMax = ln(valueRange.endInclusive)

    fun toDisplay(pos: Float): Float = exp(lnMin + pos * (lnMax - lnMin))
    fun toPos(value: Float): Float = ((ln(value) - lnMin) / (lnMax - lnMin)).coerceIn(0f, 1f)

    var sliderPos by remember { mutableFloatStateOf(toPos(initialValue)) }
    var textValue by remember {
        mutableStateOf(TextFieldValue(text = Math.round(initialValue).toString()))
    }

    fun syncTextFromSlider(pos: Float) {
        val rounded = Math.round(toDisplay(pos)).toString()
        textValue = TextFieldValue(text = rounded, selection = TextRange(rounded.length))
    }

    fun syncSliderFromText(text: String) {
        text.toFloatOrNull()?.coerceIn(valueRange)?.let { sliderPos = toPos(it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { new ->
                        textValue = new
                        syncSliderFromText(new.text)
                    },
                    modifier = Modifier.onFocusChanged { focus ->
                        if (focus.isFocused) {
                            textValue = textValue.copy(
                                selection = TextRange(0, textValue.text.length)
                            )
                        }
                    },
                    suffix = { Text(unit) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Slider(
                    value = sliderPos,
                    onValueChange = { new ->
                        sliderPos = new
                        syncTextFromSlider(new)
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(toDisplay(sliderPos)) }) {
                Text(stringResource(R.string.okay))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp),
    )
}
