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

package de.tadris.fitness.recording.autostart;

import de.tadris.fitness.Instance;
import de.tadris.fitness.recording.announcement.Announcement;

/**
 * Base class for all auto start countdown related voice announcements.
 */
public abstract class CountdownAnnouncement implements Announcement {
    protected final Instance instance;

    public CountdownAnnouncement(Instance instance) {
        this.instance = instance;
    }

    @Override
    public boolean isAnnouncementEnabled() {
        return instance.userPreferences.isAutoStartCountdownAnnouncementsEnabled();
    }

}
