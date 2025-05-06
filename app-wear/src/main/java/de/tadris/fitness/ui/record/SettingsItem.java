package de.tadris.fitness.ui.record;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import de.tadris.fitness.R;
import de.tadris.fitness.data.SettingsStore;
import de.tadris.fitness.data.SettingsStore.WorkoutSettings.WorkoutSetting;
import static de.tadris.fitness.data.SettingsStore.WorkoutSettings.WorkoutSetting.*;
import de.tadris.fitness.ui.Main;

public class SettingsItem extends LinearLayout{
    private final String name;
    private WorkoutSetting setting;
    private final TextView value;

    SettingsItem(Context context, String name, int labelText, WorkoutSetting setting){
        super(context);
        this.name = name;
        this.setting = setting;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.menu_item, this, true);
        ((TextView)findViewById(R.id.label)).setText(labelText);
        value = findViewById(R.id.value);
        setValue();
        setOnClickListener(v->toggleValue());
    }
    private void toggleValue(){
        if(setting == UNAVAILABLE){
            Toast.makeText(getContext(), R.string.fail_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        setting = ws2b() ? DISABLE : ENABLE;
        setValue();
        Main.runInBackground(()->
                SettingsStore.storeSetting(
                        getContext()
                        ,name
                        ,RecordActivity.workoutSettings.workoutType
                        ,ws2b()
                )
        );
        switch(name){
            case "screenOn" -> RecordActivity.workoutSettings.screenOn = setting;
            case "autoPause" -> RecordActivity.workoutSettings.autoPause = setting;
            case "screenMap" -> RecordActivity.workoutSettings.screenMap = setting;
            case "screenCurrent" -> RecordActivity.workoutSettings.screenCurrent = setting;
            case "screenTotal" -> RecordActivity.workoutSettings.screenTotal = setting;
            case "screenSpeed" -> RecordActivity.workoutSettings.screenSpeed = setting;
            case "screenElevation" -> RecordActivity.workoutSettings.screenElevation = setting;
            case "screenHeartRate" -> RecordActivity.workoutSettings.screenHeartRate = setting;
        }
    }
    private void setValue(){
        if(setting == UNAVAILABLE){
            value.setText(R.string.unavailable);
        }else if(name.equals("screenOn")){
            value.setText(ws2b() ? R.string.settings_screen_on : R.string.settings_screen_off);
        }else{
            value.setText(ws2b() ? R.string.on : R.string.off);
        }
    }
    private boolean ws2b(){return setting == ENABLE || setting == ENABLED;}
}
