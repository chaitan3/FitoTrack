/*
 * Copyright (c) 2022 Jannis Scheibe <jannis@tadris.de>
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
package de.tadris.fitness.recording.announcement

import android.content.Context
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.recording.WorkoutRecorder
import de.tadris.fitness.recording.information.CurrentSpeed
import de.tadris.fitness.recording.information.InformationManager

class InformationAnnouncements(
    private val context: Context,
    private val recorder: WorkoutRecorder,
    private val TTSController: TTSController
) {

    private val manager: InformationManager = InformationManager(recorder.recordingType, context)
    private var lastSpokenUpdateTime = 0L
    private var lastSpokenUpdateDistance = 0
    private var lastSpokenSpeedWarningTime = 0L
    private var lowerTargetSpeedLimit = 0f
    private var upperTargetSpeedLimit = 0f

    private val preferences = Instance.getInstance(context).userPreferences
    private val intervalTime =
        (60 * 1000 * preferences.spokenUpdateTimePeriod).toLong() // in millis
    private val intervalInMeters =
        (1000.0 / Instance.getInstance(context).distanceUnitUtils.distanceUnitSystem.getDistanceFromKilometers(
            1.0
        )
                * preferences.spokenUpdateDistancePeriod).toInt()
    private val speedWarningIntervalTime = 1000L * 10

    init {
        if (preferences.hasLowerTargetSpeedLimit()) {
            lowerTargetSpeedLimit = preferences.lowerTargetSpeedLimit
        }
        if (preferences.hasUpperTargetSpeedLimit()) {
            upperTargetSpeedLimit = preferences.upperTargetSpeedLimit
        }
    }

    fun check() {
        if (!TTSController.isTtsAvailable) {
            return
        }
        checkSpeed()
        var shouldSpeak = false
        if (intervalTime != 0L && recorder.duration - lastSpokenUpdateTime > intervalTime) {
            shouldSpeak = true
        }
        if (recorder is GpsWorkoutRecorder) {
            if (intervalInMeters != 0 && recorder.distanceInMeters - lastSpokenUpdateDistance > intervalInMeters) {
                shouldSpeak = true
            }
        }
        if (shouldSpeak) {
            speak()
        } else {
            speakAnnouncements(false)
        }
    }

    private fun checkSpeed() {
        if (recorder is GpsWorkoutRecorder) {
            if (speedWarningIntervalTime == 0L || recorder.getDuration() - lastSpokenSpeedWarningTime <= speedWarningIntervalTime) {
                return
            }
            val speed: Float = recorder.currentSpeed.toFloat()
            if (speed == 0f) {
                return;
            }
            if (lowerTargetSpeedLimit != 0f && lowerTargetSpeedLimit > speed) {
                TTSController.speak(context.getString(R.string.ttsBelowTargetSpeed) + ".")
                TTSController.speak(CurrentSpeed(context).getSpokenText(recorder))
                lastSpokenSpeedWarningTime = recorder.getDuration()
            } else if (upperTargetSpeedLimit != 0f && upperTargetSpeedLimit < speed) {
                TTSController.speak(context.getString(R.string.ttsAboveTargetSpeed) + ".")
                TTSController.speak(CurrentSpeed(context).getSpokenText(recorder))
                lastSpokenSpeedWarningTime = recorder.getDuration()
            }
        }
    }

    private fun speak() {
        speakAnnouncements(true)
        lastSpokenUpdateTime = recorder.duration
        if (recorder is GpsWorkoutRecorder) {
            lastSpokenUpdateDistance = recorder.distanceInMeters
        }
    }

    private fun speakAnnouncements(playAllAnnouncements: Boolean) {
        for (announcement in manager.information) {
            if (playAllAnnouncements || announcement.isPlayedAlways) {
                TTSController.speak(recorder, announcement)
            }
        }
    }
}