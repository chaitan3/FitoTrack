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

package de.tadris.fitness.ui.workouts;

import de.tadris.fitness.data.MapStore;
import de.tadris.fitness.data.SettingsStore;
import de.tadris.fitness.util.Util;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.components.XAxis;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes;

import java.util.ArrayList;
import java.util.List;

import de.tadris.fitness.R;
import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.data.WorkoutData.HeartRateSample;
import de.tadris.fitness.data.WorkoutData.LocationSample;
import de.tadris.fitness.data.WorkoutDataStore;
import de.tadris.fitness.ui.Main;
import de.tadris.fitness.ui.record.ScreenField.Type;

public class WorkoutActivity extends Activity{
    private boolean showMap = false;
    private boolean showSpeedChart = false;
    private boolean showElevationChart = false;

    private LinearLayout gps;
    private Button map_button;
    private Button speed_button;
    private LinearLayout elevation_wrapper;
    private Button elevation_button;
    private LinearLayout heartRate_wrapper;
    private Button heartRate_button;
    private MapView mapView;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        WorkoutData workoutData;
        Bundle bundle = getIntent().getExtras();
        if(bundle == null){
            //if no workoutDataId is given, we open the last
            workoutData = WorkoutDataStore.getWorkout(WorkoutDataStore.lastWorkoutID);
        }else{
            workoutData = WorkoutDataStore.getWorkout(bundle.getLong("workoutDataId"));
        }
        if(workoutData == null){
            Log.e(Main.LOG_TAG, "SummaryActivity No workoutData, cancel");
            Toast.makeText(this, R.string.fail_no_workoutData, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.workout);

        ((ImageView)findViewById(R.id.icon)).setImageResource(workoutData.getWorkoutTypeIcon());
        TextView duration = findViewById(R.id.duration);
        if(workoutData.pauseTotal == 0){
            duration.setText(Util.prettyDurationShort(workoutData.durationTotal));
        }else{
            findViewById(R.id.duration_desc).setVisibility(View.VISIBLE);
            String duration_val = Util.prettyDurationShort(workoutData.durationTotal) + " + "
                    + Util.prettyDurationShort(workoutData.pauseTotal) + " = "
                    + Util.prettyDurationShort(workoutData.durationTotal + workoutData.pauseTotal);
            duration.setText(duration_val);
        }
        if(!workoutData.isWorkoutTypeGps()){
            findViewById(R.id.gps).setVisibility(View.GONE);
        }else{
            gps = findViewById(R.id.gps);
            set(findViewById(R.id.distance), Type.DISTANCE_KM, workoutData.distanceTotal);

            map_button = findViewById(R.id.map_button);
            map_button.setOnClickListener(v->{
                map_button.setText(R.string.loading);
                if(!workoutData.locationSamples.isEmpty()){
                    showMap(workoutData);
                }else if(showSpeedChart || showElevationChart){
                    showMap = true;
                }else{
                    showMap = true;
                    Main.runInBackground(()->WorkoutDataStore.readLocations(this, workoutData));
                }
            });

            String speed_val = getText(R.string.avg) + " " + getValue(Type.SPEED_KMH, workoutData.speedAvg)
                    + " | " + getText(R.string.max) + " " + get(Type.SPEED_KMH, workoutData.speedMax);
            ((TextView)findViewById(R.id.speed)).setText(speed_val);

            speed_button = findViewById(R.id.speed_button);
            speed_button.setOnClickListener(v->{
                speed_button.setText(R.string.loading);
                if(!workoutData.locationSamples.isEmpty()){
                    showSpeedChart(workoutData);
                }else if(showMap || showElevationChart){
                    showSpeedChart = true;
                }else{
                    showSpeedChart = true;
                    Main.runInBackground(()->WorkoutDataStore.readLocations(this, workoutData));
                }
            });

            if(workoutData.elevationGainTotal < 10 && workoutData.elevationLossTotal < 10){
                findViewById(R.id.elevation_wrapper).setVisibility(View.GONE);
            }else{
                elevation_wrapper = findViewById(R.id.elevation_wrapper);
                if(workoutData.elevationGainTotal > 10 && workoutData.elevationLossTotal > 10){
                    String elevation_val = getText(R.string.gain) + " " + getValue(Type.ELEVATION, workoutData.elevationGainTotal)
                            + " | " + getText(R.string.loss) + " " + get(Type.ELEVATION, workoutData.elevationLossTotal);
                    ((TextView)findViewById(R.id.elevation)).setText(elevation_val);
                }else if(workoutData.elevationGainTotal > 10){
                    String elevation_val = getText(R.string.gain) + " " + get(Type.ELEVATION, workoutData.elevationGainTotal);
                    ((TextView)findViewById(R.id.elevation)).setText(elevation_val);
                }else if(workoutData.elevationLossTotal > 10){
                    String elevation_val = getText(R.string.loss) + " " + get(Type.ELEVATION, workoutData.elevationLossTotal);
                    ((TextView)findViewById(R.id.elevation)).setText(elevation_val);
                }

                elevation_button = findViewById(R.id.elevation_button);
                elevation_button.setOnClickListener(v->{
                    elevation_button.setText(R.string.loading);

                    if(!workoutData.locationSamples.isEmpty()){
                        showElevationChart(workoutData);
                    }else if(showMap || showSpeedChart){
                        showElevationChart = true;
                    }else{
                        showElevationChart = true;
                        Main.runInBackground(()->WorkoutDataStore.readLocations(this, workoutData));
                    }
                });
            }
        }

        if(workoutData.heartRateAvg == 0){
            findViewById(R.id.heartRate_wrapper).setVisibility(View.GONE);
        }else{
            heartRate_wrapper = findViewById(R.id.heartRate_wrapper);
            String temp = getText(R.string.avg) + " " + getValue(Type.HEART_RATE_BPM, workoutData.heartRateAvg)
                    + " | " + getText(R.string.max) + " " + get(Type.HEART_RATE_BPM, workoutData.heartRateMax);
            ((TextView)findViewById(R.id.heartRate)).setText(temp);

            heartRate_button = findViewById(R.id.heartRate_button);
            heartRate_button.setOnClickListener(v->{
                heartRate_button.setText(R.string.loading);
                if(workoutData.heartRateSamples.isEmpty()){
                    Main.runInBackground(()->WorkoutDataStore.readHeartRates(this, workoutData));
                }else{
                    showHeartRateChart(workoutData);
                }
            });
        }

        if(workoutData.caloriesTotal < 10){
            findViewById(R.id.calories_wrapper).setVisibility(View.GONE);
        }else{
            set(findViewById(R.id.calories), Type.CALORIES, workoutData.caloriesTotal);
        }
        findViewById(R.id.delete).setOnClickListener(v->onDeleteClick(workoutData.id));

        findViewById(R.id.scrollView).requestFocus();
    }
    @Override public void onDestroy(){
        if(mapView != null) mapView.destroyAll();
        super.onDestroy();
    }

    private void set(TextView textView, Type type, double value){
        textView.setText(get(type, value));
    }
    private String get(Type type, double value){
        return switch(type){
            case DISTANCE_KM -> value > 1000 ?
                    getValue(type, value) + " " + getString(R.string.km) :
                    getValue(type, value) + " " + getString(R.string.m);
            case SPEED_KMH -> Util.speedDoubleToString(value*3.6) + " " + getString(R.string.kmh);
            case ELEVATION -> getValue(type, value) + " " + getString(R.string.m);
            case HEART_RATE_BPM -> getValue(type, value) + " " + getString(R.string.bpm);
            case CALORIES -> getValue(type, value) + " " + getString(R.string.kcal);
            default -> null;//UNKNOWN, DURATION, SPEED_MS, PAUSE
        };
    }
    private String getValue(Type type, double value){
        return switch(type){
            case DISTANCE_KM -> value > 1000 ?
                    Util.distanceDoubleToString(value/1000) :
                    String.valueOf(Math.round(value));
            case SPEED_KMH -> Util.speedDoubleToString(value*3.6);
            case ELEVATION, HEART_RATE_BPM, CALORIES -> String.valueOf(Math.round(value));
            default -> null;//UNKNOWN, DURATION, SPEED_MS, PAUSE
        };
    }

    public void locationsRead(WorkoutData workoutData){
        runOnUiThread(()->{
            if(showMap) showMap(workoutData);
            if(showSpeedChart) showSpeedChart(workoutData);
            if(showElevationChart) showElevationChart(workoutData);
        });
    }
    private void showMap(WorkoutData workoutData){
        if(!workoutData.isWorkoutTypeGps() || workoutData.locationSamples.isEmpty()) return;
        try{
            mapView = new MapView(this);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(Main.vw80, Main.vh80);
            layoutParams.setMargins(Main.vw10, 10, Main.vw10, 0);
            mapView.setLayoutParams(layoutParams);

            AndroidGraphicFactory.createInstance(getApplicationContext());
            mapView.setBuiltInZoomControls(false);
            mapView.setMapScaleBar(null);
            mapView.setClickable(false);
            mapView.setCenter(workoutData.locationSamples.get(0).toLatLong());

            if(SettingsStore.getMapSource(this)){
                TileCache tileCache = AndroidUtil.createTileCache(
                        this
                        ,"local"
                        ,mapView.getModel().displayModel.getTileSize()
                        ,1f
                        ,mapView.getModel().frameBufferModel.getOverdrawFactor()
                );
                MultiMapDataStore multiMapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
                for(MapStore.File file : MapStore.getLocalMaps(this)){
                    try{
                        multiMapDataStore.addMapDataStore(new MapFile(file.getLocalFile(this)), false, false);
                    }catch(Exception e){
                        Log.e(Main.LOG_TAG, "addMapDataStore failed for map: " + file.name);
                        Toast.makeText(this, getString(R.string.corrupt_map, file.name), Toast.LENGTH_SHORT).show();
                    }
                }
                TileRendererLayer tileLocalLayer = AndroidUtil.createTileRendererLayer(
                        tileCache,
                        mapView.getModel().mapViewPosition,
                        multiMapDataStore,
                        MapsforgeThemes.OSMARENDER
                );
                mapView.addLayer(tileLocalLayer);
            }else{
                TileCache tileCache = AndroidUtil.createTileCache(
                        this
                        ,"online"
                        ,mapView.getModel().displayModel.getTileSize()
                        ,1f
                        ,mapView.getModel().frameBufferModel.getOverdrawFactor()
                );
                OpenStreetMapMapnik tileSource = OpenStreetMapMapnik.INSTANCE;
                tileSource.setUserAgent("de.tadris.fitness.wearos");
                TileDownloadLayer tileDownloadLayer = new TileDownloadLayer(
                        tileCache
                        ,mapView.getModel().mapViewPosition
                        ,tileSource
                        ,AndroidGraphicFactory.INSTANCE
                );
                mapView.addLayer(tileDownloadLayer);
                tileDownloadLayer.onResume();
            }

            List<LatLong> latLongs = new ArrayList<>();
            for(int i = 0; i < workoutData.locationSamples.size()-5; i+=5){
                latLongs.add(workoutData.locationSamples.get(i).toLatLong());
            }
            latLongs.add(workoutData.locationSamples.get(workoutData.locationSamples.size()-1).toLatLong());

            Paint p = AndroidGraphicFactory.INSTANCE.createPaint();
            p.setColor(getColor(R.color.colorPrimary));
            p.setStrokeWidth(10);
            p.setStyle(Style.STROKE);
            Polyline polyline = new Polyline(p, AndroidGraphicFactory.INSTANCE);
            for(int i=0; i<latLongs.size(); i++){
                polyline.addPoint(latLongs.get(i));
            }
            mapView.addLayer(polyline);

            BoundingBox bounds = (new BoundingBox(latLongs)).extendMeters(50);
            mapView.setCenter(bounds.getCenterPoint());
            mapView.setZoomLevel(LatLongUtils.zoomForBounds(
                    new Dimension(Main.vw80, Main.vh80)
                    ,bounds
                    ,mapView.getModel().displayModel.getTileSize()
            ));
            gps.addView(mapView, gps.indexOfChild(map_button));
            map_button.setVisibility(View.GONE);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "SummaryActivity.showMap exception: " + e.getMessage());
            Toast.makeText(this, R.string.error_map_load, Toast.LENGTH_LONG).show();
        }
    }
    private void showSpeedChart(WorkoutData workoutData){
        if(workoutData.locationSamples.isEmpty()) return;

        LineChart chartSpeed = new LineChart(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(Main.vw80, Main.vh50);
        layoutParams.setMargins(Main.vw10, 10, 0, 0);
        chartSpeed.setLayoutParams(layoutParams);

        chartSpeed.getDescription().setEnabled(false);
        chartSpeed.getLegend().setEnabled(false);
        chartSpeed.setTouchEnabled(false);

        //The chart has a width of Main.vw80, more entries won't show
        int skipFrames = workoutData.locationSamples.size() / Main.vw80;
        if(skipFrames < 5) skipFrames = 5;
        long firstDuration = -1;
        float minSpeed = 100;
        float maxSpeed = 0;

        ArrayList<Entry> values = new ArrayList<>();
        for(int i=0; i<workoutData.locationSamples.size()-skipFrames; i+=skipFrames){
            LocationSample locationSample = workoutData.locationSamples.get(i);
            if(locationSample.speed == 0) continue;
            long duration = timeToDuration(workoutData, locationSample.time);
            if(firstDuration == -1) firstDuration = duration;
            float speedKMH = (float)(locationSample.speed * 3.6);//meters/second > kmh
            values.add(new Entry(duration, speedKMH));
            if(speedKMH < minSpeed) minSpeed = speedKMH;
            if(speedKMH > maxSpeed) maxSpeed = speedKMH;
        }
        LocationSample locationSample = workoutData.locationSamples.get(workoutData.locationSamples.size()-1);
        long duration = timeToDuration(workoutData, locationSample.time);
        float speedKMH = (float)(locationSample.speed * 3.6);//meters/second > kmh
        values.add(new Entry(duration, speedKMH));

        LineDataSet lineDataSet = new LineDataSet(values, "Speed");
        lineDataSet.setColor(getColor(R.color.colorPrimary));
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawValues(false);
        chartSpeed.setData(new LineData(lineDataSet));

        XAxis timeAxis = chartSpeed.getXAxis();
        timeAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        timeAxis.setTextColor(getColor(R.color.textWhite));
        timeAxis.setValueFormatter(Util.timeAxisValueFormatter);
        timeAxis.setLabelCount(4, true);
        timeAxis.setAxisMinimum(firstDuration);
        timeAxis.setAxisMaximum(workoutData.durationTotal);

        YAxis speedAxis = chartSpeed.getAxisLeft();
        speedAxis.setTextColor(getColor(R.color.textWhite));
        speedAxis.setAxisMinimum(minSpeed < 1 ? 0 : minSpeed*0.9F);
        speedAxis.setAxisMaximum(maxSpeed*1.1F);

        chartSpeed.getAxisRight().setEnabled(false);

        gps.addView(chartSpeed, gps.indexOfChild(speed_button));
        speed_button.setVisibility(View.GONE);
    }
    private void showElevationChart(WorkoutData workoutData){
        if(workoutData.locationSamples.isEmpty()) return;

        LineChart elevationChart = new LineChart(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(Main.vw80, Main.vh50);
        layoutParams.setMargins(Main.vw10, 10, 0, 0);
        elevationChart.setLayoutParams(layoutParams);

        elevationChart.getDescription().setEnabled(false);
        elevationChart.getLegend().setEnabled(false);
        elevationChart.setTouchEnabled(false);

        //The chart has a width of Main.vw80, more entries won't show
        int skipFrames = workoutData.locationSamples.size() / Main.vw80;
        if(skipFrames < 5) skipFrames = 5;
        long firstDuration = -1;
        float minElevation = 10000;
        float maxElevation = 0;

        ArrayList<Entry> values = new ArrayList<>();
        for(int i=0; i<workoutData.locationSamples.size()-skipFrames; i+=skipFrames){
            LocationSample locationSample = workoutData.locationSamples.get(i);
            if(locationSample.ele == 0) continue;
            long duration = timeToDuration(workoutData, locationSample.time);
            if(duration == 0) continue;
            if(firstDuration == -1) firstDuration = duration;
            float ele = (float)(locationSample.ele);
            values.add(new Entry(duration, ele));
            if(ele < minElevation) minElevation = ele;
            if(ele > maxElevation) maxElevation = ele;
        }
        LocationSample locationSample = workoutData.locationSamples.get(workoutData.locationSamples.size()-1);
        long duration = timeToDuration(workoutData, locationSample.time);
        float ele = (float)(locationSample.ele);
        values.add(new Entry(duration, ele));

        LineDataSet lineDataSet = new LineDataSet(values, "Elevation");
        lineDataSet.setColor(getColor(R.color.colorPrimary));
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawValues(false);
        elevationChart.setData(new LineData(lineDataSet));

        XAxis timeAxis = elevationChart.getXAxis();
        timeAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        timeAxis.setTextColor(getColor(R.color.textWhite));
        timeAxis.setValueFormatter(Util.timeAxisValueFormatter);
        timeAxis.setLabelCount(4, true);
        timeAxis.setAxisMinimum(firstDuration);
        timeAxis.setAxisMaximum(workoutData.durationTotal);

        YAxis elevationAxis = elevationChart.getAxisLeft();
        elevationAxis.setTextColor(getColor(R.color.textWhite));
        elevationAxis.setAxisMinimum(minElevation * 0.9F);
        elevationAxis.setAxisMaximum(maxElevation * 1.1F);

        elevationChart.getAxisRight().setEnabled(false);

        elevation_wrapper.addView(elevationChart, elevation_wrapper.indexOfChild(elevation_button));
        elevation_button.setVisibility(View.GONE);
    }
    public void heartRatesRead(WorkoutData workoutData){runOnUiThread(()->showHeartRateChart(workoutData));}
    private void showHeartRateChart(WorkoutData workoutData){
        if(workoutData.heartRateSamples.isEmpty()) return;

        LineChart chartHeartRate = new LineChart(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(Main.vw80, Main.vh50);
        layoutParams.setMargins(Main.vw10, 10, 0, 0);
        chartHeartRate.setLayoutParams(layoutParams);

        chartHeartRate.getDescription().setEnabled(false);
        chartHeartRate.getLegend().setEnabled(false);
        chartHeartRate.setTouchEnabled(false);

        //The chart has a width of Main.vw80, more entries won't show
        int skipFrames = workoutData.heartRateSamples.size() / Main.vw80;
        if(skipFrames < 5) skipFrames = 5;

        long firstDuration = 0;
        ArrayList<Entry> values = new ArrayList<>();
        for(int i=0; i<workoutData.heartRateSamples.size()-skipFrames; i+=skipFrames){
            HeartRateSample heartRateSample = workoutData.heartRateSamples.get(i);
            long duration = timeToDuration(workoutData, heartRateSample.time);
            if(duration == 0) continue;
            if(firstDuration == 0) firstDuration = duration;
            values.add(new Entry(duration, heartRateSample.heartRate));
        }
        HeartRateSample heartRateSample = workoutData.heartRateSamples.get(workoutData.heartRateSamples.size()-1);
        values.add(new Entry(heartRateSample.time - workoutData.id, heartRateSample.heartRate));

        LineDataSet lineDataSet = new LineDataSet(values, "Heart rate");
        lineDataSet.setColor(getColor(R.color.colorPrimary));
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawValues(false);
        chartHeartRate.setData(new LineData(lineDataSet));

        XAxis timeAxis = chartHeartRate.getXAxis();
        timeAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        timeAxis.setTextColor(getColor(R.color.textWhite));
        timeAxis.setValueFormatter(Util.timeAxisValueFormatter);
        timeAxis.setLabelCount(4, true);
        timeAxis.setAxisMinimum(firstDuration);
        timeAxis.setAxisMaximum(workoutData.durationTotal);

        chartHeartRate.getAxisLeft().setTextColor(getColor(R.color.textWhite));
        chartHeartRate.getAxisRight().setEnabled(false);

        heartRate_wrapper.addView(chartHeartRate, heartRate_wrapper.indexOfChild(heartRate_button));
        heartRate_button.setVisibility(View.GONE);
    }
    private long timeToDuration(WorkoutData workoutData, long time){
        if(workoutData.checkpoints.isEmpty()) return 0;
        WorkoutData.Checkpoint checkpoint = workoutData.checkpoints.get(0);
        for(int i=1; i < workoutData.checkpoints.size(); i++){
            if(workoutData.checkpoints.get(i).time > time){
                if(workoutData.checkpoints.get(i).duration == checkpoint.duration){
                    return 0;//during a break, don't show
                }else{
                    break;
                }
            }
            checkpoint = workoutData.checkpoints.get(i);
        }
        return time - checkpoint.time + checkpoint.duration;
    }

    private void onDeleteClick(long workoutDataId){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(" ");//This makes sure the dialog shows properly for a round screen
        builder.setMessage(R.string.alert_delete_message);
        builder.setPositiveButton(R.string.alert_delete_positive, (d, w)->{
            Main.runInBackground(()->WorkoutDataStore.deleteWorkout(this, workoutDataId));
            finish();
        });
        builder.setNegativeButton(R.string.alert_delete_negative, (d, w)->finish());
        builder.create().show();
    }
}