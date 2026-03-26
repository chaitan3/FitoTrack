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

package de.tadris.fitness.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

object PermissionUtils {

    @JvmStatic
    fun checkStoragePermissions(context: Context, alsoWritePermission: Boolean = true) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            // does not need storage permission anymore (scoped storage)
            true
        } else {
            checkPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    && (!alsoWritePermission || checkPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }

    @JvmStatic
    fun checkPermission(context: Context, permission: String) =
        ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

}