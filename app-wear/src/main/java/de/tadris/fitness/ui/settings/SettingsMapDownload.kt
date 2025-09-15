package de.tadris.fitness.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme.colorScheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import de.tadris.fitness.R
import de.tadris.fitness.data.MapStore
import de.tadris.fitness.ui.ColorFOT
import de.tadris.fitness.ui.ColorWhite
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.settings.SettingsActivity.DownloadState.Failed
import de.tadris.fitness.ui.Main
import de.tadris.fitness.util.prettySize

@Composable
fun SettingsMapDownload(
	file: MapStore.File,
	bytesDone: MutableLongState,
	downloadState: SettingsActivity.DownloadState
){
	Column(Modifier.fillMaxSize().padding(
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
				text = file.name.removeSuffix(".map"),
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
				text =
					if(downloadState == Failed) stringResource(R.string.failed)
					else bytesDone.longValue.prettySize() + "/" +  file.size.prettySize(),
				textAlign = TextAlign.Center
			)
		}
	}
	CircularProgressIndicator(
		modifier = Modifier.fillMaxSize(),
		progress = { bytesDone.longValue.toFloat() / file.size },
		strokeWidth = 3.dp,
		colors = ProgressIndicatorDefaults.colors(
			indicatorColor = if(downloadState == Failed) colorScheme.error else ColorFOT
		)
	)
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewSettingsMapDownload() {
	Main.GetScreenSpecs()
	AppScaffold { FOTTheme {
		SettingsMapDownload(
			file = MapStore.File("europe/austria.map", 1000000, 0),
			bytesDone = remember { mutableLongStateOf(500000) },
			downloadState = Failed
		)
	} }
}
