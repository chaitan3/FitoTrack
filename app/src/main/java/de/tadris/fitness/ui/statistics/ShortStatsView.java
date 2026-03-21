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

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager2.widget.ViewPager2;

import de.tadris.fitness.R;
import de.tadris.fitness.ui.adapter.ShortStatsAdapter;

public class ShortStatsView extends ConstraintLayout {
    ViewPager2 myViewPager2;
    ShortStatsAdapter adapter;

    public ShortStatsView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(getContext(), R.layout.view_short_stats, this);

        myViewPager2 = findViewById(R.id.viewpager_short_stats);
        adapter = new ShortStatsAdapter(this.getContext());
        myViewPager2.setAdapter(adapter);
    }

    public void refresh()
    {
        myViewPager2.getAdapter().notifyDataSetChanged();
    }
}
