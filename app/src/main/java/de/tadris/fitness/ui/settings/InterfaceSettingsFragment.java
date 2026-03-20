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

package de.tadris.fitness.ui.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.data.preferences.UserPreferences;
import de.tadris.fitness.util.NumberPickerUtils;
import de.tadris.fitness.util.unit.DistanceUnitSystem;

public class InterfaceSettingsFragment extends FitoTrackSettingFragment {

    private SharedPreferences sharedPreferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        setPreferencesFromResource(R.xml.preferences_user_interface, rootKey);

        // Update the settings UI to reflect the current language setting
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String str = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        if (str.isEmpty()) {
            editor.putString("language", "1");
            ((ListPreference) findPreference("language")).setValue("1");
        } else {
            for (String value : getResources().getStringArray(R.array.pref_language_values)) {
                if (str.startsWith(value)) {
                    editor.putString("language", value);
                    ((ListPreference) findPreference("language")).setValue(value);
                    break;
                }
            }
        }
        editor.apply();

        bindPreferenceSummaryToValue(findPreference("language"));
        bindPreferenceSummaryToValue(findPreference("unitSystem"));
        bindPreferenceSummaryToValue(findPreference("mapStyle"));
        bindPreferenceSummaryToValue(findPreference("trackStyle"));
        bindPreferenceSummaryToValue(findPreference("trackStyleUsage"));
        bindPreferenceSummaryToValue(findPreference("themeSetting"));
        bindPreferenceSummaryToValue(findPreference("dateFormat"));
        bindPreferenceSummaryToValue(findPreference("timeFormat"));
        bindPreferenceSummaryToValue(findPreference("firstDayOfWeek"));
        bindPreferenceSummaryToValue(findPreference("energyUnit"));

        findPreference("language").setOnPreferenceChangeListener((preference, newValue) -> {
            if (newValue.equals("1")) {  // System default
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.create());
            } else {
                AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(newValue.toString())
                );
            }
            return true;
        });

        findPreference("themeSetting").setOnPreferenceChangeListener((preference, newValue) -> {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, newValue);
            Toast.makeText(requireContext(), R.string.hintRestart, Toast.LENGTH_LONG).show();
            return true;
        });

        findPreference("weight").setOnPreferenceClickListener(preference -> {
            showWeightPicker();
            return true;
        });
        findPreference(UserPreferences.STEP_LENGTH).setOnPreferenceClickListener(preference -> {
            showStepLengthPicker();
            return true;
        });
        refreshStepLengthSummary();

        Preference mapFilePref = findPreference("offlineMapDirectoryName");
        bindPreferenceSummaryToValue(mapFilePref);
        mapFilePref.setOnPreferenceClickListener(preference -> {
            showFilePicker();
            return true;
        });

        findPreference("offlineMapDownload").setOnPreferenceClickListener(preference -> {
            openMapDownloader();
            return true;
        });
    }

    private void showWeightPicker() {
        Instance.getInstance(getContext()).distanceUnitUtils.setUnit(); // Maybe the user changed unit system
        DistanceUnitSystem unitSystem = Instance.getInstance(getContext()).distanceUnitUtils.getDistanceUnitSystem();

        final AlertDialog.Builder d = new AlertDialog.Builder(requireActivity());
        d.setTitle(getString(R.string.pref_weight));
        View v = getLayoutInflater().inflate(R.layout.dialog_picker, null);
        NumberPicker np = v.findViewById(R.id.picker);
        np.setMaxValue((int) unitSystem.getWeightFromKilogram(400));
        np.setMinValue((int) unitSystem.getWeightFromKilogram(20));
        np.setFormatter(value -> value + " " + unitSystem.getWeightUnit());
        final String preferenceVariable = "weight";
        np.setValue((int) Math.round(unitSystem.getWeightFromKilogram(sharedPreferences.getInt(preferenceVariable, 80))));
        np.setWrapSelectorWheel(false);
        NumberPickerUtils.fixNumberPicker(np);

        d.setView(v);

        d.setNegativeButton(R.string.cancel, null);
        d.setPositiveButton(R.string.okay, (dialog, which) -> {
            int unitValue = np.getValue();
            int kilograms = (int) Math.round(unitSystem.getKilogramFromUnit(unitValue));
            sharedPreferences.edit().putInt(preferenceVariable, kilograms).apply();
        });

        d.create().show();
    }

    private void showStepLengthPicker() {
        UserPreferences preferences = Instance.getInstance(getContext()).userPreferences;
        Instance.getInstance(getContext()).distanceUnitUtils.setUnit(); // Maybe the user changed unit system
        DistanceUnitSystem unitSystem = Instance.getInstance(getContext()).distanceUnitUtils.getDistanceUnitSystem();

        final AlertDialog.Builder d = new AlertDialog.Builder(requireActivity());
        d.setTitle(getString(R.string.pref_step_length));
        View v = getLayoutInflater().inflate(R.layout.dialog_picker, null);
        NumberPicker np = v.findViewById(R.id.picker);
        np.setMaxValue((int) unitSystem.getDistanceFromCentimeters(150));
        np.setMinValue((int) unitSystem.getDistanceFromCentimeters(50));
        np.setFormatter(value -> value + " " + unitSystem.getReallyShortDistanceUnit());
        np.setValue((int) Math.round(unitSystem.getDistanceFromCentimeters(preferences.getStepLength() * 100)));
        np.setWrapSelectorWheel(false);
        NumberPickerUtils.fixNumberPicker(np);

        d.setView(v);

        d.setNegativeButton(R.string.cancel, null);
        d.setPositiveButton(R.string.okay, (dialog, which) -> {
            int unitValue = np.getValue();
            double meters = unitSystem.getCentimetersFromReallyShortDistance(unitValue) / 100;
            preferences.setStepLength((float) meters);
            refreshStepLengthSummary();
        });

        d.create().show();
    }

    private void refreshStepLengthSummary() {
        Instance instance = Instance.getInstance(getContext());
        DistanceUnitSystem unitSystem = instance.distanceUnitUtils.getDistanceUnitSystem();
        String summary = Math.round(unitSystem.getDistanceFromCentimeters(instance.userPreferences.getStepLength() * 100)) + " " + unitSystem.getReallyShortDistanceUnit();
        findPreference(UserPreferences.STEP_LENGTH).setSummary(summary);
    }

    private static final int FOLDER_IMPORT_SELECT_CODE = 1;

    private void showFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, FOLDER_IMPORT_SELECT_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == FOLDER_IMPORT_SELECT_CODE) {
            Uri uri = data.getData();
            requireActivity().getContentResolver().takePersistableUriPermission(data.getData(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            sharedPreferences.edit().putString("offlineMapDirectoryName", uri.toString()).apply();
            findPreference("offlineMapDirectoryName").setSummary(uri.toString());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void openMapDownloader() {
        String mapFileName = Instance.getInstance(getContext()).userPreferences.getOfflineMapDirectoryName();
        if (mapFileName != null && DocumentFile.fromTreeUri(requireContext(), Uri.parse(mapFileName)).canWrite()) {
            startActivity(new Intent(requireContext(), DownloadMapsActivity.class));
        } else {
            Toast.makeText(requireContext(), R.string.downloadMapsSpecifyDirectory, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected String getTitle() {
        return getString(R.string.preferencesUserInterfaceTitle);
    }
}
