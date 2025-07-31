package br.com.alexandre.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * File handling utilities for enhanced file operations
 */
object FileUtils {

    private const val TAG = "FileUtils"

    /**
     * Get MIME type from file extension
     */
    fun getMimeType(url: String): String {
        val extension = getFileExtension(url)
        return when (extension.lowercase()) {
            "pdf" -> "application/pdf"
            "csv" -> "text/csv"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "txt" -> "text/plain"
            "html", "htm" -> "text/html"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "zip" -> "application/zip"
            else -> {
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) 
                    ?: "application/octet-stream"
            }
        }
    }

    /**
     * Get file extension from URL or file path
     */
    fun getFileExtension(url: String): String {
        return try {
            val fileName = url.substringAfterLast('/').substringBefore('?')
            if (fileName.contains('.')) {
                fileName.substringAfterLast('.')
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting file extension from: $url", e)
            ""
        }
    }

    /**
     * Check if file type is allowed for download
     */
    fun isFileTypeAllowed(mimeType: String): Boolean {
        val allowedTypes = setOf(
            "application/pdf",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv",
            "application/csv",
            "text/comma-separated-values",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "text/plain",
            "application/json",
            "application/xml",
            "application/octet-stream"
        )
        return allowedTypes.contains(mimeType.lowercase())
    }

    /**
     * Check if URL points to a downloadable file based on extension
     */
    fun isDownloadableFile(url: String): Boolean {
        val extension = getFileExtension(url).lowercase()
        val downloadableExtensions = setOf(
            "pdf", "csv", "xls", "xlsx", "doc", "docx", 
            "txt", "json", "xml", "zip"
        )
        return downloadableExtensions.contains(extension)
    }

    /**
     * Create a unique file name with timestamp
     */
    fun createUniqueFileName(originalName: String): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val extension = if (originalName.contains('.')) {
            originalName.substringAfterLast('.')
        } else {
            "unknown"
        }
        val baseName = if (originalName.contains('.')) {
            originalName.substringBeforeLast('.')
        } else {
            originalName
        }
        return "${baseName}_${timeStamp}.${extension}"
    }

    /**
     * Get safe file name by removing invalid characters
     */
    fun getSafeFileName(fileName: String): String {
        return fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(100)
    }

    /**
     * Create temporary file for camera capture
     */
    @Throws(IOException::class)
    fun createTempImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "TEMP_IMAGE_${timeStamp}_"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        
        // Ensure directory exists
        storageDir?.mkdirs()
        
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    /**
     * Save file to external storage (Android 10+ compatibility)
     */
    fun saveFileToDownloads(
        context: Context,
        data: ByteArray,
        fileName: String,
        mimeType: String
    ): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, 
                    contentValues
                )
                
                uri?.let { fileUri ->
                    context.contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                        outputStream.write(data)
                    }
                    fileUri
                }
            } else {
                // Use direct file access for older versions
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(data)
                }
                
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file to downloads", e)
            null
        }
    }

    /**
     * Get file size in human readable format
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.1f GB", gb)
    }

    /**
     * Check if external storage is available for writing
     */
    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    /**
     * Check if external storage is available for reading
     */
    fun isExternalStorageReadable(): Boolean {
        val state = Environment.getExternalStorageState()
        return state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY
    }

    /**
     * Get cache directory size
     */
    fun getCacheDirectorySize(context: Context): Long {
        return try {
            getDirSize(context.cacheDir)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cache directory size", e)
            0L
        }
    }

    /**
     * Clear cache directory
     */
    fun clearCacheDirectory(context: Context): Boolean {
        return try {
            deleteDir(context.cacheDir)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache directory", e)
            false
        }
    }

    /**
     * Get directory size recursively
     */
    private fun getDirSize(dir: File): Long {
        var size = 0L
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    getDirSize(file)
                } else {
                    file.length()
                }
            }
        } else {
            size = dir.length()
        }
        return size
    }

    /**
     * Delete directory recursively
     */
    private fun deleteDir(dir: File): Boolean {
        return try {
            if (dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    deleteDir(file)
                }
            }
            dir.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting directory: ${dir.path}", e)
            false
        }
    }

    /**
     * Create directory if it doesn't exist
     */
    fun ensureDirectoryExists(dir: File): Boolean {
        return try {
            if (!dir.exists()) {
                dir.mkdirs()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating directory: ${dir.path}", e)
            false
        }
    }

    /**
     * Get optimized cache size based on available storage
     */
    fun getOptimalCacheSize(context: Context): Long {
        return try {
            val cacheDir = context.cacheDir
            val availableBytes = cacheDir.usableSpace
            // Use 10% of available space, max 100MB, min 10MB
            (availableBytes * 0.1).toLong().coerceIn(10 * 1024 * 1024L, 100 * 1024 * 1024L)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating optimal cache size", e)
            50 * 1024 * 1024L // Default 50MB
        }
    }
}