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

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import java.util.Set;

import de.tadris.fitness.R;

public class WearSyncDeviceList extends Fragment implements CommsBT.BTInterface{
    private WearSyncActivity activity;
    private TextView title;
    private LinearLayout list;
    private ImageView icon;
    private AnimatedVectorDrawable iconDrawable;

    @Override public void onAttach(@NonNull Context context){
        super.onAttach(context);
        try{
            activity = (WearSyncActivity) context;
        }catch(ClassCastException e){
            Log.e(WearSyncActivity.LOG_TAG, "WearSyncDeviceList.onAttach exception: " + e.getMessage());
        }
    }
    @Override public @Nullable View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        View rootView = inflater.inflate(R.layout.fragment_wear_sync, container, false);
        title = rootView.findViewById(R.id.wearSyncTitle);
        title.setText(R.string.wear_device_select_title);
        list = rootView.findViewById(R.id.wearSyncList);
        icon = rootView.findViewById(R.id.wearSyncIcon);
        iconDrawable = (AnimatedVectorDrawable) icon.getBackground();
        iconDrawable.start();

        return rootView;
    }
    @Override public void onResume(){
        super.onResume();
        activity.commsBT.addListener(this);
    }
    @Override public void onPause(){
        super.onPause();
        activity.commsBT.delListener(this);
    }
    private void showIcon(){
        list.setVisibility(View.GONE);
        icon.setVisibility(View.VISIBLE);
        iconDrawable.start();
    }
    private void showList(){
        icon.setVisibility(View.GONE);
        list.setVisibility(View.VISIBLE);
        iconDrawable.stop();
    }

    @SuppressLint("MissingPermission")//Permissions are handled in WearSyncActivity.onCreate
    @Override public void onBTStartDone(){
        Log.d(WearSyncActivity.LOG_TAG, "onBTStartDone");
        if(activity.commsBT.status == CommsBT.Status.CONNECTED) return;
        Set<BluetoothDevice> bluetoothDevices = activity.commsBT.getDevices();
        if(bluetoothDevices == null || bluetoothDevices.isEmpty()){
            onBTError(R.string.wear_error_no_devices);
            return;
        }
        activity.runOnUiThread(()->{
            for(BluetoothDevice bluetoothDevice : bluetoothDevices){
                TextView device = new TextView(activity, null, 0, R.style.wearDeviceStyle);
                device.setText(bluetoothDevice.getName());
                list.addView(device);
                device.setOnClickListener(v->activity.commsBT.connectDevice(bluetoothDevice));
            }
            title.setText(R.string.wear_device_select_title);
            showList();
        });
    }
    @Override public void onBTConnecting(String deviceName){
        Log.d(WearSyncActivity.LOG_TAG, "onBTConnecting " + deviceName);
        activity.runOnUiThread(()->{
            showIcon();
            title.setText(getString(R.string.wear_connecting_to).replace("watch", deviceName));
        });
    }
    @Override public void onBTConnectFailed(){
        Log.d(WearSyncActivity.LOG_TAG, "onBTConnectFailed");
        activity.runOnUiThread(()->{
            title.setText(getString(R.string.wear_error_connect));
            showList();
        });
    }
    @Override public void onBTConnected(String deviceName){}
    @Override public void onBTDisconnected(){}
    @Override public void onBTResponse(JSONObject response){}
    @Override public void onBTBytesReceived(int length){}
    @Override public void onBTError(int message){
        activity.runOnUiThread(()->{
            Log.d(WearSyncActivity.LOG_TAG, "onBTError: " + getString(message));
            title.setText(message);
            showList();
        });
    }
}
