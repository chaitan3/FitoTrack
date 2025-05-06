/*
 * Copyright (c) 2025 Bart Vullings <dev@windkracht8.com>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU General Public License as published by
 * 	the Free Software Foundation, either version 3 of the License, or
 * 	(at your option) any later version.
 *
 * 	FitoTrack is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 *
 * 	You should have received a copy of the GNU General Public License
 * 	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.tadris.fitness.ui.settings;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import de.tadris.fitness.R;
import de.tadris.fitness.data.MapStore;
import de.tadris.fitness.data.SettingsStore;
import de.tadris.fitness.ui.Main;
import de.tadris.fitness.util.Util;

public class MapDownload extends Activity{
    private CircularProgressIndicator progress_circular;
    private TextView progress_label;
    private String progress_label_text = "_progress_/_size_";
    public static MapStore.File file;
    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_map_download);
        progress_circular = findViewById(R.id.progress_circular);
        ((TextView)findViewById(R.id.file)).setText(file.name);
        progress_label = findViewById(R.id.progress_label);
        progress_label_text = progress_label_text.replace("_size_", Util.prettySize(file.size));
        Main.runInBackground(()->MenuMaps.mapStore.getRemoteFile(this, file));
    }
    @Override public void onDestroy(){
        super.onDestroy();
        file = null;
    }

    public void onProgress(long bytesDone){
        int progress = (int) ((bytesDone * 100) / file.size);
        runOnUiThread(()->{
            progress_circular.setProgress(progress);
            progress_label.setText(progress_label_text.replace("_progress_", Util.prettySize(bytesDone)));
        });
    }
    public void onDone(){
        SettingsStore.storeMapSource(this, true);
        runOnUiThread(this::finishAndRemoveTask);
    }
    public void onWarning(int error){
        runOnUiThread(()->Toast.makeText(this, error, Toast.LENGTH_SHORT).show());
    }
    public void onError(int error){
        runOnUiThread(()->{
            progress_circular.setIndicatorColor(getColor(R.color.stateFailed));
            progress_label.setText(R.string.failed);
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });
    }
}
