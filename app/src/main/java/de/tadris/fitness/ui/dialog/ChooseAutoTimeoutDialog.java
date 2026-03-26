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

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;

/**
 * Build a dialog to choose the auto workout timeout after which the workout is automatically stopped
 */
public class ChooseAutoTimeoutDialog extends NumberPickerDialog<Integer> {
    private static final int STEP_WIDTH = 5;
    private static final int NO_TIMEOUT = 0;
    private static final int MAX_VALUE = 60;

    private final ChooseAutoTimeoutDialog.AutoTimeoutSelectListener listener;
    private final int initialTimeoutM;

    /**
     * @param context           The context this dialog should be shown in
     * @param listener          The listener that is called when the user selects a delay
     * @param initialTimeoutM   Initially selected timeout in minutes
     */
    public ChooseAutoTimeoutDialog(Activity context, ChooseAutoTimeoutDialog.AutoTimeoutSelectListener listener, int initialTimeoutM) {
        super(context, context.getString(R.string.pref_auto_timeout_title));
        this.listener = listener;
        this.initialTimeoutM = initialTimeoutM;
    }


    /**
     * Initialize dialog with default timeout from preferences.
     * @param context           The context this dialog should be shown in
     * @param listener          The listener that is called when the user selects a delay
     */
    public ChooseAutoTimeoutDialog(Activity context, ChooseAutoTimeoutDialog.AutoTimeoutSelectListener listener) {
        super(context, context.getString(R.string.pref_auto_timeout_title));
        this.listener = listener;
        this.initialTimeoutM = Instance.getInstance(context).userPreferences.getAutoTimeout();
    }

    @Override
    protected int getOptionCount() {
        return (MAX_VALUE - NO_TIMEOUT) / STEP_WIDTH + 1;
    }

    @Override
    protected Integer getInitOption() {
        return initialTimeoutM;
    }

    @Override
    protected String format(Integer delayM) {
        return delayM == NO_TIMEOUT
                ? context.getText(R.string.notimeout).toString()
                : delayM + " " + context.getText(R.string.timeMinuteShort);
    }

    @Override
    protected int toOptionNum(Integer delayM) {
        int res = delayM / STEP_WIDTH;
        if (res < 0) {
            return 0;
        } else if (res >= getOptionCount()) {
            return getOptionCount() - 1;
        } else {
            return res;
        }
    }

    @Override
    protected Integer fromOptionNum(int optionNum) {
        return optionNum * STEP_WIDTH;
    }

    @Override
    protected void onSelectOption(Integer delayM) {
        listener.onSelectAutoTimeout(delayM);
    }

    public interface AutoTimeoutSelectListener {
        /**
         * @param delayM Selected auto workout timeout in minutes
         */
        void onSelectAutoTimeout(int delayM);
    }
}
