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

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutData.Companion.getWorkoutTypeFromInt
import de.tadris.fitness.data.WorkoutData.Companion.getWorkoutTypeName
import de.tadris.fitness.data.WorkoutData.WorkoutType

object SettingsStore {
	fun storeSetting(context: Context, name: String, type: WorkoutType?, value: Boolean) =
		getEditor(context, type).putBoolean(name, value).apply()

	@JvmStatic
    fun getBTEnabled(context: Context): Boolean =
		getSharedPreferences(context, null)
			.getBoolean("BTEnabled", false)
	@JvmStatic
    fun storeBTEnabled(context: Context, value: Boolean) =
		getEditor(context, null).putBoolean("BTEnabled", value).apply()
	@JvmStatic
    fun getMapSource(context: Context): Boolean =
		getSharedPreferences(context, null)
			.getBoolean("MapSource", false)
	@JvmStatic
    fun storeMapSource(context: Context, value: Boolean) =
		getEditor(context, null).putBoolean("MapSource", value).apply()

	fun getLastWorkoutType(context: Context): WorkoutType {
		val sharedPreferences = getSharedPreferences(context, null)
		val lastWorkoutTypeInt = sharedPreferences.getInt("lastWorkoutType", -1)
		if(lastWorkoutTypeInt == -1) return WorkoutType.WALK
		val workoutType = getWorkoutTypeFromInt(lastWorkoutTypeInt)
		if(workoutType == null) return WorkoutType.WALK
		return workoutType
	}

	fun storeLastWorkoutType(context: Context, workoutType: WorkoutType)  =
		getEditor(context, null)
			.putInt("lastWorkoutType", workoutType.ordinal)
			.apply()

	private fun getEditor(context: Context, workoutType: WorkoutType?): SharedPreferences.Editor =
		getSharedPreferences(context, workoutType).edit()

	private fun getSharedPreferences(context: Context, workoutType: WorkoutType?): SharedPreferences =
		context.getSharedPreferences(
			if(workoutType == null) "main"
			else context.getString(getWorkoutTypeName(workoutType)),
			Context.MODE_PRIVATE
		)

	class WorkoutSettings(context: Context, workoutType: WorkoutType) {
		data class Screen(
			var name: Int,
			var navRoute: String,
			var enabled: MutableState<Boolean> = mutableStateOf(false),
			val needsLocation: Boolean,
			val needsHeartRate: Boolean
		){
			fun toggle(context: Context, workoutType: WorkoutType) {
				this.enabled.value = !this.enabled.value
				storeSetting(
					context = context,
					name = navRoute,
					type = workoutType,
					value = this.enabled.value
				)
			}
		}
		val screens = listOf(
			Screen(
				name = R.string.settings_screen_map,
				navRoute = "ScreenMap",
				enabled = mutableStateOf(true),
				needsLocation = true,
				needsHeartRate = false
			),
			Screen(
				name = R.string.settings_screen_default,
				navRoute = "ScreenDefault",
				enabled = mutableStateOf(true),
				needsLocation = false,
				needsHeartRate = false
			),
			Screen(
				name = R.string.settings_screen_target,
				navRoute = "ScreenTarget",
				needsLocation = false,
				needsHeartRate = false
			),
			Screen(
				name = R.string.settings_screen_ele,
				navRoute = "ScreenElevation",
				needsLocation = true,
				needsHeartRate = false
			),
			Screen(
				name = R.string.settings_screen_heart_rate,
				navRoute = "ScreenHeartRate",
				needsLocation = false,
				needsHeartRate = true
			),
			Screen(
				name = R.string.settings_screen_speed,
				navRoute = "ScreenSpeed",
				needsLocation = true,
				needsHeartRate = false
			)
		)
		val screenOn: MutableState<Boolean> = mutableStateOf(false)
		fun toggleScreenOn(context: Context, workoutType: WorkoutType) {
			screenOn.value = !screenOn.value
			storeSetting(context, "screenOn", workoutType, screenOn.value)
		}
		val autoPause: MutableState<Boolean> = mutableStateOf(false)
		fun toggleAutoPause(context: Context, workoutType: WorkoutType) {
			autoPause.value = !autoPause.value
			storeSetting(context, "autoPause", workoutType, autoPause.value)
		}

		init {
			val sp = getSharedPreferences(context, workoutType)
			screenOn.value = sp.getBoolean("screenOn", screenOn.value)
			autoPause.value = sp.getBoolean("autoPause", autoPause.value)
			screens.forEach {
				it.enabled.value = sp.getBoolean(
					it.navRoute,
					it.navRoute in setOf("ScreenDefault", "ScreenMap")
				)
			}
		}
	}
}
