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

import android.text.InputFilter;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;

public class NumberPickerUtils {

    /**
     * If custom formatting is used in the Android NumberPicker, it is blank unit the user touches it.
     * This method fixes it and can be called when creating NumberPickers
     */
    public static void fixNumberPicker(NumberPicker numberPicker) {
        View editView = numberPicker.getChildAt(0);

        if (editView instanceof EditText) {
            // Remove default input filter
            ((EditText) editView).setFilters(new InputFilter[0]);
        }
    }

}
