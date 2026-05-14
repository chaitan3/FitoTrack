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

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.ui.FitoTrackActivity;

public class FitoTrackSettingsActivity extends FitoTrackActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    public static final String EXTRA_FRAGMENT = "de.tadris.fitness.EXTRA_FRAGMENT";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupActionBar();

        setTitle(R.string.settings);

        if (savedInstanceState == null) {
            String fragmentClassName = getIntent().getStringExtra(EXTRA_FRAGMENT);
            if (fragmentClassName != null) {
                Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                        getClassLoader(), fragmentClassName);
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.settings_container, fragment)
                        .commit();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Instance.getInstance(this).distanceUnitUtils.setUnit();
        Instance.getInstance(this).energyUnitUtils.setUnit();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_container, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }

}
