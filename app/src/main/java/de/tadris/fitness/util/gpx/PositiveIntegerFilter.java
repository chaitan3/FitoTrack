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
package de.tadris.fitness.util.gpx;

/**
 * A filter object, that will exclude negative values during jackson serialization.
 * <p>
 * Adapted from https://stackoverflow.com/a/56493905
 */
public class PositiveIntegerFilter {
    public PositiveIntegerFilter() {
    }

    @Override
    public boolean equals(final Object other) {
        // Trick required to be compliant with the Jackson Custom attribute processing
        if (other == null) {
            return true;
        }
        int value = (Integer) other;
        return value < 0;
    }
}
