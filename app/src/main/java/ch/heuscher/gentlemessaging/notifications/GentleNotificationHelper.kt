package ch.heuscher.gentlemessaging.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import ch.threema.app.R

/**
 * Lightweight notification helper for Gentle Messages.
 * Shows simple Android notifications for incoming messages.
 */
object GentleNotificationHelper {

    private const val CHANNEL_ID = "gentle_messages"
    private const val CHANNEL_NAME = "Neue Nachrichten"

    /**
     * Ensure notification channel exists (required API 26+).
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Benachrichtigungen für eingehende Nachrichten"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Show a notification for an incoming message.
     * Uses chatId.hashCode() as notification ID so each chat gets its own notification.
     */
    fun showMessageNotification(
        context: Context,
        senderName: String,
        messageText: String,
        chatId: String
    ) {
        val notificationId = chatId.hashCode()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    /**
     * Cancel the notification for a specific chat (e.g. when the user opens that chat).
     */
    fun cancelNotification(context: Context, chatId: String) {
        val notificationId = chatId.hashCode()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
    }
}
