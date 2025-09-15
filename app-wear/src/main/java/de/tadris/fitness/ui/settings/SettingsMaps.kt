package de.tadris.fitness.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import de.tadris.fitness.data.SettingsStore.getMapSource
import de.tadris.fitness.data.SettingsStore.storeMapSource
import de.tadris.fitness.ui.ColorFOT
import de.tadris.fitness.ui.ConfirmDialog
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.Main
import de.tadris.fitness.util.prettySize

@Composable
fun SettingsMaps(
	onDownloadClick: () -> Unit,
	onUpdatesClick: () -> Unit
) {
	val context = LocalContext.current
	val columnState = rememberTransformingLazyColumnState()
	val transformationSpec = rememberTransformationSpec()
	var delMapFile by remember { mutableStateOf<MapStore.File?>(null) }
	val localMaps = remember { MapStore.getLocalMaps(context).toMutableStateList() }

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
			item {
				var mapSource by remember { mutableStateOf(getMapSource(context)) }
				OutlinedButton(
					modifier = Modifier.fillMaxWidth(),
					transformation = SurfaceTransformation(transformationSpec),
					onClick = {
						mapSource = !mapSource
						storeMapSource(context, mapSource)
					},
					enabled = localMaps.isNotEmpty()
				) {
					Column {
						Text(text = stringResource(R.string.map_source))
						Text(
							text = stringResource(id = if(mapSource) R.string.local else R.string.online),
							color = ColorFOT
						)
					}
				}
			}
			item {
				OutlinedButton(
					modifier = Modifier.fillMaxWidth(),
					transformation = SurfaceTransformation(transformationSpec),
					onClick = onDownloadClick
				) { Text(text = stringResource(R.string.download)) }
			}
			if(localMaps.isNotEmpty()) {
				item {
					OutlinedButton(
						modifier = Modifier.fillMaxWidth(),
						transformation = SurfaceTransformation(transformationSpec),
						onClick = onUpdatesClick
					) { Text(text = stringResource(R.string.updates)) }
				}
				localMaps.forEach {
					item {
						OutlinedButton(
							modifier = Modifier.fillMaxWidth(),
							transformation = SurfaceTransformation(transformationSpec),
							onClick = { delMapFile = it }
						) {
							Column {
								Text(text = it.name)
								Text(
									text = it.size.prettySize(),
									color = ColorFOT
								)
							}
						}
					}
				}
			}
		}
	}
	if(delMapFile != null){
		ConfirmDialog(
			textString = stringResource(R.string.delete_map, delMapFile?.name ?: "error"),
			confirmText = R.string.delete,
			dismissText = R.string.cancel,
			onConfirm = {
				delMapFile?.let { MapStore.deleteLocalMap(context, it) }
				localMaps.remove(delMapFile)
				delMapFile = null
				if(localMaps.isEmpty()) storeMapSource(context, false)
			},
			onDismiss = { delMapFile = null }
		)
	}
}
@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun PreviewSettingsMaps(){
	Main.GetScreenSpecs()
	FOTTheme { SettingsMaps(
		onDownloadClick = {},
		onUpdatesClick = {}
	) }
}
