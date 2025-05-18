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
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.List;

import de.tadris.fitness.R;
import de.tadris.fitness.data.MapStore;
import de.tadris.fitness.data.SettingsStore;
import de.tadris.fitness.ui.Main;
import de.tadris.fitness.util.Util;

public class MenuMaps extends MenuScreen{
    private MenuItem mapSource;
    private MenuItem loading;
    static MapStore mapStore;

    private List<MapStore.File> files;
    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        label.setText(R.string.maps);
        mapStore = new MapStore();

        mapSource = new MenuItem(this, R.string.map_source);
        mapSource.setOnClickListener(v->onMapSourceClick(mapSource));
        setMapSourceValue(mapSource);
        list.addView(mapSource);

        MenuItem download = new MenuItem(this, R.string.download);
        download.value.setVisibility(View.GONE);
        download.setOnClickListener(v->startActivity(new Intent(this, MenuMapsDownload.class)));
        list.addView(download);
    }
    @Override public void onResume(){
        super.onResume();
        for(int i=list.getChildCount()-1; i>2; i--) list.removeViewAt(i);
        loading = new MenuItem(this, R.string.loading);
        loading.value.setVisibility(View.GONE);
        list.addView(loading);

        Main.runInBackground(()->{
            files = MapStore.getLocalMaps(this);
            runOnUiThread(()->showMaps(files));
        });
    }
    @Override public void onDestroy(){
        super.onDestroy();
        if(mapStore != null){
            Main.runInBackground(()->{
                mapStore.close();
                mapStore = null;
            });
        }
    }
    private void showMaps(List<MapStore.File> files){
        loading.setVisibility(View.GONE);
        if(files.isEmpty()) return;

        MenuItem updates = new MenuItem(this, R.string.updates);
        updates.value.setVisibility(View.GONE);
        updates.setOnClickListener(v->onUpdateClick());
        list.addView(updates);
        for(MapStore.File file : files){
            MenuItem item = new MenuItem(this, file.path);
            item.value.setText(Util.prettySize(file.size));
            item.setOnClickListener(v->{
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialog));
                builder.setMessage(getString(R.string.delete_map, file.name));
                builder.setPositiveButton(R.string.delete, (d, w)->deleteMap(item, file));
                builder.setNegativeButton(R.string.cancel, (d, w)->d.dismiss());
                builder.create().show();
            });
            list.addView(item);
        }
    }
    private void deleteMap(MenuItem item, MapStore.File file){
        Main.runInBackground(()->{
            if(mapStore.deleteLocalMap(this, file)){
                if(MapStore.getLocalMaps(this).isEmpty()){
                    SettingsStore.storeMapSource(this, false);
                    setMapSourceValue(mapSource);
                    runOnUiThread(()->{for(int i=list.getChildCount()-1; i>2; i--) list.removeViewAt(i);});
                }else{
                    runOnUiThread(()->item.setVisibility(View.GONE));
                }
            }else{
                runOnUiThread(()->Toast.makeText(this, R.string.failed, Toast.LENGTH_SHORT).show());
            }
        });
    }
    public static void onMapSourceClick(MenuItem mapSource){
        if(SettingsStore.getMapSource(mapSource.getContext())){
            //was local
            SettingsStore.storeMapSource(mapSource.getContext(), false);
        }else{
            //was online
            if(MapStore.getLocalMaps(mapSource.getContext()).isEmpty()){
                Toast.makeText(mapSource.getContext(), R.string.fail_no_maps, Toast.LENGTH_SHORT).show();
                return;
            }
            SettingsStore.storeMapSource(mapSource.getContext(), true);
        }
        setMapSourceValue(mapSource);
    }
    public static void setMapSourceValue(MenuItem mapSource){
        mapSource.value.setText(SettingsStore.getMapSource(mapSource.getContext()) ? R.string.local : R.string.online);
    }
    private void onUpdateClick(){Main.runInBackground(this::checkForUpdates);}
    private void checkForUpdates(){
        if(files == null){
            toast(R.string.no_updates);
            return;
        }
        boolean hasUpdates = false;
        for(MapStore.File file : files){
            try{
                if(mapStore.checkHasUpdate(file)){
                    if(!file.getLocalFile(this).delete()){
                        toast(R.string.failed);
                        continue;
                    }
                    hasUpdates = true;
                    MapDownload.file = file;
                    startActivity(new Intent(this, MapDownload.class));
                }
            }catch(Exception e){
                toast(R.string.failed);
            }
        }
        if(!hasUpdates) toast(R.string.no_updates);
    }
    private void toast(int message){
        runOnUiThread(()->Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
}
