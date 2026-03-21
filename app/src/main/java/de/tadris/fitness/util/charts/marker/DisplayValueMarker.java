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

package de.tadris.fitness.util.charts.marker;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ICandleDataSet;

import de.tadris.fitness.R;

public class DisplayValueMarker extends MarkerView {

    private TextView tvContent;
    private ValueFormatter formatter;
    private String unit;
    private ChartData data;

    /**
     * Constructor. Sets up the MarkerView with a custom layout resource.
     *
     * @param context
     */
    public DisplayValueMarker(Context context, ValueFormatter formatter, String unit, ChartData data) {
        super(context, R.layout.marker_view);
        tvContent = findViewById(R.id.marker_text);
        this.formatter = formatter;
        this.unit = " " + unit.trim();
        this.data = data;
    }

    // callbacks everytime the MarkerView is redrawn, can be used to update the
    // content (user-interface)
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (!(data instanceof CandleData)) {
            tvContent.setText(formatter.getFormattedValue(e.getY()) + unit); // set the entry-value as the display text
        } else {
            ICandleDataSet candleDataSet = (ICandleDataSet) data.getDataSetByIndex(highlight.getDataSetIndex());
            float x = e.getX();
            CandleEntry candleEntry = candleDataSet.getEntriesForXValue(x).get(0);
            String min = formatter.getFormattedValue(candleEntry.getLow()) + unit;
            String mean = formatter.getFormattedValue(candleEntry.getOpen()) + unit;
            String max = formatter.getFormattedValue(candleEntry.getHigh()) + unit;
            tvContent.setText("\u25b2\t" + max + "\n \u2300\t" + mean + "\n\u25bc\t" + min);
        }
        super.refreshContent(e, highlight);
    }
}
