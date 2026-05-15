/*
 * Copyright (c) 2026 Jannis Scheibe <jannis@tadris.de>
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

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.view.InputListener;

import de.tadris.fitness.recording.component.GpsComponent;
import de.tadris.fitness.recording.event.LocationChangeEvent;

public class NavigationModeHandler implements View.OnTouchListener, View.OnClickListener, InputListener {
    private boolean focusedInitially = false;
    private NavigationMode navigationMode = NavigationMode.Automatic;
    private boolean navigationModeUpdateReq = true;
    @Nullable
    private NavigationModeListener navigationModeListener = null;
    private LatLong currentGpsPosition = null;
    private final MapView mapView;

    public enum NavigationMode {
        Automatic,
        Manual,
        ManualInScope
    }

    public interface NavigationModeListener {

        void onNavigationModeChanged(final NavigationMode mode);

    };

    NavigationModeHandler(final MapView mapView) {
        this.mapView = mapView;
    }

    void init() {
        EventBus.getDefault().register(this);
        mapView.addInputListener(this);
    }

    void deinit() {
        EventBus.getDefault().unregister(this);
    }

    public void setNavigationModeListener(final NavigationModeListener listener) {
        removeNavigationModeListener();
        navigationModeListener = listener;
    }

    public void removeNavigationModeListener() {
        navigationModeListener = null;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                updateMode(NavigationMode.ManualInScope, UpdateMode.OnChange);
            }
            break;
            case MotionEvent.ACTION_UP: {
                if(distanceThresholdExceeds()){
                    updateMode(NavigationMode.Manual, UpdateMode.OnChange);
                } else {
                    updateMode(NavigationMode.Automatic, UpdateMode.OnChange);
                }
            }
            break;
            case MotionEvent.ACTION_MOVE: {
                if (distanceThresholdExceeds()) {
                    updateMode(NavigationMode.Manual, UpdateMode.OnChange);
                }
            }
            break;
        }

        managePositioning();
        return false;
    }

    private enum UpdateMode {
        ForceUpdate,
        OnChange
    }

    private void updateMode(final NavigationMode mode, final UpdateMode strategy) {
        switch (strategy) {
            case ForceUpdate: {
                navigationModeUpdateReq = true;
            }
            break;
            case OnChange: {
                navigationModeUpdateReq = navigationMode != NavigationMode.Manual
                        && navigationMode != mode;
            }
        }

        if (navigationModeUpdateReq) {
            navigationMode = mode;
        }
    }

    @Override
    public void onMoveEvent() {
    }

    @Override
    public void onZoomEvent() {
    }

    @Override
    public void onClick(View v) {
        updateMode(NavigationMode.Automatic, UpdateMode.ForceUpdate);
        managePositioning();
    }

    @Subscribe
    public void onLocationChange(LocationChangeEvent e) {
        currentGpsPosition = GpsComponent.locationToLatLong(e.location);

        if (navigationMode == NavigationMode.ManualInScope && distanceThresholdExceeds()) {
            updateMode(NavigationMode.Manual, UpdateMode.OnChange);
        }

        managePositioning();
    }


    private boolean shouldFocus() {
        return currentGpsPosition != null
                && (!focusedInitially || navigationMode == NavigationMode.Automatic);
    }

    private boolean distanceThresholdExceeds() {
        if (currentGpsPosition == null) {
            return false;
        }

        final LatLong center = mapView.getBoundingBox().getCenterPoint();
        final double distanceMeter = Math.abs(currentGpsPosition.sphericalDistance(center));

        return distanceMeter > 50;
    }

    private void managePositioning() {
        if (currentGpsPosition != null) {
            announcePositionUpdate();
            announceNavigationMode();
        }
    }

    private void announcePositionUpdate() {
        if (shouldFocus()) {
            mapView.getModel().mapViewPosition.animateTo(currentGpsPosition);

            if (!focusedInitially) {
                focusedInitially = true;
            }
        }
    }

    private void announceNavigationMode() {
        if (navigationMode != null && navigationModeUpdateReq) {
            if (navigationModeListener != null) {
                navigationModeListener.onNavigationModeChanged(navigationMode);
            }
            navigationModeUpdateReq = false;
        }
    }
}
