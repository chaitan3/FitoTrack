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

package de.tadris.fitness.ui.settings;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.data.preferences.UserPreferences;
import de.tadris.fitness.util.NumberPickerUtils;
import de.tadris.fitness.util.unit.DistanceUnitSystem;
import de.tadris.fitness.util.unit.DistanceUnitUtils;
import kotlin.collections.ArraysKt;

public class VoiceAnnouncementsSettingsFragment extends FitoTrackSettingFragment {

    UserPreferences userPreferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        userPreferences = Instance.getInstance(getContext()).userPreferences;
        setPreferencesFromResource(R.xml.preferences_voice_announcements, rootKey);
        bindPreferenceSummaryToValue(findPreference("announcementMode"));

        findPreference("speechConfig").setOnPreferenceClickListener(preference -> {
            showSpeechConfig();
            return true;
        });

        findPreference("paceControlConfig").setOnPreferenceClickListener(preference -> {
            showPaceControlConfig();
            return true;
        });
    }

    private void showSpeechConfig() {
        Instance.getInstance(getContext()).distanceUnitUtils.setUnit();

        final AlertDialog.Builder d = new AlertDialog.Builder(requireActivity());
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        d.setTitle(getString(R.string.pref_announcements_config_title));
        View v = getLayoutInflater().inflate(R.layout.dialog_spoken_updates_picker, null);

        NumberPicker npT = v.findViewById(R.id.spokenUpdatesTimePicker);
        npT.setMaxValue(60);
        npT.setMinValue(0);
        npT.setValue(preferences.getInt(UserPreferences.VOICE_ANNOUNCEMENTS_INTERVAL_TIME, 0));
        npT.setWrapSelectorWheel(false);
        String[] npTValues = new String[61];
        npTValues[0] = getString(R.string.speechConfigNoSpeech);
        for (int i = 1; i <= 60; i++) {
            npTValues[i] = i + " " + getString(R.string.timeMinuteShort);
        }
        npT.setDisplayedValues(npTValues);
        NumberPickerUtils.fixNumberPicker(npT);

        float[] intervals = new float[]{0, 0.5f, 1.0f, 1.5f, 2, 3, 4, 5, 6, 7, 8, 9, 10};

        final String distanceUnit = " " + Instance.getInstance(getContext()).distanceUnitUtils.getDistanceUnitSystem().getLongDistanceUnit();
        NumberPicker npD = v.findViewById(R.id.spokenUpdatesDistancePicker);
        npD.setMaxValue(intervals.length - 1);
        npD.setMinValue(0);
        int currentIndex = Arrays.binarySearch(intervals, preferences.getFloat(UserPreferences.VOICE_ANNOUNCEMENTS_INTERVAL_DISTANCE, 0));
        npD.setValue(currentIndex == -1 ? 0 : currentIndex);
        npD.setWrapSelectorWheel(false);

        List<String> distanceTitles = ArraysKt.map(intervals, (Float i) -> {
            if (i == 0) {
                return getString(R.string.speechConfigNoSpeech);
            } else {
                return String.format(Locale.getDefault(), "%.1f", i) + distanceUnit;
            }
        });
        npD.setDisplayedValues(distanceTitles.toArray(new String[0]));

        NumberPickerUtils.fixNumberPicker(npD);

        d.setView(v);

        d.setNegativeButton(R.string.cancel, null);
        d.setPositiveButton(R.string.okay, (dialog, which) ->
                preferences.edit()
                        .putInt(UserPreferences.VOICE_ANNOUNCEMENTS_INTERVAL_TIME, npT.getValue())
                        .putFloat(UserPreferences.VOICE_ANNOUNCEMENTS_INTERVAL_DISTANCE, intervals[npD.getValue()])
                        .apply());

        d.create().show();
    }

    private void showPaceControlConfig() {
        DistanceUnitUtils distanceUtils = Instance.getInstance(getContext()).distanceUnitUtils;
        DistanceUnitSystem unitSystem = distanceUtils.getDistanceUnitSystem();
        distanceUtils.setUnit();

        final AlertDialog.Builder d = new AlertDialog.Builder(requireActivity());
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        final String unit = unitSystem.getSpeedUnit();

        d.setTitle(getString(R.string.pref_announcements_pace_control_title));
        View v = getLayoutInflater().inflate(R.layout.dialog_pace_control_range_picker, null);

        CheckBox enableLowerLimit = v.findViewById(R.id.enableLowerPaceLimit);
        CheckBox enableUpperLimit = v.findViewById(R.id.enableUpperPaceLimit);

        EditText lowerLimit = v.findViewById(R.id.lowerPaceLimitPicker);
        EditText upperLimit = v.findViewById(R.id.upperPaceLimitPicker);

        setupPaceControlFields(enableLowerLimit, lowerLimit);
        setupPaceControlFields(enableUpperLimit, upperLimit);

        ((TextView) v.findViewById(R.id.lowerPaceLimitUnit)).setText(unit);
        ((TextView) v.findViewById(R.id.upperPaceLimitUnit)).setText(unit);

        lowerLimit.setText(getSpeedString(userPreferences.getLowerTargetSpeedLimit(), unitSystem));
        upperLimit.setText(getSpeedString(userPreferences.getUpperTargetSpeedLimit(), unitSystem));

        enableLowerLimit.setChecked(userPreferences.hasLowerTargetSpeedLimit());
        enableUpperLimit.setChecked(userPreferences.hasUpperTargetSpeedLimit());

        d.setView(v);

        d.setNegativeButton(R.string.cancel, null);
        d.setPositiveButton(R.string.okay, (dialog, which) -> {
            final boolean enableLower = enableLowerLimit.isChecked();
            final boolean enableUpper = enableUpperLimit.isChecked();
            float lower = 0;
            float upper = 0;
            try {
                lower = enableLower ? getSpeed(lowerLimit, unitSystem) : 0;
                upper = enableUpper ? getSpeed(upperLimit, unitSystem) : Float.POSITIVE_INFINITY;
            } catch (ParseException e) {
                Toast.makeText(requireContext(), getString(R.string.invalidNumberFormat), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
            float upper_tmp = upper;
            upper = Math.max(lower, upper_tmp);
            lower = Math.min(lower, upper_tmp);
            userPreferences.setHasLowerTargetSpeedLimit(enableLower);
            userPreferences.setHasUpperTargetSpeedLimit(enableUpper);
            if (enableLower) {
                userPreferences.setLowerTargetSpeedLimit(lower);
            }
            if (enableUpper) {
                userPreferences.setUpperTargetSpeedLimit(upper);
            }
        });

        d.create().show();
    }

    private float getSpeed(EditText speed, DistanceUnitSystem unit) throws ParseException {
        Number value = NumberFormat.getInstance(Locale.getDefault()).parse(speed.getText().toString());
        return (float) unit.getMeterPerSecondFromSpeed(value.doubleValue());
    }

    private String getSpeedString(float speed, DistanceUnitSystem unit) {
        return String.format(
                Locale.getDefault(),
                "%.2f",
                unit.getSpeedFromMeterPerSecond(speed));
    }

    // Set the automatic focus, checked change, empty field depending on whether the checkbox
    // gets checked or the field is empty
    private void setupPaceControlFields(CheckBox checkBox, EditText limit) {
        // Thanks, dstibbe (https://stackoverflow.com/a/34256139/1458919)
        char separator = DecimalFormatSymbols.getInstance().getDecimalSeparator();
        limit.setKeyListener(DigitsKeyListener.getInstance("0123456789." + separator));
        if (!checkBox.isChecked()) {
            limit.setText("");
        }
        checkBox.setOnCheckedChangeListener((view, isChecked) -> {
            if (!isChecked) {
                limit.setText("");
            } else {
                limit.requestFocus();
            }
        });
        limit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {}

            @Override
            public void onTextChanged(CharSequence text, int start, int count, int after) {
                checkBox.setChecked(text.length() != 0);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    @Override
    protected String getTitle() {
        return getString(R.string.voiceAnnouncementsTitle);
    }
}
