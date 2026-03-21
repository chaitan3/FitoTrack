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

package de.tadris.fitness.ui.workout;

import static java.lang.Math.max;

import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.overlay.FixedPixelCircle;

import de.tadris.fitness.Instance;
import de.tadris.fitness.data.BaseSample;
import de.tadris.fitness.data.RouteSample;
import de.tadris.fitness.data.preferences.UserPreferences;
import de.tadris.fitness.map.ColoringStrategy;
import de.tadris.fitness.map.GradientColoringStrategy;
import de.tadris.fitness.map.MapManager;
import de.tadris.fitness.map.MapSampleSelectionListener;
import de.tadris.fitness.map.SimpleColoringStrategy;
import de.tadris.fitness.map.WorkoutLayer;
import de.tadris.fitness.ui.workout.diagram.SpeedConverter;
import de.tadris.fitness.util.WorkoutCalculator;

public abstract class MapWorkoutActivity extends WorkoutActivity implements MapSampleSelectionListener {

    protected MapView mapView;
    protected WorkoutLayer workoutLayer;
    private FixedPixelCircle highlightingCircle;

    protected RouteSample selectedSample = null;

    void addMap() {
        mapView = new MapManager(this).setupMap();
        String trackStyle = Instance.getInstance(this).userPreferences.getTrackStyle();
        // emulate current behaviour


        ColoringStrategy coloringStrategy;

        // predefined set of settings that play with the colors, the mapping of the color to some
        // value and whether to blend or not. In the future it would be nice to have a nice editor
        // in the settings to tweak the numbers here and possibly create good looking colors.
        switch (trackStyle) {
            case "purple_rain":
                /* a nice set of colors generated from colorbrewer */
                coloringStrategy = GradientColoringStrategy.fromPattern(GradientColoringStrategy.PATTERN_PURPLE, true);
                break;
            case "pink_mist":
                /* Pink is nice */
                coloringStrategy = GradientColoringStrategy.fromPattern(GradientColoringStrategy.PATTERN_PINK, false);
                break;
            case "rainbow_warrior":
                /* Attempt to use different colors, this would be best suited for a fixed scale e.g. green is target value , red is to fast , yellow it to slow */
                coloringStrategy = GradientColoringStrategy.fromPattern(GradientColoringStrategy.PATTERN_MAP, true);
                break;
            case "height_map":
                /* based on height map colors from green till almost black*/
                coloringStrategy = GradientColoringStrategy.fromPattern(GradientColoringStrategy.PATTERN_HEIGHT_MAP, true);
                break;
            case "bright_night":
                coloringStrategy = GradientColoringStrategy.fromPattern(GradientColoringStrategy.PATTERN_BRIGHT, false);
                break;
            case "mondriaan":
                coloringStrategy = GradientColoringStrategy.fromPattern(GradientColoringStrategy.PATTERN_YELLOW_RED_BLUE, false);
                break;
            default: // theme_color
                /* default: original color based on theme*/
                coloringStrategy = new SimpleColoringStrategy(getThemePrimaryColor());
                break;
        }

        workoutLayer = new WorkoutLayer(data.getSamples(), new SimpleColoringStrategy(getThemePrimaryColor()), coloringStrategy);
        workoutLayer.addMapSampleSelectionListener(this);

        if (Instance.getInstance(this).userPreferences.getTrackStyleMode().equals(UserPreferences.STYLE_USAGE_ALWAYS)) {
            // Always show coloring
            workoutLayer.setSampleConverter(data, new SpeedConverter(this));
        }

        mapView.addLayer(workoutLayer);

        final BoundingBox bounds = workoutLayer.getBoundingBox().extendMeters(50);
        mHandler.postDelayed(() -> {
            mapView.getModel().mapViewPosition.setMapPosition(new MapPosition(bounds.getCenterPoint(),
                    (LatLongUtils.zoomForBounds(mapView.getDimension(), bounds,
                            mapView.getModel().displayModel.getTileSize()))));
            mapView.animate().alpha(1f).setDuration(1000).start();
        }, 1000);

        mapRoot = new LinearLayout(this);
        mapRoot.setOrientation(LinearLayout.VERTICAL);
        mapRoot.addView(mapView);

        root.addView(mapRoot, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                fullScreenItems ? ViewGroup.LayoutParams.MATCH_PARENT : getMapHeight()));
        mapView.setAlpha(0);

        if (showPauses) {
            Paint pBlue = AndroidGraphicFactory.INSTANCE.createPaint();
            pBlue.setColor(Color.BLUE);
            for (WorkoutCalculator.PauseLocation pauseLocation : WorkoutCalculator.getPausesFromWorkout(data)) {
                float radius = Math.min(10, max(2, (float) Math.sqrt((float) pauseLocation.getPause().getDuration() / 1000)));
                mapView.addLayer(new FixedPixelCircle(pauseLocation.getLocation(), radius, pBlue, null));
            }
        }

        Paint pGreen = AndroidGraphicFactory.INSTANCE.createPaint();
        pGreen.setColor(Color.GREEN);
        mapView.addLayer(new FixedPixelCircle(data.getSamples().get(0).toLatLong(), 10, pGreen, null));

        Paint pRed = AndroidGraphicFactory.INSTANCE.createPaint();
        pRed.setColor(Color.RED);
        mapView.addLayer(new FixedPixelCircle(data.getSamples().get(data.getSamples().size() - 1).toLatLong(), 10, pRed, null));

        mapView.setClickable(false);

    }

    @Override
    public void onMapSelectionChanged(RouteSample sample) {
        //nada onChartSelectionChanged(sample)
    }

    @Override
    protected void onChartSelectionChanged(BaseSample clickedSample) {
        //remove any previous layer
        if (selectedSample != null) {
            if (highlightingCircle != null) {
                mapView.getLayerManager().getLayers().remove(highlightingCircle);
            }
        }

        selectedSample = null;

        // If no sample was selected return now. This can happen onNothingSelected
        if (clickedSample == null){
            return;
        }

        // Find real sample with same id as the clicked one
        RouteSample diagramSample = (RouteSample) clickedSample;
        for (RouteSample realSample : data.getSamples()) {
            if (diagramSample.getId() == realSample.getId()) {
                selectedSample = realSample;
            }
        }

        // if a sample was selected show it on the map
        if (selectedSample != null) {
            Paint p = AndroidGraphicFactory.INSTANCE.createPaint();
            p.setColor(0xff693cff);
            highlightingCircle = new FixedPixelCircle(selectedSample.toLatLong(), 10, p, null);
            mapView.addLayer(highlightingCircle);

            if (!mapView.getBoundingBox().contains(selectedSample.toLatLong())) {
                mapView.getModel().mapViewPosition.animateTo(selectedSample.toLatLong());
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) {
            mapView.destroyAll();
        }
        AndroidGraphicFactory.clearResourceMemoryCache();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            for (Layer layer : mapView.getLayerManager().getLayers()) {
                if (layer instanceof TileDownloadLayer) {
                    ((TileDownloadLayer) layer).onPause();
                }
            }
        }
    }

    public void onResume() {
        super.onResume();
        if (mapView != null) {
            for (Layer layer : mapView.getLayerManager().getLayers()) {
                if (layer instanceof TileDownloadLayer) {
                    ((TileDownloadLayer) layer).onResume();
                }
            }
        }
    }

}
