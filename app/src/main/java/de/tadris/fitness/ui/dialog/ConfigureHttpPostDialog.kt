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

package de.tadris.fitness.ui.dialog

import android.app.Activity
import android.app.AlertDialog
import android.widget.TextView
import de.tadris.fitness.R
import de.tadris.fitness.util.isUrl

class ConfigureHttpPostDialog(val activity: Activity, val onSubmit: (url: String) -> Unit) {

    private val dialog = AlertDialog.Builder(activity)
        .setView(R.layout.dialog_configure_http_post)
        .setTitle(R.string.configureHttpPostRequest)
        .setPositiveButton(R.string.okay, null)
        .create()!!

    init {
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                submit()
            }
        }
        dialog.show()
    }

    private val urlTextView = dialog.findViewById<TextView>(R.id.httpPostUrl)!!

    private fun submit() {
        val url = urlTextView.text.toString()
        if (url.isUrl()) {
            dialog.dismiss()
            onSubmit(url)
        } else {
            urlTextView.error = activity.getString(R.string.enterValidUrl)
        }
    }

}