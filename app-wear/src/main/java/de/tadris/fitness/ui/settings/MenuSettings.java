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
package de.tadris.fitness.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import de.tadris.fitness.R;
import de.tadris.fitness.data.SettingsStore;
import de.tadris.fitness.ui.Main;
import de.tadris.fitness.util.CommsBT;

public class MenuSettings extends MenuScreen{
    private MenuItem bluetooth;
    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        bluetooth = new MenuItem(this, R.string.settings_BT);
        setBluetoothValue();
        bluetooth.setOnClickListener(v->onBluetoothClick());
        list.addView(bluetooth);

        MenuItem maps = new MenuItem(this, R.string.maps);
        maps.value.setVisibility(View.GONE);
        maps.setOnClickListener(v->startActivity(new Intent(this, MenuMaps.class)));
        list.addView(maps);

    }
    private void setBluetoothValue(){
        if(!de.tadris.fitness.ui.Main.hasBTPermission){
            bluetooth.value.setText(R.string.settings_BT_permission);
        }else if(SettingsStore.getBTEnabled(this)){
            bluetooth.value.setText(R.string.settings_BT_enabled);
        }else{
            bluetooth.value.setText(R.string.settings_BT_disabled);
        }
    }

    private void onBluetoothClick(){
        if(!Main.hasBTPermission){
            SettingsStore.storeBTEnabled(this, true);
            requestPermission();
        }else if(SettingsStore.getBTEnabled(this)){
            SettingsStore.storeBTEnabled(this, false);
            stopService(new Intent(this, CommsBT.class));
        }else{
            SettingsStore.storeBTEnabled(this, true);
            startService(new Intent(this, CommsBT.class));
        }
        setBluetoothValue();
    }
    private void requestPermission(){
        ActivityCompat.requestPermissions(this, new String[]{Main.getBTPermission()},1);
    }
    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int i=0; i<permissions.length; i++){
            if((permissions[i].equals(Manifest.permission.BLUETOOTH_CONNECT)
                    || permissions[i].equals(Manifest.permission.BLUETOOTH))
                    && grantResults[i] == PackageManager.PERMISSION_GRANTED
            ){
                Main.hasBTPermission = true;
                startService(new Intent(this, CommsBT.class));
            }
        }
    }
}
