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

package de.tadris.fitness.ui.settings.debug

import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.util.osm.OAuthAuthentication
import de.tadris.fitness.ui.ShareFileActivity
import de.tadris.fitness.ui.settings.ClickableSettingsItem
import de.tadris.fitness.ui.settings.SettingsCategory
import de.tadris.fitness.util.DataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File

private data class DbStats(
    val workouts: Long,
    val routeSamples: Long,
    val repetitionSamples: Long,
    val heartRateSamples: Long,
    val workoutEvents: Long,
    val workoutPauses: Long,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugSettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val handler = remember { Handler(Looper.getMainLooper()) }
    val instance = remember { Instance.getInstance(context) }

    var dbStats by remember { mutableStateOf<DbStats?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val dao = instance.db.workoutDao()
            dbStats = DbStats(
                workouts = dao.countWorkouts(),
                routeSamples = dao.countRouteSamples(),
                repetitionSamples = dao.countRepetitionSamples(),
                heartRateSamples = dao.countHeartRateSamples(),
                workoutEvents = dao.countWorkoutEvents(),
                workoutPauses = dao.countWorkoutPauses(),
            )
        }
    }

    fun copyAndShareLogs() {
        Thread {
            val destination = File(
                DataManager.getSharedDirectory(context) + "/fitotrack-logs-${System.currentTimeMillis()}.txt"
            )
            FileUtils.copyFile(instance.logger.file, destination)
            handler.post {
                val uri = DataManager.provide(context, destination)
                context.startActivity(
                    Intent(context, ShareFileActivity::class.java)
                        .putExtra(ShareFileActivity.EXTRA_FILE_URI, uri.toString())
                        .putExtra(ShareFileActivity.EXTRA_FILE_MIME, "text/plain")
                )
            }
        }.start()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preferencesDebugTitle)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item { SettingsCategory("Database") }
            item {
                val stats = dbStats
                ClickableSettingsItem(
                    title = "Row counts",
                    supporting = if (stats == null) "Loading…" else
                        "Workouts: ${stats.workouts} · Route samples: ${stats.routeSamples} · " +
                        "Repetition samples: ${stats.repetitionSamples} · HR samples: ${stats.heartRateSamples} · " +
                        "Events: ${stats.workoutEvents} · Pauses: ${stats.workoutPauses}",
                    onClick = {},
                )
            }
            item { SettingsCategory("Actions") }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.debugShareLogsTitle),
                    supporting = stringResource(R.string.debugShareLogsSummary),
                    onClick = { copyAndShareLogs() },
                )
            }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.debugSensorsTitle),
                    supporting = stringResource(R.string.debugSensorsSummary),
                    onClick = {
                        context.startActivity(Intent(context, SensorDebugActivity::class.java))
                    },
                )
            }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.debugPressureSensor),
                    onClick = {
                        context.startActivity(Intent(context, DebugPressureActivity::class.java))
                    },
                )
            }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.removeOAuthToken),
                    onClick = { OAuthAuthentication.clearAccessToken(context) },
                )
            }
        }
    }
}
