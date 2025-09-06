package de.tadris.fitness.util.charts.formatter;

import android.content.Context;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.tadris.fitness.aggregation.AggregationSpan;

public class FractionedDateFormatter extends ValueFormatter {
    Context ctx;
    SimpleDateFormat format;
    AggregationSpan span;

    public FractionedDateFormatter(Context ctx, AggregationSpan span)
    {
        setAggregationSpan(span);
        this.span = span;
        this.ctx = ctx;
    }

    public void setAggregationSpan(AggregationSpan span)
    {
        format = span.dateFormat;
    }
    public AggregationSpan getSpan() {return span;}

    @Override
    public String getFormattedValue(float value) {
        return format.format(new Date((long) (value + span.spanInterval/2)));
    }
}
