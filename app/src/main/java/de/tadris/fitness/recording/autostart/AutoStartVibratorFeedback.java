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
