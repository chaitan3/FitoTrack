/*
 * Copyright (c) 2022 Jannis Scheibe <jannis@tadris.de>
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

import static java.lang.Math.ceil;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.pow;

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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import de.tadris.fitness.Instance;
import de.tadris.fitness.data.BaseSample;
import de.tadris.fitness.data.RouteSample;
import de.tadris.fitness.data.StatsDataTypes;
import de.tadris.fitness.data.Workout;
import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.data.preferences.UserPreferences;
import de.tadris.fitness.map.ColoringStrategy;
import de.tadris.fitness.map.GradientColoringStrategy;
import de.tadris.fitness.map.MapManager;
import de.tadris.fitness.map.MapSampleSelectionListener;
import de.tadris.fitness.map.SimpleColoringStrategy;
import de.tadris.fitness.map.WorkoutLayer;
import de.tadris.fitness.ui.workout.diagram.SpeedConverter;
import de.tadris.fitness.util.WorkoutCalculator;

public abstract class GpsWorkoutActivity extends WorkoutActivity implements MapSampleSelectionListener {

    protected GpsWorkout workout;
    protected List<RouteSample> samples;

    protected MapView mapView;
    protected WorkoutLayer workoutLayer;
    private FixedPixelCircle highlightingCircle;

    protected RouteSample selectedSample = null;

    @Override
    void initBeforeContent() {
        super.initBeforeContent();
        workout = (GpsWorkout) getWorkout();
        samples = getBaseWorkoutData().castToGpsData().samples;
    }

    @Override
    Workout findWorkout(long id) {
        return Instance.getInstance(this).db.workoutDao().getWorkoutById(id);
    }

    @Override
    List<BaseSample> findSamples(long workoutId) {
        return Arrays.asList(Instance.getInstance(this).db.workoutDao().getAllSamplesOfWorkout(workoutId));
    }

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

        workoutLayer = new WorkoutLayer(samples, new SimpleColoringStrategy(getThemePrimaryColor()), coloringStrategy);
        workoutLayer.addMapSampleSelectionListener(this);

        if (Instance.getInstance(this).userPreferences.getTrackStyleMode().equals(UserPreferences.STYLE_USAGE_ALWAYS)) {
            // Always show coloring
            workoutLayer.setSampleConverter(workout, new SpeedConverter(this));
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
            for (WorkoutCalculator.Pause pause : WorkoutCalculator.getPausesFromWorkout(getBaseWorkoutData())) {
                float radius = Math.min(10, max(2, (float) Math.sqrt((float) pause.duration / 1000)));
                mapView.addLayer(new FixedPixelCircle(pause.location, radius, pBlue, null));
            }
        }

        Paint pGreen = AndroidGraphicFactory.INSTANCE.createPaint();
        pGreen.setColor(Color.GREEN);
        mapView.addLayer(new FixedPixelCircle(samples.get(0).toLatLong(), 10, pGreen, null));

        Paint pRed = AndroidGraphicFactory.INSTANCE.createPaint();
        pRed.setColor(Color.RED);
        mapView.addLayer(new FixedPixelCircle(samples.get(samples.size() - 1).toLatLong(), 10, pRed, null));

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
        for (RouteSample realSample : samples) {
            if (diagramSample.id == realSample.id) {
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

    protected WorkoutData getGpsWorkoutData() {
        return new WorkoutData(workout, samples);
    }

    @Override
    protected List<BaseSample> aggregatedSamples(int maxSampleNumber, StatsDataTypes.TimeSpan viewFieldSpan) {

        int firstViewFieldSampleOriginalIndex = 0;

        LinkedList<BaseSample> aggregatedSamples = new LinkedList<>();
        LinkedList<BaseSample> viewFieldSamples = new LinkedList<>();

        // Generating a list with all samples in the current view field
        for (BaseSample sample : samples) {
            if (sample instanceof RouteSample) {
                if (viewFieldSpan.contains(sample.relativeTime)) {
                    viewFieldSamples.add(sample);
                }
                if (viewFieldSamples.isEmpty()) {
                    firstViewFieldSampleOriginalIndex++;
                }
            }
        }

        // Aggregate samples
        int aggregationSize = viewFieldSamples.size() > maxSampleNumber ?
                (int) (pow(2,ceil(log(viewFieldSamples.size()/3.0/maxSampleNumber))) * 3) :
                1;
        int firstGroupIndexInView = firstViewFieldSampleOriginalIndex / aggregationSize;
        int currentGroupIndex = firstGroupIndexInView;
        int aggregationGpsSampleNumber = 0; // We have to count separately since for the case
        // where there is a list combining GpsSamples and other  BaseSample types
        RouteSample combinedSample = new RouteSample();
        for (ListIterator<BaseSample> i = viewFieldSamples.listIterator(); i.hasNext();) {
            int originalIndex = i.nextIndex() + firstViewFieldSampleOriginalIndex;
            int groupIndex = originalIndex / aggregationSize;
            if (groupIndex != currentGroupIndex) {
                combinedSample = combinedSample.divide(aggregationGpsSampleNumber);
                aggregatedSamples.add(combinedSample);

                combinedSample = new RouteSample();
                aggregationGpsSampleNumber = 0;
                currentGroupIndex++;
            }

            BaseSample sample = i.next();
            if (sample instanceof RouteSample) {
                RouteSample gpsSample = (RouteSample) sample;
                combinedSample = combinedSample.add(gpsSample);
                combinedSample.id = gpsSample.id;
                aggregationGpsSampleNumber++;
            }
        }

        // Index of element directly behind the last shown group
        int behindViewIndex = (currentGroupIndex + 1) * aggregationSize;
        // Index of element directly in front of the first shown group
        int beforeViewIndex = firstGroupIndexInView * aggregationSize - 1;

        // Add those samples
        int samplesSize = samples.size();
        if (behindViewIndex < samplesSize) {
            aggregatedSamples.add(samples.get(behindViewIndex));
            // Also add the last sample to the list
            aggregatedSamples.add(samples.get(samples.size()-1));
        }
        if (beforeViewIndex > 0) {
            aggregatedSamples.add(0, samples.get(beforeViewIndex));
            // Also add the first sample to the list
            aggregatedSamples.add(0, samples.get(0));
        }

        return aggregatedSamples;
    }
}
