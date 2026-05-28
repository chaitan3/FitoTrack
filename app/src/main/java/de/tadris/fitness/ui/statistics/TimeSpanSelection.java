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

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.data.preferences.UserPreferences;
import de.tadris.fitness.stats.aggregation.AggregationSpan;
import de.tadris.fitness.util.ThemeUtils;

public class TimeSpanSelection extends LinearLayout {
    private final Spinner aggregationSpanSpinner;
    private ArrayAdapter<String> aggregationSpanArrayAdapter;
    private final TextView timeSpanSelection;

    DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker datePicker, int year, int month, int day) {
            selectedDate.set(year, month, day);
            timeSpanSelection.setText(dateFormatter.format(selectedDate));
            notifyListener();
        }
    };

    LocalDateTime now = LocalDateTime.now();
    DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), dateSetListener, now.getYear(), now.getMonthValue()-1, now.getDayOfMonth());

    GregorianCalendar selectedDate = new GregorianCalendar(now.getYear(), now.getMonthValue()-1, now.getDayOfMonth());
    AggregationSpan selectedAggregationSpan;
    boolean isInstanceSelectable;
    UserPreferences preferences;
    int foregroundColor = ThemeUtils.resolveThemeColor(getContext(), android.R.attr.textColorPrimary);

    private final DateFormatter dateFormatter;

    private ArrayList<OnTimeSpanSelectionListener> listeners;

    public TimeSpanSelection(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        preferences = new UserPreferences(context);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.TimeSpanSelection);
        isInstanceSelectable = array.getBoolean(R.styleable.TimeSpanSelection_isInstanceSelectable, true);
        array.recycle();

        selectedAggregationSpan = preferences.getStatisticsAggregationSpan();
        listeners = new ArrayList<>();
        dateFormatter = new DateFormatter(selectedAggregationSpan);

        inflate(context, R.layout.view_time_span_selection, this);


        // Load views
        aggregationSpanSpinner = findViewById(R.id.aggregationSpanSpinner);
        timeSpanSelection = findViewById(R.id.timeSpanSelection);
        loadAggregationSpanEntries();

        if (!isInstanceSelectable) {
            findViewById(R.id.timeSpanSelecionLayout).getLayoutParams().width = 0;
            timeSpanSelection.getLayoutParams().width = 0;
        }

        setAggregationSpan(selectedAggregationSpan);

        aggregationSpanSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                specifyAggregationSpan();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        timeSpanSelection.setOnClickListener( (view) -> {
            if (selectedAggregationSpan != AggregationSpan.ALL) {
                datePickerDialog.show();
            }
        });
        timeSpanSelection.setTextColor(this.foregroundColor);
        timeSpanSelection.setText(dateFormatter.format(selectedDate));
    }

    private void loadAggregationSpanEntries() {
        ArrayList<String> aggregationSpanStrings = new ArrayList<>();
        for (AggregationSpan aggregationSpan : AggregationSpan.values()) {
            if (aggregationSpan != AggregationSpan.SINGLE) {
                aggregationSpanStrings.add(getContext().getString(aggregationSpan.title));
            }
        }

        aggregationSpanArrayAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, aggregationSpanStrings) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((TextView) view).setTextColor(foregroundColor);
                return view;
            }
        };

        aggregationSpanSpinner.setAdapter(aggregationSpanArrayAdapter);
    }

    private void specifyAggregationSpan() {
        String selectedString = aggregationSpanArrayAdapter.getItem(aggregationSpanSpinner.getSelectedItemPosition());

        for (AggregationSpan aggregationSpan : AggregationSpan.values()) {
            if (selectedString.equals(getContext().getString(aggregationSpan.title))) {
                selectedAggregationSpan = aggregationSpan;
                dateFormatter.setAggregationSpan(aggregationSpan);
                preferences.setStatisticsAggregationSpan(aggregationSpan);
                notifyListener();
                break;
            }
        }

        timeSpanSelection.setText(dateFormatter.format(selectedDate));
    }

    public AggregationSpan getSelectedAggregationSpan() {
        return selectedAggregationSpan;
    }

    public void loadAggregationSpanFromPreferences()
    {
        setAggregationSpan(preferences.getStatisticsAggregationSpan());
    }

    public long getSelectedDate() {
        final int firstDayOfWeek = Instance.getInstance(getContext()).userPreferences.getFirstDayOfWeek();
        GregorianCalendar calendar = (GregorianCalendar) selectedDate.clone();
        calendar.setFirstDayOfWeek(firstDayOfWeek);

        switch (selectedAggregationSpan) {
            case DAY:
                selectedDate.getTimeInMillis();
                break;
            case WEEK:
                calendar.set(GregorianCalendar.DAY_OF_WEEK, firstDayOfWeek);
                break;
            case MONTH:
                calendar.set(GregorianCalendar.DAY_OF_MONTH, 1);
                break;
            case YEAR:
                calendar.set(GregorianCalendar.DAY_OF_YEAR, 1);
                break;
            case ALL:
                calendar.set(GregorianCalendar.YEAR, 0);
                break;
        }

        return calendar.getTimeInMillis();
    }

    public void setAggregationSpan(@NotNull AggregationSpan aggregationSpan) {
        selectedAggregationSpan = aggregationSpan;
        aggregationSpanSpinner.setSelection(aggregationSpanArrayAdapter.getPosition(getContext().getString(aggregationSpan.title)), true);
        dateFormatter.setAggregationSpan(aggregationSpan);
    }

    public void addOnTimeSpanSelectionListener(OnTimeSpanSelectionListener listener) {
        listeners.add(listener);
    }

    public void removeOnTimeSpanSelectionListener(OnTimeSpanSelectionListener listener) {
        listeners.remove(listener);
    }

    private void notifyListener() {
        for (OnTimeSpanSelectionListener listener : listeners) {
            listener.onTimeSpanChanged(getSelectedAggregationSpan(), getSelectedDate());
        }
    }

    public interface OnTimeSpanSelectionListener {
        void onTimeSpanChanged(AggregationSpan aggregationSpan, long selectedDate);
    }

    public void setForegroundColor(int foregroundColor){
        this.foregroundColor = foregroundColor;
        timeSpanSelection.setTextColor(foregroundColor);
        ((TextView)aggregationSpanSpinner.getSelectedView()).setTextColor(foregroundColor);
    }
}
