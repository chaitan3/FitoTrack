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

package de.tadris.fitness.data;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import de.tadris.fitness.util.autoexport.target.ExportTarget;

@Entity(tableName = "export_target_config")
public class ExportTargetConfiguration {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /**
     * Specifies what data to export e.g. Backup or GPX files
     *
     * @see de.tadris.fitness.util.autoexport.source.ExportSource
     */
    public String source;

    /**
     * Type of target e.g. directory, cloud
     */
    public String type;

    /**
     * Additional data like for example the directory path
     */
    public String data;

    @Nullable
    public ExportTarget getTargetImplementation() {
        return ExportTarget.getExportTargetImplementation(type, data);
    }

}
