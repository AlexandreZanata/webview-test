package br.com.alexandre.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Centralized permission management for the application
 */
class PermissionHelper(private val activity: Activity) {
    
    companion object {
        const val CAMERA_PERMISSION_REQUEST = 1001
        const val STORAGE_PERMISSION_REQUEST = 1002
        const val LOCATION_PERMISSION_REQUEST = 1003
        const val NOTIFICATION_PERMISSION_REQUEST = 1004
        const val AUDIO_PERMISSION_REQUEST = 1005
        const val PHOTO_PERMISSIONS_REQUEST = 1006
        const val ALL_PERMISSIONS_REQUEST = 1007
    }

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.RECORD_AUDIO
        )
    } else {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )
    }

    private val photoPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun hasAllPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasPhotoPermissions(): Boolean {
        return photoPermissions.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    fun hasStoragePermissions(): Boolean {
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(activity, storagePermission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required for older versions
        }
    }

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    fun requestAllPermissions() {
        val permissionsToRequest = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsToRequest, ALL_PERMISSIONS_REQUEST)
            Log.d("PermissionHelper", "Requesting permissions: ${permissionsToRequest.joinToString()}")
        }
    }

    fun requestPhotoPermissions() {
        val permissionsToRequest = photoPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsToRequest, PHOTO_PERMISSIONS_REQUEST)
            Log.d("PermissionHelper", "Requesting photo permissions: ${permissionsToRequest.joinToString()}")
        }
    }

    fun requestCameraPermission() {
        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        }
    }

    fun requestStoragePermission() {
        if (!hasStoragePermissions()) {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(permission),
                STORAGE_PERMISSION_REQUEST
            )
        }
    }

    fun requestLocationPermissions() {
        if (!hasLocationPermissions()) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST
            )
        }
    }

    fun requestAudioPermission() {
        if (!hasAudioPermission()) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                AUDIO_PERMISSION_REQUEST
            )
        }
    }

    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): PermissionResult {
        val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        val denied = grantResults.any { it == PackageManager.PERMISSION_DENIED }
        
        return when (requestCode) {
            ALL_PERMISSIONS_REQUEST -> PermissionResult(
                requestCode = requestCode,
                type = PermissionType.ALL,
                granted = granted,
                denied = denied,
                permissions = permissions.toList()
            )
            PHOTO_PERMISSIONS_REQUEST -> PermissionResult(
                requestCode = requestCode,
                type = PermissionType.PHOTO,
                granted = granted,
                denied = denied,
                permissions = permissions.toList()
            )
            CAMERA_PERMISSION_REQUEST -> PermissionResult(
                requestCode = requestCode,
                type = PermissionType.CAMERA,
                granted = granted,
                denied = denied,
                permissions = permissions.toList()
            )
            STORAGE_PERMISSION_REQUEST -> PermissionResult(
                requestCode = requestCode,
                type = PermissionType.STORAGE,
                granted = granted,
                denied = denied,
                permissions = permissions.toList()
            )
            LOCATION_PERMISSION_REQUEST -> PermissionResult(
                requestCode = requestCode,
                type = PermissionType.LOCATION,
                granted = granted,
                denied = denied,
                permissions = permissions.toList()
            )
            NOTIFICATION_PERMISSION_REQUEST -> PermissionResult(
                requestCode = requestCode,
                type = PermissionType.NOTIFICATION,
                granted = granted,
                denied = denied,
                permissions = permissions.toList()
            )
            AUDIO_PERMISSION_REQUEST -> PermissionResult(
                requestCode = requestCode,
                type = PermissionType.AUDIO,
                granted = granted,
                denied = denied,
                permissions = permissions.toList()
            )
            else -> PermissionResult(
                requestCode = requestCode,
                type = PermissionType.UNKNOWN,
                granted = granted,
                denied = denied,
                permissions = permissions.toList()
            )
        }
    }

    fun getPermissionStatus(): Map<String, Boolean> {
        return mapOf(
            "camera" to hasCameraPermission(),
            "storage" to hasStoragePermissions(),
            "location" to hasLocationPermissions(),
            "notification" to hasNotificationPermission(),
            "audio" to hasAudioPermission(),
            "photo" to hasPhotoPermissions(),
            "all" to hasAllPermissions()
        )
    }

    fun getMissingPermissions(): List<String> {
        return requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
}

enum class PermissionType {
    ALL, PHOTO, CAMERA, STORAGE, LOCATION, NOTIFICATION, AUDIO, UNKNOWN
}

data class PermissionResult(
    val requestCode: Int,
    val type: PermissionType,
    val granted: Boolean,
    val denied: Boolean,
    val permissions: List<String>
)