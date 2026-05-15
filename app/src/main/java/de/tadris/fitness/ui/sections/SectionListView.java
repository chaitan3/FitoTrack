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

package de.tadris.fitness.ui.sections;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.util.unit.DistanceUnitUtils;

public class SectionListView extends LinearLayout implements SectionListContract.IListView {
    private LinearLayout listViews;
    private Spinner unitSpinner;
    private EditText lengthEdit;
    private Spinner typeSpinner;
    private TextView tableHeaderSelected;
    LinearLayout paceSpeedToggleLayout;

    private List<SectionListContract.IListViewListener> listeners = new ArrayList<>();

    public SectionListView(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        inflate(getContext(), R.layout.view_sections_list, this);
        listViews = findViewById(R.id.section_list);
        unitSpinner = findViewById(R.id.sectionLengthUnit);
        lengthEdit = findViewById(R.id.sectionLengthEdit);
        typeSpinner = findViewById(R.id.sectionTypeSpinner);
        tableHeaderSelected = findViewById(R.id.selectedCriterion);
        paceSpeedToggleLayout = findViewById(R.id.paceSpeedText);

        lengthEdit.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                lengthEdit.clearFocus();
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(lengthEdit.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        lengthEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                for (SectionListContract.IListViewListener listener : listeners) {
                    double length = Double.parseDouble(lengthEdit.getText().toString());
                    listener.sectionLengthChanged(length);
                }
            }
        });

        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                for (SectionListContract.IListViewListener listener : listeners) {
                    listener.sectionUnitChanged(i);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                SectionListModel.SectionCriterion criterion = SectionListModel.SectionCriterion.values()[i];
                for (SectionListContract.IListViewListener listener : listeners) {
                    listener.sectionCriterionChanged(criterion);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        paceSpeedToggleLayout.setOnClickListener(view -> {
            TextView viewSelected = findViewById(R.id.selectedPaceSpeed);
            TextView viewUnselected = findViewById(R.id.unselectedPaceSpeed);
            String formerSelected = viewSelected.getText().toString();
            viewSelected.setText(viewUnselected.getText());
            viewUnselected.setText(formerSelected);
            for (SectionListContract.IListViewListener listener : listeners) {
                listener.displayPaceToggled();
            }
        });
    }

    @Override
    public DistanceUnitUtils getDistanceUnitUtils() {
        return Instance.getInstance(getContext()).distanceUnitUtils;
    }

    @Override
    public List<String> getLocalizedTypeNames() {
        return SectionListModel.SectionCriterion.getStringRepresentations(this.getContext());
    }

    @Override
    public void displaySections(List<SectionListModel.Section> sections, boolean paceToggle, SectionListModel.SectionCriterion criterion) {
        SectionAdapter adapter = new SectionAdapter(sections, paceToggle, criterion, getContext());
        listViews.removeAllViews();
        for (int i = 0; i < adapter.getCount(); ++i) {
            View item = adapter.getView(i, null, listViews);
            listViews.addView(item);
        }
    }

    @Override
    public void setUnits(List<String> units) {
        unitSpinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, units));
    }

    @Override
    public void setSectionTypes(List<String> types) {
        typeSpinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, types));
    }

    @Override
    public void setSelectedUnit(int i) {
        unitSpinner.setSelection(i);
    }

    @Override
    public void setSectionLengthEditText(double length) {
        lengthEdit.setText(String.valueOf(length));
        for (SectionListContract.IListViewListener listener : listeners) {
            listener.sectionLengthChanged(length);
        }
    }

    @Override
    public void setSectionType(SectionListModel.SectionCriterion criterion) {
        if (typeSpinner.getCount() > criterion.getId())
            typeSpinner.setSelection(criterion.getId());
    }

    @Override
    public void setSelectedUnitColumnHeader(SectionListModel.SectionCriterion criterion) {
        String text = SectionListModel.SectionCriterion.getStringRepresentations(getContext()).get(criterion.getId());
        tableHeaderSelected.setText(text);
    }

    @Override
    public void subscribe(SectionListContract.IListViewListener listener) {
        listeners.add(listener);
    }
}
