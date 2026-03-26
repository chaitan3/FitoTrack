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
package de.tadris.fitness.map

import android.content.Context
import androidx.preference.PreferenceManager
import de.tadris.fitness.ui.settings.XmlThemeStyleSettingFragment
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu

class FitoTrackRenderThemeMenuCallback(
    private val context: Context,
    private val listener: RenderThemeMenuListener?
) : XmlRenderThemeMenuCallback {


    override fun getCategories(styleMenu: XmlRenderThemeStyleMenu): Set<String> {
        listener?.onRenderThemeMenuIsAvailable(styleMenu)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val id = prefs.getString(
            XmlThemeStyleSettingFragment.KEY_PREFIX + styleMenu.id,
            styleMenu.defaultValue
        )!!

        val baseLayer = styleMenu.getLayer(id.replace(XmlThemeStyleSettingFragment.KEY_PREFIX, ""))
        val result = baseLayer.categories
        // add the categories from overlays that are enabled
        for (overlay in baseLayer.overlays) {
            if (prefs.getBoolean(
                    XmlThemeStyleSettingFragment.KEY_PREFIX + overlay.id,
                    overlay.isEnabled
                )
            ) {
                result.addAll(overlay.categories)
            }
        }
        return result
    }

    interface RenderThemeMenuListener {
        fun onRenderThemeMenuIsAvailable(styleMenu: XmlRenderThemeStyleMenu)
    }
}