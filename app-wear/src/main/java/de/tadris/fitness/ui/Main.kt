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
package de.tadris.fitness.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.tooling.preview.devices.WearDevices
import de.tadris.fitness.R
import de.tadris.fitness.data.SettingsStore
import de.tadris.fitness.ui.record.RecordActivity
import de.tadris.fitness.ui.settings.MenuSettings
import de.tadris.fitness.ui.workouts.WorkoutsActivity
import de.tadris.fitness.util.CommsBT
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Main : ComponentActivity() {
	companion object {
		const val LOG_TAG: String = "FitoTrack"
		private var executorService: ExecutorService? = null
		@JvmField
		var hasBTPermission = false
		@JvmField
		var isScreenRound = false
		@JvmField
		var heightPixels: Int = 0
		var widthPixels: Int = 0
		@JvmField
		var old_vh25: Int = 0
		@JvmField
		var old_vh50: Int = 0
		@JvmField
		var old_vh75: Int = 0
		@JvmField
		var vh80px: Int = 0
		@JvmField
		var old_vw10: Int = 0
		@JvmField
		var vw80px: Int = 0
		var dp10: Int = 0
		@JvmField
		var menuItemHeight: Int = 0
		@JvmStatic
		val bTPermission: String
			get() = if(Build.VERSION.SDK_INT >= 31) Manifest.permission.BLUETOOTH_CONNECT else Manifest.permission.BLUETOOTH

		@JvmStatic
		fun runInBackgroundJAVA(runnable: Runnable?) {
			if(executorService == null) executorService = Executors.newCachedThreadPool()
			executorService?.execute(runnable)
		}

		var screenHeight = 0.dp
		var vh5 = 0.dp
		var vh25 = 0.dp
		var vh80 = 0.dp
		var screenWidth = 0.dp
		var vw5 = 0.dp
		var vw7 = 0.dp
		var vw10 = 0.dp
		var vw12 = 0.dp
		var vw25 = 0.dp
		var vw80 = 0.dp
		@Composable
		fun GetScreenSpecs(){
			isScreenRound = LocalConfiguration.current.isScreenRound
			screenHeight = LocalConfiguration.current.screenHeightDp.dp
			vh5 = screenHeight / 20
			vh25 = screenHeight / 4
			vh80 = (LocalConfiguration.current.screenHeightDp * 0.8).dp
			screenWidth = LocalConfiguration.current.screenWidthDp.dp
			vw5 = screenWidth / 20
			vw7 = screenWidth / 14
			vw10 = screenWidth / 10
			vw12 = screenWidth / 8
			vw25 = screenWidth / 4
			vw80 = (LocalConfiguration.current.screenWidthDp * 0.8).dp
		}
	}
	override fun onCreate(savedInstanceState: Bundle?) {
		installSplashScreen()
		super.onCreate(savedInstanceState)
		hasBTPermission = ActivityCompat.checkSelfPermission(this, bTPermission) == PackageManager.PERMISSION_GRANTED
		val displayMetrics = resources.displayMetrics
		heightPixels = displayMetrics.heightPixels
		widthPixels = displayMetrics.widthPixels
		old_vh25 = (heightPixels * .25).toInt()
		old_vh50 = (heightPixels * .5).toInt()
		old_vh75 = heightPixels - old_vh25
		vh80px = (heightPixels * .8).toInt()
		old_vw10 = (widthPixels * .1).toInt()
		vw80px = (widthPixels * .8).toInt()
		dp10 = resources.getDimension(R.dimen._10dp).toInt()
		menuItemHeight = resources.getDimension(R.dimen.menuItemHeight).toInt()

		setTheme(android.R.style.Theme_DeviceDefault)
		setContent { FOTTheme { MainView(
			onWorkoutsClick = { startActivity(Intent(this, WorkoutsActivity::class.java)) },
			onRecordClick = { startActivity(Intent(this, RecordActivity::class.java)) },
			onSettingsClick = { startActivity(Intent(this, MenuSettings::class.java)) }
		) } }

	}
	override fun onResume() {
		super.onResume()
		if(hasBTPermission && SettingsStore.getBTEnabled(this)) {
			runOnUiThread {
				startService(Intent(this, CommsBT::class.java))
			}
		}
	}
	override fun onDestroy() {
		stopService(Intent(this, CommsBT::class.java))
		super.onDestroy()
	}
}

@Composable
fun MainView(
	onWorkoutsClick: () -> Unit,
	onRecordClick: () -> Unit,
	onSettingsClick: () -> Unit
) {
	Main.GetScreenSpecs()
	AppScaffold {
		Column(modifier = Modifier.fillMaxSize().padding(
			start = Main.vw12,
			end = Main.vw12,
			top = 20.dp,
			bottom = Main.vh5
		)) {
			TextButton(
				modifier = Modifier.fillMaxWidth().weight(1f),
				onClick = onWorkoutsClick
			) {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Icon(
						modifier = Modifier.padding(4.dp),
						imageVector = ImageVector.vectorResource(R.drawable.ic_list),
						contentDescription = "workouts icon"
					)
					Text(stringResource(R.string.main_workouts_text))
				}
			}
			TextButton(
				modifier = Modifier.fillMaxWidth().weight(1.5f),
				onClick = onRecordClick
			) {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Icon(
						modifier = Modifier.padding(vertical = 8.dp),
						imageVector = ImageVector.vectorResource(R.drawable.ic_add_white),
						contentDescription = "record icon"
					)
					BasicText(
						text = stringResource(R.string.main_record_text),
						color = { ColorWhite },
						maxLines = 1,
						autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 24.sp)
					)
				}
			}
			TextButton(
				modifier = Modifier.fillMaxWidth().weight(1f),
				onClick = onSettingsClick
			) {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Icon(
						modifier = Modifier.padding(6.dp),
						imageVector = ImageVector.vectorResource(R.drawable.ic_edit),
						contentDescription = "settings icon"
					)
					Text(stringResource(R.string.main_settings_text))
				}
			}
		}
	}
}
@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun PreviewMainView(
){
	Main.isScreenRound = true
	FOTTheme { MainView(
		onWorkoutsClick = {},
		onRecordClick = {},
		onSettingsClick = {}
	) }
	PreviewRoundScreen()
}
