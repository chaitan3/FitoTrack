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

import java.util.List;

import de.tadris.fitness.util.unit.DistanceUnitUtils;

public interface SectionListContract {

    interface IListViewListener {
        void displayPaceToggled();

        void sectionLengthChanged(double length);

        void sectionUnitChanged(int selectedUnitID);

        void sectionCriterionChanged(SectionListModel.SectionCriterion criterion);
    }

    interface IListView {
        DistanceUnitUtils getDistanceUnitUtils(); // TODO: Make DistanceUnitContext independent?!

        List<String> getLocalizedTypeNames();

        // View interactions
        void displaySections(List<SectionListModel.Section> list, boolean paceToggle, SectionListModel.SectionCriterion criterion);

        void setUnits(List<String> units);

        void setSectionTypes(List<String> types);

        void setSelectedUnit(int i);

        void setSectionLengthEditText(double length);

        void setSectionType(SectionListModel.SectionCriterion criterion);

        void setSelectedUnitColumnHeader(SectionListModel.SectionCriterion criterion);

        void subscribe(IListViewListener listener);
    }
}
