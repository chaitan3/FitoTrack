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

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.health.services.client.data.ExerciseState;

import de.tadris.fitness.R;
import de.tadris.fitness.ui.Main;

public class ScreenStop extends Screen{
    private ImageView pauseResume;

    @Override public @Nullable View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ){
        View rootView = inflater.inflate(R.layout.record_screen_stop, container, false);

        time = rootView.findViewById(R.id.time);
        time.setText(recordActivity.currentTime);

        if(RecordActivity.workoutData.isWorkoutTypeGps()){
            distance = rootView.findViewById(R.id.topField);
            distance.setType(ScreenField.Type.DISTANCE_KM);
        }else{
            heartRate = rootView.findViewById(R.id.topField);
            heartRate.setType(ScreenField.Type.HEART_RATE_BPM);
        }

        duration = rootView.findViewById(R.id.duration);
        duration.setType(ScreenField.Type.DURATION);
        duration.setCenter();

        pause = rootView.findViewById(R.id.pause);
        pause.setType(ScreenField.Type.PAUSE);

        pauseResume = rootView.findViewById(R.id.pauseResume);
        pauseResume.setOnClickListener(v->pauseResumeClicked());

        ImageView end = rootView.findViewById(R.id.end);
        end.setOnClickListener(v->recordActivity.endExercise());

        rootView.findViewById(R.id.settings).setOnClickListener(v->startActivity(new Intent(getContext(), SettingsActivity.class)));

        targetProgress = rootView.findViewById(R.id.targetProgress);

        if(Main.isScreenRound){
            time.setPadding(Main.vw10, 0, Main.vw10, 0);
            if(distance != null) distance.setPadding(0, 0, Main.vw10, 0);
            if(heartRate != null) heartRate.setPadding(0, 0, Main.vw10, 0);
            duration.setPadding(Main.vw10, 0, Main.vw10, 0);
            pause.setPadding(Main.vw10, 0, 0, 0);
            pauseResume.setPadding(Main.vw10, 0, 0, 0);
            end.setPadding(0, 0, Main.vw10, 0);
        }
        return rootView;
    }

    private void pauseResumeClicked(){
        recordActivity.pauseResumeClicked();
        pauseResume.setEnabled(false);
    }

    @Override void updateState(ExerciseState exerciseState){
        super.updateState(exerciseState);
        if(exerciseState.equals(ExerciseState.ACTIVE)){
            pauseResume.setImageResource(R.drawable.ic_pause);
            pauseResume.setEnabled(true);
        }else if(exerciseState.isPaused()){
            pauseResume.setImageResource(R.drawable.ic_resume);
            pause.setVisibility(View.VISIBLE);
            duration.setPadding(0, 0, Main.vw10, 0);
            pauseResume.setEnabled(true);
        }
    }
}