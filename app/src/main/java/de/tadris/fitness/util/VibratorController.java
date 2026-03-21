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


import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * This class provides an easier interface to the {@link Vibrator} class.
 *
 * It automatically disables the vibrator during calls, if configured in TTS preferences.
 */
public class VibratorController {
    private boolean enabled = true;  // always enabled
    private Vibrator vibrator;

    /**
     * Build an instance.
     *
     * @param context the context it should run in
     */
    public VibratorController(Context context) {
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        if (!vibrator.hasVibrator()) {
            enabled = false;
        }
    }

    /**
     * Vibrate for a certain amount of time.
     * @param millis how long do you want the phone to vibrate
     */
    public void vibrate(int millis) {
        if (!enabled) {
            return;
        }
        if (Build.VERSION.SDK_INT < 26) {
            vibrator.vibrate(millis);
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    /**
     * Stop vibrating.
     */
    public void cancel() {
        if (!enabled) {
            return;
        }
        vibrator.cancel();
    }
}
