/*
 * Copyright (c) 2022 Jannis Scheibe <jannis@tadris.de>
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
package de.tadris.fitness.ui.settings

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.preference.Preference
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.osm.OAuthAuthentication
import de.tadris.fitness.ui.ShareFileActivity
import de.tadris.fitness.util.DataManager
import org.apache.commons.io.FileUtils
import java.io.File
import kotlin.concurrent.thread

class DebugSettingsFragment : FitoTrackSettingFragment() {

    private val handler = Handler()
    lateinit var instance: Instance

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        instance = Instance.getInstance(context)
        addPreferencesFromResource(R.xml.preferences_debug)

        findPreference<Preference>("shareLogs")?.setOnPreferenceClickListener {
            copyLogs()
            true
        }

        findPreference<Preference>("debugSensors")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), SensorDebugActivity::class.java))
            true
        }

        findPreference<Preference>("debugPressure")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), DebugPressureActivity::class.java))
            true
        }

        findPreference<Preference>("resetOAuthToken")?.setOnPreferenceClickListener {
            context?.let { ctx -> OAuthAuthentication.clearAccessToken(ctx) }
            true
        }

    }

    private fun copyLogs() {
        thread {
            val destination =
                File(DataManager.getSharedDirectory(context) + "/fitotrack-logs-${System.currentTimeMillis()}.txt")
            FileUtils.copyFile(instance.logger.file, destination)
            handler.post {
                shareLogs(destination)
            }
        }
    }

    private fun shareLogs(file: File) {
        val uri = DataManager.provide(requireContext(), file)
        val intent = Intent(requireContext(), ShareFileActivity::class.java)
        intent.putExtra(ShareFileActivity.EXTRA_FILE_URI, uri.toString())
        intent.putExtra(ShareFileActivity.EXTRA_FILE_MIME, "text/plain")
        startActivity(intent)
    }

    override fun getTitle() = getString(R.string.preferencesDebugTitle)

}