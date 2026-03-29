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

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.documentfile.provider.DocumentFile
import de.tadris.fitness.Instance
import de.tadris.fitness.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterfaceSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val instance = remember { Instance.getInstance(context) }
    val prefs = remember { instance.userPreferences }
    val sharedPrefs = remember { prefs.preferences }

    // Refresh unit system so conversions are up-to-date
    remember { instance.distanceUnitUtils.setUnit(); true }
    val unitSystem = remember { instance.distanceUnitUtils.distanceUnitSystem }

    // --- Language state ---
    val languageValues = stringArrayResource(R.array.pref_language_values)
    val languageEntries = stringArrayResource(R.array.pref_languages)
    val currentLocaleTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val initialLanguage = if (currentLocaleTag.isEmpty()) {
        "1"
    } else {
        languageValues.firstOrNull { currentLocaleTag.startsWith(it) } ?: "1"
    }
    var language by remember { mutableStateOf(initialLanguage) }

    // --- Unit system ---
    var unitSystemId by remember { mutableStateOf(prefs.distanceUnitSystemId) }

    // --- Weight ---
    var weightKg by remember { mutableStateOf(prefs.userWeight) }
    var showWeightDialog by remember { mutableStateOf(false) }

    // --- Step length ---
    var stepLengthMeters by remember { mutableFloatStateOf(prefs.stepLength) }
    var showStepLengthDialog by remember { mutableStateOf(false) }

    // --- Map preferences ---
    var mapStyle by remember { mutableStateOf(prefs.mapStyle) }
    var trackStyleMode by remember { mutableStateOf(prefs.trackStyleMode) }
    var trackStyle by remember { mutableStateOf(prefs.trackStyle) }
    var offlineMapDir by remember { mutableStateOf(prefs.offlineMapDirectoryName ?: "") }

    // --- Appearance ---
    var themeSetting by remember { mutableStateOf(sharedPrefs.getString("themeSetting", "0") ?: "0") }
    var dateFormat by remember { mutableStateOf(prefs.dateFormatSetting) }
    var timeFormat by remember { mutableStateOf(prefs.timeFormatSetting) }
    var firstDayOfWeek by remember { mutableStateOf(sharedPrefs.getString("firstDayOfWeek", "system") ?: "system") }
    var energyUnit by remember { mutableStateOf(prefs.energyUnit) }

    // --- File picker ---
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            sharedPrefs.edit().putString("offlineMapDirectoryName", uri.toString()).apply()
            offlineMapDir = uri.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preferencesUserInterfaceTitle)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = padding) {

            // ===== General section =====

            // 1. Language
            item {
                ListPreferenceItem(
                    title = stringResource(R.string.language),
                    entries = languageEntries,
                    entryValues = languageValues,
                    selectedValue = language,
                    onValueChange = { value ->
                        language = value
                        sharedPrefs.edit().putString("language", value).apply()
                        if (value == "1") {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.create())
                        } else {
                            AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags(value)
                            )
                        }
                    },
                )
            }

            // 2. Unit System
            item {
                ListPreferenceItem(
                    title = stringResource(R.string.pref_unit_system),
                    entries = stringArrayResource(R.array.pref_unit_systems),
                    entryValues = stringArrayResource(R.array.pref_unit_system_values),
                    selectedValue = unitSystemId,
                    onValueChange = { value ->
                        unitSystemId = value
                        prefs.distanceUnitSystemId = value
                    },
                )
            }

            // 3. Weight
            item {
                val displayWeight = Math.round(unitSystem.getWeightFromKilogram(weightKg.toDouble()))
                val weightUnit = unitSystem.weightUnit
                ClickableSettingsItem(
                    title = stringResource(R.string.pref_weight),
                    supporting = "$displayWeight $weightUnit",
                    onClick = { showWeightDialog = true },
                )
            }

            // 4. Step Length
            item {
                val displayStepLength = Math.round(unitSystem.getDistanceFromCentimeters((stepLengthMeters * 100).toDouble()))
                val stepUnit = unitSystem.reallyShortDistanceUnit
                ClickableSettingsItem(
                    title = stringResource(R.string.pref_step_length),
                    supporting = "$displayStepLength $stepUnit",
                    onClick = { showStepLengthDialog = true },
                )
            }

            // ===== Map category =====
            item {
                SettingsCategory(title = stringResource(R.string.preferencesCategoryMap))
            }

            // 5. Map Style
            item {
                ListPreferenceItem(
                    title = stringResource(R.string.mapStyle),
                    entries = stringArrayResource(R.array.pref_map_layers),
                    entryValues = stringArrayResource(R.array.pref_map_layers_values),
                    selectedValue = mapStyle,
                    onValueChange = { value ->
                        mapStyle = value
                        prefs.mapStyle = value
                    },
                )
            }

            // 6. Track Style Usage
            item {
                ListPreferenceItem(
                    title = stringResource(R.string.prefTrackStyleUsageTitle),
                    entries = stringArrayResource(R.array.pref_track_style_usage),
                    entryValues = stringArrayResource(R.array.pref_track_style_usage_values),
                    selectedValue = trackStyleMode,
                    onValueChange = { value ->
                        trackStyleMode = value
                        prefs.trackStyleMode = value
                    },
                )
            }

            // 7. Track Style
            item {
                ListPreferenceItem(
                    title = stringResource(R.string.trackStyle),
                    entries = stringArrayResource(R.array.pref_track_style),
                    entryValues = stringArrayResource(R.array.pref_track_style_values),
                    selectedValue = trackStyle,
                    onValueChange = { value ->
                        trackStyle = value
                        prefs.trackStyle = value
                    },
                )
            }

            // 8. Offline Map Directory
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.pref_map_directory),
                    supporting = offlineMapDir.ifEmpty { null },
                    onClick = { folderPickerLauncher.launch(null) },
                )
            }

            // 9. Render Style (legacy fragment)
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.pref_render_theme_style),
                    onClick = {
                        context.startActivity(
                            Intent(context, FitoTrackSettingsActivity::class.java)
                                .putExtra(
                                    FitoTrackSettingsActivity.EXTRA_FRAGMENT,
                                    XmlThemeStyleSettingFragment::class.java.name
                                )
                        )
                    },
                )
            }

            // 10. Download Maps
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.downloadMapsTitle),
                    onClick = {
                        val mapDir = prefs.offlineMapDirectoryName
                        if (mapDir != null) {
                            val docFile = DocumentFile.fromTreeUri(context, Uri.parse(mapDir))
                            if (docFile != null && docFile.canWrite()) {
                                context.startActivity(
                                    Intent(context, DownloadMapsActivity::class.java)
                                )
                                return@ClickableSettingsItem
                            }
                        }
                        Toast.makeText(
                            context,
                            R.string.downloadMapsSpecifyDirectory,
                            Toast.LENGTH_LONG
                        ).show()
                    },
                )
            }

            // ===== Appearance category =====
            item {
                SettingsCategory(title = stringResource(R.string.preferencesCategoryAppearance))
            }

            // 11. Theme
            item {
                ListPreferenceItem(
                    title = stringResource(R.string.theme),
                    entries = stringArrayResource(R.array.pref_theme_setting),
                    entryValues = stringArrayResource(R.array.pref_theme_setting_values),
                    selectedValue = themeSetting,
                    onValueChange = { value ->
                        themeSetting = value
                        sharedPrefs.edit().putString("themeSetting", value).apply()
                        Toast.makeText(context, R.string.hintRestart, Toast.LENGTH_LONG).show()
                    },
                )
            }

            // 12. Date Format
            item {
                ListPreferenceItem(
                    title = stringResource(R.string.preferenceDateFormat),
                    entries = stringArrayResource(R.array.pref_date_format),
                    entryValues = stringArrayResource(R.array.pref_date_format_values),
                    selectedValue = dateFormat,
                    onValueChange = { value ->
                        dateFormat = value
                        prefs.dateFormatSetting = value
                    },
                )
            }

            // 13. Time Format
            item {
                ListPreferenceItem(
                    title = stringResource(R.string.preferenceTimeFormat),
                    entries = stringArrayResource(R.array.pref_time_format),
                    entryValues = stringArrayResource(R.array.pref_time_format_values),
                    selectedValue = timeFormat,
                    onValueChange = { value ->
                        timeFormat = value
                        prefs.timeFormatSetting = value
                    },
                )
            }

            // 14. Week Start
            item {
                ListPreferenceItem(
                    title = stringResource(R.string.preferenceFirstDayOfWeek),
                    entries = stringArrayResource(R.array.pref_first_day_of_week),
                    entryValues = stringArrayResource(R.array.pref_first_day_of_week_values),
                    selectedValue = firstDayOfWeek,
                    onValueChange = { value ->
                        firstDayOfWeek = value
                        sharedPrefs.edit().putString("firstDayOfWeek", value).apply()
                    },
                )
            }

            // 15. Energy Unit
            item {
                ListPreferenceItem(
                    title = stringResource(R.string.pref_energy_unit),
                    entries = stringArrayResource(R.array.pref_energy_units),
                    entryValues = stringArrayResource(R.array.pref_energy_unit_values),
                    selectedValue = energyUnit,
                    onValueChange = { value ->
                        energyUnit = value
                        prefs.energyUnit = value
                    },
                )
            }
        }
    }

    if (showWeightDialog) {
        WeightPickerDialog(
            currentWeightKg = weightKg,
            unitSystem = unitSystem,
            onDismiss = { showWeightDialog = false },
            onConfirm = { kilograms ->
                sharedPrefs.edit().putInt("weight", kilograms).apply()
                weightKg = kilograms
                showWeightDialog = false
            },
        )
    }

    if (showStepLengthDialog) {
        StepLengthPickerDialog(
            currentStepLengthMeters = stepLengthMeters,
            unitSystem = unitSystem,
            onDismiss = { showStepLengthDialog = false },
            onConfirm = { meters ->
                prefs.stepLength = meters
                stepLengthMeters = meters
                showStepLengthDialog = false
            },
        )
    }
}

@Composable
private fun WeightPickerDialog(
    currentWeightKg: Int,
    unitSystem: de.tadris.fitness.util.unit.DistanceUnitSystem,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val minWeight = unitSystem.getWeightFromKilogram(20.0).toFloat()
    val maxWeight = unitSystem.getWeightFromKilogram(400.0).toFloat()
    val initial = unitSystem.getWeightFromKilogram(currentWeightKg.toDouble()).toFloat()
    var sliderValue by remember { mutableFloatStateOf(initial) }
    val unit = unitSystem.weightUnit

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pref_weight)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${Math.round(sliderValue)} $unit",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = minWeight..maxWeight,
                    steps = (maxWeight - minWeight).toInt() - 1,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(Math.round(unitSystem.getKilogramFromUnit(sliderValue.toDouble())).toInt())
            }) {
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
private fun StepLengthPickerDialog(
    currentStepLengthMeters: Float,
    unitSystem: de.tadris.fitness.util.unit.DistanceUnitSystem,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit,
) {
    val minStep = unitSystem.getDistanceFromCentimeters(50.0).toFloat()
    val maxStep = unitSystem.getDistanceFromCentimeters(150.0).toFloat()
    val initial = unitSystem.getDistanceFromCentimeters((currentStepLengthMeters * 100).toDouble()).toFloat()
    var sliderValue by remember { mutableFloatStateOf(initial) }
    val unit = unitSystem.reallyShortDistanceUnit

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pref_step_length)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${Math.round(sliderValue)} $unit",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = minStep..maxStep,
                    steps = (maxStep - minStep).toInt() - 1,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val meters = (unitSystem.getCentimetersFromReallyShortDistance(sliderValue.toDouble()) / 100).toFloat()
                onConfirm(meters)
            }) {
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
