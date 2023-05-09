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

package com.raywenderlich.android.memo.services

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.raywenderlich.android.memo.R
import com.raywenderlich.android.memo.helper.NotificationHelper
import com.raywenderlich.android.memo.helper.secondsToTime
import com.raywenderlich.android.memo.model.TimerState
import com.raywenderlich.android.memo.ui.TIMER_ACTION
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

const val SERVICE_COMMAND = "Command"
const val NOTIFICATION_TEXT = "NotificationText"

class TimerService : Service(),CoroutineScope {

  var serviceState: TimerState = TimerState.INITIALIZED

  private val helper by lazy { NotificationHelper(this) }

  private var currentTime: Int = 0
  private var startedAtTimestamp: Int = 0
    set(value) {
      currentTime = value
      field = value
    }

  private val handler = Handler(Looper.getMainLooper())
  private var runnable: Runnable = object : Runnable {
    override fun run() {
      currentTime++
      broadcastUpdate()
      // Repeat every 1 second
      handler.postDelayed(this, 1000)
    }
  }

  private val job = Job()
  override val coroutineContext: CoroutineContext
    get() = Dispatchers.IO + job

  /**
   * Since this is a foreground service, we don’t need binding so we return null instead of IBinder.
   */
  override fun onBind(intent: Intent): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    super.onStartCommand(intent, flags, startId)
    /**
     * Here, you go through the Intent extras to get the value that the key SERVICE_COMMAND saves.
     * This value indicates which action the service should execute.
     */
    intent?.extras?.run {
      when (getSerializable(SERVICE_COMMAND) as TimerState) {
        TimerState.START -> startTimer()
        TimerState.PAUSE -> pauseTimerService()
        TimerState.STOP -> endTimerService()
        else -> return START_NOT_STICKY
      }
    }
    /**
     * If the system kills the service because the memory runs out, START_NOT_STICKY tells the system not to recreate the service with an undefined Intent.
     * Alternative constants are START_STICKY and START_REDELIVER_INTENT.
     */
    return START_NOT_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    /**
     * Remove all Handler callbacks for the ticking time to keep the memory clean.
     * Also, clean up the resources by canceling the whole timer Job since you’ll destroy the service in the next step.
     */
    handler.removeCallbacks(runnable)
    job.cancel()
  }

  private fun startTimer(elapsedTime: Int? = null) {
    serviceState = TimerState.START

    startedAtTimestamp = elapsedTime ?: 0

    // publish notification
    startForeground(NotificationHelper.NOTIFICATION_ID, helper.getNotification())

    broadcastUpdate()

    startCoroutineTimer()
  }

  private fun broadcastUpdate() {
    // update notification
    if (serviceState == TimerState.START) {
      // count elapsed time
      val elapsedTime = (currentTime - startedAtTimestamp)

      // Here, we send a broadcast with the elapsed time to MainActivity.
      // With it, MainActivity can update the time in the TextView below the card’s view.
      sendBroadcast(
        Intent(TIMER_ACTION)
          .putExtra(NOTIFICATION_TEXT, elapsedTime)
      )

      //Updates the status bar notification
      helper.updateNotification(
        getString(R.string.time_is_running, elapsedTime.secondsToTime())
      )

    } else if (serviceState == TimerState.PAUSE) {
      helper.updateNotification(getString(R.string.get_back))

    }
  }

  private fun pauseTimerService() {
    serviceState = TimerState.PAUSE
    handler.removeCallbacks(runnable)
    broadcastUpdate()
  }

  private fun endTimerService() {
    serviceState = TimerState.STOP
    handler.removeCallbacks(runnable)
    job.cancel()
    broadcastUpdate()
    stopService()
  }

  private fun stopService() {
    // This call tells the system that it should remove this service from foreground state.
    // If argument is true, it removes the notification
    stopForeground(true)
    // Stopping service
    stopSelf()
  }

  private fun startCoroutineTimer() {
    launch(coroutineContext) {
      handler.post(runnable)
    }
  }
}