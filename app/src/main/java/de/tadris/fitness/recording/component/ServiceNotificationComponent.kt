/*
 * Copyright (c) 2025 Jannis Scheibe <jannis@tadris.de>
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

package de.tadris.fitness.recording.component

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import de.tadris.fitness.BuildConfig
import de.tadris.fitness.R
import de.tadris.fitness.recording.RecorderService
import de.tadris.fitness.recording.RecordingState
import de.tadris.fitness.ui.record.RecordWorkoutActivity
import de.tadris.fitness.util.NotificationHelper
import java.util.Locale

class ServiceNotificationComponent(private val context: Context) : RecorderServiceComponent {

    companion object {
        const val NOTIFICATION_ID: Int = 10
    }

    private lateinit var service: RecorderService

    override fun register(service: RecorderService) {
        this.service = service
    }

    override fun unregister() {

    }

    override fun checkSlow() {
        updateNotification()
    }

    private fun updateNotification() {
        val notificationManager = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationManager.notify(NOTIFICATION_ID, getNotification())
    }

    fun getNotification(): Notification {
        var contentText: String = context.getText(R.string.trackerWaitingMessage).toString()
        if (service.stateHolder.state != RecordingState.IDLE) {
            contentText = String.format(
                Locale.getDefault(), "\n%s\n%s: %s",
                getRecordingStateString(),
                context.getText(R.string.workoutDuration),
                service.instance.distanceUnitUtils.getHourMinuteSecondTime(service.instance.recorder.getDuration())
            )
        }
        if (BuildConfig.DEBUG && serviceStartTime != null) {
            contentText = String.format(
                "%s\n\nServiceCreateTime: %s",
                contentText,
                service.instance.userDateTimeUtils.formatTime(serviceStartTime)
            )
        }
        val builder: Notification.Builder = Notification.Builder(context)
            .setContentTitle(context.getText(R.string.trackerRunning))
            .setContentText(contentText)
            .setStyle(Notification.BigTextStyle().bigText(contentText))
            .setSmallIcon(R.drawable.notification)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationHelper.createChannels(context)
            builder.setChannelId(NotificationHelper.CHANNEL_WORKOUT)
        }

        val recorderActivityIntent = Intent(context, service.instance.recorder.getActivityClass())
        recorderActivityIntent.setAction(RecordWorkoutActivity.RESUME_ACTION)
        val flag =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pendingIntent = PendingIntent.getActivity(context, 0, recorderActivityIntent, flag)
        builder.setContentIntent(pendingIntent)

        return builder.build()
    }

    private fun getRecordingStateString(): String {
        return when (service.stateHolder.state) {
            RecordingState.IDLE -> context.getString(R.string.recordingStateIdle)
            RecordingState.RUNNING -> context.getString(R.string.recordingStateRunning)
            RecordingState.AUTO_PAUSE, RecordingState.MANUAL_PAUSE -> context.getString(R.string.recordingStatePaused)
            RecordingState.STOPPED -> context.getString(R.string.recordingStateStopped)
        }
    }

}