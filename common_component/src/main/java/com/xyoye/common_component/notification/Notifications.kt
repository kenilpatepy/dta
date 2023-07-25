package com.xyoye.common_component.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.xyoye.common_component.extension.buildNotificationChannel
import com.xyoye.common_component.extension.buildNotificationChannelGroup

/**
 * Created by xyoye on 2022/9/14
 */

object Notifications {

    object ChannelGroup {
        const val SCREENCAST = "screen_cast"
    }

    object Channel {
        const val SCREENCAST_PROVIDE = "screencast_provide_channel"
        const val SCREENCAST_RECEIVE = "screencast_receive_channel"
    }

    object Id {
        const val SCREENCAST_PROVIDE = 1001
        const val SCREENCAST_RECEIVE = 1002
    }

    fun setupNotificationChannels(context: Context) {
        try {
            createChannels(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createChannels(context: Context) {
        val notificationService = NotificationManagerCompat.from(context)

        notificationService.createNotificationChannelGroupsCompat(
            listOf(
                buildNotificationChannelGroup(ChannelGroup.SCREENCAST) {
                    setName("cast screen")
                }
            )
        )

        notificationService.createNotificationChannelsCompat(
            listOf(
                buildNotificationChannel(
                    Channel.SCREENCAST_PROVIDE,
                    NotificationManagerCompat.IMPORTANCE_LOW
                ) {
                    setName("Screencast content provision service")
                    setGroup(ChannelGroup.SCREENCAST)
                    setShowBadge(false)
                },
                buildNotificationChannel(
                    Channel.SCREENCAST_RECEIVE,
                    NotificationManagerCompat.IMPORTANCE_LOW
                ) {
                    setName("Casting content receiving service")
                    setGroup(ChannelGroup.SCREENCAST)
                    setShowBadge(false)
                }
            )
        )
    }
}