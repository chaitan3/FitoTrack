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

package de.tadris.fitness;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import de.tadris.fitness.data.AppDatabase;
import de.tadris.fitness.data.preferences.UserPreferences;
import de.tadris.fitness.util.DataManager;
import de.tadris.fitness.util.FitoTrackThemes;
import de.tadris.fitness.util.ShortcutsUtils;
import de.tadris.fitness.util.UserDateTimeUtils;
import de.tadris.fitness.util.WorkoutLogger;
import de.tadris.fitness.util.autoexport.AutoExportPlanner;
import de.tadris.fitness.util.unit.DistanceUnitUtils;
import de.tadris.fitness.util.unit.EnergyUnitUtils;

public class Instance {

    private static Instance instance;

    /**
     * Please use getInstance with context. Without context could return null if no Instance was created
     */
    @Nullable
    @Deprecated
    public static Instance getInstance() {
        return getInstance(null);
    }

    public static Instance getInstance(Context context) {
        if (context == null) {
            Log.w("Instance", "no Context Provided");
        }
        if (instance == null && context != null) {
            instance = new Instance(context);
        }
        return instance;
    }

    public final AppDatabase db;
    public final UserPreferences userPreferences;
    public final FitoTrackThemes themes;
    public final UserDateTimeUtils userDateTimeUtils;
    public final DistanceUnitUtils distanceUnitUtils;
    public final EnergyUnitUtils energyUnitUtils;
    public final AutoExportPlanner planner;
    public final WorkoutLogger logger;
    public final ShortcutsUtils shortcuts;

    private Instance(Context context) {
        instance = this;
        userPreferences = new UserPreferences(context);
        themes = new FitoTrackThemes(context);
        userDateTimeUtils = new UserDateTimeUtils(userPreferences);
        distanceUnitUtils = new DistanceUnitUtils(context);
        energyUnitUtils = new EnergyUnitUtils(context);
        db = AppDatabase.provideDatabase(context);
        planner = new AutoExportPlanner(context);
        logger = new WorkoutLogger(context);
        shortcuts = new ShortcutsUtils(context);

        startBackgroundClean(context);
    }

    private void startBackgroundClean(Context context) {
        DataManager.cleanFilesASync(context);
    }

}
