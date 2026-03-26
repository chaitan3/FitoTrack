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

package de.tadris.fitness.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.Arrays;

import de.tadris.fitness.ui.FitoTrackActivity;
import de.tadris.fitness.ui.statistics.fragments.StatsFragment;
import de.tadris.fitness.ui.statistics.fragments.StatsHistoryFragment;
import de.tadris.fitness.ui.statistics.fragments.StatsOverviewFragment;

public class StatisticsAdapter extends FragmentStateAdapter {

    ArrayList<Fragment> fragments;

    public StatisticsAdapter(FragmentManager fragmentManager, Lifecycle lifecycle, FitoTrackActivity context) {
        super(fragmentManager, lifecycle);
        fragments = new ArrayList<>(Arrays.asList(
                new StatsOverviewFragment(context),
                new StatsHistoryFragment(context)
        ));
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }

    public ArrayList<String> getTitles() {
        ArrayList<String> list = new ArrayList<>();
        for (Fragment fragment : fragments) {
            StatsFragment statsFragment = (StatsFragment)fragment;
            list.add(statsFragment.getTitle());
        }
        return list;
    }
}
