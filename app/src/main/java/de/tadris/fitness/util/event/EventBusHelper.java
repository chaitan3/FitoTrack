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

package de.tadris.fitness.util.event;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;

public class EventBusHelper {

    /**
     * Register an object to a certain {@link EventBus} instance.
     *
     * @param eventBus the {@link EventBus} instance the object should register to
     * @param object the object that should be registered
     *
     * @return whether registering was successful
     */
    public static boolean saveRegisterTo(@Nullable EventBus eventBus, @NonNull Object object) {
        if (eventBus == null) {
            return false;
        }
        if (!eventBus.isRegistered(object)) {
            eventBus.register(object);
        }
        return true;
    }


    /**
     * Unregister an object from the {@link EventBus} instance it is currently registered to.
     */
    public static void saveUnregisterFrom(@Nullable EventBus eventBus, @NonNull Object object) {
        if (eventBus == null) {
            return;
        }
        if (eventBus.isRegistered(object)) {
            eventBus.unregister(object);
        }
    }
}
