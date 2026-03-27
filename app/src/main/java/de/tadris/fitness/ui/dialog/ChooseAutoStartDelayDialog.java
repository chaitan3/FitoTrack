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
 * Build a dialog to choose the delay after which a workout will be started automatically
 */
public class ChooseAutoStartDelayDialog extends NumberPickerDialog<Integer> {

    private static final int NO_DELAY = 0;

    private static final int[] delaysS = {NO_DELAY, 5, 10, 20, 30, 45, 60, 90, 120, 180, 300, 600};

    private final AutoStartDelaySelectListener listener;
    private final int initialDelayS;

    /**
     * @param context       The context this dialog should be shown in
     * @param listener      The listener that is called when the user selects a delay
     * @param initialDelayMs Initially selected auto start delay in seconds
     */

    public ChooseAutoStartDelayDialog(Activity context, AutoStartDelaySelectListener listener,
                                      long initialDelayMs) {
        super(context, context.getString(R.string.pref_auto_start_delay_title),
                context.getString(R.string.customAutoStartDelay));
        this.listener = listener;
        this.initialDelayS = (int) initialDelayMs / 1_000;
    }

    @Override
    protected int getOptionCount() {
        return delaysS.length;
    }

    @Override
    protected Integer getInitOption() {
        return initialDelayS;
    }

    @Override
    protected String format(Integer delayS) {
        if (delayS < 60) {
            return delayS == NO_DELAY
                    ? context.getText(R.string.noAutoStartDelay).toString()
                    : delayS + " " + context.getText(R.string.timeSecondsShort);
        } else {
            return Instance.getInstance(context).distanceUnitUtils.getMinuteSecondTime(delayS * 1_000, true);
        }
    }

    @Override
    protected int toOptionNum(Integer delayS) {
        // if the user selected a custom delay, it might not be a perfect fit, so instead preselect
        // the next greater from the provided options
        int num = 0;
        for (int delay : delaysS) {
            if (delayS <= delay) {
                return num;
            }
            num++;
        }
        return delaysS.length - 1;
    }

    @Override
    protected Integer fromOptionNum(int optionNum) {
        if (optionNum < 0) {
            optionNum = 0;
        } else if (optionNum >= delaysS.length) {
            optionNum = delaysS.length - 1;
        }
        return delaysS[optionNum];
    }

    @Override
    protected void onSelectOption(Integer delayS) {
        listener.onSelectAutoStartDelay(delayS);
    }

    public interface AutoStartDelaySelectListener {
        /**
         * @param delayS Selected auto pause timeout in seconds
         */
        void onSelectAutoStartDelay(int delayS);
    }

    @Override
    protected void onNeutral() {
        new AutoStartDelayPickerDialogFragment(context,
                listener::onSelectAutoStartDelay, initialDelayS * 1_000).show();
    }
}