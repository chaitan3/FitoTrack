/*
 * Copyright (c) 2025 Bart Vullings <dev@windkracht8.com>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU General Public License as published by
 * 	the Free Software Foundation, either version 3 of the License, or
 * 	(at your option) any later version.
 *
 * 	FitoTrack is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 *
 * 	You should have received a copy of the GNU General Public License
 * 	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.tadris.fitness.ui.record

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import de.tadris.fitness.R

const val NOTIFICATION_CHANNEL_ID = "FitoTrack_Ongoing"
class RecordService : Service() {
	lateinit var notificationManager: NotificationManager
	var ongoingActivity: OngoingActivity? = null
	override fun onBind(intent: Intent?): IBinder? { return null }

	override fun onCreate() {
		super.onCreate()
		notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
		var exists = false
		for(channel in notificationManager.notificationChannels) {
			val channelId = channel.id
			if(channelId == NOTIFICATION_CHANNEL_ID) {
				exists = true
			} else {
				notificationManager.deleteNotificationChannel(channelId)
			}
		}
		if(!exists) {
			notificationManager.createNotificationChannel(
				NotificationChannel(
					NOTIFICATION_CHANNEL_ID,
					getString(R.string.notification_active),
					NotificationManager.IMPORTANCE_HIGH
				)
			)
		}
	}
	override fun onDestroy() {
		super.onDestroy()
		notificationManager.cancelAll()
	}

	@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)
		val pendingIntent = PendingIntent.getActivity(
			baseContext,
			0,
			Intent(this, RecordActivity::class.java),
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
		)
		val ongoingActivityStatus = Status.Builder()
			.addTemplate(getString(R.string.notification_active))
			.build()
		val notification: NotificationCompat.Builder = NotificationCompat.Builder(
			baseContext,
			NOTIFICATION_CHANNEL_ID
		)
			.setSmallIcon(currentWorkoutTypeIcon)
			.setOnlyAlertOnce(true)
			.setContentIntent(pendingIntent)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setOngoing(true)
		if(ongoingActivity == null) {
			ongoingActivity = OngoingActivity.Builder(
				baseContext,
				1,
				notification
			)
				.setStaticIcon(currentWorkoutTypeIcon)
				.setTouchIntent(pendingIntent)
				.setStatus(ongoingActivityStatus)
				.build()
			ongoingActivity?.apply(baseContext)
			ServiceCompat.startForeground(
				this,
				1,
				notification.build(),
				ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
			)
		} else {
			notificationManager.notify(1, notification.build())
			ongoingActivity?.update(baseContext, ongoingActivityStatus)
		}
		return START_STICKY
	}

}
