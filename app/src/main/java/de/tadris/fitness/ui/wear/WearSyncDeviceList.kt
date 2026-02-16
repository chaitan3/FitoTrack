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

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import de.tadris.fitness.R
import de.tadris.fitness.ui.wear.WearSyncActivity.Companion.LOG_TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission") //Permissions are handled in WearSyncActivity.onCreate
class WearSyncDeviceList : Fragment() {
	lateinit var title: TextView
	lateinit var known: LinearLayout
	lateinit var list: LinearLayout
	lateinit var icon: ImageView
	lateinit var iconDrawable: AnimatedVectorDrawable

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val rootView = inflater.inflate(R.layout.fragment_wear_sync, container, false)
		title = rootView.findViewById(R.id.wearSyncTitle)
		known = rootView.findViewById(R.id.wearSyncKnown)
		list = rootView.findViewById(R.id.wearSyncList)
		icon = rootView.findViewById(R.id.wearSyncIcon)
		iconDrawable = icon.background as AnimatedVectorDrawable
		rootView.findViewById<View>(R.id.wearSyncNew)
			.setOnClickListener {
				showIcon()
				CoroutineScope(Dispatchers.Default).launch {
					this@WearSyncDeviceList.loadBTDevices()
				}
			}
		lifecycleScope.launch {
			CommsBT.status.collect { status ->
				Log.d(LOG_TAG, "WearSyncActivity: CommsBT status change: $status")
				when(CommsBT.status.value) {
					CommsBT.Status.CONNECTING -> {
						CoroutineScope(Dispatchers.Main).launch {
							showIcon()
							title.text = getString(
								R.string.wear_connecting_to,
								CommsBT.deviceName
							)
						}
					}
					CommsBT.Status.DISCONNECTED -> {
						val knownDevices: Set<BluetoothDevice> = CommsBT.knownDevices
						CoroutineScope(Dispatchers.Main).launch {
							title.setText(R.string.wear_device_select_title)
							list.removeAllViews()
							knownDevices.forEach{ deviceFound(known, it) }
							showList()
						}
					}
					CommsBT.Status.ERROR -> {
						CoroutineScope(Dispatchers.Main).launch {
							title.text = getString(R.string.wear_error_connect)
							showList()
						}
					}
					else -> {}
				}
			}
		}
		return rootView
	}

	private fun showIcon() {
		list.visibility = View.GONE
		icon.visibility = View.VISIBLE
		iconDrawable.start()
	}

	private fun showList() {
		icon.visibility = View.GONE
		list.visibility = View.VISIBLE
		iconDrawable.stop()
	}

	fun loadBTDevices() {
		val bluetoothDevices = CommsBT.getBondedDevices()
		if(bluetoothDevices?.isNotEmpty() != true) {
			onBTError(R.string.wear_error_no_devices)
			return
		}
		CoroutineScope(Dispatchers.Main).launch {
			list.removeAllViews()
			bluetoothDevices.forEach{ deviceFound(list, it) }
			showList()
		}
	}

	fun deviceFound(layout: LinearLayout, bluetoothDevice: BluetoothDevice) {
		val device = TextView(context, null, 0, R.style.wearDeviceStyle)
		device.text = bluetoothDevice.getName()
		layout.addView(device)
		device.setOnClickListener { CommsBT.connectDevice(bluetoothDevice) }
		device.setOnLongClickListener { view ->
			AlertDialog.Builder(context)
				.setMessage(R.string.wear_sync_del)
				.setPositiveButton(R.string.delete) { _, _ ->
					view.visibility = View.GONE
					CommsBT.delKnownAddress(bluetoothDevice.getAddress())
				}
				.setNegativeButton(R.string.cancel, null)
				.create()
				.show()
			true
		}
	}

	fun onBTError(message: Int) {
		CoroutineScope(Dispatchers.Main).launch {
			Log.d(LOG_TAG, "onBTError: " + getString(message))
			title.setText(message)
			showList()
		}
	}
}
