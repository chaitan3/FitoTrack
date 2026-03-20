/*
 * Copyright (c) 2021 Jannis Scheibe <jannis@tadris.de>
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

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.overlay.FixedPixelCircle;

import java.util.ArrayList;
import java.util.List;

import de.tadris.fitness.R;
import de.tadris.fitness.data.RouteSample;
import de.tadris.fitness.data.WorkoutCutter;
import de.tadris.fitness.data.WorkoutCutter;
import de.tadris.fitness.map.SimpleColoringStrategy;
import de.tadris.fitness.map.WorkoutLayer;

public class EditWorkoutStartEndActivity extends ShowWorkoutMapDiagramActivity {

    public final static int INTENT_RESULT_CODE_WORKOUT_MODIFIED = 0x2a;

    MenuItem startMenuItem;
    MenuItem endMenuItem;
    MenuItem applyMenuItem;

    private FixedPixelCircle newStartLayer;
    private FixedPixelCircle newEndLayer;
    private WorkoutLayer newWorkoutLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        workoutLayer.setColoringStrategy(data, new SimpleColoringStrategy(getThemePrimaryColor()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_workout_start_end_menu, menu);
        startMenuItem = menu.findItem(R.id.actionMoveStartHere);
        endMenuItem = menu.findItem(R.id.actionMoveEndHere);
        applyMenuItem = menu.findItem(R.id.actionMoveApply);
        return true;
    }

    RouteSample selectedStartSample = null;
    RouteSample selectedEndSample = null;


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.actionMoveHelp) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.editWorkoutStartEnd)
                    .setMessage(R.string.editStartEndHelp)
                    .setPositiveButton(R.string.okay, null)
                    .show();
        }

        if (item.getItemId() == R.id.actionMoveStartHere) {
            if (selectedSample != null) {
                if (selectedEndSample != null && selectedSample.getRelativeTime() > selectedEndSample.getRelativeTime()) {
                    // do not allow to place the start sample behind the end marker

                } else {
                    selectedStartSample = selectedSample;
                }
            }
        }

        if (item.getItemId() == R.id.actionMoveEndHere) {
            if (selectedSample != null){
                if (selectedStartSample != null && selectedSample.getRelativeTime() < selectedStartSample.getRelativeTime()) {
                    // do not allow to place the and market before the start marker(in time)
                } else {
                    selectedEndSample = selectedSample;
                }
            }
        }

        /* make things look a bit nicer */
        if (newStartLayer != null) {
            mapView.getLayerManager().getLayers().remove(newStartLayer);
        }
        if (newEndLayer != null) {
            mapView.getLayerManager().getLayers().remove(newEndLayer);
        }
        if (newWorkoutLayer != null) {
            mapView.getLayerManager().getLayers().remove(newWorkoutLayer);
        }

        List<RouteSample> samples = data.getSamples();
        RouteSample drawStartSample = selectedStartSample != null ? selectedStartSample : samples.get(0);
        RouteSample drawEndSample = selectedEndSample != null ? selectedEndSample : samples.get(samples.size() - 1);
        List<RouteSample> newWorkoutSamples = new ArrayList<RouteSample>();

        boolean contained = false;
        for (RouteSample s : samples) {
            if (!contained && s.getId() == drawStartSample.getId()) {
                contained = true;
            }
            if (contained) {
                //update relative time here?
                newWorkoutSamples.add(s);
            }
            if (s.getId() == drawEndSample.getId()) {
                break;
            }
        }
        //Draw new track
        if (selectedStartSample != null || selectedEndSample != null) {
            workoutLayer.setColoringStrategy(data, new SimpleColoringStrategy(Color.GRAY));
            newWorkoutLayer = new WorkoutLayer(newWorkoutSamples, new SimpleColoringStrategy(getThemePrimaryColor()), null);
            mapView.addLayer(newWorkoutLayer);

        }
        //Draw "new" start
        if (selectedStartSample != null) {
            Paint p = AndroidGraphicFactory.INSTANCE.createPaint();
            p.setColor(Color.GREEN);
            newStartLayer = new FixedPixelCircle(selectedStartSample.toLatLong(), 12, p, null);
            mapView.addLayer(newStartLayer);
        }
        // Draw "new" end
        if (selectedEndSample != null) {
            Paint p = AndroidGraphicFactory.INSTANCE.createPaint();
            p.setColor(Color.RED);
            newEndLayer = new FixedPixelCircle(selectedEndSample.toLatLong(), 12, p, null);
            mapView.addLayer(newEndLayer);
        }

        if (item.getItemId() == R.id.actionMoveApply) {
            if (selectedStartSample != null || selectedEndSample != null){
                new AlertDialog.Builder(this)
                        .setTitle(R.string.saveChangesQuestion)
                        .setMessage(R.string.saveChangesQuestionMessage)
                        .setPositiveButton(R.string.save, (dialog, which) -> applyChanges())
                        .setNegativeButton(R.string.cancel, null)
                        .create().show();
            } else {
                Toast.makeText(this, R.string.nothingSelected, Toast.LENGTH_SHORT).show();
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void applyChanges() {
        // Cut workout
        new WorkoutCutter(this, data, selectedStartSample, selectedEndSample).save();

        Intent intent=new Intent();
        setResult(INTENT_RESULT_CODE_WORKOUT_MODIFIED,intent);
        finish();
    }

}