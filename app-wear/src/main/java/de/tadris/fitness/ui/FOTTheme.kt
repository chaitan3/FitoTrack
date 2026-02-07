/*
 * Copyright (c) 2026 Bart Vullings <dev@windkracht8.com>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU General Public License as published by
 * 	the Free Software Foundation, either version 3 of the License, or
 * 	(at your option) any later version.
 *
 * 	FitoTrack is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 *
 * 	You should have received a copy of the GNU General Public License
 * 	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.tadris.fitness.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

val ColorFOT = Color(0xFFFB8C00)
val ColorWhite = Color(0xFFFFFFFF)
val ColorBlack = Color(0xFF000000)
val ColorTransparent = Color(0x00000000)
val ColorInactive = Color(0xFFAAAAAA)
val ColorProgressDone = Color(0xFF4CAF50)
val ColorProgressDoneDark = Color(0xFF244D26)
val ColorLocationCircle = Color(0xFF2A7FFF)
val ColorProgressBg = Color(0xFF444444)
val ColorProgressAhead = Color(0xFF008800)
val ColorProgressBehind = Color(0xFFCC0000)

internal val fotColorScheme = ColorScheme(
	primary = ColorWhite,
	onPrimary = ColorBlack,
	background = ColorBlack,
	onBackground = ColorWhite,
	outline = ColorFOT,
	onSurface = ColorWhite,
	error = Color(0xFF9E0000)
)

@Composable
fun FOTTheme(content: @Composable () -> Unit) {
	MaterialTheme(
		colorScheme = fotColorScheme,
		content = content
	)
}