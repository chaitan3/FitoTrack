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

package de.tadris.fitness.ui.dialog;


import android.app.Activity;

import de.tadris.fitness.R;

/**
 * Build a dialog to choose the delay after which a workout will be started automatically
 */
public class AutoStartDelayPickerDialogFragment extends DurationPickerDialogFragment {

    /**
     * @param context           The context this dialog should be shown in
     * @param listener          The listener that is called when the user selects a delay
     * @param initialDelayMs    Initially selected auto start delay in milliseconds
     */
    public AutoStartDelayPickerDialogFragment(Activity context, AutoStartDelayPickListener listener,
                                              long initialDelayMs) {
        super(context, duration -> listener.onAutoStartDelayPick((int) (duration / 1000)),
                initialDelayMs, R.string.pref_auto_start_delay_title, false);
    }

    public interface AutoStartDelayPickListener {
        /**
         * @param delayS Selected auto start delay in seconds
         */
        void onAutoStartDelayPick(int delayS);
    }

}
