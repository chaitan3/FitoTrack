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

package de.tadris.fitness.data.autoexport;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import de.tadris.fitness.Instance;
import de.tadris.fitness.data.ExportTargetConfiguration;
import de.tadris.fitness.data.autoexport.source.ExportSource;
import de.tadris.fitness.data.autoexport.target.ExportTarget;

public class AutoExporter extends Worker {

    public static final String DATA_SOURCE_TYPE = "source_type";
    public static final String DATA_SOURCE_DATA = "source_data";
    public static final String DATA_TARGET_CONFIG_ID = "target_config_id";

    private ExportSource source;
    private ExportTarget target;

    public AutoExporter(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            boolean result = readData();
            if (!result) return Result.failure();
            ExportSource.ExportedFile file = source.provideFile(getApplicationContext());
            target.exportFile(getApplicationContext(), file);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }

    private boolean readData() {
        Data data = getInputData();
        source = ExportSource.getExportSourceByName(data.getString(DATA_SOURCE_TYPE), data.getString(DATA_SOURCE_DATA));
        ExportTargetConfiguration targetConfiguration = Instance.getInstance(getApplicationContext()).db.exportTargetDao().findById(data.getLong(DATA_TARGET_CONFIG_ID, 0));
        if (targetConfiguration == null) return false; // target configuration has been deleted
        target = targetConfiguration.getTargetImplementation();
        return true;
    }
}
