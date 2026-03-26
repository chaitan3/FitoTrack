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

package de.tadris.fitness.stats;

import static java.lang.Math.min;

import android.content.Context;

import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.tadris.fitness.Instance;
import de.tadris.fitness.aggregation.AggregationSpan;
import de.tadris.fitness.data.WorkoutType;
import de.tadris.fitness.data.WorkoutTypeManager;
import de.tadris.fitness.util.charts.DataSetStyles;
import de.tadris.fitness.util.charts.formatter.DayTimeFormatter;
import de.tadris.fitness.util.charts.formatter.TimeFormatter;

public class StatsProvider {
    Context ctx;
    StatsDataProvider dataProvider;

    public StatsProvider(Context ctx) {
        this.ctx = ctx;
        dataProvider = new StatsDataProvider(ctx);
    }

    public enum Reduction {
        MINIMUM,
        MAXIMUM,
        SUM,
        AVERAGE
    }

    public BarDataSet numberOfActivities(StatsDataTypes.TimeSpan timeSpan) throws NoDataException {
        return createBarSetPerActivity(timeSpan, WorkoutProperty.NUMBER);
    }

    public BarDataSet totalDurations(StatsDataTypes.TimeSpan timeSpan) throws NoDataException {
        return createBarSetPerActivity(timeSpan, WorkoutProperty.DURATION);
    }

    public BarDataSet totalDistances(StatsDataTypes.TimeSpan timeSpan) throws NoDataException {
        return createBarSetPerActivity(timeSpan, WorkoutProperty.LENGTH);
    }

    public BarDataSet createBarSetPerActivity(StatsDataTypes.TimeSpan timeSpan, WorkoutProperty workoutProperty) throws NoDataException {

        ArrayList<BarEntry> barEntries = new ArrayList<>();
        int barNumber = 0;

        HashMap<WorkoutType, Float> valuePerType = new HashMap<>();

        ArrayList<StatsDataTypes.DataPoint> workouts = dataProvider.getData(workoutProperty, WorkoutTypeManager.getInstance().getAllTypes(ctx), timeSpan);

        if (workouts.isEmpty()) {
            throw new NoDataException();
        }

        for (StatsDataTypes.DataPoint dataPoint : workouts) {
            Float previousValue = valuePerType.get(dataPoint.workoutType);
            if (previousValue == null) previousValue = 0f;

            valuePerType.put(dataPoint.workoutType, previousValue + (long) dataPoint.value);
        }

        // Sort numberOfWorkouts map
        ArrayList<Map.Entry<WorkoutType, Float>> sortedValues = new ArrayList<>(valuePerType.entrySet());
        Collections.sort(sortedValues, (first, second) -> second.getValue().compareTo(first.getValue()));


        for (Map.Entry<WorkoutType, Float> entry : sortedValues) {
            float value = entry.getValue();

            barEntries.add(new BarEntry((float) barNumber, value, entry.getKey()));

            barNumber++;
        }

        BarDataSet dataSet = DataSetStyles.applyDefaultBarStyle(ctx, new BarDataSet(barEntries, workoutProperty.name()));
        dataSet.setValueFormatter(workoutProperty.getValueFormatter(ctx, dataSet.getYMax()));
        return dataSet;
    }

    /**
     * Uses a reduction method to calculate a single value from all entries in a specific time span
     * for a given list of workout types and a given workout property.
     *
     * @param timeSpan     All workouts done during this time span are respected.
     * @param workoutTypes All workouts of a type present in this list are respected.
     * @param property     The property which should be used for the reduction.
     * @param reduction    The reduction method which should be used.
     * @return A single value calculated by a reduction.
     * @throws NoDataException in case there is no data fitting to the given parameters.
     */
    public double getValue(StatsDataTypes.TimeSpan timeSpan, List<WorkoutType> workoutTypes, WorkoutProperty property, Reduction reduction) throws NoDataException {
        ArrayList<StatsDataTypes.DataPoint> values = dataProvider.getData(property, workoutTypes, timeSpan);
        if (values.isEmpty()) {
            throw new NoDataException();
        }

        double value;

        if (reduction == Reduction.MINIMUM) {
            if (property == WorkoutProperty.START || property == WorkoutProperty.END) {
                value = values.get(0).value % TimeUnit.DAYS.toMillis(1);
                for (StatsDataTypes.DataPoint point : values)
                    if (value > point.value % TimeUnit.DAYS.toMillis(1))
                        value = point.value % TimeUnit.DAYS.toMillis(1);
            } else {
                value = values.get(0).value;
                for (StatsDataTypes.DataPoint point : values)
                    if (value > point.value)
                        value = point.value;
            }
        } else if (reduction == Reduction.MAXIMUM) {
            if (property == WorkoutProperty.START || property == WorkoutProperty.END) {
                value = values.get(0).value % TimeUnit.DAYS.toMillis(1);
                for (StatsDataTypes.DataPoint point : values)
                    if (value < point.value % TimeUnit.DAYS.toMillis(1))
                        value = point.value % TimeUnit.DAYS.toMillis(1);
            } else {
                value = values.get(0).value;
                for (StatsDataTypes.DataPoint point : values)
                    if (value < point.value)
                        value = point.value;
            }
        } else {
            value = 0;

            if (property == WorkoutProperty.START || property == WorkoutProperty.END) {
                for (StatsDataTypes.DataPoint point : values)
                    value += (point.value % TimeUnit.DAYS.toMillis(1));
            } else {
                for (StatsDataTypes.DataPoint point : values)
                    value += point.value;

            }
            if (reduction == Reduction.AVERAGE) {
                value /= values.size();
            }
        }
        return value;
    }

    public BarDataSet createHistogramData(List<Double> values, int bins, String label) {
        Double[] weights = new Double[values.size()];
        Arrays.fill(weights, 1.0);
        return createWeightedHistogramData(values, Arrays.asList(weights), bins, label);
    }

    public BarDataSet createWeightedHistogramData(List<Double> values, List<Double> weights, int bins, String label) {
        Collections.sort(values);
        double min = values.get(0);
        double max = values.get(values.size()-1);
        double binWidth = (max-min)/bins;
        double[] histogram = new double[bins];
        int binIndex = 0;

        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) <= min((binIndex + 1) * binWidth + min, max))
                histogram[binIndex] += weights.get(i);
            else {
                binIndex++;
            }
        }

        ArrayList<BarEntry> barEntries = new ArrayList<>();
        for (int i = 0; i < bins; i++) {
            barEntries.add(new BarEntry((float) (min + binWidth * i), (float) histogram[i]));
        }

        return DataSetStyles.applyDefaultBarStyle(ctx,
                new BarDataSet(barEntries, label));
    }

    public CandleDataSet getCandleData(AggregationSpan span, List<WorkoutType> workoutTypes, WorkoutProperty workoutProperty) throws NoDataException {
        ArrayList<CandleEntry> candleEntries = getCombinedCandleData(span, workoutTypes, workoutProperty);

        CandleDataSet dataSet = DataSetStyles.applyDefaultCandleStyle(ctx, new CandleDataSet(candleEntries,
                ctx.getString(workoutProperty.getTitleRes())));
        if (workoutProperty == WorkoutProperty.START || workoutProperty == WorkoutProperty.END) {
            dataSet.setValueFormatter(new DayTimeFormatter(ctx));
        } else {
            dataSet.setValueFormatter(workoutProperty.getValueFormatter(ctx, dataSet.getYMax()));
        }
        return dataSet;
    }

    public BarDataSet getSumData(AggregationSpan span, List<WorkoutType> workoutTypes, WorkoutProperty workoutProperty) throws NoDataException {
        ArrayList<BarEntry> barEntries = getCombinedSumData(span, workoutTypes, workoutProperty);
        BarDataSet dataSet = DataSetStyles.applyDefaultBarStyle(ctx, new BarDataSet(barEntries,
                ctx.getString(workoutProperty.getTitleRes())));
        dataSet.setValueFormatter(workoutProperty.getValueFormatter(ctx, dataSet.getYMax()));
        return dataSet;
    }

    public static TimeFormatter getCorrectTimeFormatter(TimeUnit unit, long maxTime) {
        if (unit.toMillis(maxTime) > TimeUnit.HOURS.toMillis(1))
            return new TimeFormatter(unit, false, true, true);
        else
            return new TimeFormatter(unit, true, true, false);
    }

    private ArrayList<StatsDataTypes.DataPoint> findDataPointsInAggregationSpan(ArrayList<StatsDataTypes.DataPoint> data, Calendar startTime, AggregationSpan span) {
        // Retrieve the workoutProperty for all workouts in the specific time span
        StatsDataTypes.TimeSpan timeSpan = new StatsDataTypes.TimeSpan(startTime.getTimeInMillis(), span.getAggregationEnd(startTime).getTimeInMillis());
        ArrayList<StatsDataTypes.DataPoint> intervalData = new ArrayList<>();

        // Create list of data points belonging to the same time span
        Iterator<StatsDataTypes.DataPoint> dataPointIterator = data.iterator();
        while (dataPointIterator.hasNext()) {
            StatsDataTypes.DataPoint dataPoint = dataPointIterator.next();
            if (timeSpan.contains(dataPoint.time)) {
                intervalData.add(dataPoint);
                data.remove(dataPointIterator);
            }
        }
        return intervalData;
    }

    public ArrayList<CandleEntry> getCombinedCandleData(AggregationSpan span, List<WorkoutType> workoutTypes, WorkoutProperty workoutProperty) throws NoDataException {
        ArrayList<StatsDataTypes.DataPoint> data = dataProvider.getData(workoutProperty,
                workoutTypes);

        if (data.isEmpty()) {
            throw new NoDataException();
        }

        if (workoutProperty == WorkoutProperty.START || workoutProperty == WorkoutProperty.END) {
            // Time data -> convert to time ot the day
            for (int i = 0; i < data.size(); i++) {
                data.get(i).value %= TimeUnit.DAYS.toMillis(1); // day->milliseconds
            }
        }

        ArrayList<CandleEntry> candleEntries = new ArrayList<>();

        if (span == AggregationSpan.SINGLE) {
            // No aggregation
            for (StatsDataTypes.DataPoint dataPoint : data) {
                float value = (float) dataPoint.value;
                candleEntries.add(new CandleEntry((float) dataPoint.time, value, value, value, value, dataPoint));
            }
        } else {
            // Find start and end time of workouts
            long oldestWorkoutTime = Collections.min(data, StatsDataTypes.DataPoint.timeComparator).time;
            long newestWorkoutTime = Collections.max(data, StatsDataTypes.DataPoint.timeComparator).time;

            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(oldestWorkoutTime);

            span.setCalendarToAggregationStart(calendar, Instance.getInstance(ctx).userPreferences.getFirstDayOfWeek());


            // Iterate all time spans from first workout time to last workout time
            while (calendar.getTimeInMillis() < newestWorkoutTime) {
                ArrayList<StatsDataTypes.DataPoint> intervalData = findDataPointsInAggregationSpan(data, calendar, span);

                // Calculate min, max and average of the data of the span and store in the candle list
                if (intervalData.size() > 0) {
                    float min = (float) Collections.min(intervalData, StatsDataTypes.DataPoint.valueComparator).value;
                    float max = (float) Collections.max(intervalData, StatsDataTypes.DataPoint.valueComparator).value;
                    float mean = calculateValueAverage(intervalData);
                    candleEntries.add(new CandleEntry((float) calendar.getTimeInMillis(), max, min, mean, mean));
                }

                // Increment time span
                if (span != AggregationSpan.ALL) {
                    calendar.add(span.calendarField, 1);
                } else  {
                    calendar.setTimeInMillis(Long.MAX_VALUE);
                }
            }
        }

        return candleEntries;
    }

    public ArrayList<BarEntry> getCombinedSumData(AggregationSpan span, List<WorkoutType> workoutTypes, WorkoutProperty workoutProperty) throws NoDataException {
        ArrayList<StatsDataTypes.DataPoint> data = dataProvider.getData(workoutProperty,
                workoutTypes);

        if (data.isEmpty()) {
            throw new NoDataException();
        }

        ArrayList<BarEntry> barEntries = new ArrayList<>();

        if (span == AggregationSpan.SINGLE) {
            // No aggregation
            for (StatsDataTypes.DataPoint dataPoint : data) {
                float value = (float) dataPoint.value;
                barEntries.add(new BarEntry((float) dataPoint.time, value));
            }
        } else {
            // Find start and end time of workouts
            long oldestWorkoutTime = Collections.min(data, StatsDataTypes.DataPoint.timeComparator).time;
            long newestWorkoutTime = Collections.max(data, StatsDataTypes.DataPoint.timeComparator).time;

            // Find start time of aggregation span
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(oldestWorkoutTime);
            span.setCalendarToAggregationStart(calendar, Instance.getInstance(ctx).userPreferences.getFirstDayOfWeek());

            // Iterate all time spans from first workout time to last workout time
            while (calendar.getTimeInMillis() < newestWorkoutTime) {
                ArrayList<StatsDataTypes.DataPoint> intervalData = findDataPointsInAggregationSpan(data, calendar, span);

                // Calculate the sum of the data from the span and store in the bar list
                if (intervalData.size() > 0) {
                    float sum = calculateValueSum(intervalData);
                    barEntries.add(new BarEntry((float) calendar.getTimeInMillis(), sum));
                }

                // Increment time span
                if (span != AggregationSpan.ALL) {
                    calendar.add(span.calendarField, 1);
                } else  {
                    calendar.setTimeInMillis(Long.MAX_VALUE);
                }
            }
        }

        return barEntries;
    }

    public static LineDataSet convertCandleToMeanLineData(CandleDataSet candleDataSet) {
        ArrayList<Entry> lineData = new ArrayList<>();

        for (CandleEntry entry : candleDataSet.getValues()) {
            lineData.add(new Entry(entry.getX(), entry.getClose()));
        }

        LineDataSet dataSet = new LineDataSet(lineData, candleDataSet.getLabel());
        dataSet.setValueFormatter(candleDataSet.getValueFormatter());
        return dataSet;
    }

    private float calculateValueAverage(ArrayList<StatsDataTypes.DataPoint> marks) {
        float average = 0f;
        if (!marks.isEmpty()) {
            average = calculateValueSum(marks) / marks.size();
        }
        return average;
    }

    private float calculateValueSum(ArrayList<StatsDataTypes.DataPoint> marks) {
        float sum = 0f;
        for (StatsDataTypes.DataPoint mark : marks) {
            sum += mark.value;
        }
        return sum;
    }

}
