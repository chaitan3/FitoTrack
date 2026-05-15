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

package de.tadris.fitness.ui.map;

import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.XmlThemeResourceProvider;

import java.io.InputStream;

/**
 * Enumeration of all internal rendering themes.
 */
public enum InternalRenderTheme implements XmlRenderTheme {

    OLD("/assets/mapsforge/default.xml"), DEFAULT("/assets/mapsforge/osmarender.xml");

    private final String path;
    private XmlRenderThemeMenuCallback menuCallback;
    private XmlThemeResourceProvider resourceProvider;

    InternalRenderTheme(String path) {
        this.path = path;
    }

    @Override
    public XmlRenderThemeMenuCallback getMenuCallback() {
        return menuCallback;
    }

    /**
     * @return the prefix for all relative resource paths.
     */
    @Override
    public String getRelativePathPrefix() {
        return "/assets/mapsforge/";
    }

    @Override
    public InputStream getRenderThemeAsStream() {
        return getClass().getResourceAsStream(this.path);
    }

    @Override
    public XmlThemeResourceProvider getResourceProvider() {
        return this.resourceProvider;
    }

    @Override
    public void setMenuCallback(XmlRenderThemeMenuCallback menuCallback) {
        this.menuCallback = menuCallback;
    }

    @Override
    public void setResourceProvider(XmlThemeResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
    }
}
