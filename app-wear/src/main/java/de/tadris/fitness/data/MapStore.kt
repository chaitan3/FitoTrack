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
package de.tadris.fitness.data

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import de.tadris.fitness.R
import de.tadris.fitness.util.logD
import de.tadris.fitness.util.logE
import de.tadris.fitness.util.runInBackground
import de.tadris.fitness.util.toast
import de.tadris.fitness.util.tryIgnore
import kotlinx.coroutines.delay
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPFileFilter
import java.util.Arrays

class MapStore {
	companion object {
		const val URL = "ftp-stud.hs-esslingen.de"
		const val USERNAME = "anonymous"
		const val PASSWORD = "de.tadris.fitness.wearos"
		const val ROOT_PATH = "pub/.3/download.mapsforge.org/maps/v5/"
		fun getLocalMaps(context: Context): List<File> {
			val localMaps = mutableListOf<File>()
			try {
				val files = context.filesDir.listFiles()
				if(files == null) return localMaps
				Arrays.sort(files)
				for(i in files.indices.reversed()) {
					val filename = files[i].getName()
					if(filename.endsWith(".map")) {
						localMaps.add(
							File(
								filename,
								files[i].length(),
								files[i].lastModified()
							)
						)
					}
				}
			} catch(e: Exception) {
				logE("MapStore.getLocalMaps: $e")
				logE("MapStore.getLocalMaps: " + e.message)
			}
			return localMaps
		}
		fun deleteLocalMap(context: Context, file: File): Boolean {
			logD("MapStore.deleteLocalMap: " + file.name)
			val fileLocal = file.getLocalFile(context)
			if(!fileLocal.exists()) {
				logD("MapStore.deleteLocalMap file does not exist")
				return false
			}
			return fileLocal.delete()
		}
	}
	var ftpClient: FTPClient? = null
	fun close() {
		tryIgnore { ftpClient?.disconnect() }
		ftpClient = null
	}

	fun getRemoteDirs(path: String): List<Dir> {
		val dirs = mutableListOf<Dir>()
		try {
			for(ftpFile in fTPClient?.listDirectories(ROOT_PATH + path) ?: return dirs) {
				dirs.add(Dir(ftpFile.name, path))
			}
		} catch(e: Exception) {
			logE("MapStore.getRemoteDirs: $e")
			logE("MapStore.getRemoteDirs: " + e.message)
		}
		return dirs
	}
	fun getRemoteFiles(path: String): List<File> {
		val files = mutableListOf<File>()
		try {
			val filter = FTPFileFilter { ftpFile: FTPFile ->
				ftpFile.isFile && ftpFile.name.endsWith(".map")
			}
			for(ftpFile in fTPClient?.listFiles(ROOT_PATH + path, filter) ?: return files) {
				files.add(
					File(
						ftpFile.name,
						path,
						ftpFile.size,
						ftpFile.timestamp.getTimeInMillis()
					)
				)
			}
		} catch(e: Exception) {
			logE("MapStore.getRemoteFiles: $e")
			logE("MapStore.getRemoteFiles: " + e.message)
		}
		return files
	}
	fun getRemoteFile(
		context: Context,
		fileRemote: File,
		onError: (Int) -> Unit,
		onProgress: (Long) -> Unit,
		onDone: () -> Unit
	) {
		logD("MapStore.getRemoteFile: " + fileRemote.path)
		val fileLocal = java.io.File(fileRemote.toLocal())
		if(fileLocal.exists() && (fileLocal.lastModified() > fileRemote.lastModified || !fileLocal.delete())) {
			onError(R.string.error_maps_file_exists)
			return
		}
		cancelDownload = false
		try {
			val connectivityManager =
				context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
			connectivityManager.requestNetwork(
				NetworkRequest.Builder()
					.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
					.build(),
				object : ConnectivityManager.NetworkCallback() {
					override fun onAvailable(network: Network) {
						super.onAvailable(network)
						logD("MapStore.getRemoteFile WIFI is available")
						runInBackground { getRemoteFileDownload(context, fileRemote, onError, onProgress, onDone) }
					}
					override fun onUnavailable() {
						super.onUnavailable()
						logD("MapStore.getRemoteFile WIFI is not available")
						context.toast(R.string.fail_no_fast)
						runInBackground { getRemoteFileDownload(context, fileRemote, onError, onProgress, onDone) }
					}
				},
				2000
			)
		} catch(e: Exception) {
			logD("MapStore.getRemoteFile: $e")
			logD("MapStore.getRemoteFile: " + e.message)
			context.toast(R.string.fail_fast)
			runInBackground { getRemoteFileDownload(context, fileRemote, onError, onProgress, onDone) }
		}
	}

	var cancelDownload = false
	suspend fun getRemoteFileDownload(
		context: Context,
		fileRemote: File,
		onError: (Int) -> Unit,
		onProgress: (Long) -> Unit,
		onDone: () -> Unit
	) {
		try {
			val inputStream = fTPClient?.retrieveFileStream(ROOT_PATH + fileRemote.path)
			val fileOutputStream = context.openFileOutput(fileRemote.toLocal(), Context.MODE_PRIVATE)
			var bytesDone: Long = 0
			var lastReadTime = System.currentTimeMillis()
			while(!cancelDownload && System.currentTimeMillis() - lastReadTime < 3000) {
				if(inputStream!!.available() == 0) {
					delay(100)
					continue
				}
				val buffer = ByteArray(1048576) //1MB
				val numBytes = inputStream.read(buffer)
				if(numBytes < 0) {
					onError(R.string.error_maps_download)
					return
				}
				fileOutputStream.write(buffer, 0, numBytes)
				bytesDone += numBytes.toLong()

				onProgress(bytesDone)
				if(inputStream.available() == 0 && bytesDone >= fileRemote.size) {
					onDone()
					return
				}
				lastReadTime = System.currentTimeMillis()
			}
			logE("MapStore.getRemoteFile not enough bytes and no new data after 3 sec")
		} catch(e: Exception) {
			logE("MapStore.getRemoteFile: $e")
			logE("MapStore.getRemoteFile: " + e.message)
		}
		logD("exit")
		onError(R.string.error_maps_download)
	}

	fun checkHasUpdate(file: File): Boolean {
		for(ftpFile in fTPClient?.listFiles(ROOT_PATH + file.path) ?: return false) {
			return file.lastModified < ftpFile.timestamp.getTimeInMillis()
		}
		return false
	}

	val fTPClient: FTPClient?
		get() {
			if(ftpClient != null) return ftpClient
			ftpClient = FTPClient()
			ftpClient?.connect(URL)
			ftpClient?.login(USERNAME, PASSWORD)
			ftpClient?.enterLocalPassiveMode()
			ftpClient?.setFileType(FTPClient.BINARY_FILE_TYPE)
			return ftpClient
		}

	class Dir(val name: String, dir: String) { val path: String = "$dir/$name" }

	class File {
        var name: String
        val path: String
        val size: Long
		val lastModified: Long

		constructor(path: String, size: Long, lastModified: Long) {
			this.path = path.replace("_", "/")
			name = this.path
			val index = this.path.lastIndexOf("/")
			if(index > 0) name = path.substring(index + 1)
			this.size = size
			this.lastModified = lastModified
		}
		constructor(name: String, dir: String, size: Long, lastModified: Long) {
			this.name = name
			path = "$dir/$name"
			this.size = size
			this.lastModified = lastModified
		}
		fun toLocal(): String = path.replace("/", "_")
		fun getLocalFile(context: Context): java.io.File =
			java.io.File(context.filesDir, toLocal())
	}
}
