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

import androidx.annotation.StringRes;

import de.tadris.fitness.R;
import de.tadris.fitness.util.NumberPickerUtils;

/**
 * Build a dialog to choose an arbitrary duration ([hh:]mm:ss)
 */
public class DurationPickerDialogFragment implements AlertDialogWrapper {

    public Activity context;
    public DurationPickListener listener;
    public long initialDuration;

    private AlertDialog dialog;
    private @StringRes int title = R.string.setDuration;
    private boolean showHours;

    /**
     * @param context           The context this dialog should be shown in
     * @param listener          The listener that is called when the user selects a duration
     * @param initialDuration   Initially selected duration in milliseconds
     *
     * @apiNote This constructor is provided for convenience reasons, it will use the default title
     * and make sure the hours picker is shown.
     * @see #DurationPickerDialogFragment(Activity, DurationPickListener, long, int, boolean)
     */
    public DurationPickerDialogFragment(Activity context, DurationPickListener listener, long initialDuration) {
        this.context = context;
        this.listener = listener;
        this.initialDuration = initialDuration;
        this.showHours = true;
    }

    /**
     *
     * @param context           The context this dialog should be shown in
     * @param listener          The listener that is called when the user selects a duration
     * @param initialDuration   Initially selected duration in milliseconds
     * @param title             The title this fragment should get
     * @param showHours         Whether hours picker should be shown
     */
    public DurationPickerDialogFragment(Activity context, DurationPickListener listener,
                                        long initialDuration, @StringRes int title, boolean showHours) {
        this.context = context;
        this.listener = listener;
        this.initialDuration = initialDuration;
        this.title = title;
        this.showHours = showHours;
    }

    /**
     * Show the duration picker dialog.
     */
    public void show() {
        final AlertDialog.Builder d = new AlertDialog.Builder(context);
        d.setTitle(title);
        View v = context.getLayoutInflater().inflate(R.layout.dialog_duration_picker, null);
        NumberPicker hours = v.findViewById(R.id.durationPickerHours);
        hours.setFormatter(value -> value + " " + context.getString(R.string.timeHourShort));
        hours.setMinValue(0);
        hours.setMaxValue(24);

        if (showHours) {
            hours.setValue(getInitialHours());
            hours.setVisibility(View.VISIBLE);
            v.findViewById(R.id.hoursSeparator).setVisibility(View.VISIBLE);
        } else {
            hours.setValue(0);
            hours.setVisibility(View.GONE);
            v.findViewById(R.id.hoursSeparator).setVisibility(View.GONE);
        }
        NumberPickerUtils.fixNumberPicker(hours);

        NumberPicker minutes = v.findViewById(R.id.durationPickerMinutes);
        minutes.setFormatter(value -> value + " " + context.getString(R.string.timeMinuteShort));
        minutes.setMinValue(0);
        minutes.setMaxValue(59);
        minutes.setValue(getInitialMinutes());
        NumberPickerUtils.fixNumberPicker(minutes);

        NumberPicker seconds = v.findViewById(R.id.durationPickerSeconds);
        seconds.setFormatter(value -> value + " " + context.getString(R.string.timeSecondsShort));
        seconds.setMinValue(0);
        seconds.setMaxValue(59);
        seconds.setValue(getInitialSeconds());
        NumberPickerUtils.fixNumberPicker(seconds);

        d.setView(v);

        d.setNegativeButton(R.string.cancel, null);
        d.setPositiveButton(R.string.okay, (dialog, which) ->
                listener.onDurationPick(getMillisFromPick(hours.getValue(), minutes.getValue(),
                        seconds.getValue())));

        dialog = d.create();
        dialog.show();
    }

    /**
     * Get the underlying dialog instance.
     */
    public AlertDialog getDialog() {
        return dialog;
    }

    private int getInitialHours() {
        return (int) (initialDuration / 1000 / 60 / 60);
    }

    private int getInitialMinutes() {
        return (int) (initialDuration / 1000 / 60 % 60);
    }

    private int getInitialSeconds() {
        return (int) (initialDuration / 1000 % 60);
    }

    private static long getMillisFromPick(int hours, int minutes, int seconds) {
        long secondInMillis = 1000L;
        long minuteInMillis = secondInMillis * 60;
        long hourInMillis = minuteInMillis * 60;
        return hours * hourInMillis + minutes * minuteInMillis + seconds * secondInMillis;
    }

    public interface DurationPickListener {
        /**
         * @param duration Selected duration in milliseconds
         */
        void onDurationPick(long duration);
    }

}
