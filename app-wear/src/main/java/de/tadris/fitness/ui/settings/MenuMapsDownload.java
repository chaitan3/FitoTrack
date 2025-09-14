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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.util.List;

import de.tadris.fitness.R;
import de.tadris.fitness.data.MapStore;
import de.tadris.fitness.ui.Main;
import de.tadris.fitness.util.Util;

public class MenuMapsDownload extends MenuScreen{
    private MenuItem loading;
    private String path;
    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        path = getIntent().getStringExtra("path");
        if(path == null || path.isEmpty()){
            path = "";
            label.setText(R.string.maps);
        }else{
            label.setText(path.substring(0, path.length()-1));
        }

        loading = new MenuItem(this, R.string.loading);
        loading.value.setVisibility(View.GONE);
        list.addView(loading);

        Main.runInBackgroundJAVA(()->{
            List<MapStore.Dir> dirs = MenuMaps.mapStore.getRemoteDirs(path);
            List<MapStore.File> files = MenuMaps.mapStore.getRemoteFiles(path);
            runOnUiThread(()->{
                if(dirs.isEmpty() && files.isEmpty()){
                    Toast.makeText(this, R.string.error_maps_empty, Toast.LENGTH_SHORT).show();
                    finishAndRemoveTask();
                }else{
                    showDirectories(dirs);
                    showFiles(files);
                }
            });
        });
    }
    private void showDirectories(List<MapStore.Dir> dirs){
        for(MapStore.Dir dir : dirs){
            MenuItem item = new MenuItem(this, dir.name);
            item.value.setVisibility(View.GONE);
            item.setOnClickListener(v->{
                Intent intent = new Intent(this, MenuMapsDownload.class);
                intent.putExtra("path", dir.path + "/");
                startActivity(intent);
            });
            list.addView(item);
        }
    }
    private void showFiles(List<MapStore.File> files){
        for(MapStore.File file : files){
            MenuItem item = new MenuItem(this, file.name);
            item.value.setText(Util.prettySize(file.size));
            item.setOnClickListener(v->onItemClicked(file));
            list.addView(item);
        }
        loading.setVisibility(View.GONE);
    }
    private void onItemClicked(MapStore.File file){
        MapDownloadActivity.file = file;
        startActivity(new Intent(this, MapDownloadActivity.class));
        finishAndRemoveTask();
    }
}
