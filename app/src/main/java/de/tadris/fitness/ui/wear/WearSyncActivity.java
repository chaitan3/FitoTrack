/*
 * Copyright (c) 2025 Jannis Scheibe <jannis@tadris.de>
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
package de.tadris.fitness.ui.wear;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.tadris.fitness.R;
import de.tadris.fitness.data.GpsSample;
import de.tadris.fitness.data.GpsWorkout;
import de.tadris.fitness.data.IndoorSample;
import de.tadris.fitness.data.IndoorWorkout;
import de.tadris.fitness.ui.FitoTrackActivity;

public class WearSyncActivity extends FitoTrackActivity implements CommsBT.BTInterface{
    static final String LOG_TAG = "FitoTrack.WearSync";

    private ExecutorService executorService;
    private FragmentManager fragmentManager;
    CommsBT commsBT;
    private boolean hasBTPermission = false;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_sync);
        setupActionBar();
        setTitle(R.string.wear_sync);
        commsBT = new CommsBT();
        commsBT.addListener(this);
        fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.wearSync, new WearSyncDeviceList()).commit();
    }
    @Override protected void onResume(){
        super.onResume();
        if(commsBT == null) finishAndRemoveTask();
        if(Build.VERSION.SDK_INT >= 31){
            hasBTPermission = hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    && hasPermission(android.Manifest.permission.BLUETOOTH_SCAN);
            if(!hasBTPermission){
                ActivityCompat.requestPermissions(this
                        ,new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT
                                ,Manifest.permission.BLUETOOTH_SCAN
                        }
                        ,1
                );
            }
        }else{
            hasBTPermission = hasPermission(Manifest.permission.BLUETOOTH);
            if(!hasBTPermission){
                ActivityCompat.requestPermissions(this
                        ,new String[]{Manifest.permission.BLUETOOTH}
                        ,1
                );
            }
        }
        if(hasBTPermission) runInBackground(()->commsBT.startBT(this));
    }
    @Override protected void onStop(){
        super.onStop();
        if(commsBT != null){
            runInBackground(()->{
                commsBT.stopBT();
                commsBT = null;
            });
        }
    }
    private boolean hasPermission(String permission){
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }
    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int i=0; i<permissions.length; i++){
            if(permissions[i].equals(Manifest.permission.BLUETOOTH_CONNECT) ||
                    permissions[i].equals(Manifest.permission.BLUETOOTH_SCAN) ||
                    permissions[i].equals(Manifest.permission.BLUETOOTH)){
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    hasBTPermission = true;
                    runInBackground(()->commsBT.startBT(this));
                }else{
                    onBTError(R.string.wear_error_denied);
                }
                return;
            }
        }
    }
    void runInBackground(Runnable runnable){
        if(executorService == null) executorService = Executors.newCachedThreadPool();
        executorService.execute(runnable);
    }

    @Override public void onBTStartDone(){}
    @Override public void onBTConnecting(String deviceName){}
    @Override public void onBTConnectFailed(){}
    @Override public void onBTConnected(String deviceName){
        Log.d(LOG_TAG, "onBTConnected " + deviceName);
        runOnUiThread(()->fragmentManager.beginTransaction().replace(R.id.wearSync, new WearSyncWorkoutsList()).commit());
    }
    @Override public void onBTDisconnected(){}
    @Override public void onBTResponse(JSONObject response){}
    @Override public void onBTBytesReceived(int length){}
    @Override public void onBTError(int message){}
}
