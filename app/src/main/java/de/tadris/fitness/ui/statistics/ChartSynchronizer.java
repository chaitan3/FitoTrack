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

package de.tadris.fitness.ui.statistics;

import android.graphics.Matrix;
import android.view.MotionEvent;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;

import java.util.ArrayList;
import java.util.List;

public class ChartSynchronizer {

    List<Chart<?>> synchronyCharts;

    public ChartSynchronizer() {
        synchronyCharts = new ArrayList<>();
    }

    public OnChartGestureListener addChart(Chart<?> chart) {
        synchronyCharts.add(chart);
        return gestureListener(chart);
    }

    protected void syncCharts(Chart<?> sender, List<Chart<?>> retrievers) {
        Matrix senderMatrix;
        float[] senderValues = new float[9];
        senderMatrix = sender.getViewPortHandler().getMatrixTouch();
        senderMatrix.getValues(senderValues);

        Matrix retrieverMatrix;
        float[] retrieverValues = new float[9];

        for (Chart<?> retriever : retrievers) {
            if (retriever != sender) {
                retrieverMatrix = retriever.getViewPortHandler().getMatrixTouch();
                retrieverMatrix.getValues(retrieverValues);
                retrieverValues[Matrix.MSCALE_X] = senderValues[Matrix.MSCALE_X];
                retrieverValues[Matrix.MTRANS_X] = senderValues[Matrix.MTRANS_X];
                retrieverMatrix.setValues(retrieverValues);
                retriever.getViewPortHandler().refresh(retrieverMatrix, retriever, true);
            }
        }
    }

    public OnChartGestureListener gestureListener(Chart<?> chart) {
        return new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartLongPressed(MotionEvent me) {

            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {
                syncCharts(chart, synchronyCharts);
            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {

            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
                syncCharts(chart, synchronyCharts);
            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {
                syncCharts(chart, synchronyCharts);
            }
        };
    }
}
