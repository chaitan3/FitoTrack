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

package de.tadris.fitness.util;

import android.app.UiModeManager;
import android.content.Context;
import android.preference.PreferenceManager;

import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatDelegate;

import de.tadris.fitness.R;
import de.tadris.fitness.data.WorkoutType;
import de.tadris.fitness.data.WorkoutTypeManager;

public class FitoTrackThemes {

    private static final int THEME_SETTING_AUTO = 0;
    private static final int THEME_SETTING_LIGHT = 2;
    private static final int THEME_SETTING_DARK = 1;

    private final Context context;

    public FitoTrackThemes(Context context) {
        this.context = context;
    }

    @StyleRes
    public int getDefaultTheme() {
        return R.style.AppTheme;
    }

    @StyleRes
    public int getWorkoutTypeTheme(WorkoutType type) {
        switch (type.id) {
            case WorkoutTypeManager.WORKOUT_TYPE_ID_RUNNING:
            case WorkoutTypeManager.WORKOUT_TYPE_ID_TREADMILL:
                return R.style.Running;
            case WorkoutTypeManager.WORKOUT_TYPE_ID_HIKING:
                return R.style.Hiking;
            case WorkoutTypeManager.WORKOUT_TYPE_ID_CYCLING:
                return R.style.Bicycling;
            case WorkoutTypeManager.WORKOUT_TYPE_ID_SKATEBOARDING:
            case WorkoutTypeManager.WORKOUT_TYPE_ID_INLINE_SKATING:
                return R.style.Skating;
            case WorkoutTypeManager.WORKOUT_TYPE_ID_ROWING:
            case WorkoutTypeManager.WORKOUT_TYPE_ID_SWIMMING:
                return R.style.WaterSports;
            default:
                return R.style.AppTheme;
        }
    }

    public void updateDarkModeSetting() {
        int themeSetting = getThemeSetting();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
            int uiMode = UiModeManager.MODE_NIGHT_AUTO;
            if (themeSetting == THEME_SETTING_LIGHT) {
                uiMode = UiModeManager.MODE_NIGHT_NO;
            } else if (themeSetting == THEME_SETTING_DARK) {
                uiMode = UiModeManager.MODE_NIGHT_YES;
            }
            uiModeManager.setApplicationNightMode(uiMode);
        } else {
            int mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (themeSetting == THEME_SETTING_LIGHT) {
                mode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (themeSetting == THEME_SETTING_DARK) {
                mode = AppCompatDelegate.MODE_NIGHT_YES;
            }
            AppCompatDelegate.setDefaultNightMode(mode);
        }
    }

    private int getThemeSetting() {
        String setting = PreferenceManager.getDefaultSharedPreferences(context).getString("themeSetting", String.valueOf(THEME_SETTING_LIGHT));
        assert setting != null;
        return Integer.parseInt(setting);
    }

}
