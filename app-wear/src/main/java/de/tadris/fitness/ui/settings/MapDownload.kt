/*
 * Copyright (c) 2025 Bart Vullings <dev@windkracht8.com>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU General Public License as published by
 * 	the Free Software Foundation, either version 3 of the License, or
 * 	(at your option) any later version.
 *
 * 	FitoTrack is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 *
 * 	You should have received a copy of the GNU General Public License
 * 	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.tadris.fitness.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme.colorScheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import de.tadris.fitness.R
import de.tadris.fitness.data.MapStore
import de.tadris.fitness.data.SettingsStore.storeMapSource
import de.tadris.fitness.ui.ColorFOT
import de.tadris.fitness.ui.ColorWhite
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.Main
import de.tadris.fitness.util.Util
import de.tadris.fitness.util.runInBackground
import de.tadris.fitness.util.toast
import de.tadris.fitness.util.toastFromBG

class MapDownloadActivity : ComponentActivity() {
	companion object {
		@JvmField
		var file: MapStore.File? = null
	}
	var progressLabelText = "_progress_/_size_"
	var progressLabel by mutableStateOf("")
	var progress by mutableFloatStateOf(0f)
	var isError by mutableStateOf(false)
	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val file = file
		if(file == null){
			finishAndRemoveTask()
			return
		}
		progressLabelText = progressLabelText.replace("_size_", Util.prettySize(file.size))
		progressLabel = progressLabelText.replace("_progress_", "0")
		setContent { FOTTheme { MapDownload(
			filename = file.name,
			progressLabel,
			progress,
			isError
		) } }
		runInBackground { MenuMaps.mapStore.getRemoteFile(this, file) }
	}
	public override fun onDestroy() {
		super.onDestroy()
		file = null
	}
	fun onProgress(bytesDone: Long) {
		runOnUiThread {
			progress = (bytesDone * 100f) / (file?.size ?: 100000)
			progressLabel = progressLabelText.replace(
				"_progress_",
				Util.prettySize(bytesDone)
			)
		}
	}
	fun onDone() {
		storeMapSource(this, true)
		runOnUiThread { finishAndRemoveTask() }
	}
	fun onWarning(error: Int) { toastFromBG(error) }
	fun onError(error: Int) {
		runOnUiThread {
			isError = true
			progressLabel = getString(R.string.failed)
			toast(error)
		}
	}
}

@Composable
fun MapDownload(
	filename: String,
	progressLabel: String,
	progress: Float,
	isError: Boolean
){
	Column(modifier = Modifier.fillMaxSize().padding(
		start = Main.vw12,
		end = Main.vw12,
		top = 20.dp,
		bottom = Main.vh5
	)) {
		Row(
			modifier = Modifier.fillMaxWidth().weight(1f),
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				modifier = Modifier.fillMaxWidth(),
				text = stringResource(R.string.downloading),
				textAlign = TextAlign.Center
			)
		}
		Row(
			modifier = Modifier.fillMaxWidth().weight(1f),
			verticalAlignment = Alignment.CenterVertically
		) {
			BasicText(
				text = filename,
				color = { ColorWhite },
				maxLines = 1,
				autoSize = TextAutoSize.StepBased()
			)
		}
		Row(
			modifier = Modifier.fillMaxWidth().weight(1f),
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				modifier = Modifier.fillMaxWidth(),
				text = progressLabel,
				textAlign = TextAlign.Center
			)
		}
	}
	CircularProgressIndicator(
		modifier = Modifier.fillMaxSize(),
		progress = { progress },
		strokeWidth = 1.dp,
		colors = ProgressIndicatorDefaults.colors(
				indicatorColor = if(isError) colorScheme.error else ColorFOT
		)
	)
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewMapDownload() {
	Main.GetScreenSpecs()
	AppScaffold { FOTTheme {
		MapDownload(
			filename = "some_country.map",
			progressLabel = "50/100",
			progress = 0.5f,
			isError = false
		)
	} }
}
