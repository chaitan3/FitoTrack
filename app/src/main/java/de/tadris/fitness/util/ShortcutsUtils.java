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
package de.tadris.fitness.util;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.ImageView;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.core.graphics.drawable.IconCompat;

import java.util.ArrayList;
import java.util.List;

import de.tadris.fitness.data.RecordingType;
import de.tadris.fitness.data.WorkoutType;
import de.tadris.fitness.data.WorkoutTypeManager;
import de.tadris.fitness.ui.record.RecordingConstants;
import de.tadris.fitness.util.charts.BitmapHelper;

public class ShortcutsUtils {
    private static final String TAG = "ShortcutsUtils";
    public static final int SHORTCUTS_NUMBER = 5;

    private final Context context;

    public ShortcutsUtils(Context context) { this.context = context; }

    public void updateShortcuts(List<WorkoutType> workoutTypes) {
        // Assuming workoutTypes is sorted by rank

        List<ShortcutInfoCompat> shortcuts = new ArrayList<ShortcutInfoCompat>();
        for (int rank = 0; rank < SHORTCUTS_NUMBER; rank++) {
            WorkoutType activity = workoutTypes.get(rank);

            Intent intent = new Intent(context,
                     RecordingType.findById(activity.id).recorderActivityClass)
                        .setAction(RecordingConstants.LAUNCH_ACTION)
                        .putExtra(RecordingConstants.WORKOUT_TYPE_EXTRA, activity.id);

            ImageView iconView = new ImageView(context);
            iconView.setImageIcon(
                android.graphics.drawable.Icon.createWithResource(
                  context,
                  Icon.getIcon(activity.icon))
            );
            iconView.setColorFilter(
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    activity.color, BlendModeCompat.SRC_ATOP));

            ShortcutInfoCompat shortcut =
                new ShortcutInfoCompat.Builder(context, activity.id)
                    .setShortLabel(activity.title)
                    .setIcon(IconCompat.createWithBitmap(
                        BitmapHelper.drawableToBitmap(iconView.getDrawable())))
                    .setIntent(intent)
                    .setRank(rank)
                    .build();
            shortcuts.add(shortcut);
      };

      ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts);
    }

    public void init() {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
          Log.d(TAG, "not configuring shortcuts: build version too low");
          return;
      }
      updateShortcuts(
              WorkoutTypeManager.getInstance().getAllTypesSorted(context)
      );
    }
}
