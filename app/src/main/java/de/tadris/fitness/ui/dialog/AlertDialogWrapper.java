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

import android.app.AlertDialog;

/**
 * This interface is intended to be used by FitoTracks dialogs as a common way of showing (and
 * accessing) them.
 */
public interface AlertDialogWrapper {

    /**
     * Creates and shows the dialog.
     */
    void show();

    /**
     * Get the underlying {@link AlertDialog} object.
     * @return Underlying dialog
     * @apiNote This function will return null before the dialog was {@link #show() created}.
     */
    AlertDialog getDialog();
}
