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
package de.tadris.fitness.ui.record

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

@Composable
fun RecordingWindowEffects(showOnLockScreen: Boolean) {
    val view = LocalView.current
    DisposableEffect(showOnLockScreen) {
        val activity = view.context as? Activity
        view.keepScreenOn = true
        if (showOnLockScreen) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                activity?.setShowWhenLocked(true)
                activity?.setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                activity?.window?.addFlags(
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
        }
        onDispose {
            view.keepScreenOn = false
            if (showOnLockScreen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                activity?.setShowWhenLocked(false)
                activity?.setTurnScreenOn(false)
            }
        }
    }
}
