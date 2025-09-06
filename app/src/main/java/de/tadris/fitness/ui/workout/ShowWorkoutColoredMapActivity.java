/*
 * Copyright (c) 2021 Jannis Scheibe <jannis@tadris.de>
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

package de.tadris.fitness.ui.workout;

import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import java.util.HashMap;
import java.util.Map;

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.data.preferences.UserPreferences;
import de.tadris.fitness.ui.workout.diagram.ConverterManager;
import de.tadris.fitness.ui.workout.diagram.SampleConverter;

public abstract class ShowWorkoutColoredMapActivity extends MapWorkoutActivity {

    protected ConverterManager converterManager;

    private MenuItem autoColoring, noColoring;
    private final Map<MenuItem, SampleConverter> converterMenu = new HashMap<>();

    private static final int COLORING_PROPERTY_AUTO = 0;
    private static final int COLORING_PROPERTY_NONE = 1;
    private static final int COLORING_PROPERTY_CUSTOM = 2;

    private SampleConverter coloringConverter;
    private int coloringPropertyMode = COLORING_PROPERTY_AUTO;

    @Override
    void initBeforeContent() {
        super.initBeforeContent();
        converterManager = new ConverterManager(this, data);

        String coloringMode = Instance.getInstance(this).userPreferences.getTrackStyleMode();
        if (coloringMode.equals(UserPreferences.STYLE_USAGE_ALWAYS) || (isDiagramActivity() && coloringMode.equals(UserPreferences.STYLE_USAGE_DIAGRAM))) {
            coloringPropertyMode = COLORING_PROPERTY_AUTO;
        } else {
            coloringPropertyMode = COLORING_PROPERTY_NONE;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshColoring();
    }

    protected abstract boolean isDiagramActivity();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.workout_map_menu, menu);

        SubMenu coloringMenu = menu.findItem(R.id.actionSelectColoring).getSubMenu();

        autoColoring = coloringMenu.add(R.string.auto);
        noColoring = coloringMenu.add(R.string.noColoring);
        for (int i = 0; i < converterManager.availableConverters.size(); i++) {
            SampleConverter converter = converterManager.availableConverters.get(i);
            MenuItem item = coloringMenu.add(R.id.actionSelectColoring, Menu.NONE, i, converter.getName());
            converterMenu.put(item, converter);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item == autoColoring) {
            coloringPropertyMode = COLORING_PROPERTY_AUTO;
            refreshColoring();
            return true;
        } else if (item == noColoring) {
            coloringPropertyMode = COLORING_PROPERTY_NONE;
            refreshColoring();
            return true;
        } else if (converterMenu.containsKey(item)) {
            coloringPropertyMode = COLORING_PROPERTY_CUSTOM;
            coloringConverter = converterMenu.get(item);
            refreshColoring();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void refreshColoring() {
        SampleConverter converter = null;
        if (coloringPropertyMode == COLORING_PROPERTY_AUTO) {
            if (!converterManager.selectedConverters.isEmpty()) {
                converter = converterManager.selectedConverters.get(0);
            } else {
                converter = converterManager.availableConverters.get(0);
            }
        } else if (coloringPropertyMode == COLORING_PROPERTY_CUSTOM) {
            converter = coloringConverter;
        }
        workoutLayer.setSampleConverter(data, converter);
    }
}