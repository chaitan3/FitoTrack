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
import de.tadris.fitness.data.RecordingType
import de.tadris.fitness.recording.event.AggregatedRecordingValuesEvent
import de.tadris.fitness.recording.information.CurrentSpeed
import de.tadris.fitness.recording.information.InformationManager

class InformationAnnouncements(
    private val context: Context,
    private val recordingType: RecordingType,
    private val TTSController: TTSController
) {

    private val info get() = AggregatedRecordingValuesEvent.getCurrent()
    private val manager: InformationManager = InformationManager(recordingType, context)
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
        if (intervalTime != 0L && info.activeDuration - lastSpokenUpdateTime > intervalTime) {
            shouldSpeak = true
        }
        if (recordingType == RecordingType.GPS) {
            if (intervalInMeters != 0 && info.distance - lastSpokenUpdateDistance > intervalInMeters) {
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
        if (recordingType == RecordingType.GPS) {
            if (speedWarningIntervalTime == 0L || info.activeDuration - lastSpokenSpeedWarningTime <= speedWarningIntervalTime) {
                return
            }
            val speed: Float = info.currentSpeed.toFloat()
            if (speed == 0f) {
                return;
            }
            if (lowerTargetSpeedLimit != 0f && lowerTargetSpeedLimit > speed) {
                TTSController.speak(context.getString(R.string.ttsBelowTargetSpeed) + ".")
                TTSController.speak(CurrentSpeed(context).getSpokenText(info))
                lastSpokenSpeedWarningTime = info.activeDuration
            } else if (upperTargetSpeedLimit != 0f && upperTargetSpeedLimit < speed) {
                TTSController.speak(context.getString(R.string.ttsAboveTargetSpeed) + ".")
                TTSController.speak(CurrentSpeed(context).getSpokenText(info))
                lastSpokenSpeedWarningTime = info.activeDuration
            }
        }
    }

    private fun speak() {
        speakAnnouncements(true)
        lastSpokenUpdateTime = info.activeDuration
        if (recordingType == RecordingType.GPS) {
            lastSpokenUpdateDistance = info.distance.toInt()
        }
    }

    private fun speakAnnouncements(playAllAnnouncements: Boolean) {
        for (announcement in manager.information) {
            if (playAllAnnouncements || announcement.isPlayedAlways) {
                TTSController.speak(info, announcement)
            }
        }
    }
}