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
package de.tadris.fitness.recording.autostart

import android.location.Location
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.CountDownTimer
import de.tadris.fitness.R
import de.tadris.fitness.recording.RecorderService
import de.tadris.fitness.recording.StartReason
import de.tadris.fitness.recording.actions.WorkoutActionStart
import de.tadris.fitness.recording.announcement.TTSController
import de.tadris.fitness.recording.component.GpsComponent
import de.tadris.fitness.recording.component.RecorderServiceComponent
import de.tadris.fitness.recording.event.LocationChangeEvent
import de.tadris.fitness.util.ToneGeneratorController
import de.tadris.fitness.util.VibratorController
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class AutoStartComponent : RecorderServiceComponent {

    companion object {
        private const val GPS_GOOD_ACCURACY_M = 30.0
        private const val MIN_MOVE_DISTANCE_M = 13.0
    }

    // Injected via register() — the service is the DI entry point
    private lateinit var service: RecorderService
    private lateinit var ttsController: TTSController
    private lateinit var toneController: ToneGeneratorController
    private lateinit var vibratorController: VibratorController

    // All state is only ever touched from the main thread (ThreadMode.MAIN on all subscribers).
    private var countdownTimer: CountDownTimer? = null
    private var pendingMode: AutoStartMode? = null
    private var lastLocation: Location? = null

    override fun register(service: RecorderService) {
        this.service = service
        ttsController = TTSController(service, "AutoStart")
        toneController = ToneGeneratorController(service, AudioManager.STREAM_NOTIFICATION)
        vibratorController = VibratorController(service)
        EventBus.getDefault().register(this)
        postState(AutoStartStateEvent.State.Idle)
    }

    override fun unregister() {
        val wasActive = countdownTimer != null || pendingMode != null
        cancelCountdown()
        pendingMode = null
        if (wasActive) ttsController.speak(service.getString(R.string.workoutAutoStartAborted))
        EventBus.getDefault().unregister(this)
        postState(AutoStartStateEvent.State.Idle)
        ttsController.destroyWhenDone()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRequest(e: AutoStartRequestEvent) {
        cancelCountdown()
        lastLocation = null
        if (e.delayMs > 0) startCountdown(e.delayMs, e.mode) else proceedWithMode(e.mode)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCancel(e: AutoStartCancelEvent) {
        val wasActive = countdownTimer != null || pendingMode != null
        cancelCountdown()
        pendingMode = null
        if (wasActive) ttsController.speak(service.getString(R.string.workoutAutoStartAborted))
        postState(AutoStartStateEvent.State.Idle)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onLocationChange(e: LocationChangeEvent) {
        val loc = e.location
        when (pendingMode) {
            AutoStartMode.WAIT_FOR_GPS -> {
                if (loc.hasAccuracy() && loc.accuracy < GPS_GOOD_ACCURACY_M) triggerStart()
            }
            AutoStartMode.ON_MOVE -> {
                val prev = lastLocation
                lastLocation = loc
                if (prev == null) return
                val timeDiffMs = (loc.elapsedRealtimeNanos - prev.elapsedRealtimeNanos) / 1_000_000L
                if (timeDiffMs < RecorderService.MIN_DURATION_DIFF) return
                val distanceM = GpsComponent.locationToLatLong(prev)
                    .sphericalDistance(GpsComponent.locationToLatLong(loc))
                if (distanceM >= MIN_MOVE_DISTANCE_M) triggerStart()
            }
            else -> {}
        }
    }
    
    /**
     * If the recording has started this component is no longer required.
     */
    @Subscribe
    fun onStartRecording(e :WorkoutActionStart) {
        unregister()
    }

    private fun startCountdown(delayMs: Long, mode: AutoStartMode) {
        postState(AutoStartStateEvent.State.Countdown(delayMs, (delayMs / 1000).toInt()))
        countdownTimer = object : CountDownTimer(delayMs, 1000) {
            override fun onTick(ms: Long) {
                val remainingS = ((ms + 500) / 1000).toInt()
                vibratorController.vibrate(50)
                if (ttsController.isTtsAvailable) {
                    ttsController.speak(remainingS.toString())
                } else {
                    toneController.playTone(ToneGenerator.TONE_PROP_BEEP, 200)
                }
                postState(AutoStartStateEvent.State.Countdown(ms, remainingS))
            }
            override fun onFinish() {
                postState(AutoStartStateEvent.State.Countdown(0, 0))
                proceedWithMode(mode)
            }
        }.start()
    }

    private fun proceedWithMode(mode: AutoStartMode) {
        when (mode) {
            AutoStartMode.INSTANT -> triggerStart()
            AutoStartMode.WAIT_FOR_GPS -> {
                pendingMode = mode
                ttsController.speak(service.getString(R.string.autoStartCountdownMsgGps))
                postState(AutoStartStateEvent.State.WaitingForGps)
                // onLocationChange with sticky=true fires immediately if a fix is already available
            }
            AutoStartMode.ON_MOVE -> {
                pendingMode = mode
                lastLocation = null
                ttsController.speak(service.getString(R.string.autoStartCountdownMsgMove))
                postState(AutoStartStateEvent.State.WaitingForMove)
            }
        }
    }

    private fun triggerStart() {
        pendingMode = null
        cancelCountdown()
        EventBus.getDefault().post(WorkoutAutoStartedEvent)
        EventBus.getDefault().post(WorkoutActionStart(StartReason.AUTO_START))
        postState(AutoStartStateEvent.State.Idle)
    }

    private fun cancelCountdown() {
        countdownTimer?.cancel()
        countdownTimer = null
    }

    private fun postState(state: AutoStartStateEvent.State) {
        EventBus.getDefault().postSticky(AutoStartStateEvent(state))
    }
}
