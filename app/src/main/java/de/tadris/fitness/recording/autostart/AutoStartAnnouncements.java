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

import android.content.Context;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.recording.announcement.TTSController;
import de.tadris.fitness.recording.event.AggregatedRecordingValuesEvent;
import de.tadris.fitness.util.event.EventBusMember;

/**
 * This class automatically plays voice announcements during auto start countdown so the user knows
 * how long until the workout starts and if it started at all.
 * Voice announcements have to be enabled for auto start countdown in recording settings, otherwise
 * it will not play anything at all.
 *
 * @apiNote Make sure to register to the {@link EventBus} instance on which {@link AutoStartWorkout}
 * broadcasts its events.
 * @see #registerTo(EventBus)
 * @see #unregisterFromBus()
 */
public class AutoStartAnnouncements implements EventBusMember {
    private Context context;
    private EventBus eventBus;
    private Instance instance;
    private TTSController ttsController;
    private ArrayList<CountdownTimeAnnouncement> countdownTimeAnnouncementList = new ArrayList<>();
    private CountdownTimeAnnouncement lastSpoken;

    public AutoStartAnnouncements(Context context, Instance instance, TTSController ttsController) {
        this.context = context;
        this.instance = instance;
        this.ttsController = ttsController;

        // initialize default set of announcements
        for (int i = 10; i > 0; i--) {
            countdownTimeAnnouncementList.add(new ShortSecondCountdownTimeAnnouncement(instance, i));
        }
        for (int i = 15; i <= 60; i += 5) {
            countdownTimeAnnouncementList.add(new LongSecondCountdownTimeAnnouncement(context, instance,  i));
        }
        for (int i = 75; i <= 600; i += 15) {
            if (i % 60 == 0) {
                countdownTimeAnnouncementList.add(new MinuteCountdownTimeAnnouncement(context, instance,  i));
            } else {
                countdownTimeAnnouncementList.add(new MinuteSecondCountdownTimeAnnouncement(context, instance, i));
            }
        }
        for (int i = 10 * 60 + 30; i < 61 * 30; i += 30) {
            if (i % 60 == 0) {
                countdownTimeAnnouncementList.add(new MinuteCountdownTimeAnnouncement(context, instance,  i));
            } else {
                countdownTimeAnnouncementList.add(new MinuteSecondCountdownTimeAnnouncement(context, instance,  i));
            }
        }
    }

    @Override
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Automatically play sounds on certain auto start countdown values.
     * Those sounds are played using the current notification volume.
     */
    @Subscribe
    public void onAutoStartCountdownChange(AutoStartWorkout.CountdownChangeEvent event) {
        // announce current countdown time
        for (CountdownTimeAnnouncement countdownTimeAnnouncement : countdownTimeAnnouncementList) {
            if (event.countdownS == countdownTimeAnnouncement.getCountdownS() &&
                    (lastSpoken == null || event.countdownS != lastSpoken.getCountdownS())) {   // prevent duplicate announcements
                ttsController.speak(countdownTimeAnnouncement);
                lastSpoken = countdownTimeAnnouncement;
                break;  // there can and should only be one announcement at a time
            }
        }
    }

    /**
     * Automatically play sounds when certain auto start countdown states are entered.
     * Those sounds are played using the current notification volume.
     */
    @Subscribe
    public void onAutoStartStateChange(AutoStartWorkout.StateChangeEvent event) {
        switch (event.newState) {
            case WAITING_FOR_GPS:
                // tell the user we're waiting for more a accurate GPS position
                ttsController.speak(new CountdownAnnouncement(instance) {
                    @Override
                    public String getSpokenText(@NonNull AggregatedRecordingValuesEvent recorder) {
                        return context.getString(R.string.autoStartCountdownMsgGps);
                    }
                });
                break;
            case WAITING_FOR_MOVE:
                // tell the user we're waiting for more a accurate GPS position
                ttsController.speak(new CountdownAnnouncement(instance) {
                    @Override
                    public String getSpokenText(@NonNull AggregatedRecordingValuesEvent recorder) {
                        return context.getString(R.string.autoStartCountdownMsgMove);
                    }
                });
                break;
            case ABORTED_BY_USER:
                if (event.oldState == AutoStartWorkout.State.COUNTDOWN ||
                        event.oldState == AutoStartWorkout.State.WAITING_FOR_GPS) {
                    ttsController.speak(new CountdownAnnouncement(instance) {
                        @Override
                        public String getSpokenText(@NonNull AggregatedRecordingValuesEvent recorder) {
                            return context.getString(R.string.workoutAutoStartAborted);
                        }
                    });
                }
            default:
                break;
        }
    }
}
