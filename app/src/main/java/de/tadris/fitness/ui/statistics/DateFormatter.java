package de.tadris.fitness.ui.statistics;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import de.tadris.fitness.aggregation.AggregationSpan;

/**
 * Converts an aggregation span to a string.
 */
public class DateFormatter {

    private AggregationSpan aggregationSpan;

    public DateFormatter(@NotNull AggregationSpan aggregationSpan) {
        setAggregationSpan(aggregationSpan);
    }

    public void setAggregationSpan(AggregationSpan aggregationSpan) {
        this.aggregationSpan = aggregationSpan;
    }

    public String format(Calendar calendar) {
        String formatString;
        SimpleDateFormat format;
        switch (aggregationSpan) {
            case DAY:
                format = new SimpleDateFormat("dd. MMM yyyy");
                formatString = format.format(calendar.getTime());
                break;
            case WEEK:
                format = new SimpleDateFormat("yyyy 'W'w");
                formatString = format.format(calendar.getTime());
                break;
            case MONTH:
                format = new SimpleDateFormat("MMM yyyy");
                formatString = format.format(calendar.getTime());
                break;
            case YEAR:
                format = new SimpleDateFormat("yyyy");
                formatString = format.format(calendar.getTime());
                break;
            default:
                formatString = "-";
        }
        return formatString;
    }
}
