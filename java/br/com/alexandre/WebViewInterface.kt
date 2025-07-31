package br.com.alexandre

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView

/**
 * Enhanced WebView interface for seamless communication between WebView and native code
 */
class WebViewInterface(
    private val context: Context,
    private val webView: WebView,
    private val photoManager: PhotoManager,
    private val notificationManager: CustomNotificationManager
) {

    @JavascriptInterface
    fun getLocation(): String {
        return photoManager.getLocationData()
    }

    @JavascriptInterface
    fun captureScreenshot() {
        Handler(Looper.getMainLooper()).post {
            photoManager.captureWebViewScreenshot(webView)
        }
    }

    @JavascriptInterface
    fun setUserId(id: String) {
        Log.d("WebViewInterface", "User ID set: $id")
    }

    @JavascriptInterface
    fun takePhoto() {
        Handler(Looper.getMainLooper()).post {
            photoManager.takePhoto { result ->
                dispatchPhotoEvent("photoSelected", result)
            }
        }
    }

    @JavascriptInterface
    fun selectFromGallery(multiple: Boolean = false) {
        Handler(Looper.getMainLooper()).post {
            photoManager.selectFromGallery(multiple) { result ->
                dispatchPhotoEvent("gallerySelected", result)
            }
        }
    }

    @JavascriptInterface
    fun showNotification(title: String, message: String, type: String = "info") {
        notificationManager.showNotification(title, message, type)
    }

    @JavascriptInterface
    fun startMessagePolling(intervalMinutes: Int) {
        notificationManager.startMessagePolling(intervalMinutes)
    }

    @JavascriptInterface
    fun stopMessagePolling() {
        notificationManager.stopMessagePolling()
    }

    @JavascriptInterface
    fun updateUnreadCount(count: Int) {
        notificationManager.updateUnreadCount(count)
    }

    @JavascriptInterface
    fun checkPermissions(): String {
        return photoManager.checkPermissionsStatus()
    }

    @JavascriptInterface
    fun requestPermissions() {
        Handler(Looper.getMainLooper()).post {
            photoManager.requestPermissions()
        }
    }

    @JavascriptInterface
    fun getDeviceInfo(): String {
        return """
            {
                "platform": "Android",
                "version": "${android.os.Build.VERSION.RELEASE}",
                "model": "${android.os.Build.MODEL}",
                "manufacturer": "${android.os.Build.MANUFACTURER}",
                "hasCamera": ${photoManager.hasCamera()},
                "supportsNotifications": ${notificationManager.supportsNotifications()}
            }
        """.trimIndent()
    }

    private fun dispatchPhotoEvent(eventType: String, data: Any?) {
        val jsonData = when (data) {
            is String -> "\"$data\""
            is List<*> -> data.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
            else -> "null"
        }
        
        val jsCode = """
            (function() {
                try {
                    const event = new CustomEvent('$eventType', {
                        detail: {
                            data: $jsonData,
                            timestamp: Date.now()
                        }
                    });
                    document.dispatchEvent(event);
                    console.log('Dispatched $eventType event with data:', $jsonData);
                } catch (e) {
                    console.error('Error dispatching $eventType event:', e);
                }
            })();
        """.trimIndent()
        
        Handler(Looper.getMainLooper()).post {
            webView.evaluateJavascript(jsCode, null)
        }
    }

    fun dispatchMessageNotification(title: String, message: String, data: Map<String, Any>?) {
        val dataJson = data?.let { 
            it.entries.joinToString(prefix = "{", postfix = "}") { entry ->
                "\"${entry.key}\": \"${entry.value}\""
            }
        } ?: "{}"
        
        val jsCode = """
            (function() {
                try {
                    const event = new CustomEvent('newMessageNotification', {
                        detail: {
                            title: "$title",
                            message: "$message",
                            data: $dataJson,
                            timestamp: Date.now()
                        }
                    });
                    document.dispatchEvent(event);
                    console.log('Dispatched newMessageNotification event');
                } catch (e) {
                    console.error('Error dispatching newMessageNotification event:', e);
                }
            })();
        """.trimIndent()
        
        Handler(Looper.getMainLooper()).post {
            webView.evaluateJavascript(jsCode, null)
        }
    }
}