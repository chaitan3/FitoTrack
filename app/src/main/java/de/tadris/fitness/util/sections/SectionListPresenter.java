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

package de.tadris.fitness.util.sections;

public class SectionListPresenter implements SectionListContract.IListViewListener {
    private SectionListModel listModel;
    private SectionListContract.IListView view;
    private DistanceTranslator translator;

    public SectionListPresenter(SectionListContract.IListView view, SectionListModel model) {
        this.listModel = model;
        this.view = view;
        translator = new DistanceTranslator(view.getDistanceUnitUtils(), model);
        view.subscribe(this);
        loadDefaults();
        view.setSectionTypes(view.getLocalizedTypeNames());
    }

    private void loadDefaults() {
        double length;
        int uid = listModel.getSelectedUnitID();
        if (listModel.getCriterion() == SectionListModel.SectionCriterion.TIME) {
            listModel.setUnits(translator.timeUnits());
            length = translator.getDefaultValueForTime(uid);
            length = translator.getNormalizedLength(listModel.getCriterion(), uid, length);
        } else {
            listModel.setUnits(translator.distanceUnits());
            length = translator.getDefaultValueForDistance(uid);
            length = translator.getNormalizedLength(listModel.getCriterion(), uid, length);
        }
        listModel.setSectionLength(length);
    }

    @Override
    public void displayPaceToggled() {
        listModel.setPaceToggle(!listModel.getPaceToggle());
        view.displaySections(listModel.getSectionList(), listModel.getPaceToggle(), listModel.getCriterion());
    }

    @Override
    public void sectionLengthChanged(double length) {
        double actualLength = translator.getNormalizedLength(listModel.getCriterion(), listModel.getSelectedUnitID(), length);
        listModel.setSectionLength(actualLength);
        view.displaySections(listModel.getSectionList(), listModel.getPaceToggle(), listModel.getCriterion());
    }

    @Override
    public void sectionUnitChanged(int selectedUnitID) {
        listModel.setSelectedUnitID(selectedUnitID);
        switch (listModel.getCriterion()) {
            case TIME:
                double timeVal = translator.getDefaultValueForTime(selectedUnitID);
                timeVal = translator.getNormalizedLength(listModel.getCriterion(), selectedUnitID, timeVal);
                listModel.setSectionLength(timeVal);
                break;
            case DISTANCE:
            case ASCENT:
            case DESCENT:
                double distVal = translator.getDefaultValueForDistance(selectedUnitID);
                distVal = translator.getNormalizedLength(listModel.getCriterion(), selectedUnitID, distVal);
                listModel.setSectionLength(distVal);
                break;
        }
        view.setSectionLengthEditText(translator.getDisplayLength(listModel.getCriterion(), listModel.getSelectedUnitID(), listModel.getSectionLength()));
    }

    @Override
    public void sectionCriterionChanged(SectionListModel.SectionCriterion criterion) {
        listModel.setCriterion(criterion);
        switch (criterion) {
            case TIME:
                listModel.setUnits(translator.timeUnits());
                listModel.setSelectedUnitID(1); // default: min
                break;
            case DISTANCE:
                listModel.setUnits(translator.distanceUnits());
                listModel.setSelectedUnitID(0); // default: km / miles
                break;
            case ASCENT:
            case DESCENT:
                listModel.setUnits(translator.distanceUnits());
                listModel.setSelectedUnitID(1); // default : m
                break;
        }
        view.setUnits(listModel.getUnits());
        view.setSelectedUnit(listModel.getSelectedUnitID());
        view.setSelectedUnitColumnHeader(listModel.getCriterion());
    }
}