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

import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import de.tadris.fitness.R
import de.tadris.fitness.data.MapStore
import de.tadris.fitness.data.SettingsStore
import de.tadris.fitness.data.SettingsStore.storeMapSource
import de.tadris.fitness.ui.FOTTheme
import de.tadris.fitness.ui.Main
import de.tadris.fitness.ui.settings.SettingsActivity.DownloadState
import de.tadris.fitness.util.CommsBT
import de.tadris.fitness.util.runInBackground
import de.tadris.fitness.util.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
	var btEnabled = mutableStateOf<Boolean?>(null)
	val mapStore by mutableStateOf(MapStore())
	var downloadBytesDone = mutableLongStateOf(0)
	enum class DownloadState { Idle, Downloading, Failed, Done }
	var downloadState = mutableStateOf<DownloadState>(DownloadState.Idle)

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		btEnabled.value =
			if(!Main.hasBTPermission) null
			else if(SettingsStore.getBTEnabled(this)) true
			else false

		setContent { FOTTheme { Settings(
			btEnabled,
			onBTClick = ::onBTClick,
			mapStore,
			onMapClick = ::onMapClick,
			downloadBytesDone,
			downloadState
		) } }
	}

	public override fun onDestroy() {
		super.onDestroy()
		mapStore.close()
	}
	fun onBTClick(){
		if(!Main.hasBTPermission){
			if(Build.VERSION.SDK_INT >= 31) { requestPermissionBT.launch(BLUETOOTH_CONNECT) }
			else { requestPermissionBT.launch(BLUETOOTH) }
		}else if(SettingsStore.getBTEnabled(this)){
			btEnabled.value = false
			SettingsStore.storeBTEnabled(this, false)
			stopService(Intent(this, CommsBT::class.java))
		}else{
			btEnabled.value = true
			SettingsStore.storeBTEnabled(this, true)
			startService(Intent(this, CommsBT::class.java))
		}
	}
	val requestPermissionBT = registerForActivityResult(contract =
		ActivityResultContracts.RequestPermission()){ if(it) btEnabled.value = true }

	fun onMapClick(downloadFile: MapStore.File){
		CoroutineScope(Dispatchers.Main).launch {
			window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
			downloadState.value = DownloadState.Downloading
			mapStore.getRemoteFile(
				context = this@SettingsActivity,
				fileRemote = downloadFile,
				onError = ::onDownloadError,
				onProgress = ::onDownloadProgress,
				onDone = ::onDownloadDone
			)
		}
	}
	fun onDownloadProgress(bytesDone: Long) {
		CoroutineScope(Dispatchers.Main).launch { downloadBytesDone.longValue = bytesDone }
	}
	fun onDownloadDone() {
		CoroutineScope(Dispatchers.Main).launch {
			downloadState.value = DownloadState.Done
			window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		}
		storeMapSource(this, true)
	}
	fun onDownloadError(error: Int) {
		downloadState.value = DownloadState.Failed
		CoroutineScope(Dispatchers.Main).launch {
			toast(error)
			window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		}
	}
}

@Composable
fun Settings(
	btEnabled: MutableState<Boolean?>,
	onBTClick: () -> Unit,
	mapStore: MapStore,
	onMapClick: (MapStore.File) -> Unit,
	downloadBytesDone: MutableLongState,
	downloadState: MutableState<DownloadState>
){
	val context = LocalContext.current
	var downloadFile by remember { mutableStateOf<MapStore.File?>(null) }
	val navController = rememberSwipeDismissableNavController()
	var checkingForUpdates by remember { mutableStateOf<Boolean?>(null) }
	fun downloadMap(file: MapStore.File){
		downloadFile = file
		onMapClick(file)
	}
	fun checkForUpdates(){
		runInBackground {
			checkingForUpdates = true
			for(map in MapStore.getLocalMaps(context)) {
				if(mapStore.checkHasUpdate(map)) {
					downloadMap(map)
					return@runInBackground
				}
			}
			checkingForUpdates = false
		}
	}
	LaunchedEffect(downloadState.value) {
		if(downloadState.value == DownloadState.Downloading) {
			navController.popBackStack(route = "SettingsMaps", inclusive = false)
			navController.navigate("SettingsMapDownload")
		}else if(downloadState.value == DownloadState.Done){
			navController.popBackStack(route = "SettingsMaps", inclusive = false)
			if(checkingForUpdates == true) checkForUpdates()
		}
	}
	LaunchedEffect(checkingForUpdates) {
		if(checkingForUpdates == true) context.toast(R.string.updates_check)
		else if(checkingForUpdates == false) context.toast(R.string.updates_done)
	}
	AppScaffold {
		SwipeDismissableNavHost(
			navController = navController,
			startDestination = "SettingsMain"
		) {
			composable("SettingsMain") {
				SettingsMain(
					btEnabled.value,
					onBTClick,
					onMapsClick = { navController.navigate("SettingsMaps") }
				)
			}
			composable("SettingsMaps") {
				SettingsMaps(
					onDownloadClick = { navController.navigate("SettingsMapsDownload") },
					onUpdatesClick = { checkForUpdates() }
				)
			}
			fun String.encode() = replace("/", "_")
			fun String.decode() = replace("_", "/")
			composable(
				route = "SettingsMapsDownload?path={path}",
				arguments = listOf(navArgument("path") {
					type = NavType.StringType
					nullable = true
				})
			) {
				val path = it.arguments?.getString("path")?.decode() ?: ""
				SettingsMapsDownload(
					mapStore,
					path,
					onDirClick = { pathDir ->
						navController.navigate("SettingsMapsDownload?path=" + pathDir.encode())
					},
					onMapClick = { file -> downloadMap(file) }
				)
			}
			composable("SettingsMapDownload") {
				BackHandler {
					mapStore.cancelDownload = true
					navController.popBackStack()
				}
				downloadFile?.let {
					SettingsMapDownload(
						file = it,
						bytesDone = downloadBytesDone,
						downloadState = downloadState.value
					)
				}
			}
		}
	}
}
