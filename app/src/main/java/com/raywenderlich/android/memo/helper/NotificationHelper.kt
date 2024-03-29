/*
 * Copyright (c) 2021 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * This project and source code may use libraries or frameworks that are
 * released under various Open-Source licenses. Use of those libraries and
 * frameworks are governed by their own individual licenses.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.android.memo.helper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.raywenderlich.android.memo.R
import com.raywenderlich.android.memo.ui.MainActivity

private const val CHANNEL_ID = "StarWarsChannel"
private const val CHANNEL_NAME = "StarWarsChannelName"
private const val CHANNEL_DESCRIPTION = "StarWarsChannelDescription"

class NotificationHelper(private val context: Context) {

  // NotificationManager
  private val notificationManager by lazy {
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  // Notification Channel()
  @RequiresApi(Build.VERSION_CODES.O)
  private fun createChannel() =
    // Notification channel with CHANNEL_ID as its ID, CHANNEL_NAME as its name and the default importance.
    NotificationChannel(
      CHANNEL_ID,
      CHANNEL_NAME,
      NotificationManager.IMPORTANCE_DEFAULT
    ).apply {

      //Set the notification channel’s description to CHANNEL_DESCRIPTION and sound to null
      // which just means that there is no sound played when a notification is triggered in the channel.
      description = CHANNEL_DESCRIPTION
      setSound(null, null)
    }

  //Notification Builder
  //You’re using NotificationCompat.Builder to create a status bar notification builder.
  private val notificationBuilder: NotificationCompat.Builder by lazy {
    NotificationCompat.Builder(context, CHANNEL_ID)
      .setContentTitle(context.getString(R.string.app_name))
      .setSound(null)
      .setContentIntent(contentIntent)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      // This kind of notification should be set as auto-cancelable, which means that when the user clicks it, it’s automatically dismissed.
      .setAutoCancel(true)
  }


  // ContentIntent
  // Launches MainActivity when the user presses the notification.
  private val contentIntent by lazy {
    PendingIntent.getActivity(
      context,
      0,
      Intent(context, MainActivity::class.java),
      PendingIntent.FLAG_UPDATE_CURRENT
    )
  }

  // Notification

  fun getNotification(): Notification {
    // If the version of Android is equal to or greater than Android 8, the system creates a notification channel and returns Notification.
    // Unfortunately, the Support Library for versions before Android 8 doesn’t provide notification channel APIs.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      notificationManager.createNotificationChannel(createChannel())
    }

    return notificationBuilder.build()
  }

  fun updateNotification(notificationText: String? = null) {
    // Update the text in the notification published in the status bar via the notificationBuilder
    notificationText?.let { notificationBuilder.setContentText(it) }
    // We need to notify the Notification Manager about which notification to update. To do that, you use a unique NOTIFICATION_ID.
    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
  }

  //

  companion object {
    const val NOTIFICATION_ID = 99
  }
}