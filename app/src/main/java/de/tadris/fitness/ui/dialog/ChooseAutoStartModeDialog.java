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
import de.tadris.fitness.recording.autostart.AutoStartWorkout;

public class ChooseAutoStartModeDialog extends NumberPickerDialog<AutoStartWorkout.Mode> {
    private final AutoStartModeSelectListener listener;
    private final AutoStartWorkout.Mode initialMode;

    /**
     * @param context       The context this dialog should be shown in
     * @param listener      The listener that is called when the user selects a delay
     * @param initialMode Initially selected auto start delay in seconds
     */
    public ChooseAutoStartModeDialog(Activity context,
                                     AutoStartModeSelectListener listener,
                                     AutoStartWorkout.Mode initialMode) {
        super(context, context.getString(R.string.pref_auto_start_mode_title));
        this.listener = listener;
        this.initialMode = initialMode;
    }

    @Override
    protected int getOptionCount() {
        return AutoStartWorkout.Mode.values().length;
    }

    @Override
    protected AutoStartWorkout.Mode getInitOption() {
        return initialMode;
    }

    @Override
    protected String format(AutoStartWorkout.Mode mode) {
        switch (mode) {
            case INSTANT:
                return context.getString(R.string.pref_auto_start_mode_instant);
            case ON_MOVE:
                return context.getString(R.string.pref_auto_start_mode_on_move);
            case WAIT_FOR_GPS:
                return context.getString(R.string.pref_auto_start_mode_wait_for_gps);
            default:
                return context.getString(R.string.pref_auto_start_mode_unknown);
        }
    }

    @Override
    protected int toOptionNum(AutoStartWorkout.Mode mode) {
        // this is okay here, no ones going to change the order of mode enums while the dialog is
        // shown ;-)
        return mode.ordinal();
    }

    @Override
    protected AutoStartWorkout.Mode fromOptionNum(int optionNum) {
        return AutoStartWorkout.Mode.values()[optionNum];
    }

    @Override
    protected void onSelectOption(AutoStartWorkout.Mode mode) {
        listener.onSelectAutoStartMode(mode);
    }

    public interface AutoStartModeSelectListener {
        /**
         * @param mode Selected auto start mode
         */
        void onSelectAutoStartMode(AutoStartWorkout.Mode mode);
    }
}
