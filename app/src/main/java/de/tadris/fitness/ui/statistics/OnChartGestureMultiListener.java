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
