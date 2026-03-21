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

package de.tadris.fitness.ui.dialog

import android.app.AlertDialog
import android.content.Context
import android.widget.ArrayAdapter
import de.tadris.fitness.R
import de.tadris.fitness.aggregation.WorkoutInformationManager
import de.tadris.fitness.data.Workout

class SelectShareInformationDialog(
    private val ctx: Context,
    informationManager: WorkoutInformationManager,
    workout: Workout,
    private val slot: Int,
    private val callback: (slot: Int, informationTypeId: String) -> Unit
) {

    private val informationList = informationManager.getAvailableInformationFor(workout)

    fun show() {
        val builder = AlertDialog.Builder(ctx);
        val arrayAdapter = ArrayAdapter<String>(ctx, R.layout.select_dialog_singlechoice_material)
        informationList.forEach { arrayAdapter.add(ctx.getString(it.titleRes)) }
        builder.setAdapter(arrayAdapter, this::handleOnSelectEvent)
        builder.create().show()
    }

    private fun handleOnSelectEvent(_dialog: Any, which: Int) {
        val selectedInformation = informationList[which]
        this.callback.invoke(slot, selectedInformation.id)
    }
}