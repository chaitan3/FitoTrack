package de.tadris.fitness.ui.record;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.tadris.fitness.R;
import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.ui.Main;

public class ScreenTotal extends Screen{
    @Override
    public @Nullable View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ){
        View rootView = inflater.inflate(R.layout.record_screen_total, container, false);
        time = rootView.findViewById(R.id.time);
        time.setText(recordActivity.currentTime);

        speedAvg = rootView.findViewById(R.id.speedAvg);
        speedAvg.setType(ScreenField.Type.SPEED_KMH);
        speedAvg.setUnitAverage();

        eleGain = rootView.findViewById(R.id.eleGain);
        eleGain.setType(ScreenField.Type.ELEVATION);
        eleGain.setUnitIcon(R.drawable.ic_uphill);

        eleLoss = rootView.findViewById(R.id.eleLoss);
        eleLoss.setType(ScreenField.Type.ELEVATION);
        eleLoss.setUnitIcon(R.drawable.ic_downhill);

        calories = rootView.findViewById(R.id.calories);
        calories.setType(ScreenField.Type.CALORIES);

        targetProgress = rootView.findViewById(R.id.targetProgress);
        if(Main.isScreenRound && RecordActivity.workoutData.targetType != WorkoutData.TargetType.NONE){
            calories.setPadding(Main.vw10, 0, Main.vw10, 0);
        }
        return rootView;
    }
}