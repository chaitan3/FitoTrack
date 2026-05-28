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

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.preferences.UserPreferences
import de.tadris.fitness.util.unit.DistanceUnitSystem
import java.text.NumberFormat
import kotlin.math.roundToInt

private data class VoiceAnnouncementPreference(
    val key: String,
    @StringRes val titleRes: Int,
    val defaultValue: Boolean = false,
    val enabledWhen: String? = null,
)

private data class AnnouncementSection(
    @StringRes val titleRes: Int,
    val preferences: List<VoiceAnnouncementPreference>,
)

private val sections = listOf(
    AnnouncementSection(
        titleRes = R.string.pref_announcements_content,
        preferences = listOf(
            VoiceAnnouncementPreference("announcement_countdown", R.string.announcementCountdown, defaultValue = true),
            VoiceAnnouncementPreference("announcement_system_actions", R.string.announcementWorkoutActions, defaultValue = true),
            VoiceAnnouncementPreference("announcement_currentTime", R.string.currentTime),
            VoiceAnnouncementPreference("announcement_duration", R.string.workoutDuration, defaultValue = true),
            VoiceAnnouncementPreference("announcement_duration_with_seconds", R.string.workoutDurationPrecisionToSeconds, enabledWhen = "announcement_duration"),
            VoiceAnnouncementPreference("announcement_current_heart_rate", R.string.workoutHeartRate),
        ),
    ),
    AnnouncementSection(
        titleRes = R.string.workoutRecordingTypeGps,
        preferences = listOf(
            VoiceAnnouncementPreference("announcement_gps-lost", R.string.announcementGPSStatus, defaultValue = true),
            VoiceAnnouncementPreference("announcement_distance", R.string.workoutDistance, defaultValue = true),
            VoiceAnnouncementPreference("announcement_current_speed", R.string.currentSpeed, defaultValue = true),
            VoiceAnnouncementPreference("announcement_avgSpeedMotion", R.string.avgSpeedInMotion),
            VoiceAnnouncementPreference("announcement_avgSpeedTotal", R.string.avgSpeedTotalShort),
            VoiceAnnouncementPreference("announcement_avg_pace", R.string.workoutPace),
            VoiceAnnouncementPreference("announcement_ascent", R.string.workoutAscent),
        ),
    ),
    AnnouncementSection(
        titleRes = R.string.workoutRecordingTypeIndoor,
        preferences = listOf(
            VoiceAnnouncementPreference("announcement_repetitions", R.string.workoutRepetitions, defaultValue = true),
            VoiceAnnouncementPreference("announcement_avg_frequency", R.string.workoutAvgFrequencyShort),
            VoiceAnnouncementPreference("announcement_current_frequency", R.string.workoutCurrentFrequency),
            VoiceAnnouncementPreference("announcement_current_intensity", R.string.workoutIntensity),
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAnnouncementsSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val instance = remember { Instance.getInstance(context) }
    val prefs = remember { instance.userPreferences }
    val sharedPrefs = remember { prefs.preferences }

    remember { instance.distanceUnitUtils.setUnit(); true }
    val unitSystem = remember { instance.distanceUnitUtils.distanceUnitSystem }

    // All announcement checkbox states keyed by preference key
    val state = remember {
        mutableStateMapOf<String, Boolean>().also { map ->
            sections.flatMap { it.preferences }.forEach { pref ->
                map[pref.key] = sharedPrefs.getBoolean(pref.key, pref.defaultValue)
            }
        }
    }

    var announcementMode by remember {
        mutableStateOf(sharedPrefs.getString("announcementMode", "headphones") ?: "headphones")
    }
    var showSpeechDialog by remember { mutableStateOf(false) }
    var showPaceDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.voiceAnnouncementsTitle)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.pref_announcements_config_title),
                    supporting = stringResource(R.string.pref_announcements_config_summary),
                    onClick = { showSpeechDialog = true },
                )
            }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.pref_announcements_pace_control_title),
                    supporting = stringResource(R.string.pref_announcements_pace_control_summary),
                    onClick = { showPaceDialog = true },
                )
            }
            item {
                ListPreferenceItem(
                    title = stringResource(R.string.pref_announcement_mode),
                    entries = stringArrayResource(R.array.pref_announcement_mode),
                    entryValues = stringArrayResource(R.array.pref_announcement_mode_values),
                    selectedValue = announcementMode,
                    onValueChange = { value ->
                        announcementMode = value
                        sharedPrefs.edit().putString("announcementMode", value).apply()
                    },
                )
            }

            sections.forEach { section ->
                item {
                    SettingsCategory(stringResource(section.titleRes))
                }
                items(section.preferences) { pref ->
                    val enabled = pref.enabledWhen?.let { state[it] == true } ?: true
                    SwitchSettingsItem(
                        title = stringResource(pref.titleRes),
                        checked = state[pref.key] == true,
                        enabled = enabled,
                        onCheckedChange = { checked ->
                            state[pref.key] = checked
                            sharedPrefs.edit().putBoolean(pref.key, checked).apply()
                        },
                    )
                }
            }
        }
    }

    if (showSpeechDialog) {
        SpeechConfigDialog(
            prefs = prefs,
            distanceUnit = unitSystem.longDistanceUnit,
            onDismiss = { showSpeechDialog = false },
        )
    }

    if (showPaceDialog) {
        PaceControlDialog(
            prefs = prefs,
            unitSystem = unitSystem,
            onDismiss = { showPaceDialog = false },
        )
    }
}

@Composable
private fun SpeechConfigDialog(
    prefs: UserPreferences,
    distanceUnit: String,
    onDismiss: () -> Unit,
) {
    val distanceIntervals = remember { floatArrayOf(0f, 0.5f, 1.0f, 1.5f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f) }
    val noAnnouncement = stringResource(R.string.speechConfigNoSpeech)
    val minuteShort = stringResource(R.string.timeMinuteShort)

    val initialDistanceIndex = distanceIntervals.indexOfFirst { it == prefs.spokenUpdateDistancePeriod }.coerceAtLeast(0)
    var timeIndex by remember { mutableStateOf(prefs.spokenUpdateTimePeriod.toFloat()) }
    var distanceIndex by remember { mutableStateOf(initialDistanceIndex.toFloat()) }

    fun formatTime(index: Float) =
        index.roundToInt().let { if (it == 0) noAnnouncement else "$it $minuteShort" }

    fun formatDistance(index: Float) =
        distanceIntervals[index.roundToInt()].let { d ->
            if (d == 0f) noAnnouncement else "%.1f $distanceUnit".format(d)
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pref_announcements_config_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.timeMinuteShort).let { formatTime(timeIndex) },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Slider(
                    value = timeIndex,
                    onValueChange = { timeIndex = it },
                    valueRange = 0f..60f,
                    steps = 59,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = formatDistance(distanceIndex),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Slider(
                    value = distanceIndex,
                    onValueChange = { distanceIndex = it },
                    valueRange = 0f..(distanceIntervals.size - 1).toFloat(),
                    steps = distanceIntervals.size - 2,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                prefs.preferences.edit()
                    .putInt(UserPreferences.VOICE_ANNOUNCEMENTS_INTERVAL_TIME, timeIndex.roundToInt())
                    .putFloat(UserPreferences.VOICE_ANNOUNCEMENTS_INTERVAL_DISTANCE, distanceIntervals[distanceIndex.roundToInt()])
                    .apply()
                onDismiss()
            }) {
                Text(stringResource(R.string.okay))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun PaceControlDialog(
    prefs: UserPreferences,
    unitSystem: DistanceUnitSystem,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val speedUnit = unitSystem.speedUnit

    var enableLower by remember { mutableStateOf(prefs.hasLowerTargetSpeedLimit()) }
    var enableUpper by remember { mutableStateOf(prefs.hasUpperTargetSpeedLimit()) }
    var lowerText by remember {
        mutableStateOf(
            if (prefs.hasLowerTargetSpeedLimit())
                "%.2f".format(unitSystem.getSpeedFromMeterPerSecond(prefs.lowerTargetSpeedLimit.toDouble()))
            else ""
        )
    }
    var upperText by remember {
        mutableStateOf(
            if (prefs.hasUpperTargetSpeedLimit())
                "%.2f".format(unitSystem.getSpeedFromMeterPerSecond(prefs.upperTargetSpeedLimit.toDouble()))
            else ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pref_announcements_pace_control_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.lowerPaceLimit), Modifier.weight(1f))
                    Switch(
                        checked = enableLower,
                        onCheckedChange = { enableLower = it; if (!it) lowerText = "" },
                    )
                }
                OutlinedTextField(
                    value = lowerText,
                    onValueChange = { lowerText = it; if (it.isNotEmpty()) enableLower = true },
                    enabled = enableLower,
                    suffix = { Text(speedUnit) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.upperPaceLimit), Modifier.weight(1f))
                    Switch(
                        checked = enableUpper,
                        onCheckedChange = { enableUpper = it; if (!it) upperText = "" },
                    )
                }
                OutlinedTextField(
                    value = upperText,
                    onValueChange = { upperText = it; if (it.isNotEmpty()) enableUpper = true },
                    enabled = enableUpper,
                    suffix = { Text(speedUnit) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    val nf = NumberFormat.getInstance()
                    var lower = if (enableLower) unitSystem.getMeterPerSecondFromSpeed(
                        nf.parse(lowerText)!!.toDouble()
                    ).toFloat() else 0f
                    var upper = if (enableUpper) unitSystem.getMeterPerSecondFromSpeed(
                        nf.parse(upperText)!!.toDouble()
                    ).toFloat() else Float.POSITIVE_INFINITY
                    // Ensure lower <= upper
                    val tmp = upper; upper = maxOf(lower, tmp); lower = minOf(lower, tmp)
                    prefs.setHasLowerTargetSpeedLimit(enableLower)
                    prefs.setHasUpperTargetSpeedLimit(enableUpper)
                    if (enableLower) prefs.lowerTargetSpeedLimit = lower
                    if (enableUpper) prefs.upperTargetSpeedLimit = upper
                    onDismiss()
                } catch (e: Exception) {
                    Toast.makeText(context, R.string.invalidNumberFormat, Toast.LENGTH_LONG).show()
                }
            }) {
                Text(stringResource(R.string.okay))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
