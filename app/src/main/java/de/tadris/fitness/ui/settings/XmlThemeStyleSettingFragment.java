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

package de.tadris.fitness.ui.settings;

import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.data.AppDatabase;
import de.tadris.fitness.data.Workout;
import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.map.FitoTrackRenderThemeMenuCallback;
import de.tadris.fitness.map.MapManager;
import de.tadris.fitness.map.SimpleColoringStrategy;
import de.tadris.fitness.map.WorkoutLayer;
import de.tadris.fitness.ui.FitoTrackActivity;

public class XmlThemeStyleSettingFragment extends FitoTrackSettingFragment implements FitoTrackRenderThemeMenuCallback.RenderThemeMenuListener {

    protected Handler handler = new Handler();

    private static final String TAG = "XmlThemeStyleSettings";

    public static final String KEY_PREFIX = "XmlRenderThemeMenu-";
    private static final String KEY_OVERLAY_CATEGORY = KEY_PREFIX + "overlayCategories";

    private String language;
    private XmlRenderThemeStyleMenu styleOptions;

    private MapView mapView;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_render_theme);
        language = Locale.getDefault().getLanguage();
        addExampleWorkout();
        setupOptions();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        ViewGroup v = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);

        addExampleWorkout();
        v.addView(mapView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getMapHeight()));

        return v;
    }

    private void addExampleWorkout() {
        AppDatabase db = Instance.getInstance(getContext()).db;
        MapManager mapManager = new MapManager(requireActivity());
        mapManager.setStyleMenuListener(this);
        mapView = mapManager.setupMap();

        Workout workout = db.workoutDao().getLastWorkout();
        if (workout != null) {
            WorkoutData workoutData = WorkoutData.fromWorkout(getContext(), workout);
            WorkoutLayer workoutLayer = new WorkoutLayer(
                    workoutData.getSamples(),
                    new SimpleColoringStrategy(((FitoTrackActivity) requireActivity()).getThemePrimaryColor()),
                    null);
            mapView.addLayer(workoutLayer);
            final BoundingBox bounds = workoutLayer.getBoundingBox().extendMeters(50);
            handler.postDelayed(() -> mapView.getModel().mapViewPosition.setMapPosition(new MapPosition(bounds.getCenterPoint(),
                    (LatLongUtils.zoomForBounds(mapView.getDimension(), bounds,
                            mapView.getModel().displayModel.getTileSize())))), 1000);
            mapView.repaint();
        }
    }

    private int getMapHeight() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels / 3;
    }

    private void setupOptions() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();

        if (styleOptions == null) {
            Preference infoPreference = new Preference(preferenceScreen.getContext());
            infoPreference.setTitle("N/A");
            infoPreference.setSummary(R.string.pref_render_theme_style_info);
            infoPreference.setEnabled(true);
            infoPreference.setSelectable(false);
            preferenceScreen.addPreference(infoPreference);
            return;
        }

        ListPreference stylePreference = new ListPreference(preferenceScreen.getContext());
        stylePreference.setTitle(R.string.pref_render_theme_style);
        stylePreference.setKey(KEY_PREFIX + styleOptions.getId());
        Set<XmlRenderThemeStyleLayer> visibleLayers = styleOptions.getLayers().values().stream().filter(XmlRenderThemeStyleLayer::isVisible).collect(
                Collectors.toSet());
        stylePreference.setEntries(visibleLayers.stream().map(layer -> layer.getTitle(language)).toArray(CharSequence[]::new));
        stylePreference.setEntryValues(visibleLayers.stream().map(layer -> KEY_PREFIX + layer.getId()).toArray(CharSequence[]::new));
        stylePreference.setEnabled(true);
        stylePreference.setPersistent(true);
        stylePreference.setDefaultValue(KEY_PREFIX + styleOptions.getDefaultValue());
        stylePreference.setOnPreferenceChangeListener(onStylePrefChangedListener);
        preferenceScreen.addPreference(stylePreference);

        PreferenceCategory preferenceOverlayCategory = new PreferenceCategory(preferenceScreen.getContext());
        preferenceOverlayCategory.setTitle(R.string.preferenceCategoryRenderThemeCategories);
        preferenceOverlayCategory.setKey(KEY_OVERLAY_CATEGORY);
        preferenceScreen.addPreference(preferenceOverlayCategory);

        String layerId = getLayerIdFromSelection(stylePreference.getValue());
        stylePreference.setSummary(styleOptions.getLayer(layerId).getTitle(language));
        addOrReplaceCategories(layerId, preferenceOverlayCategory);
    }

    private String getLayerIdFromSelection(String selection) {
        String layerId;
        if (selection == null || !styleOptions.getLayers().containsKey(selection.replace(KEY_PREFIX, ""))) {
            layerId = styleOptions.getLayer(styleOptions.getDefaultValue()).getId();
        } else {
            layerId = selection.replace(KEY_PREFIX, "");
        }
        return layerId;
    }

    private void addOrReplaceCategories(String selection, PreferenceCategory preferenceOverlayCategory) {
        preferenceOverlayCategory.removeAll();
        String layerId = getLayerIdFromSelection(selection);
        for (XmlRenderThemeStyleLayer overlay : styleOptions.getLayer(layerId).getOverlays()) {
            CheckBoxPreference checkbox = new CheckBoxPreference(requireContext());
            checkbox.setKey(KEY_PREFIX + overlay.getId());
            checkbox.setPersistent(true);
            checkbox.setTitle(overlay.getTitle(language));
            if (findPreference(KEY_PREFIX + overlay.getId()) == null) {
                checkbox.setChecked(overlay.isEnabled());
            }
            checkbox.setOnPreferenceChangeListener(onStyleEditListener);
            preferenceOverlayCategory.addPreference(checkbox);
        }
    }

    private final Preference.OnPreferenceChangeListener onStylePrefChangedListener = (preference, value) -> {
        PreferenceCategory preferenceOverlayCategory = this.getPreferenceScreen().findPreference(KEY_OVERLAY_CATEGORY);
        String selection = (String) value;
        preference.setSummary(styleOptions.getLayer(getLayerIdFromSelection(selection)).getTitle(language));
        addOrReplaceCategories(selection, preferenceOverlayCategory);
        refreshMapLayers();
        return true;
    };

    private final Preference.OnPreferenceChangeListener onStyleEditListener = (preference, value) -> {
        refreshMapLayers();
        return true;
    };

    private void refreshMapLayers() {
        for (Layer layer : mapView.getLayerManager().getLayers()) {
            if (layer instanceof TileLayer) {
                ((TileLayer<? extends Job>) layer).getTileCache().purge();
            }
        }
        MapManager mapManager = new MapManager(requireActivity());
        mapManager.setStyleMenuListener(this);
        mapManager.refreshOfflineLayer(mapView);
        mapView.getLayerManager().redrawLayers();
    }

    @Override
    public void onRenderThemeMenuIsAvailable(@NonNull XmlRenderThemeStyleMenu styleMenu) {
        if (styleOptions != null) {
            // we already have style options
            return;
        }
        this.styleOptions = styleMenu;
        Log.d(TAG, "New render theme menu available: " + styleMenu.getId());
        requireActivity().runOnUiThread(this::setupOptions);
    }

    @Override
    protected String getTitle() {
        return getString(R.string.pref_render_theme_style);
    }
}
