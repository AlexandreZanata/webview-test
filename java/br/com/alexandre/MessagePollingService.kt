package br.com.alexandre

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Background message polling service for real-time chat notifications
 */
class MessagePollingService(private val context: Context) {
    
    private var scheduledExecutor: ScheduledExecutorService? = null
    private var pollingTask: ScheduledFuture<*>? = null
    private var isPolling = false
    private var currentInterval = 3000L // Default 3 seconds
    private var messageCallback: ((title: String, message: String, data: Map<String, Any>?) -> Unit)? = null
    private var lastMessageId: String? = null
    
    companion object {
        private const val TAG = "MessagePollingService"
        private const val DEFAULT_POLLING_URL = "https://i9gestao-sistemas.com.br/diario_frota/api.php"
        private const val MIN_POLLING_INTERVAL = 3000L // 3 seconds minimum
        private const val MAX_POLLING_INTERVAL = 300000L // 5 minutes maximum
    }

    fun setMessageCallback(callback: (title: String, message: String, data: Map<String, Any>?) -> Unit) {
        messageCallback = callback
    }

    fun startPolling(intervalMs: Long) {
        try {
            // Ensure interval is within acceptable bounds
            currentInterval = intervalMs.coerceIn(MIN_POLLING_INTERVAL, MAX_POLLING_INTERVAL)
            
            if (isPolling) {
                stopPolling()
            }

            scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
            
            pollingTask = scheduledExecutor?.scheduleAtFixedRate({
                try {
                    checkForNewMessages()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in polling task", e)
                }
            }, 0, currentInterval, TimeUnit.MILLISECONDS)

            isPolling = true
            Log.d(TAG, "Message polling started with interval: ${currentInterval}ms")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting message polling", e)
            isPolling = false
        }
    }

    fun stopPolling() {
        try {
            pollingTask?.cancel(true)
            scheduledExecutor?.shutdown()
            
            try {
                if (scheduledExecutor?.awaitTermination(1, TimeUnit.SECONDS) == false) {
                    scheduledExecutor?.shutdownNow()
                }
            } catch (e: InterruptedException) {
                scheduledExecutor?.shutdownNow()
                Thread.currentThread().interrupt()
            }
            
            scheduledExecutor = null
            pollingTask = null
            isPolling = false
            
            Log.d(TAG, "Message polling stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping message polling", e)
        }
    }

    private fun checkForNewMessages() {
        try {
            // Simulate API call to check for new messages
            // In a real implementation, this would call your actual API endpoint
            val response = makeApiCall()
            
            if (response != null) {
                parseAndHandleMessages(response)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for new messages", e)
        }
    }

    private fun makeApiCall(): String? {
        return try {
            // Create a simple HTTP request to check for new messages
            val url = URL("$DEFAULT_POLLING_URL?action=checkNewMessages&lastId=${lastMessageId ?: ""}")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 10000
                setRequestProperty("User-Agent", "WebViewApp/1.0")
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { reader ->
                    reader.readText()
                }
            } else {
                Log.w(TAG, "API call returned code: $responseCode")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error making API call", e)
            // Return mock data for testing purposes
            createMockResponse()
        }
    }

    private fun createMockResponse(): String {
        // Mock response for testing - remove in production
        val mockMessages = listOf(
            mapOf(
                "id" to System.currentTimeMillis().toString(),
                "title" to "New Message",
                "message" to "You have a new chat message",
                "timestamp" to System.currentTimeMillis(),
                "type" to "message"
            )
        )
        
        return JSONObject().apply {
            put("status", "success")
            put("hasNewMessages", Math.random() > 0.8) // 20% chance of new message for testing
            put("messages", mockMessages)
        }.toString()
    }

    private fun parseAndHandleMessages(response: String) {
        try {
            val jsonResponse = JSONObject(response)
            val hasNewMessages = jsonResponse.optBoolean("hasNewMessages", false)
            
            if (hasNewMessages) {
                val messagesArray = jsonResponse.optJSONArray("messages")
                
                messagesArray?.let { array ->
                    for (i in 0 until array.length()) {
                        val messageObj = array.getJSONObject(i)
                        
                        val messageId = messageObj.optString("id")
                        val title = messageObj.optString("title", "New Message")
                        val message = messageObj.optString("message", "You have a new message")
                        val timestamp = messageObj.optLong("timestamp", System.currentTimeMillis())
                        val type = messageObj.optString("type", "message")
                        
                        // Update last message ID to avoid duplicates
                        lastMessageId = messageId
                        
                        // Create data map for the callback
                        val data = mapOf(
                            "id" to messageId,
                            "timestamp" to timestamp,
                            "type" to type
                        )
                        
                        // Notify via callback on main thread
                        Handler(Looper.getMainLooper()).post {
                            messageCallback?.invoke(title, message, data)
                        }
                        
                        Log.d(TAG, "New message processed: $title")
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message response", e)
        }
    }

    fun isPolling(): Boolean = isPolling

    fun getCurrentInterval(): Long = currentInterval

    fun updatePollingInterval(intervalMs: Long) {
        val newInterval = intervalMs.coerceIn(MIN_POLLING_INTERVAL, MAX_POLLING_INTERVAL)
        
        if (newInterval != currentInterval) {
            val wasPolling = isPolling
            
            if (wasPolling) {
                stopPolling()
                startPolling(newInterval)
            } else {
                currentInterval = newInterval
            }
            
            Log.d(TAG, "Polling interval updated to: ${newInterval}ms")
        }
    }

    fun getStatus(): Map<String, Any> {
        return mapOf(
            "isPolling" to isPolling,
            "currentInterval" to currentInterval,
            "lastMessageId" to (lastMessageId ?: "none"),
            "minInterval" to MIN_POLLING_INTERVAL,
            "maxInterval" to MAX_POLLING_INTERVAL
        )
    }
}