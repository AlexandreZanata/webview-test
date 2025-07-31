package br.com.alexandre

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Random

/**
 * Advanced notification handling for chat messages and system notifications
 */
class CustomNotificationManager(
    private val context: Context
) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val messagePollingService = MessagePollingService(context)
    private var unreadCount = 0
    private var webViewInterface: WebViewInterface? = null

    companion object {
        const val CHANNEL_ID = "br.com.alexandre.NOTIFICATION_CHANNEL"
        const val MESSAGE_CHANNEL_ID = "br.com.alexandre.MESSAGE_CHANNEL"
        const val CHANNEL_NAME = "App Notifications"
        const val MESSAGE_CHANNEL_NAME = "Chat Messages"
        const val CHANNEL_DESCRIPTION = "Important system notifications"
        const val MESSAGE_CHANNEL_DESCRIPTION = "New chat message notifications"
    }

    init {
        createNotificationChannels()
        setupMessagePollingCallback()
    }

    fun setWebViewInterface(interface: WebViewInterface) {
        webViewInterface = interface
    }

    fun supportsNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            notificationManager.areNotificationsEnabled()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // General notification channel
            val generalChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                setShowBadge(true)
            }

            // Message notification channel
            val messageChannel = NotificationChannel(
                MESSAGE_CHANNEL_ID,
                MESSAGE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = MESSAGE_CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setShowBadge(true)
            }

            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemNotificationManager.createNotificationChannel(generalChannel)
            systemNotificationManager.createNotificationChannel(messageChannel)

            Log.d("NotificationManager", "Notification channels created")
        }
    }

    private fun setupMessagePollingCallback() {
        messagePollingService.setMessageCallback { title, message, data ->
            showMessageNotification(title, message)
            webViewInterface?.dispatchMessageNotification(title, message, data)
        }
    }

    fun showNotification(title: String, message: String, type: String = "info") {
        try {
            if (!supportsNotifications()) {
                Toast.makeText(context, "Notification permission required", Toast.LENGTH_SHORT).show()
                return
            }

            val notificationId = Random().nextInt(100000)
            val channelId = if (type.lowercase() == "message") MESSAGE_CHANNEL_ID else CHANNEL_ID

            val icon = when (type.lowercase()) {
                "alert", "warning" -> android.R.drawable.ic_dialog_alert
                "message" -> android.R.drawable.ic_dialog_email
                "success" -> android.R.drawable.ic_dialog_info
                else -> android.R.drawable.ic_dialog_info
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("notification_type", type)
                putExtra("notification_message", message)
            }

            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getActivity(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(
                    if (type.lowercase() in listOf("alert", "warning", "message")) 
                        NotificationCompat.PRIORITY_HIGH 
                    else 
                        NotificationCompat.PRIORITY_DEFAULT
                )
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            // Add vibration for important notifications
            if (type.lowercase() in listOf("alert", "warning", "message")) {
                builder.setVibrate(longArrayOf(0, 500, 200, 500))
            }

            // Show badge count for messages
            if (type.lowercase() == "message") {
                unreadCount++
                builder.setNumber(unreadCount)
            }

            Handler(Looper.getMainLooper()).post {
                try {
                    notificationManager.notify(notificationId, builder.build())
                    Log.d("NotificationManager", "Notification displayed: $title")
                } catch (e: SecurityException) {
                    Log.e("NotificationManager", "Permission denied for notification", e)
                    Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationManager", "Error creating notification", e)
        }
    }

    private fun showMessageNotification(title: String, message: String) {
        showNotification(title, message, "message")
    }

    fun updateUnreadCount(count: Int) {
        unreadCount = count
        Log.d("NotificationManager", "Unread count updated: $count")
    }

    fun clearUnreadCount() {
        unreadCount = 0
        notificationManager.cancel(MESSAGE_CHANNEL_ID.hashCode())
    }

    fun startMessagePolling(intervalMinutes: Int) {
        try {
            val intervalMs = (intervalMinutes * 60 * 1000).toLong().coerceAtLeast(3000L) // Minimum 3 seconds
            messagePollingService.startPolling(intervalMs)
            Log.d("NotificationManager", "Message polling started with interval: ${intervalMs}ms")
        } catch (e: Exception) {
            Log.e("NotificationManager", "Error starting message polling", e)
        }
    }

    fun stopMessagePolling() {
        try {
            messagePollingService.stopPolling()
            Log.d("NotificationManager", "Message polling stopped")
        } catch (e: Exception) {
            Log.e("NotificationManager", "Error stopping message polling", e)
        }
    }

    fun isPollingActive(): Boolean {
        return messagePollingService.isPolling()
    }

    fun getPollingStatus(): String {
        return """
            {
                "isActive": ${messagePollingService.isPolling()},
                "interval": ${messagePollingService.getCurrentInterval()},
                "unreadCount": $unreadCount,
                "supportsNotifications": ${supportsNotifications()}
            }
        """.trimIndent()
    }

    fun destroy() {
        stopMessagePolling()
    }
}