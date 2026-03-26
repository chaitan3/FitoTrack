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

package de.tadris.fitness.recording.event;

import android.bluetooth.BluetoothDevice;

import java.util.List;

public class HeartRateChangeEvent {

    public final BluetoothDevice device;
    public final int heartRate;
    public final boolean contactDetected;
    public final int energyExpanded;
    public final List<Integer> rrIntervals;

    public HeartRateChangeEvent(BluetoothDevice device, int heartRate, boolean contactDetected, int energyExpanded, List<Integer> rrIntervals) {
        this.device = device;
        this.heartRate = heartRate;
        this.contactDetected = contactDetected;
        this.energyExpanded = energyExpanded;
        this.rrIntervals = rrIntervals;
    }

}
