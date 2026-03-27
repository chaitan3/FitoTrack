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
package de.tadris.fitness.ui.settings

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.CompoundButton
import android.widget.NumberPicker
import android.widget.Switch
import androidx.preference.Preference
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.preferences.UserPreferences
import de.tadris.fitness.recording.announcement.TTSController
import de.tadris.fitness.recording.event.TTSReadyEvent
import de.tadris.fitness.ui.dialog.ChooseAutoTimeoutDialog
import de.tadris.fitness.ui.dialog.ChooseAutoTimeoutDialog.AutoTimeoutSelectListener
import de.tadris.fitness.util.NumberPickerUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class RecordingSettingsFragment : FitoTrackSettingFragment(), AutoTimeoutSelectListener {

    lateinit var instance: Instance
    lateinit var preferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        EventBus.getDefault().register(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        instance = Instance.getInstance(context)
        preferences = Instance.getInstance(requireContext()).userPreferences
        addPreferencesFromResource(R.xml.preferences_recording)
        disableSpeechConfig()
        checkTTS()
        findPreference<Preference>("intervals")!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                showIntervalSetManagement()
                true
            }
        findPreference<Preference>("autoTimeoutConfig")!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    showAutoTimeoutConfig()
                    true
                }
        findPreference<Preference>("currentSpeedAverageTimeConfig")!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    showCurrentSpeedAverageTimePicker()
                    true
                }
    }

    override fun getTitle() = getString(R.string.preferencesRecordingTitle)

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    private var ttsController: TTSController? = null
    private fun checkTTS() {
        ttsController = TTSController(requireContext())
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTTSReady(e: TTSReadyEvent) {
        if (context != null) {
            if (e.ttsAvailable) {
                showSpeechConfig()
            }
            ttsController?.destroy()
        }
    }

    private fun showSpeechConfig() {
        findPreference<Preference>("speech")!!.isEnabled = true
        findPreference<Preference>("intervals")!!.isEnabled = true
        findPreference<Preference>("speech")!!.setSummary(R.string.pref_voice_announcements_summary)
        findPreference<Preference>("intervals")!!.setSummary(R.string.manageIntervalsSummary)
    }

    private fun disableSpeechConfig() {
        findPreference<Preference>("speech")!!.isEnabled = false
        findPreference<Preference>("intervals")!!.isEnabled = false
        findPreference<Preference>("speech")!!.setSummary(R.string.ttsNotAvailable)
        findPreference<Preference>("intervals")!!.setSummary(R.string.ttsNotAvailable)
    }

    private fun showIntervalSetManagement() {
        startActivity(Intent(requireContext(), ManageIntervalSetsActivity::class.java))
    }

    private fun showCurrentSpeedAverageTimePicker() {
        val d = AlertDialog.Builder(requireActivity())
        val disabledAlpha = 0.3f
        d.setTitle(getString(R.string.preferenceCurrentSpeedTime))
        val v = layoutInflater.inflate(R.layout.dialog_current_speed, null)
        val sw = v.findViewById<Switch>(R.id.useAverageForCurrentSpeed)

        // number picker: 0-120 seconds, only enabled when sw is checked, transparent if disabled
        val np = v.findViewById<NumberPicker>(R.id.currentSpeedAverageTime)
        sw.isChecked = preferences.useAverageForCurrentSpeed
        np.isEnabled = sw.isChecked
        np.alpha = if (sw.isChecked) 1f else disabledAlpha
        np.maxValue = 120
        np.minValue = 0
        np.setFormatter { value: Int ->
            resources.getQuantityString(
                R.plurals.seconds,
                value,
                value
            )
        }
        np.value = preferences.timeForCurrentSpeed
        np.wrapSelectorWheel = false
        NumberPickerUtils.fixNumberPicker(np)
        sw.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            np.isEnabled = isChecked
            np.alpha = if (isChecked) 1f else disabledAlpha
        }
        d.setView(v)
        d.setNegativeButton(R.string.cancel, null)
        d.setPositiveButton(R.string.okay) { _: DialogInterface?, _: Int ->
            preferences.useAverageForCurrentSpeed = sw.isChecked
            if (sw.isChecked) {
                preferences.timeForCurrentSpeed = np.value
            }
        }
        d.create().show()
    }

    private fun showAutoTimeoutConfig() {
        ChooseAutoTimeoutDialog(requireActivity(), this).show()
    }

    override fun onSelectAutoTimeout(timeoutM: Int) {
        Instance.getInstance(context).userPreferences.autoTimeout = timeoutM
    }
}