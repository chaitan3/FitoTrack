package de.tadris.fitness.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import de.tadris.fitness.R
import de.tadris.fitness.data.MapStore
import de.tadris.fitness.ui.ColorFOT
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.Main
import de.tadris.fitness.util.prettySize
import de.tadris.fitness.util.runInBackground

@Composable
fun SettingsMapsDownload(
	mapStore: MapStore,
	path: String,
	onDirClick: (String) -> Unit,
	onMapClick: (MapStore.File) -> Unit
) {
	val columnState = rememberTransformingLazyColumnState()
	val transformationSpec = rememberTransformationSpec()
	var dirs by remember { mutableStateOf<List<MapStore.Dir>?>(null)}
	var files by remember { mutableStateOf<List<MapStore.File>?>(null)}
	runInBackground {
		dirs = mapStore.getRemoteDirs(path)
		files = mapStore.getRemoteFiles(path)
	}
	ScreenScaffold(
		modifier = Modifier,
		scrollState = columnState,
		contentPadding = rememberResponsiveColumnPadding()
	) { contentPadding ->
		TransformingLazyColumn(state = columnState, contentPadding = contentPadding) {
			item {
				Text(
					text = stringResource(R.string.maps),
					color = ColorFOT
				)
			}
			if(dirs == null) {
				item { Text(text = stringResource(R.string.loading)) }
			} else {
				for(dir in dirs) {
					item {
						OutlinedButton(
							modifier = Modifier.fillMaxWidth(),
							transformation = SurfaceTransformation(transformationSpec),
							onClick = { onDirClick(dir.path) }
						) { Text(text = dir.name) }
					}
				}
			}
			if(files != null) {
				for(file in files) {
					item {
						OutlinedButton(
							modifier = Modifier.fillMaxWidth(),
							transformation = SurfaceTransformation(transformationSpec),
							onClick = { onMapClick(file) }
						) {
							Column {
								Text(text = file.name)
								Text(
									text = file.size.prettySize(),
									color = ColorFOT
								)
							}
						}
					}
				}
			}
		}
	}
}
@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewSettingsMapsDownload(){
	Main.GetScreenSpecs()
	FOTTheme { SettingsMapsDownload(
		MapStore(),
		path = "",
		onDirClick = {},
		onMapClick = {}
	) }
}
