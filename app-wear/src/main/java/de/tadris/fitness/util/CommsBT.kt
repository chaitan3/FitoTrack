/*
 * Copyright (c) 2026 Bart Vullings <dev@windkracht8.com>
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
package de.tadris.fitness.util

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutDataStore
import de.tadris.fitness.data.WorkoutDataStore.getFullWorkout
import de.tadris.fitness.data.WorkoutDataStore.readWorkouts
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Base64
import java.util.UUID
import java.util.zip.GZIPOutputStream

val FITOTRACK_UUID: UUID = UUID.fromString("44fb140d-74e8-4334-8df0-aa717f873db3")
const val COMMS_VERSION = 2
class CommsBT : Service() {
	var disconnect = true
	var bluetoothAdapter: BluetoothAdapter? = null
	var bluetoothServerSocket: BluetoothServerSocket? = null
	var bluetoothSocket: BluetoothSocket? = null
	var commsBTConnect: CommsBTConnect? = null
	var commsBTConnected: CommsBTConnected? = null
	companion object {
		val error = MutableSharedFlow<Int>()
		suspend fun onError(message: Int) = error.emit(message)
	}

	val responseQueue = JSONArray()
	override fun onBind(intent: Intent?): IBinder? { return null }
	override fun onCreate() {
		super.onCreate()
		startBT()
	}
	override fun onDestroy() {
		stopBT()
		super.onDestroy()
	}
	val btStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent) {
			if(BluetoothAdapter.ACTION_STATE_CHANGED == intent.action) {
				val btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
				if(btState == BluetoothAdapter.STATE_TURNING_OFF) {
					logD("CommsBT.btStateReceiver: stop")
					stopBT()
				} else if(btState == BluetoothAdapter.STATE_ON) {
					logD("CommsBT.btStateReceiver: start")
					startBT()
				}
			}
		}
	}

	suspend fun gotRequest(request: String) {
		logD("CommsBT.gotRequest: $request")
		try {
			val requestMessage = JSONObject(request)
			val requestType = requestMessage.getString("requestType")
			when(requestType) {
				"sync" -> sendResponseSync()
				"getWorkout" ->
					sendResponseGetWorkout(requestMessage.getLong("workoutId"))
				else -> logE("CommsBT.gotRequest Unknown requestType: $requestType")
			}
		} catch(e: Exception) {
			logE("CommsBT.gotRequest: " + e.message)
			onError(R.string.error_bt_request)
		}
	}
	suspend fun sendResponseSync() {
		try {
			val response = JSONObject()
			response.put("version", COMMS_VERSION)
			response.put("requestType", "sync")
			val jsonWorkouts = JSONArray()
			readWorkouts(this)
			WorkoutDataStore.workouts.forEach { jsonWorkouts.put(it.toJson(this)) }
			response.put("workouts", jsonWorkouts)
			responseQueue.put(response)
		} catch(e: Exception) {
			logE("CommsBT.sendResponse JSONObject: " + e.message)
			onError(R.string.error_bt_response)
		}
	}
	suspend fun sendResponseGetWorkout(id: Long) {
		try {
			val response = JSONObject()
			response.put("version", COMMS_VERSION)
			response.put("requestType", "getWorkout")
			val workoutData = getFullWorkout(this, id)
			if(workoutData == null) {
				response.put("error", 404)
			} else {
				val workoutDataJson = workoutData.toJson(this)
				val bos = ByteArrayOutputStream()
				var gos = GZIPOutputStream(bos)
				gos.write(workoutData.locationsToJsonString().toByteArray())
				gos.close()
				workoutDataJson.put(
					"locations",
					Base64.getEncoder().encodeToString(bos.toByteArray())
				)

				bos.reset()
				gos = GZIPOutputStream(bos)
				gos.write(workoutData.heartRatesToJsonString().toByteArray())
				gos.close()
				workoutDataJson.put(
					"heartRates",
					Base64.getEncoder().encodeToString(bos.toByteArray())
				)

				response.put("workout", workoutDataJson)
			}
			responseQueue.put(response)
		} catch(e: Exception) {
			logE("CommsBT.sendResponse JSONObject : " + e.message)
			onError(R.string.error_bt_response)
		}
	}

	fun startBT() {
		logD("CommsBT.startBT")
		disconnect = false
		if(bluetoothAdapter == null) {
			val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
			bluetoothAdapter = bluetoothManager.adapter
			if(bluetoothAdapter == null || bluetoothAdapter?.isEnabled != true) {
				logD("CommsBT.startBT bluetooth disabled")
				return
			}
			registerReceiver(btStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
		}
		if(commsBTConnect == null) {
			logD("CommsBT.startBT start listening")
			commsBTConnect = CommsBTConnect()
			commsBTConnect?.start()
		}
	}
	fun restartBT(){
		logD("CommsBT.restartBT")
		disconnect = true
		tryIgnore { bluetoothServerSocket?.close() }
		tryIgnore { bluetoothSocket?.close() }
		bluetoothServerSocket = null
		bluetoothSocket = null
		commsBTConnect = null
		commsBTConnected = null
		disconnect = false
		commsBTConnect = CommsBTConnect()
		commsBTConnect?.start()
	}
	fun stopBT() {
		logD("CommsBT.stopBT")
		disconnect = true
		tryIgnore { unregisterReceiver(btStateReceiver) }
		tryIgnore { bluetoothServerSocket?.close() }
		tryIgnore { bluetoothSocket?.close() }
		bluetoothServerSocket = null
		bluetoothSocket = null
		bluetoothAdapter = null
		commsBTConnect = null
		commsBTConnected = null
	}

	inner class CommsBTConnect @SuppressLint("MissingPermission") //Permissions are handled in Main
	constructor() : Thread() {
		init {
			try {
				logD("CommsBTConnect")
				bluetoothServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
					"FitoTrack",
					FITOTRACK_UUID
				)
			} catch(e: Exception) {
				logD("CommsBTConnect: " + e.message)
				if(!disconnect) logE("CommsBTConnect: " + e.message)
			}
		}

		override fun run() {
			try {
				logD("CommsBTConnect.run 1 $disconnect")
				bluetoothSocket = bluetoothServerSocket?.accept()
				logD("CommsBTConnect.run 2 $disconnect")
				if(!disconnect) {
					logD("CommsBTConnect.run accepted")
					commsBTConnected = CommsBTConnected()
					commsBTConnected?.start()
				}
			} catch (e: Exception) {
				logD("CommsBTConnect.run Exception: " + e.message)
				if(!disconnect) logE("CommsBTConnect.run Exception: " + e.message)
			}
		}
	}

	inner class CommsBTConnected : Thread() {
		var inputStream: InputStream? = null
		var outputStream: OutputStream? = null
		init {
			try {
				inputStream = bluetoothSocket?.inputStream
				outputStream = bluetoothSocket?.outputStream
			} catch(e: Exception) {
				logE("CommsBTConnected init: " + e.message)
			}
		}
		override fun run() {
			logD("CommsBTConnected.run")
			runInBackground { process() }
		}
		suspend fun process() {
			while (!disconnect) {
				read()
				try {
					outputStream!!.write("".toByteArray())
				} catch(_: Exception) {
					restartBT()
					return
				}
				if(responseQueue.length() > 0 && !sendNextResponse()) {
					restartBT()
					return
				}
				delay(100)
			}
		}

		suspend fun sendNextResponse(): Boolean {
			try {
				val response = responseQueue.get(0) as JSONObject
				responseQueue.remove(0)
				logD("CommsBTConnected.sendNextResponse: $response")
				outputStream!!.write(response.toString().toByteArray())
			} catch(e: Exception) {
				logE("CommsBTConnected.sendNextResponse: " + e.message)
				onError(R.string.error_bt_response)
				return false
			}
			return true
		}

		suspend fun read() {
			try {
				if((inputStream?.available() ?: 0) < 5) return
				var lastReadTime = System.currentTimeMillis()
				var request = ""
				while(System.currentTimeMillis() - lastReadTime < 3000) {
					if((inputStream?.available() ?: 0) == 0) {
						delay(100)
						continue
					}
					val buffer = ByteArray(inputStream?.available() ?: 0)
					val numBytes = inputStream?.read(buffer) ?: 0
					if(numBytes < 0) {
						logE("CommsBTConnected.read read error, request: $request")
						onError(R.string.error_bt_request)
						return
					}
					val temp = String(buffer)
					request += temp
					if(isValidJSON(request)) {
						gotRequest(request)
						return
					}
					lastReadTime = System.currentTimeMillis()
				}
				logE("CommsBTConnected.read no valid message and no new data after 3 sec: $request")
			} catch(e: Exception) {
				logE("CommsBTConnected.read: " + e.message)
			}
			onError(R.string.error_bt_request)
		}

		fun isValidJSON(json: String): Boolean {
			if(!json.endsWith("}")) return false
			try { JSONObject(json) } catch(_: JSONException) { return false }
			return true
		}
	}
}
