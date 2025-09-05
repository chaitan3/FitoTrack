/*
 * Copyright (c) 2024 Jannis Scheibe <jannis@tadris.de>
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

package de.tadris.fitness.ui.record;

import android.Manifest;
import android.animation.Animator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.overlay.FixedPixelCircle;
import org.mapsforge.map.layer.overlay.Polyline;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.tadris.fitness.R;
import de.tadris.fitness.data.RouteSample;
import de.tadris.fitness.data.WorkoutType;
import de.tadris.fitness.data.WorkoutTypeManager;
import de.tadris.fitness.map.MapManager;
import de.tadris.fitness.recording.GpsState;
import de.tadris.fitness.recording.RecordingState;
import de.tadris.fitness.recording.StartReason;
import de.tadris.fitness.recording.component.GpsComponent;
import de.tadris.fitness.recording.event.AggregatedRecordingValuesEvent;
import de.tadris.fitness.recording.event.LocationChangeEvent;
import de.tadris.fitness.recording.event.RecordingStateChangeEvent;
import de.tadris.fitness.recording.event.SatelliteCountEvent;
import de.tadris.fitness.recording.event.WorkoutGPSStateChanged;

public class RecordGpsWorkoutActivity extends RecordWorkoutActivity {

    public static final int REQUEST_CODE_LOCATION_PERMISSION = 10;

    private MapView mapView;
    private TextView gpsStatusView;
    private TextView waitingSatellitesText;

    private NavigationModeHandler navigationModeHandler;
    private Polyline polyline;
    private FixedPixelCircle locationPoint;
    private boolean gpsFound = false;
    private final List<LatLong> recordedPositions = new ArrayList<>();

    @Nullable
    private MapControls mapControls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();


        boolean wasAlreadyRunning = false;
        if (!LAUNCH_ACTION.equals(intent.getAction())) {
            wasAlreadyRunning = true;
        } else if (getCurrentState() != RecordingState.IDLE) {
            wasAlreadyRunning = true;
        }


        if (wasAlreadyRunning) {
            activity = AggregatedRecordingValuesEvent.Companion.getCurrent().getWorkoutType();
        } else {
            Serializable workoutType = intent.getSerializableExtra(WORKOUT_TYPE_EXTRA);
            if (workoutType instanceof WorkoutType) {
                activity = (WorkoutType) workoutType;
            } else {
                String workoutTypeId = intent.getStringExtra(WORKOUT_TYPE_EXTRA);
                activity = WorkoutTypeManager.getInstance().getWorkoutTypeById(this, workoutTypeId);
            }
        }

        initBeforeContent();
        setContentView(R.layout.activity_record_workout);
        initAfterContent();

        checkPermissions();

        setTitle(R.string.recordWorkout);

        setupMap();

        ((ViewGroup) findViewById(R.id.recordMapViewerRoot)).addView(mapView);
        waitingForGPSOverlay = findViewById(R.id.recorderWaitingOverlay);
        waitingForGPSOverlay.setVisibility(View.VISIBLE);
        gpsStatusView = findViewById(R.id.recordGpsStatus);
        waitingSatellitesText = findViewById(R.id.recorderWaitingSatellites);

        setupMapControls();

        onGPSStateChanged(new WorkoutGPSStateChanged(GpsState.SIGNAL_LOST, GpsState.SIGNAL_LOST));

        if (wasAlreadyRunning) {
            if (getCurrentState() != RecordingState.IDLE) {
                recordStartButtonsRoot.setVisibility(View.INVISIBLE);
                timeView.setVisibility(View.VISIBLE);
                invalidateOptionsMenu();
            }

            recordedPositions.clear();
            RouteSample[] samples = instance.db.workoutDao().getAllSamplesOfWorkout(
                    RecordingStateChangeEvent.Companion.getCurrent().getWorkoutId()
            );
            for (RouteSample sample : samples) {
                recordedPositions.add(sample.toLatLong());
            }
            updateLine(recordedPositions);
        }
    }

    private void setupMap() {
        mapView = new MapManager(this).setupMap();
        mapView.setClickable(false); // might be set to true later by mapControls
        navigationModeHandler = new NavigationModeHandler(mapView);
        navigationModeHandler.init();
    }

    private void setupMapControls() {
        mapView.setBuiltInZoomControls(false);

        FloatingActionButton mapFocusGpsBtn = findViewById(R.id.mapGpsFocus);
        FloatingActionButton mapZoomInBtn = findViewById(R.id.mapZoomIn);
        FloatingActionButton mapZoomOutBtn = findViewById(R.id.mapZoomOut);
        mapFocusGpsBtn.setBackgroundTintList(ColorStateList.valueOf(getThemePrimaryColor()));
        mapZoomInBtn.setBackgroundTintList(ColorStateList.valueOf(getThemePrimaryColor()));
        mapZoomOutBtn.setBackgroundTintList(ColorStateList.valueOf(getThemePrimaryColor()));

        final boolean showZoomControls = instance.userPreferences.getShowWorkoutZoomControls();
        if (showZoomControls) {
            mapControls = new MapControls(mapView, navigationModeHandler, mapFocusGpsBtn, mapZoomInBtn, mapZoomOutBtn);
            mapControls.init();
        } else {
            mapFocusGpsBtn.setVisibility(View.GONE);
            mapZoomInBtn.setVisibility(View.GONE);
            mapZoomOutBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        final boolean zoomWithVolume = instance.userPreferences.getZoomWithVolumeButtons();
        if (!zoomWithVolume) {
            return super.onKeyDown(keyCode, event);
        }

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && mapControls != null) {
            mapControls.externalZoomInRequest();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && mapControls != null) {
            mapControls.externalZoomOutRequest();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void hideWaitOverlay() {
        waitingForGPSOverlay.clearAnimation();
        waitingForGPSOverlay.animate().alpha(0f).setDuration(1000).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                waitingForGPSOverlay.setVisibility(View.GONE);
            }
        }).start();
    }

    private boolean isWorkoutRunning() {
        return getCurrentState() == RecordingState.RUNNING;
    }

    private void updateLine(final List<LatLong> latLongs) {
        if (latLongs.isEmpty()){
            return;
        }

        if (polyline != null) {
            mapView.getLayerManager().getLayers().remove(polyline);
        }


        Paint p = AndroidGraphicFactory.INSTANCE.createPaint();
        p.setColor(getThemePrimaryColor());
        p.setStrokeWidth(20);
        p.setStyle(Style.STROKE);
        polyline = new Polyline(p, AndroidGraphicFactory.INSTANCE);
        polyline.setPoints(latLongs);
        mapView.addLayer(polyline);

    }

    private void updateLocationPoint(final LatLong currentLocation) {
        assert (currentLocation != null);

        if (locationPoint != null){
            mapView.getLayerManager().getLayers().remove(locationPoint);
        }

        Paint stroke = AndroidGraphicFactory.INSTANCE.createPaint();
        stroke.setColor(getResources().getColor(R.color.locationCircleStroke));
        stroke.setStyle(Style.STROKE);
        stroke.setStrokeWidth(20f);
        Paint fill = AndroidGraphicFactory.INSTANCE.createPaint();
        fill.setStyle(Style.FILL);
        fill.setColor(getResources().getColor(R.color.locationCircle));
        locationPoint = new FixedPixelCircle(currentLocation, 15f, fill, stroke);
        mapView.addLayer(locationPoint);
    }

    @Override
    protected boolean hasServicePermission() {
        return hasPermission();
    }

    private void checkPermissions() {
        if (!hasPermission()) {
            showLocationPermissionConsent();
        }
    }

    private void showLocationPermissionConsent() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.recordingPermissionNotGrantedTitle)
                .setMessage(R.string.recordingPermissionNotGrantedMessage)
                .setPositiveButton(R.string.actionGrant, (dialog, which) -> requestLocationPermission())
                .setNegativeButton(R.string.cancel, (dialog, which) -> activityFinish())
                .setOnCancelListener(dialog -> activityFinish())
                .show();
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
    }

    private boolean hasPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (hasPermission()) {
                // Restart LocationListener so it can retry to register for location updates now that we got permission
                restartService();
            } else {
                showPermissionsNotGrantedDialog(R.string.recordingPermissionNotGrantedMessage);
            }
        }
    }

    private void showPermissionsNotGrantedDialog(@StringRes int message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.recordingPermissionNotGrantedTitle)
                .setMessage(message)
                .setPositiveButton(R.string.settings, (dialog, which) -> openSystemSettings())
                .create().show();
    }

    @Override
    protected void onListenerStart() {
        checkGpsStatus();
    }

    private void checkGpsStatus() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            openDialogNoGps();
        }
    }

    private void openDialogNoGps() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.noGpsTitle)
                .setMessage(R.string.noGpsMessage)
                .setNegativeButton(R.string.cancel, (dialog, which) -> activityFinish())
                .setPositiveButton(R.string.enable, (dialog, which) -> startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setCancelable(false)
                .create().show();
    }

    @Subscribe
    public void onLocationChange(LocationChangeEvent e) {
        final LatLong latLongGps = GpsComponent.locationToLatLong(e.location);

        if (isWorkoutRunning()) {
            recordedPositions.add(latLongGps);
            updateLine(recordedPositions);
        }

        updateLocationPoint(latLongGps);
        foundGPS();
   }

    @Override
    protected void onDestroy() {
        navigationModeHandler.removeNavigationModeListener();
        navigationModeHandler.deinit();

        // Clear map
        mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();

        super.onDestroy();
    }

    @Override
    public void onPause() {
        for (Layer layer : mapView.getLayerManager().getLayers()) {
            if (layer instanceof TileDownloadLayer) {
                ((TileDownloadLayer) layer).onPause();
            }
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        for (Layer layer : mapView.getLayerManager().getLayers()) {
            if (layer instanceof TileDownloadLayer) {
                ((TileDownloadLayer) layer).onResume();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGPSStateChanged(WorkoutGPSStateChanged e) {
        GpsState state = e.newState;
        gpsStatusView.setTextColor(state.getColor());

        if (state != GpsState.SIGNAL_LOST) {
            foundGPS();
        }

        if (getCurrentState() == RecordingState.IDLE) {
            if (state == GpsState.SIGNAL_GOOD) {
                updateStartButton(true, R.string.start, v -> start(StartReason.START_BUTTON));
            } else {
                updateStartButton(false, R.string.cannotStart, null);
            }
        }
    }

    private void foundGPS() {
        if (!gpsFound) {
            gpsFound = true;
            hideWaitOverlay();
        }
    }

    @Subscribe
    public void onSatelliteCountChanged(SatelliteCountEvent e) {
        waitingSatellitesText.setText(getResources().getQuantityString(R.plurals.satelliteCount, e.getCount(), e.getCount()));
    }

}
