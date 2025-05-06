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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import de.tadris.fitness.R;
import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.ui.Main;

public class Prepare extends Fragment{
    private ImageView type;
    private ImageView location;
    private ImageView heartRate;
    private ImageView target;
    private RecordActivity recordActivity;

    @Override public void onAttach(@NonNull Context context){
        super.onAttach(context);
        try{
            recordActivity = (RecordActivity) context;
        }catch(ClassCastException e){
            Log.e(Main.LOG_TAG, "PrepareScreen.onAttach exception: " + e.getMessage());
        }
    }
    @Override public @Nullable View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ){
        View rootView = inflater.inflate(R.layout.record_prepare, container, false);
        type = rootView.findViewById(R.id.type);
        type.setImageResource(RecordActivity.workoutData.getWorkoutTypeIcon());
        type.setOnClickListener(v->startActivity(new Intent(getContext(), TypeSelectActivity.class)));

        target = rootView.findViewById(R.id.target);
        target.setOnClickListener(v->startActivity(new Intent(getContext(), TargetActivity.class)));

        rootView.findViewById(R.id.start).setOnClickListener(v->recordActivity.startExercise());

        location = rootView.findViewById(R.id.location);
        if(!RecordActivity.locationEnabled || !RecordActivity.hasLocationPermission){
            location.setColorFilter(rootView.getContext().getColor(R.color.stateFailed));
        }

        heartRate = rootView.findViewById(R.id.heartRate);
        if(!RecordActivity.hasHRPermission){
            heartRate.setColorFilter(rootView.getContext().getColor(R.color.stateFailed));
        }

        rootView.findViewById(R.id.settings).setOnClickListener(v->startActivity(new Intent(getContext(), SettingsActivity.class)));

        if(Main.isScreenRound){
            type.setPadding(Main.vw20, 0, 0, 0);
            target.setPadding(0, 0, Main.vw20, 0);
        }
        return rootView;
    }
    @Override public void onResume(){
        type.setImageResource(RecordActivity.workoutData.getWorkoutTypeIcon());
        if(RecordActivity.workoutData.targetType == WorkoutData.TargetType.NONE
                || RecordActivity.workoutData.targetValue == 0){
            target.setImageResource(R.drawable.ic_flag_unset);
        }else{
            target.setImageResource(R.drawable.ic_flag);
        }
        super.onResume();
    }

    void locationPermissionGranted(){location.setColorFilter(Screen.colorActive);}
    void locationDisabled(){location.setColorFilter(Screen.colorInactive);}
    void locationAvailable(){location.setImageResource(R.drawable.ic_gps_fixed);}
    void locationUnavailable(){location.setImageResource(R.drawable.ic_gps_no_fix);}
    void heartRatePermissionGranted(){heartRate.setColorFilter(Screen.colorActive);}
    void heartRateAvailable(){heartRate.setImageResource(R.drawable.ic_heart_fixed);}
    void heartRateUnavailable(){heartRate.setImageResource(R.drawable.ic_heart_no_fix);}

}
