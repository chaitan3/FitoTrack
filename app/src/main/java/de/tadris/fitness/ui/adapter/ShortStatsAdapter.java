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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import de.tadris.fitness.ui.statistics.ShortStatsItemView;

public class ShortStatsAdapter extends RecyclerView.Adapter<ShortStatsAdapter.ViewHolder> {

    private final Context ctx;

    /**
     * The constructor for the adapter.
     *
     * @param ctx is the current context
     */
    public ShortStatsAdapter(Context ctx) {
        this.ctx = ctx;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = new ShortStatsItemView(ctx);
        ViewGroup.LayoutParams parentParams = parent.getLayoutParams();
        view.setLayoutParams(new ViewGroup.LayoutParams(parentParams.width, parentParams.height));
        return new ViewHolder(view, ctx);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (holder.itemView instanceof ShortStatsItemView) {
            ((ShortStatsItemView) holder.itemView).updateChart();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (holder.itemView instanceof ShortStatsItemView) {
            ((ShortStatsItemView) holder.itemView).chartType = position;
            ((ShortStatsItemView) holder.itemView).updateChart();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView, Context ctx) {
            super(itemView);
        }
    }
}
