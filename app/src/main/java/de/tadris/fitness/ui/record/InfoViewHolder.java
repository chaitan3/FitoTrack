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

package de.tadris.fitness.ui.record;

import android.view.View;
import android.widget.TextView;


public class InfoViewHolder {
    private final int slot;
    private final InfoViewClickListener listener;
    private final TextView titleView;
    private final TextView valueView;

    public InfoViewHolder(int slot, InfoViewClickListener listener, TextView titleView, TextView valueView) {
        this.slot = slot;
        this.listener = listener;
        this.titleView = titleView;
        this.valueView = valueView;
        setOnClickListeners();
    }

    public void setText(String title, String value) {
        this.titleView.setText(title);
        this.valueView.setText(value);
    }

    private void setOnClickListeners() {
        titleView.setOnClickListener(getOnClickListener());
        valueView.setOnClickListener(getOnClickListener());
        titleView.setOnLongClickListener(getOnLongClickListener());
        valueView.setOnLongClickListener(getOnLongClickListener());
    }

    private View.OnClickListener getOnClickListener() {
        return v -> listener.onInfoViewClick(slot, false);
    }

    private View.OnLongClickListener getOnLongClickListener() {
        return v -> {
            listener.onInfoViewClick(slot, true);
            return true;
        };
    }

    public interface InfoViewClickListener {
        void onInfoViewClick(int slot, boolean isLongClick);
    }
}
