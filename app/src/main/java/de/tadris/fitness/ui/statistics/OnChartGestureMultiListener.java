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

package de.tadris.fitness.ui.statistics;

import android.view.MotionEvent;

import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;

import java.util.ArrayList;

public class OnChartGestureMultiListener implements OnChartGestureListener {

    public ArrayList<OnChartGestureListener> listeners;

    public OnChartGestureMultiListener(ArrayList<OnChartGestureListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        for (OnChartGestureListener listener : listeners) {
            listener.onChartGestureStart(me, lastPerformedGesture);
        }
    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        for (OnChartGestureListener listener : listeners) {
            listener.onChartGestureEnd(me, lastPerformedGesture);
        }
    }

    @Override
    public void onChartLongPressed(MotionEvent me) {
        for (OnChartGestureListener listener : listeners) {
            listener.onChartLongPressed(me);
        }
    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {
        for (OnChartGestureListener listener : listeners) {
            listener.onChartDoubleTapped(me);
        }
    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {
        for (OnChartGestureListener listener : listeners) {
            listener.onChartSingleTapped(me);
        }
    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
        for (OnChartGestureListener listener : listeners) {
            listener.onChartFling(me1, me2, velocityX, velocityY);
        }
    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
        for (OnChartGestureListener listener : listeners) {
            listener.onChartScale(me, scaleX, scaleY);
        }
    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {
        for (OnChartGestureListener listener : listeners) {
            listener.onChartTranslate(me, dX, dY);
        }
    }
}
