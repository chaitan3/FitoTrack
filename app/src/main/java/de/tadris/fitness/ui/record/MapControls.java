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

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.mapsforge.map.android.view.MapView;

import de.tadris.fitness.util.AbstractAnimatorListener;

public class MapControls implements NavigationModeHandler.NavigationModeListener {

    private final int MSG_ZOOM_CONTROLS_HIDE = 0;
    private final int ZOOM_CONTROLS_TIMEOUT = 2000;

    private MapView mapView;
    private FloatingActionButton mapFocusGpsBtn;
    private FloatingActionButton mapZoomInBtn;
    private FloatingActionButton mapZoomOutBtn;
    private NavigationModeHandler.NavigationMode navigationMode = NavigationModeHandler.NavigationMode.Automatic;

    private final Handler zoomControlsHideHandler;
    private final NavigationModeHandler navigationModeHandler;
    private boolean focusBtnNeeded = false;

    public MapControls(MapView mapview, NavigationModeHandler navigationModeHandler, FloatingActionButton mapFocusGpsBtn,
                       FloatingActionButton mapZoomInBtn,
                       FloatingActionButton mapZoomOutBtn) {
        this.mapView = mapview;
        this.mapFocusGpsBtn = mapFocusGpsBtn;
        this.mapZoomInBtn = mapZoomInBtn;
        this.mapZoomOutBtn = mapZoomOutBtn;
        this.navigationModeHandler = navigationModeHandler;

        this.zoomControlsHideHandler = new Handler(msg -> {
            if (navigationMode == NavigationModeHandler.NavigationMode.Automatic) {
                hide();
            }
            return true;
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    void init() {
        navigationModeHandler.setNavigationModeListener(this);

        mapFocusGpsBtn.setVisibility(View.GONE);
        mapFocusGpsBtn.setOnClickListener(navigationModeHandler);

        mapZoomInBtn.setVisibility(View.GONE);
        mapZoomInBtn.setOnClickListener((View v) -> {
            zoom(1);
        });

        mapZoomOutBtn.setVisibility(View.GONE);
        mapZoomOutBtn.setOnClickListener((View v) -> {
            zoom(-1);
        });

        mapView.setClickable(true);
        mapView.setOnTouchListener((View v, MotionEvent event) -> {
            if (event.getPointerCount() > 1) {
                onZoomLevelUpdate();
            }
            return navigationModeHandler.onTouch(v, event);
        });
    }

    private void zoom(int difference) {
        mapView.getModel().mapViewPosition.zoom((byte) difference);
        onZoomLevelUpdate();
        showZoomControlsWithTimeout();
    }

    private void onZoomLevelUpdate() {
        final byte zoomLevel = mapView.getModel().mapViewPosition.getZoomLevel();
        if (zoomLevel >= getMaxZoomLevel() || zoomLevel <= getMinZoomLevel()) {
            if (zoomLevel <= getMinZoomLevel()) {
                mapView.setZoomLevel(getMinZoomLevel());
                mapZoomOutBtn.setEnabled(false);
                mapZoomInBtn.setEnabled(true);
            } else if (zoomLevel >= getMaxZoomLevel()) {
                mapView.setZoomLevel(getMaxZoomLevel());
                mapZoomInBtn.setEnabled(false);
                mapZoomOutBtn.setEnabled(true);
            }
        } else {
            mapZoomInBtn.setEnabled(true);
            mapZoomOutBtn.setEnabled(true);
        }
    }

    private void fadeButton(View view, int visibility) {
        view.clearAnimation();
        view.animate().alpha(visibility == View.VISIBLE ? 1f : 0f).setDuration(500).setListener(new AbstractAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(visibility);
            }
        });
    }

    private void show() {
        fadeButton(mapFocusGpsBtn, focusBtnNeeded ? View.VISIBLE : View.GONE);
        fadeButton(mapZoomInBtn, View.VISIBLE);
        fadeButton(mapZoomOutBtn, View.VISIBLE);
    }

    private void hide() {
        fadeButton(mapFocusGpsBtn, View.GONE);
        fadeButton(mapZoomInBtn, View.GONE);
        fadeButton(mapZoomOutBtn, View.GONE);
    }

    private void showZoomControls() {
        this.zoomControlsHideHandler.removeMessages(MSG_ZOOM_CONTROLS_HIDE);
        this.show();
    }

    private void showZoomControlsWithTimeout() {
        showZoomControls();
        this.zoomControlsHideHandler.removeMessages(MSG_ZOOM_CONTROLS_HIDE);
        boolean sent = this.zoomControlsHideHandler.sendEmptyMessageDelayed(MSG_ZOOM_CONTROLS_HIDE, ZOOM_CONTROLS_TIMEOUT);
        assert(sent);
    }


    @Override
    public void onNavigationModeChanged(final NavigationModeHandler.NavigationMode mode) {
        this.navigationMode = mode;

        switch(mode){
            case ManualInScope:
                focusBtnNeeded = false;
                showZoomControls();
                break;
            case Automatic:
                focusBtnNeeded = false;
                showZoomControlsWithTimeout();
                break;
            case Manual:
                focusBtnNeeded = true;
                showZoomControls();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + mode);
        }
    }

    public void externalZoomInRequest() {
        mapZoomInBtn.callOnClick();
    }

    public void externalZoomOutRequest() {
        mapZoomOutBtn.callOnClick();
    }

    private byte getMinZoomLevel() {
        return mapView.getModel().mapViewPosition.getZoomLevelMin();
    }

    private byte getMaxZoomLevel() {
        return mapView.getModel().mapViewPosition.getZoomLevelMax();
    }

}