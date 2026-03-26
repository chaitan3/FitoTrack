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
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import de.tadris.fitness.BuildConfig;

public class DataManager {

    public static void cleanFilesASync(Context context) {
        new Thread(() -> cleanFiles(context)).start();
    }

    public static void cleanFiles(Context context) {
        File dir = new File(getSharedDirectory(context));

        if (dir.exists()) {
            // Otherwise dir.listFiles() would return null => NullPointerException
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (file.isFile()) {
                    if (file.delete()) {
                        Log.d("DataManager", "Deleted file " + file.getPath());
                    } else {
                        Log.d("DataManager", "Could not delete file " + file.getPath());
                    }
                }
            }
        }
    }

    public static String getSharedDirectory(Context context) {
        return context.getFilesDir().getAbsolutePath() + "/shared";
    }

    public static File createSharableFile(Context context, String filename) throws IOException {
        String filePath = DataManager.getSharedDirectory(context) + "/" + filename;
        File file = new File(filePath);
        File parent = file.getParentFile();
        if(parent != null){
            if(!parent.exists() && !parent.mkdirs()){
                throw new IOException("Cannot write to " + file);
            }
        }
        return file;
    }

    public static Uri provide(Context context, File file) {
        return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", file);
    }

    public static void rotateFile(File oldFile, long bytes) throws IOException {
        if (!oldFile.exists()) return; // no file to rotate
        long cutoffBytes = oldFile.length() - bytes;
        if (cutoffBytes <= 0) return; // no rotation needed

        Log.d("DataManager", "Rotating log file, cutting off " + cutoffBytes + " bytes.");

        // create tmp file
        File newFile = new File(oldFile.getParentFile(), oldFile.getName() + ".rotated");
        newFile.createNewFile();

        FileInputStream input = new FileInputStream(oldFile);
        FileOutputStream output = new FileOutputStream(newFile);

        // skip cutoffBytes bytes
        for (long l = 0; l < cutoffBytes; l++) {
            input.read(); // skip bytes
        }

        // copy the rest into the new file
        IOUtils.copy(input, output);
        input.close();
        output.close();

        oldFile.delete();
        newFile.renameTo(oldFile);

        Log.d("DataManager", "Rotation successful.");
    }

}
