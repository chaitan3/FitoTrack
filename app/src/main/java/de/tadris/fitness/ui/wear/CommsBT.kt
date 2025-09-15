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
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import de.tadris.fitness.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@SuppressLint("MissingPermission")//handled by WearSyncActivity
object CommsBT {
	const val COMMS_VERSION = 2
	const val LOG_TAG = "FitoTrack.CommsBT"
	val FITOTRACK_UUID: UUID? = UUID.fromString("44fb140d-74e8-4334-8df0-aa717f873db3")
	var sharedPreferences: SharedPreferences? = null
	var bluetoothAdapter: BluetoothAdapter? = null
	var bluetoothSocket: BluetoothSocket? = null
	var commsBTConnect: CommsBTConnect? = null
	var commsBTConnected: CommsBTConnected? = null
	val knownDevices: MutableSet<BluetoothDevice> = mutableSetOf()
	var knownAddresses: MutableSet<String> = mutableSetOf()

	enum class Status { STARTING, DISCONNECTED, CONNECTING, CONNECTED, ERROR }
	val status = MutableStateFlow(Status.STARTING)
	var error by mutableIntStateOf(-1)
	var messageStatus by mutableIntStateOf(-1)
	var bytesDone = MutableStateFlow(0)
	var response = MutableStateFlow<JSONObject?>(null)
	var deviceName = ""
	var disconnect = false
	var requestQueue = JSONArray()
	var lastRequest: JSONObject? = null

	fun start(context: Context) {
		Log.d(LOG_TAG, "CommsBT.start")
		status.value = Status.STARTING
		val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
		bluetoothAdapter = bm.adapter
		if(bluetoothAdapter?.state != BluetoothAdapter.STATE_ON) {
			onError(R.string.wear_error_off)
			return
		}

		sharedPreferences = context.getSharedPreferences("CommsBT", MODE_PRIVATE)
		knownAddresses = sharedPreferences?.getStringSet("knownAddresses", null) ?: mutableSetOf()
		Log.d(LOG_TAG, "start " + knownAddresses.size + " known devices")
		val bondedBTDevices = bluetoothAdapter?.bondedDevices ?: emptySet()
		//Find and clean known devices
		knownAddresses.forEach { checkKnownAddress(it, bondedBTDevices) }
		//Try to connect to known device
		if(knownDevices.isNotEmpty()) {
			connectDevice(knownDevices.first())
			return //having multiple watches is rare, user will have to select from DeviceSelect
		}
		status.value = Status.DISCONNECTED
	}

	fun checkKnownAddress(knownAddress: String, bondedDevices: Set<BluetoothDevice>) {
		for(device in bondedDevices) {
			if(device.address == knownAddress) {
				knownDevices.add(device)
				return
			}
		}
		delKnownAddress(knownAddress)
	}
	fun delKnownAddress(address: String) {
		if(knownAddresses.remove(address)) storeKnownAddresses()
	}
	fun storeKnownAddresses() {
		sharedPreferences?.edit {
			if(knownAddresses.isEmpty()) {
				remove("knownAddresses")
			} else {
				putStringSet("knownAddresses",
					knownAddresses
				)
			}
		}
	}
	fun stop() {
		Log.d(LOG_TAG, "CommsBT.stop")
		disconnect = true
		requestQueue = JSONArray()
		lastRequest = null
		status.value = Status.DISCONNECTED
		try { bluetoothSocket?.close() } catch(_: Exception){}
		bluetoothSocket = null
		commsBTConnect = null
		commsBTConnected = null
	}

	fun getBondedDevices(): Set<BluetoothDevice>? {
		if(bluetoothAdapter == null) {
			onError(R.string.wear_error_denied)
			return null
		}
		if(bluetoothAdapter?.state != BluetoothAdapter.STATE_ON) {
			onError(R.string.wear_error_off)
			return null
		}
		return bluetoothAdapter?.bondedDevices
	}

	fun connectDevice(device: BluetoothDevice) {
		Log.d(LOG_TAG, "connectDevice: " + device.getName())
		if(status.value in listOf(Status.CONNECTED, Status.CONNECTING) ||
			bluetoothAdapter?.isEnabled != true
		) return
		disconnect = false
		deviceName = device.name ?: "<no name>"
		status.value = Status.CONNECTING
		commsBTConnect = CommsBTConnect(device)
		commsBTConnect?.start()
	}

	fun sendSyncRequest() {
		Log.d(LOG_TAG, "sendSyncRequest")
		try {
			val request = JSONObject()
				.put("version", COMMS_VERSION)
				.put("requestType", "sync")
			requestQueue.put(request)
		} catch(e: Exception) {
			Log.e(LOG_TAG, "sendSyncRequest Exception: " + e.message)
		}
	}
	fun sendGetWorkoutRequest(workoutId: Long) {
		Log.d(LOG_TAG, "sendGetWorkoutRequest")
		try {
			val request = JSONObject()
				.put("version", COMMS_VERSION)
				.put("requestType", "getWorkout")
				.put("workoutId", workoutId)
			requestQueue.put(request)
		} catch(e: Exception) {
			Log.e(LOG_TAG, "sendGetWorkoutRequest Exception: " + e.message)
		}
	}

	class CommsBTConnect(device: BluetoothDevice) : Thread() {
		init {
			Log.d(LOG_TAG, "CommsBTConnect " + device.name)
			try {
				bluetoothSocket = device.createRfcommSocketToServiceRecord(FITOTRACK_UUID)
			} catch(e: Exception) {
				Log.e(LOG_TAG, "CommsBTConnect Exception: " + e.message)
				status.value = Status.DISCONNECTED
			}
		}
		override fun run() {
			try {
				Log.d(LOG_TAG, "CommsBTConnect.run connect")
				bluetoothSocket?.connect()
				Log.d(LOG_TAG, "CommsBTConnect.run CommsBTConnected")
				commsBTConnected = CommsBTConnected()
				Log.d(LOG_TAG, "CommsBTConnect.run CommsBTConnected.start")
				commsBTConnected?.start()
			} catch(e: Exception) {
				Log.d(LOG_TAG, "CommsBTConnect.run failed: $e")
				Log.d(LOG_TAG, "CommsBTConnect.run failed: " + e.message)
				try { bluetoothSocket?.close() } catch (_: Exception) {}
				status.value = Status.DISCONNECTED
			}
		}
	}

	class CommsBTConnected : Thread() {
		var inputStream: InputStream? = null
		var outputStream: OutputStream? = null

		init {
			Log.d(LOG_TAG, "CommsBTConnected")
			try {
				inputStream = bluetoothSocket!!.inputStream
				outputStream = bluetoothSocket!!.outputStream
				status.value = Status.CONNECTED
				knownDevices.add(bluetoothSocket!!.remoteDevice)
				if(knownAddresses.add(bluetoothSocket!!.remoteDevice.address)) {
					storeKnownAddresses()
				}
			} catch(e: Exception) {
				Log.e(LOG_TAG, "CommsBTConnected.init exception: " + e.message)
				status.value = Status.DISCONNECTED
			}
		}

		override fun run() { runBlocking { process() } }

		fun close() {
			Log.d(LOG_TAG, "CommsBTConnected.close")
			status.value = Status.DISCONNECTED
			messageStatus = -1
			try {
				requestQueue = JSONArray()
				bluetoothSocket?.close()
			} catch(_: Exception) {}
			bluetoothSocket = null
			commsBTConnected = null
			commsBTConnect = null
		}

		suspend fun process() {
			while(!disconnect) {
				try {
					outputStream!!.write("".toByteArray())
				} catch(_: Exception) {
					Log.d(LOG_TAG, "Connection closed")
					break
				}
				sendNextRequest()
				read()
				delay(100)
			}
			close()
		}

		fun sendNextRequest() {
			try {
				outputStream!!.write("".toByteArray())
				if(requestQueue.length() < 1 || lastRequest != null) return
				lastRequest = requestQueue.getJSONObject(0)
				requestQueue.remove(0)
				Log.d(LOG_TAG, "CommsBTConnected.sendNextRequest: $lastRequest")
				outputStream!!.write(lastRequest.toString().toByteArray())
			} catch(e: Exception) {
				Log.e(LOG_TAG, "CommsBTConnected.sendNextRequest Exception: " + e.message)
				onError(R.string.wear_error_tech)
				disconnect = true
			}
		}

		suspend fun read() {
			try {
				if(inputStream!!.available() < 5) return
				var lastReadTime = System.currentTimeMillis()
				var responseStr = ""
				var bytesDone = 0
				while(System.currentTimeMillis() - lastReadTime < 3000) {
					if(inputStream!!.available() == 0) {
						delay(100)
						continue
					}
					val buffer = ByteArray(inputStream!!.available())
					val numBytes = inputStream!!.read(buffer)
					if(numBytes < 0) {
						Log.e(LOG_TAG, "CommsBTConnected.read read error: $responseStr")
						lastRequest = null
						return onError(R.string.wear_error_tech)
					} else if(numBytes > 0) {
						lastReadTime = System.currentTimeMillis()
					}
					bytesDone += numBytes
					CommsBT.bytesDone.emit(bytesDone)
					val temp = String(buffer)
					responseStr += temp
					if(isValidJSON(responseStr)) {
						Log.d(LOG_TAG, "CommsBTConnected.read got message: $responseStr")
						response.emit(JSONObject(responseStr))
						lastRequest = null
						return
					}
				}
				Log.e(
					LOG_TAG,
					"CommsBTConnected.read no valid message and no new data after 3 sec: $responseStr"
				)
			} catch(e: Exception) {
				Log.e(LOG_TAG, "CommsBTConnected.read: " + e.message)
				lastRequest = null
			}
			onError(R.string.wear_error_tech)
		}

		fun isValidJSON(json: String): Boolean {
			if(!json.endsWith("}")) return false
			try { JSONObject(json) }
			catch(_: JSONException) { return false }
			return true
		}
	}

	fun onError(message: Int) {
		error = message
		status.value = Status.ERROR
	}
}
