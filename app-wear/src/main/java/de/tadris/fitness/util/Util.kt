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
package de.tadris.fitness.util

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

const val LOG_TAG = "FitoTrack"

fun logE(message: String) = Log.e(LOG_TAG, message)
fun logW(message: String) = Log.w(LOG_TAG, message)
fun logD(message: String) = Log.d(LOG_TAG, message)
fun Context.toast(message: Int) =
	Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
fun Context.toast(message: String) =
	Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
fun Activity.toastFromBG(message: Int) = runOnUiThread { toast(message) }

fun tryIgnore(block: () -> Unit) = try { block() }
catch(_: Exception) {}

fun runInBackground(block: suspend () -> Unit) =
	CoroutineScope(Dispatchers.Default).launch { block() }

val sdfDuration = SimpleDateFormat("mm:ss", Locale.GERMANY)
val sdfDurationShort = SimpleDateFormat("m:ss", Locale.GERMANY)
val sdfDurationHour = SimpleDateFormat("H:mm:ss", Locale.GERMANY)
fun Long.prettyDuration(): String =
	if(this >= 3600000) sdfDurationHour.format(this - 3600000)
	else sdfDuration.format(this)
fun Long.prettyDurationShort(): String =
	if(this >= 3600000) sdfDurationHour.format(this - 3600000)
	else sdfDurationShort.format(this)
fun Int.prettyDistance(): String =
	if(this in -1000..1000) String.format(Locale.ENGLISH, "%d", this)
	else String.format(Locale.ENGLISH,"%.1f", this.toFloat() / 1000)
fun Double.prettySpeed(): String =
	if(this*3.6 >= 20) String.format(Locale.ENGLISH, "%.0f", this*3.6)
	else String.format(Locale.ENGLISH, "%.1f", this*3.6)
fun Double.pretty(): String = String.format(Locale.ENGLISH, "%.0f", this)
fun Long.isSeconds(seconds: Int) = this >= seconds * 1000L
