package br.com.alexandre

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Enhanced Photo/Gallery management system with proper permission handling
 */
class PhotoManager(
    private val activity: Activity,
    private val permissionHelper: PermissionHelper
) {
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0
    private var photoCallback: ((result: Any?) -> Unit)? = null

    companion object {
        const val CAMERA_REQUEST_CODE = 2001
        const val GALLERY_REQUEST_CODE = 2002
        const val GALLERY_MULTIPLE_REQUEST_CODE = 2003
    }

    fun updateLocation(latitude: Double, longitude: Double) {
        userLatitude = latitude
        userLongitude = longitude
    }

    fun getLocationData(): String {
        return "{\"latitude\": $userLatitude, \"longitude\": $userLongitude}"
    }

    fun hasCamera(): Boolean {
        return activity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    fun checkPermissionsStatus(): String {
        val cameraPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        return """
            {
                "camera": $cameraPermission,
                "storage": $storagePermission,
                "allGranted": ${cameraPermission && storagePermission}
            }
        """.trimIndent()
    }

    fun requestPermissions() {
        permissionHelper.requestPhotoPermissions()
    }

    fun takePhoto(callback: (result: Any?) -> Unit) {
        photoCallback = callback
        if (!permissionHelper.hasPhotoPermissions()) {
            permissionHelper.requestPhotoPermissions()
            return
        }

        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(activity.packageManager) != null) {
                val photoFile = createImageFile()
                if (photoFile != null) {
                    val photoUri = FileProvider.getUriForFile(
                        activity,
                        "br.com.alexandre.fileprovider",
                        photoFile
                    )
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    activity.startActivityForResult(intent, CAMERA_REQUEST_CODE)
                } else {
                    callback(null)
                    Toast.makeText(activity, "Error creating image file", Toast.LENGTH_SHORT).show()
                }
            } else {
                callback(null)
                Toast.makeText(activity, "No camera app available", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("PhotoManager", "Error taking photo", e)
            callback(null)
        }
    }

    fun selectFromGallery(multiple: Boolean = false, callback: (result: Any?) -> Unit) {
        photoCallback = callback
        if (!permissionHelper.hasStoragePermissions()) {
            permissionHelper.requestPhotoPermissions()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                if (multiple && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
            }

            val requestCode = if (multiple) GALLERY_MULTIPLE_REQUEST_CODE else GALLERY_REQUEST_CODE
            activity.startActivityForResult(
                Intent.createChooser(intent, "Select image(s)"),
                requestCode
            )
        } catch (e: Exception) {
            Log.e("PhotoManager", "Error selecting from gallery", e)
            callback(null)
        }
    }

    fun captureWebViewScreenshot(webView: WebView) {
        try {
            if (!permissionHelper.hasStoragePermissions()) {
                Toast.makeText(activity, "Storage permission required", Toast.LENGTH_SHORT).show()
                return
            }

            val webViewWidth = webView.width
            val webViewHeight = webView.contentHeight

            val bitmap = Bitmap.createBitmap(webViewWidth, webViewHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            webView.draw(canvas)

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(webViewWidth, webViewHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "webpage_$timeStamp.pdf"

            saveDocumentToStorage(pdfDocument, fileName)
            pdfDocument.close()

        } catch (e: Exception) {
            Log.e("PhotoManager", "Error capturing screenshot", e)
            Toast.makeText(activity, "Error creating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveDocumentToStorage(pdfDocument: PdfDocument, fileName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = activity.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    val outputStream = activity.contentResolver.openOutputStream(uri)
                    outputStream?.use {
                        pdfDocument.writeTo(it)
                        Toast.makeText(activity, "PDF saved to Downloads/$fileName", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val pdfFile = File(downloadsDir, fileName)
                FileOutputStream(pdfFile).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                    Toast.makeText(activity, "PDF saved to Downloads/$fileName", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("PhotoManager", "Error saving PDF", e)
            throw e
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"
            val storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            storageDir?.mkdirs()

            File.createTempFile(imageFileName, ".jpg", storageDir)
        } catch (e: IOException) {
            Log.e("PhotoManager", "Error creating image file", e)
            null
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (photoCallback == null) return

        try {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    if (resultCode == Activity.RESULT_OK) {
                        photoCallback?.invoke("Camera photo captured successfully")
                    } else {
                        photoCallback?.invoke(null)
                    }
                }
                GALLERY_REQUEST_CODE -> {
                    if (resultCode == Activity.RESULT_OK && data != null) {
                        val uri = data.data
                        photoCallback?.invoke(uri?.toString())
                    } else {
                        photoCallback?.invoke(null)
                    }
                }
                GALLERY_MULTIPLE_REQUEST_CODE -> {
                    if (resultCode == Activity.RESULT_OK && data != null) {
                        val uris = mutableListOf<String>()
                        
                        data.clipData?.let { clipData ->
                            for (i in 0 until clipData.itemCount) {
                                val uri = clipData.getItemAt(i).uri
                                uris.add(uri.toString())
                            }
                        } ?: data.data?.let { uri ->
                            uris.add(uri.toString())
                        }
                        
                        photoCallback?.invoke(uris)
                    } else {
                        photoCallback?.invoke(null)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PhotoManager", "Error handling activity result", e)
            photoCallback?.invoke(null)
        } finally {
            photoCallback = null
        }
    }

    fun onPermissionsResult(granted: Boolean) {
        if (granted) {
            photoCallback?.invoke("Permissions granted")
        } else {
            photoCallback?.invoke(null)
            Toast.makeText(activity, "Permissions required for photo functionality", Toast.LENGTH_SHORT).show()
        }
    }
}