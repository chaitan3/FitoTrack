package de.tadris.fitness.ui.record;

import static android.content.Context.INPUT_SERVICE;

import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.health.services.client.data.ExerciseState;

import de.tadris.fitness.R;
import de.tadris.fitness.data.MapStore;
import de.tadris.fitness.data.SettingsStore;
import de.tadris.fitness.data.WorkoutData.LocationSample;
import de.tadris.fitness.ui.Main;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.overlay.FixedPixelCircle;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes;
import org.mapsforge.map.scalebar.DefaultMapScaleBar;
import org.mapsforge.map.scalebar.MapScaleBar;
import org.mapsforge.map.view.InputListener;

import java.util.ArrayList;
import java.util.List;

public class ScreenMap extends Screen implements InputListener{
    private MapView mapView;
    private View waiting;
    private ImageView centerType;

    private TileDownloadLayer tileOnlineLayer;
    private TileRendererLayer tileLocalLayer;
    private FixedPixelCircle location;
    private Polyline path;
    private LatLong firstLatLong;

    private boolean initMapDone = false;
    private enum CenterType{NOT, POSITION, ROUTE, FINISH}
    private CenterType currentCenterType = CenterType.POSITION;
    private long lastMapItemTime = 0;

    @Override public @Nullable View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ){
        View rootView = inflater.inflate(R.layout.record_screen_map, container, false);
        mapView = rootView.findViewById(R.id.mapView);
        targetProgress = rootView.findViewById(R.id.targetProgress);
        centerType = rootView.findViewById(R.id.centerType);
        centerType.setOnClickListener(v->flipCenterType());
        if(hasRotary(inflater.getContext())){
            rootView.findViewById(R.id.zoomOut).setVisibility(View.GONE);
            rootView.findViewById(R.id.zoomIn).setVisibility(View.GONE);
        }else{
            rootView.findViewById(R.id.zoomOut).setOnClickListener(v->onScroll(false));
            rootView.findViewById(R.id.zoomIn).setOnClickListener(v->onScroll(true));
        }
        waiting = rootView.findViewById(R.id.waiting);
        rootView.findViewById(R.id.screenUp).setOnClickListener(v->recordActivity.goUp());

        if(!Main.isScreenRound){
            rootView.findViewById(R.id.osm_copyright).setVisibility(View.GONE);
            rootView.findViewById(R.id.osm_copyright_rect).setVisibility(View.VISIBLE);
        }
        return rootView;
    }
    @Override public void onDestroy(){
        mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        super.onDestroy();
    }

    private boolean hasRotary(Context context){
        InputManager inputManager = (InputManager)context.getSystemService(INPUT_SERVICE);
        for(int input : inputManager.getInputDeviceIds()){
            InputDevice device = inputManager.getInputDevice(input);
            if(device == null) continue;
            if(device.supportsSource(InputDevice.SOURCE_ROTARY_ENCODER)) return true;
        }
        return false;
    }
    @Override void show(){
        super.show();
        if(RecordActivity.workoutData.locationSamples.isEmpty()) return;
        if(initMapDone){
            if(SettingsStore.getMapSource(recordActivity)){
                showLocalLayer();
            }else{
                showOnlineLayer();
            }
        }else{
            initMap(RecordActivity.workoutData.locationSamples.get(0).toLatLong());
            RecordActivity.workoutData.locationSamples.forEach(sample->{
                if(sample.time > lastMapItemTime + 4500){
                    path.addPoint(sample.toLatLong());
                    lastMapItemTime = sample.time;
                }
            });
        }
        lastMapItemTime = 0;
        centerMap();
        backFill = false;
    }
    @Override void hide(){
        super.hide();
        if(tileOnlineLayer != null) tileOnlineLayer.onPause();
    }
    @Override void updateState(ExerciseState exerciseState){
        super.updateState(exerciseState);
        if(location == null) return;
        if(exerciseState.equals(ExerciseState.ACTIVE)){
            location.getPaintStroke().setColor(colorActive);
        }else{
            location.getPaintStroke().setColor(colorInactive);
            location.setLatLong(location.getPosition());
        }
    }

    @Override void onScroll(boolean direction){
        byte zoomLevel = mapView.getModel().mapViewPosition.getZoomLevel();
        if(direction && zoomLevel < 18){
            mapView.setZoomLevel((byte) (zoomLevel + 1));
            onZoomEvent();
        }
        if(!direction && zoomLevel > 10){
            mapView.setZoomLevel((byte) (zoomLevel - 1));
            onZoomEvent();
        }
    }
    @Override public void onMoveEvent(){
        if(currentCenterType != CenterType.NOT){
            currentCenterType = CenterType.NOT;
            centerType.setImageResource(R.drawable.ic_map_center_not);
        }
    }
    @Override public void onZoomEvent(){
        if(currentCenterType == CenterType.ROUTE || currentCenterType == CenterType.FINISH){
            currentCenterType = CenterType.NOT;
            centerType.setImageResource(R.drawable.ic_map_center_not);
        }
    }

    private void initMap(LatLong latLong){
        initMapDone = true;
        try{
            mapView.setZoomLevel((byte) 15);
            mapView.setZoomLevelMin((byte) 10);
            mapView.setZoomLevelMax((byte) 18);
            mapView.setBuiltInZoomControls(false);
            mapView.addInputListener(this);
            mapView.setCenter(latLong);
            waiting.setVisibility(View.GONE);

            AndroidGraphicFactory.createInstance(recordActivity);
            DefaultMapScaleBar scaleBar = new DefaultMapScaleBar(
                    mapView.getModel().mapViewPosition,
                    mapView.getModel().mapViewDimension,
                    AndroidGraphicFactory.INSTANCE,
                    mapView.getModel().displayModel
            );
            scaleBar.setScaleBarPosition(MapScaleBar.ScaleBarPosition.BOTTOM_CENTER);
            scaleBar.setScaleBarMode(DefaultMapScaleBar.ScaleBarMode.SINGLE);
            mapView.setMapScaleBar(scaleBar);

            if(SettingsStore.getMapSource(recordActivity)){
                showLocalLayer();
            }else{
                showOnlineLayer();
            }

            //Path
            Paint paintPath = AndroidGraphicFactory.INSTANCE.createPaint();
            paintPath.setStyle(Style.STROKE);
            paintPath.setStrokeWidth(10);
            paintPath.setColor(Screen.colorPrimary);
            path = new Polyline(paintPath, AndroidGraphicFactory.INSTANCE);
            path.addPoint(latLong);
            mapView.addLayer(path);

            //Location
            Paint paintLocation = AndroidGraphicFactory.INSTANCE.createPaint();
            paintLocation.setStyle(Style.STROKE);
            paintLocation.setStrokeWidth(8f);
            paintLocation.setColor(colorActive);
            Paint paintFill = AndroidGraphicFactory.INSTANCE.createPaint();
            paintFill.setStyle(Style.FILL);
            paintFill.setColor(colorCircleFill);
            location = new FixedPixelCircle(latLong, 4f, paintFill, paintLocation);
            location.setLatLong(latLong);
            mapView.addLayer(location);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "ScreenMap.initMap exception: " + e.getMessage());
            Toast.makeText(recordActivity, R.string.error_map_load, Toast.LENGTH_SHORT).show();
        }
    }
    private void showLocalLayer(){
        if(!initMapDone) return;
        if(tileLocalLayer != null){
            if(!tileLocalLayer.isVisible()){
                if(tileOnlineLayer != null){
                    tileOnlineLayer.setVisible(false);
                    tileOnlineLayer.onPause();
                }
                tileLocalLayer.setVisible(true);
            }
            return;
        }
        TileCache tileCache = AndroidUtil.createTileCache(
                recordActivity
                ,"local"
                ,mapView.getModel().displayModel.getTileSize()
                ,Main.widthPixels
                ,Main.heightPixels
                ,mapView.getModel().frameBufferModel.getOverdrawFactor()
        );
        MultiMapDataStore multiMapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
        for(MapStore.File file : MapStore.getLocalMaps(recordActivity)){
            try{
                multiMapDataStore.addMapDataStore(new MapFile(file.getLocalFile(recordActivity)), false, false);
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "addMapDataStore failed for map: " + file.name);
                Toast.makeText(recordActivity, getString(R.string.corrupt_map, file.name), Toast.LENGTH_SHORT).show();
            }
        }
        tileLocalLayer = AndroidUtil.createTileRendererLayer(
                tileCache,
                mapView.getModel().mapViewPosition,
                multiMapDataStore,
                MapsforgeThemes.OSMARENDER
        );
        mapView.getLayerManager().getLayers().add(0, tileLocalLayer);
        if(tileOnlineLayer != null) tileOnlineLayer.setVisible(false);
    }
    private void showOnlineLayer(){
        if(!initMapDone) return;
        if(tileOnlineLayer != null){
            if(!tileOnlineLayer.isVisible()){
                if(tileLocalLayer != null) tileLocalLayer.setVisible(false);
                tileOnlineLayer.setVisible(true);
            }
            tileOnlineLayer.onResume();
            return;
        }
        TileCache tileCache = AndroidUtil.createTileCache(
                recordActivity
                ,"online"
                ,mapView.getModel().displayModel.getTileSize()
                ,Main.widthPixels
                ,Main.heightPixels
                ,mapView.getModel().frameBufferModel.getOverdrawFactor()
        );
        OpenStreetMapMapnik tileSource = OpenStreetMapMapnik.INSTANCE;
        tileSource.setUserAgent("de.tadris.fitness.wearos");
        tileOnlineLayer = new TileDownloadLayer(
                tileCache
                ,mapView.getModel().mapViewPosition
                ,tileSource
                ,AndroidGraphicFactory.INSTANCE
        );
        mapView.getLayerManager().getLayers().add(0, tileOnlineLayer);
        tileOnlineLayer.onResume();
        if(tileLocalLayer != null) tileLocalLayer.setVisible(false);
    }

    @Override void updateLocation(LocationSample sample){
        if(backFill || sample.time < lastMapItemTime + 4500) return;
        lastMapItemTime = sample.time;
        LatLong latLong = sample.toLatLong();
        if(!initMapDone) initMap(latLong);
        path.addPoint(latLong);
        if(isVisible()){
            location.setLatLong(latLong);
            centerMap();
        }
    }
    private void flipCenterType(){
        switch(currentCenterType){
            case NOT, FINISH:
                currentCenterType = CenterType.POSITION;
                centerType.setImageResource(R.drawable.ic_map_center_pos);
                break;
            case POSITION:
                currentCenterType = CenterType.ROUTE;
                centerType.setImageResource(R.drawable.ic_map_center_route);
                break;
            case ROUTE:
                currentCenterType = CenterType.FINISH;
                centerType.setImageResource(R.drawable.ic_map_center_finish);
                break;
        }
        centerMap();
    }
    private void centerMap(){
        if(location == null) return;
        switch(currentCenterType){
            case NOT:
                location.requestRedraw();
                break;
            case POSITION:
                mapView.setCenter(location.getPosition());
                break;
            case ROUTE:
                centerLatLongs(path.getLatLongs());
                break;
            case FINISH:
                if(firstLatLong == null) firstLatLong = RecordActivity.workoutData.locationSamples.get(0).toLatLong();
                List<LatLong> latLongs = new ArrayList<>();
                latLongs.add(firstLatLong);
                latLongs.add(location.getPosition());
                centerLatLongs(latLongs);
                break;
        }
    }
    private void centerLatLongs(List<LatLong> latLongs){
        BoundingBox box = (new BoundingBox(latLongs)).extendMeters(50);
        mapView.setCenter(box.getCenterPoint());
        mapView.setZoomLevel(LatLongUtils.zoomForBounds(
                new Dimension(Main.vw80, Main.vh80)
                ,box
                ,mapView.getModel().displayModel.getTileSize()
        ));
    }
}
