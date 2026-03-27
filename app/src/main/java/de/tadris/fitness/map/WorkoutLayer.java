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

package de.tadris.fitness.map;

import androidx.annotation.Nullable;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.tadris.fitness.data.RouteSample;
import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.ui.workout.diagram.SampleConverter;

public class WorkoutLayer extends Layer {

    private static final byte STROKE_MIN_ZOOM = 12;
    private final GraphicFactory graphicFactory;
    private final boolean keepAligned;
    private Paint paintStroke;
    private double strokeIncrease = 1;
    private BoundingBox boundingBox;
    private final List<RouteSample> samples;
    private Set<MapSampleSelectionListener> listeners;

    private ColoringStrategy fallbackColoringStrategy; // if workout is displayed without coloring this will be used

    @Nullable
    private SampleConverter sampleConverter; // get values from samples
    @Nullable
    private ColoringStrategy coloringStrategy; // get colors from values

    private static Paint getDEFAULT_PAINT_STROKE() {
        Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(14f);
        return paint;
    }

    public WorkoutLayer(List<RouteSample> samples, ColoringStrategy fallbackColoringStrategy, @Nullable ColoringStrategy coloringStrategy) {
        this(getDEFAULT_PAINT_STROKE(), samples);
        this.fallbackColoringStrategy = fallbackColoringStrategy;
        this.coloringStrategy = coloringStrategy;
        listeners = new HashSet<>();
    }

    public void addMapSampleSelectionListener(MapSampleSelectionListener listener){
        this.listeners.add(listener);
    }

    private WorkoutLayer(Paint paintStroke, List<RouteSample> samples) {
        super();
        this.keepAligned = false;
        this.paintStroke = paintStroke;
        this.graphicFactory = AndroidGraphicFactory.INSTANCE;
        this.samples = samples;

        //We need to calculate the Bounding box hence need to still convert the items to latLongs
        List<LatLong> points = new ArrayList<>();
        for (RouteSample sample : samples) {
            points.add(sample.toLatLong());
        }
        boundingBox = points.isEmpty() ? null : new BoundingBox(points);
    }

    private void onSampleSelected(RouteSample sample) {
        for (MapSampleSelectionListener listener : listeners) {
            listener.onMapSelectionChanged(sample);
        }
    }

    /**
     * State variable to hold whether a selection was done
     */
    private boolean hasSelection = false;

    public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {

        if (listeners.isEmpty()) {
            return false;
        }
        double max_distance = Math.max(20 / 2 * this.displayModel.getScaleFactor(),
                this.paintStroke.getStrokeWidth() / 2);

        // on tap find the closest workout sample and if within a certain range select it
        RouteSample sample = findClosestSample(tapLatLong);

        assert sample != null;
        double distance =  sample.toLatLong().sphericalDistance(tapLatLong);
        System.out.println("distance " + distance + " max (" + max_distance + ")");
        if (distance < max_distance) {
            onSampleSelected(sample);
            hasSelection = true;
            return true;
        } else if (hasSelection) {//if the user clicks outside the area deselect item
            onSampleSelected(null);
            hasSelection = false;
        }
        return false;
    }

    /**
     * @param latLong location
     * @return the sample that is closest to the selected location
     */
    private RouteSample findClosestSample(LatLong latLong) {
        RouteSample sample = null;
        double shortestDistance = 0;

        for (int i = 0; i < this.samples.size() - 1; i++) {
            double dist = latLong.sphericalDistance(samples.get(i).toLatLong());
            if (sample == null || (dist < shortestDistance)) {
                sample = samples.get(i);
                shortestDistance = dist;
            }
        }
        return sample;
    }

    @Override
    public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint, Rotation rotation) {
        if (this.samples.isEmpty() || this.paintStroke == null) {
            return;
        }

        if (this.boundingBox != null && !this.boundingBox.intersects(boundingBox)) {
            return;
        }

        Iterator<RouteSample> sampleIterator = this.samples.iterator();

        RouteSample sample = sampleIterator.next();
        long mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.getTileSize());
        float x = (float) (MercatorProjection.longitudeToPixelX(sample.getLongitude(), mapSize) - topLeftPoint.x);
        float y = (float) (MercatorProjection.latitudeToPixelY(sample.getLatitude(), mapSize) - topLeftPoint.y);

        if (this.keepAligned) {
            this.paintStroke.setBitmapShaderShift(topLeftPoint);
        }
        float strokeWidth = this.paintStroke.getStrokeWidth();
        if (this.strokeIncrease > 1) {
            float scale = (float) Math.pow(this.strokeIncrease, Math.max(zoomLevel - STROKE_MIN_ZOOM, 0));
            this.paintStroke.setStrokeWidth(strokeWidth * scale);
        }

        Path path = this.graphicFactory.createPath();
        while (sampleIterator.hasNext()) {
            path.moveTo(x, y);
            sample = sampleIterator.next();
            x = (float) (MercatorProjection.longitudeToPixelX(sample.getLongitude(), mapSize) - topLeftPoint.x);
            y = (float) (MercatorProjection.latitudeToPixelY(sample.getLatitude(), mapSize) - topLeftPoint.y);
            paintStroke.setColor(getColorFromSample(sample));
            path.lineTo(x, y);
            canvas.drawPath(path, this.paintStroke);
            path.clear();
        }
        this.paintStroke.setStrokeWidth(strokeWidth);
    }

    private int getColorFromSample(RouteSample sample) {
        if (coloringStrategy != null && sampleConverter != null) {
            double value = sampleConverter.getValue(sample);
            return coloringStrategy.getColor(value);
        } else {
            return fallbackColoringStrategy.getColor(0);
        }
    }

    public void setFallbackColoringStrategy(ColoringStrategy strategy) {
        this.fallbackColoringStrategy = strategy;
        requestRedraw();
    }

    public void setColoringStrategy(WorkoutData workout, ColoringStrategy coloringStrategy) {
        this.coloringStrategy = coloringStrategy;
        refreshColoringMinMax(workout);
    }

    public void setSampleConverter(WorkoutData workout, @Nullable SampleConverter sampleConverter) {
        this.sampleConverter = sampleConverter;
        refreshColoringMinMax(workout);
    }

    private void refreshColoringMinMax(WorkoutData data) {
        if (coloringStrategy != null && sampleConverter != null) {
            coloringStrategy.setMin(sampleConverter.getMinValue(data));
            coloringStrategy.setMax(sampleConverter.getMaxValue(data));
        }
        requestRedraw();
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }
}
