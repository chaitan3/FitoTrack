/*
 * Copyright (c) 2026 Jannis Scheibe <jannis@tadris.de>
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
package de.tadris.fitness.data.preferences

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface PreferenceProvider {
    val preferences: SharedPreferences
}

class BooleanSharedPreference(val key: String, val default: Boolean) :
    ReadWriteProperty<PreferenceProvider, Boolean> {

    override fun getValue(thisRef: PreferenceProvider, property: KProperty<*>) =
        thisRef.preferences.getBoolean(key, default)

    override fun setValue(thisRef: PreferenceProvider, property: KProperty<*>, value: Boolean) =
        thisRef.preferences.edit { putBoolean(key, value) }
}

class IntSharedPreference(val key: String, val default: Int) :
    ReadWriteProperty<PreferenceProvider, Int> {

    override fun getValue(thisRef: PreferenceProvider, property: KProperty<*>) =
        thisRef.preferences.getInt(key, default)

    override fun setValue(thisRef: PreferenceProvider, property: KProperty<*>, value: Int) =
        thisRef.preferences.edit { putInt(key, value) }
}

class FloatSharedPreference(val key: String, val default: Float) :
    ReadWriteProperty<PreferenceProvider, Float> {

    override fun getValue(thisRef: PreferenceProvider, property: KProperty<*>) =
        thisRef.preferences.getFloat(key, default)

    override fun setValue(thisRef: PreferenceProvider, property: KProperty<*>, value: Float) =
        thisRef.preferences.edit { putFloat(key, value) }
}

class StringSharedPreference(val key: String, val default: String) :
    ReadWriteProperty<PreferenceProvider, String> {

    override fun getValue(thisRef: PreferenceProvider, property: KProperty<*>) =
        thisRef.preferences.getString(key, default)!!

    override fun setValue(thisRef: PreferenceProvider, property: KProperty<*>, value: String) =
        thisRef.preferences.edit { putString(key, value) }
}

class NullableStringSharedPreference(val key: String, val default: String? = null) :
    ReadOnlyProperty<PreferenceProvider, String?> {

    override fun getValue(thisRef: PreferenceProvider, property: KProperty<*>) =
        thisRef.preferences.getString(key, default)
}

class StringSetSharedPreference(val key: String, val default: Set<String?>) :
    ReadWriteProperty<PreferenceProvider, Set<String?>> {

    override fun getValue(thisRef: PreferenceProvider, property: KProperty<*>) =
        thisRef.preferences.getStringSet(key, default)!!

    override fun setValue(
        thisRef: PreferenceProvider,
        property: KProperty<*>,
        value: Set<String?>
    ) =
        thisRef.preferences.edit { putStringSet(key, value) }
}
