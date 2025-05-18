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

package de.tadris.fitness.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.Button;

import androidx.core.app.ActivityCompat;
import androidx.core.splashscreen.SplashScreen;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.tadris.fitness.R;
import de.tadris.fitness.data.SettingsStore;
import de.tadris.fitness.ui.record.RecordActivity;
import de.tadris.fitness.ui.settings.MenuSettings;
import de.tadris.fitness.ui.workouts.WorkoutsActivity;
import de.tadris.fitness.util.CommsBT;

public class Main extends Activity{
    public final static String LOG_TAG = "FitoTrack";
    private boolean showSplash = true;

    private static ExecutorService executorService;

    public static boolean hasBTPermission = false;
    public static boolean isScreenRound;
    public static int heightPixels;
    public static int widthPixels;
    public static int vh25;
    public static int vh50;
    public static int vh75;
    public static int vh80;
    public static int vw10;
    public static int vw15;
    public static int vw20;
    public static int vw30;
    public static int vw80;
    public static int _10dp = 0;
    public static int menuItemHeight = 0;

    @Override protected void onCreate(Bundle savedInstanceState){
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(()->showSplash);
        super.onCreate(savedInstanceState);
        hasBTPermission = ActivityCompat.checkSelfPermission(this, getBTPermission()) == PackageManager.PERMISSION_GRANTED;
        isScreenRound = getResources().getConfiguration().isScreenRound();
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        heightPixels = displayMetrics.heightPixels;
        widthPixels = displayMetrics.widthPixels;
        vh25 = (int) (heightPixels * .25);
        vh50 = (int) (heightPixels * .5);
        vh75 = heightPixels - vh25;
        vh80 = (int) (heightPixels * .8);
        vw10 = (int) (widthPixels * .1);
        vw15 = (int) (widthPixels * .15);
        vw20 = (int) (widthPixels * .2);
        vw30 = (int) (widthPixels * .3);
        vw80 = (int) (widthPixels * .8);
        _10dp = (int) getResources().getDimension(R.dimen._10dp);
        menuItemHeight = (int) getResources().getDimension(R.dimen.menuItemHeight);

        setContentView(R.layout.main);

        Button workouts = findViewById(R.id.workouts);
        workouts.setOnClickListener(v->onWorkoutsClick());
        Button record = findViewById(R.id.record);
        record.setOnClickListener(v->onRecordClick());
        Button settings = findViewById(R.id.settings);
        settings.setOnClickListener(v->onSettingsClick());

        if(isScreenRound){
            workouts.setPadding(vw20, 0, vw20, 0);
            settings.setPadding(vw20, 0, vw20, 0);
        }
        showSplash = false;
    }
    @Override protected void onResume(){
        super.onResume();
        if(hasBTPermission && SettingsStore.getBTEnabled(this))
            startService(new Intent(this, CommsBT.class));
    }
    public static String getBTPermission(){
        return Build.VERSION.SDK_INT >= 31 ? Manifest.permission.BLUETOOTH_CONNECT : Manifest.permission.BLUETOOTH;
    }
    @Override protected void onDestroy(){
        stopService(new Intent(this, CommsBT.class));
        super.onDestroy();
    }
    private void onWorkoutsClick(){startActivity(new Intent(this, WorkoutsActivity.class));}
    private void onRecordClick(){startActivity(new Intent(this, RecordActivity.class));}
    private void onSettingsClick(){startActivity(new Intent(this, MenuSettings.class));}

    public static void runInBackground(Runnable runnable){
        if(executorService == null) executorService = Executors.newCachedThreadPool();
        executorService.execute(runnable);
    }
}