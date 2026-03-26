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
import android.app.AlertDialog;
import android.view.View;
import android.widget.NumberPicker;

import de.tadris.fitness.R;
import de.tadris.fitness.util.NumberPickerUtils;

/**
 * Base class for building a number picker dialog
 */
public abstract class NumberPickerDialog<T> implements AlertDialogWrapper {

    protected final Activity context;
    private final String title;
    private AlertDialog dialog;
    private boolean useNeutral;
    private String neutralText;

    /**
     *
     * @param context The context this dialog should be shown in
     * @param title The dialog's title
     */
    public NumberPickerDialog(Activity context, String title) {
        this.context = context;
        this.title = title;
        this.useNeutral = false;
    }

    /**
     * Using this constructor will add the neutral button to the dialog.<p>
     * Make sure to overwrite {@link #onNeutral()}, if you need to do something in this case.
     * @param context       The context this dialog should be shown in
     * @param title         The dialog's title
     * @param neutralText   The neutral button's text
     */
    public NumberPickerDialog(Activity context, String title, String neutralText) {
        this.context = context;
        this.title = title;
        this.useNeutral = true;
        this.neutralText = neutralText;
    }

    /**
     * Show the number picker dialog.
     */
    public void show() {
        final AlertDialog.Builder d = new AlertDialog.Builder(context);
        d.setTitle(title);
        View v = context.getLayoutInflater().inflate(R.layout.dialog_auto_timeout_picker, null);

        NumberPicker npT = v.findViewById(R.id.autoTimeoutPicker);
        npT.setMaxValue(getOptionCount() - 1);
        npT.setMinValue(0);
        npT.setFormatter(value -> format(fromOptionNum(value)));
        npT.setValue(toOptionNum(getInitOption()));
        npT.setWrapSelectorWheel(false);
        NumberPickerUtils.fixNumberPicker(npT);

        d.setView(v);

        d.setNegativeButton(R.string.cancel, null);
        if (useNeutral) {
            d.setNeutralButton(neutralText, (dialog, which) -> onNeutral());
        }
        d.setPositiveButton(R.string.okay, (dialog, which) ->
                onSelectOption(fromOptionNum(npT.getValue())));
        dialog = d.create();
        dialog.show();
    }

    /**
     * Get the dialog instance.
     */
    public AlertDialog getDialog() {
        return dialog;
    }

    /**
     * Get the amount of different options to be displayed
     */
    protected abstract int getOptionCount();

    /**
     * Get the initially selected option
     */
    protected abstract T getInitOption();

    /**
     * Specify how a specific option should be presented to the user
     * @param option the option that should be displayed
     * @return the string that will be shown for this option
     */
    protected abstract String format(T option);

    /**
     * Get the option number in the dialog for a specific option
     * @return option num (must be in [0, {@link #getOptionCount()} - 1])
     * @see #fromOptionNum(int)
     */
    protected abstract int toOptionNum(T option);

    /**
     * Get the original option for a specific option number
     * @see #toOptionNum(Object)
     */
    protected abstract T fromOptionNum(int optionNum);

    /**
     * Provide the selected option
     */
    protected abstract void onSelectOption(T option);

    /**
     * Will be called, when the neutral button gets pressed
     */
    protected void onNeutral() { }
}