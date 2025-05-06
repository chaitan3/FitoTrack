/*
 * Copyright (c) 2025 Bart Vullings <dev@windkracht8.com>
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

package de.tadris.fitness.ui.record;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.tadris.fitness.R;

public class ScreenCurrent extends Screen{
    @Override
    public @Nullable View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ){
        View rootView = inflater.inflate(R.layout.record_screen_current, container, false);
        time = rootView.findViewById(R.id.time);
        time.setText(recordActivity.currentTime);

        speed = rootView.findViewById(R.id.speed);
        speed.setType(ScreenField.Type.SPEED_KMH);

        duration = rootView.findViewById(R.id.duration);
        duration.setType(ScreenField.Type.DURATION);

        distance = rootView.findViewById(R.id.distance);
        distance.setType(ScreenField.Type.DISTANCE_KM);

        heartRate = rootView.findViewById(R.id.heartRate);
        heartRate.setType(ScreenField.Type.HEART_RATE_BPM);

        targetProgress = rootView.findViewById(R.id.targetProgress);
        return rootView;
    }
}