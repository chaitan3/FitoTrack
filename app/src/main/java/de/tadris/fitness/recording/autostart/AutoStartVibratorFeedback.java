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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import de.tadris.fitness.util.VibratorController;
import de.tadris.fitness.util.event.EventBusMember;

public class AutoStartVibratorFeedback implements EventBusMember {
    private EventBus eventBus;
    private VibratorController vibratorController;

    public AutoStartVibratorFeedback(VibratorController vibratorController) {
        this.vibratorController = vibratorController;
    }

    @Override
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Subscribe
    public void onAutoStartCountdownChanged(AutoStartWorkout.CountdownChangeEvent event) {
        if (0 < event.countdownS && event.countdownS <= 3) {
            vibratorController.vibrate(500);
        }
    }

    @Subscribe
    public void onAutoStartStateChanged(AutoStartWorkout.StateChangeEvent event) {
        if (event.newState != event.oldState && event.newState ==  AutoStartWorkout.State.AUTO_START_REQUESTED) {
            vibratorController.vibrate(1000);
        }
    }
}
