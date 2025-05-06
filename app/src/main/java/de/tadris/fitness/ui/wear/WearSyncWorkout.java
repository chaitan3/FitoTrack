/*
 * Copyright (c) 2025 Jannis Scheibe <jannis@tadris.de>
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
package de.tadris.fitness.ui.wear;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.tadris.fitness.R;

class WearSyncWorkout extends LinearLayout{
    boolean done;
    final long workoutId;
    private final TextView wearSyncWorkoutStatus;

    WearSyncWorkout(Context context, String name, long workoutId, boolean exists){
        super(context);
        this.workoutId = workoutId;
        done = exists;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_wear_sync_workout, this, true);
        ((TextView)findViewById(R.id.wearSyncWorkoutName)).setText(name);
        wearSyncWorkoutStatus = findViewById(R.id.wearSyncWorkoutStatus);
        if(exists) wearSyncWorkoutStatus.setText(R.string.wear_status_done);
    }

    void setDownloading(int length){
        wearSyncWorkoutStatus.setText(getContext()
                .getString(R.string.wear_status_downloading, String.valueOf(length / 1000))
        );
    }
    void setProcessing(){wearSyncWorkoutStatus.setText(R.string.wear_status_processing);}
    void setDone(){
        done = true;
        wearSyncWorkoutStatus.setText(R.string.wear_status_done);
    }
    void setFail(){
        done = true;
        wearSyncWorkoutStatus.setText(R.string.wear_status_fail);
    }
}
