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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import de.tadris.fitness.R;

/**
 * This class provides some common methods related to {@link NfcAdapter}.
 */
public class NfcAdapterHelper {
    private static final String TAG = "NfcAdapterHelper";

    private static NfcAdapter nfcAdapter;

    /**
     * Check if the device has NFC support.
     */
    public static boolean isNfcPresent(Context context) {
        return NfcAdapter.getDefaultAdapter(context) != null;
    }

    /**
     * Check if NFC is enabled in device settings.
     */
    public static boolean isNfcEnabled(Context context) {
        return NfcAdapter.getDefaultAdapter(context).isEnabled();
    }

    /**
     * Create a dialog asking the user to enable NFC in device settings.
     * @param context which context the dialog should be shown in
     * @return the dialog as {@link AlertDialog}
     */
    public static AlertDialog createNfcEnableDialog(Context context) {
        return new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_nfc_disabled_title)
                .setMessage(R.string.dialog_nfc_disabled)
                .setNegativeButton(R.string.cancel, (dialog, which) -> {} )
                .setPositiveButton(R.string.action_nfc_settings, (dialog, which) ->
                        context.startActivity(new Intent(Settings.ACTION_NFC_SETTINGS)))
                .create();
    }

    /**
     * Setup and enable the NFC foreground dispatch system for a given {@link Activity}.
     * @param target the target activity (must be running in foreground) that will receive the NFC
     *              intents
     * @return whether enabling was successful
     * @see #disableNfcForegroundDispatch(Activity)
     * @apiNote this method must only be called in the main thread and while the target activity is
     * in the foreground.<br>
     * Don't forget to call {@link #disableNfcForegroundDispatch(Activity)} before the target
     * activity's {@link Activity#onPause()} handler completes.
     */
    public static boolean enableNfcForegroundDispatch(Activity target) {
        // make sure NFC is supported by the device and enabled
        if (!isNfcPresent(target) || !isNfcEnabled(target)) {
            return false;
        }

        // let's use the default NFC adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(target);
        if (nfcAdapter == null) {
            return false;
        }

        // setup foreground dispatch system

        int flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pendingIntent = PendingIntent.getActivity(
                target, 0,
                new Intent(target, target.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                flag);
        try {
            // intercept everything (filters: null, techLists: null)
            nfcAdapter.enableForegroundDispatch(target, pendingIntent, null, null);
        } catch (IllegalStateException ex) {
            Log.e(TAG, "Failed to enable NFC foreground dispatcher. " +
                    "Activity is not in foreground.");
            return false;
        }
        return true;
    }

    /**
     * Disable the NFC foreground dispatch system for a given {@link Activity}.
     * @param target the target activity (must be running in foreground) that has NFC foreground
     *               dispatching enabled
     * @return whether disabling was successful
     * @see #enableNfcForegroundDispatch(Activity)
     * @apiNote you must call this method from main thread and before the target activity's
     * {@link Activity#onPause()} callback handler returns, if you previously called
     * {@link #enableNfcForegroundDispatch(Activity)}.
     */
    public static boolean disableNfcForegroundDispatch(Activity target) {
        if (nfcAdapter == null || !isNfcEnabled(target) || !isNfcPresent(target)) {
            return false;
        }

        try {
            nfcAdapter.disableForegroundDispatch(target);
        } catch (IllegalStateException ex) {
            Log.e(TAG, "Failed to disable NFC foreground dispatcher. " +
                    "Activity is not in foreground.");
            return false;
        }
        return true;
    }
}
