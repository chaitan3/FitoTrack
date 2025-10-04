/*
 * Copyright (c) 2025 Jannis Scheibe <jannis@tadris.de>
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
package de.tadris.fitness.ui.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import de.tadris.fitness.R
import de.tadris.fitness.ui.FitoTrackActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WearSyncActivity : FitoTrackActivity() {
	companion object { const val LOG_TAG: String = "FitoTrack.WearSync" }
	private var fragmentManager: FragmentManager? = null
	private var hasBTPermission = false
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_wear_sync)
		setupActionBar()
		setTitle(R.string.wear_sync)
		fragmentManager = supportFragmentManager
		fragmentManager?.beginTransaction()
			?.replace(R.id.wearSync, WearSyncDeviceList())
			?.commit()

		lifecycleScope.launch {
			CommsBT.status.collect { status ->
				if(CommsBT.status.value == CommsBT.Status.CONNECTED) {
					CoroutineScope(Dispatchers.Main).launch {
						fragmentManager?.beginTransaction()
							?.replace(R.id.wearSync, WearSyncWorkoutsList())
							?.commit()
					}
				}
			}
		}
	}

	override fun onResume() {
		super.onResume()
		if(Build.VERSION.SDK_INT >= 31) {
			hasBTPermission = hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
			if(!hasBTPermission)
				requestPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
		} else {
			hasBTPermission = hasPermission(Manifest.permission.BLUETOOTH)
			if(!hasBTPermission)
				requestPermission.launch(Manifest.permission.BLUETOOTH)
		}
		if(hasBTPermission) {
			CoroutineScope(Dispatchers.Default).launch {
				CommsBT.start(this@WearSyncActivity)
			}
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		CoroutineScope(Dispatchers.Default).launch { CommsBT.stop() }
	}

	fun hasPermission(permission: String): Boolean =
		ActivityCompat.checkSelfPermission(this, permission) ==
				PackageManager.PERMISSION_GRANTED

	val requestPermission = registerForActivityResult(
		ActivityResultContracts.RequestPermission()){
		hasBTPermission = it
		if(hasBTPermission) {
			CoroutineScope(Dispatchers.Default).launch {
				CommsBT.start(this@WearSyncActivity)
			}
		} else {
			Toast.makeText(this, R.string.wear_error_denied, Toast.LENGTH_SHORT).show()
		}
	}
}
