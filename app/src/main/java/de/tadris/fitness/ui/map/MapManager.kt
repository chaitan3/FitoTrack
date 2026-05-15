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
package de.tadris.fitness.ui.map

import android.app.Activity
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import de.tadris.fitness.Instance
import de.tadris.fitness.ui.map.tilesource.HumanitarianTileSource
import de.tadris.fitness.ui.map.tilesource.MapnikTileSource
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.rendertheme.ContentRenderTheme
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.datastore.MultiMapDataStore
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.download.TileDownloadLayer
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.model.DisplayModel
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.XmlRenderTheme
import org.mapsforge.map.rendertheme.ZipRenderTheme
import org.mapsforge.map.rendertheme.ZipXmlThemeResourceProvider
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

class MapManager(private val activity: Activity) {

    private val userPreferences = Instance.getInstance(activity).userPreferences
    private val contentResolver = activity.contentResolver

    private lateinit var mapView: MapView
    private lateinit var tileCache: TileCache

    fun setupMap(): MapView {
        mapView = MapView(activity)
        initMapProvider(activity)
        mapView.setBuiltInZoomControls(false)
        mapView.setZoomLevel(18.toByte())
        Thread {
            handleSetupLayersThread()
        }.start()
        return mapView
    }

    fun refreshOfflineLayer(mapView: MapView) {
        this.mapView = mapView
        initTileCache(false)
        mapView.layerManager.layers.remove(0)
        setupOfflineMap()
    }

    private fun handleSetupLayersThread() {
        val chosenTileLayer = userPreferences.mapStyle
        val isOffline = chosenTileLayer.startsWith("offline")
        initTileCache(!isOffline)
        if (isOffline) {
            setupOfflineMap()
        } else {
            setupOnlineMap(chosenTileLayer)
        }
        sync { mapView.layerManager.redrawLayers() }
    }

    private fun initTileCache(persistent: Boolean) {
        tileCache = AndroidUtil.createTileCache(
            mapView.context, userPreferences.mapStyle, mapView.model.displayModel.tileSize,
            1f, mapView.model.frameBufferModel.overdrawFactor, persistent
        )
    }

    private fun setupOnlineMap(chosenTileLayer: String) {
        val tileSource = when (chosenTileLayer) {
            "osm.humanitarian" -> HumanitarianTileSource.INSTANCE
            "osm.mapnik" -> MapnikTileSource.INSTANCE
            else -> MapnikTileSource.INSTANCE
        }
        tileSource.userAgent = "mapsforge-android"
        val downloadLayer = TileDownloadLayer(
            tileCache,
            mapView.model.mapViewPosition,
            tileSource,
            AndroidGraphicFactory.INSTANCE
        )
        sync {
            mapView.layerManager.layers.add(0, downloadLayer)
            mapView.setZoomLevelMin(tileSource.zoomLevelMin)
            mapView.setZoomLevelMax(tileSource.zoomLevelMax)
            downloadLayer.start()
        }
    }

    private fun setupOfflineMap() {
        val multiMapDataStore = MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL)
        var theme: XmlRenderTheme? = null
        val documentFile = findMapDirectory() ?: return
        val files = documentFile.listFiles()
        for (file in files) {
            // Go through all files in the map directory
            if (isRelevantFile(file)) {
                val filename = file.name!!
                try {
                    if (filename.endsWith(".map")) {
                        loadMapFileTo(file, multiMapDataStore)
                    } else if (filename.endsWith(".zip")) {
                        if (theme == null) {
                            theme = loadRenderThemeZip(file)
                        }
                    } else {
                        if (theme == null) {
                            theme = loadRenderThemeXml(file)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        if (theme == null) {
            theme = InternalRenderTheme.DEFAULT
        }

        val renderLayer = TileRendererLayer(
            tileCache,
            multiMapDataStore,
            mapView.model.mapViewPosition,
            AndroidGraphicFactory.INSTANCE
        )
        renderLayer.setXmlRenderTheme(theme)

        sync {
            mapView.layerManager.layers.add(0, renderLayer)
        }
    }

    private fun loadRenderThemeXml(file: DocumentFile): XmlRenderTheme {
        return ContentRenderTheme(contentResolver, file.uri)
    }

    private fun loadRenderThemeZip(file: DocumentFile): XmlRenderTheme? {
        val xmlThemes = ZipXmlThemeResourceProvider.scanXmlThemes(
            ZipInputStream(
                BufferedInputStream(contentResolver.openInputStream(file.uri))
            )
        )
        // For the first zip file: load first theme of its content as XmlRenderTheme
        return if (xmlThemes.isNotEmpty()) {
            ZipRenderTheme(
                xmlThemes[0],
                ZipXmlThemeResourceProvider(
                    ZipInputStream(
                        BufferedInputStream(
                            contentResolver.openInputStream(file.uri)
                        )
                    )
                )
            )
        } else {
            null
        }
    }

    private fun loadMapFileTo(file: DocumentFile, dataStore: MultiMapDataStore) {
        // For map files: load as MapFile and add to data store
        val inputStream = contentResolver.openInputStream(file.uri) as FileInputStream?
        val mapFile = MapFile(inputStream, 0, null)
        dataStore.addMapDataStore(mapFile, true, true)
    }

    private fun isRelevantFile(file: DocumentFile): Boolean {
        val filename = file.name
        return file.isFile &&
                file.canRead() &&
                filename != null &&
                (filename.endsWith(".map") || filename.endsWith(".xml") || filename.endsWith(".zip"))
    }

    private fun findMapDirectory(): DocumentFile? {
        val directoryPath = userPreferences.offlineMapDirectoryName ?: return null
        val mapDirectoryUri = Uri.parse(directoryPath)
        return DocumentFile.fromTreeUri(activity, mapDirectoryUri)
    }

    private fun sync(action: () -> Unit) {
        activity.runOnUiThread(action)
    }

    companion object {
        @JvmStatic
        fun initMapProvider(activity: Activity) {
            // This sets the device scale factor so the map is displayed accordingly
            AndroidGraphicFactory.createInstance(activity.application)
            initSvgCache(activity)
            DisplayModel.setDefaultUserScaleFactor(0.85f)
        }

        private fun initSvgCache(activity: Activity) {
            val svgCache = File(activity.cacheDir, "svg")
            if (!svgCache.exists()) svgCache.mkdir()
            AndroidGraphicFactory.INSTANCE.setSvgCacheDir(svgCache)
        }
    }
}