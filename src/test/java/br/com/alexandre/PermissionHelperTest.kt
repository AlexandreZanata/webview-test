package br.com.alexandre.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*

@RunWith(MockitoJUnitRunner::class)
class PermissionHelperTest {

    @Mock
    private lateinit var mockActivity: Activity

    private lateinit var permissionHelper: PermissionHelper

    @Before
    fun setup() {
        permissionHelper = PermissionHelper(mockActivity)
    }

    @Test
    fun testHasCameraPermission_granted() {
        // Mock permission granted
        mockStatic(ContextCompat::class.java).use { mockedContextCompat ->
            mockedContextCompat.`when`<Int> {
                ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.CAMERA)
            }.thenReturn(PackageManager.PERMISSION_GRANTED)

            assertTrue(permissionHelper.hasCameraPermission())
        }
    }

    @Test
    fun testHasCameraPermission_denied() {
        // Mock permission denied
        mockStatic(ContextCompat::class.java).use { mockedContextCompat ->
            mockedContextCompat.`when`<Int> {
                ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.CAMERA)
            }.thenReturn(PackageManager.PERMISSION_DENIED)

            assertFalse(permissionHelper.hasCameraPermission())
        }
    }

    @Test
    fun testHandlePermissionResult_allGranted() {
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
        val grantResults = intArrayOf(PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED)

        val result = permissionHelper.handlePermissionResult(
            PermissionHelper.ALL_PERMISSIONS_REQUEST,
            permissions,
            grantResults
        )

        assertEquals(PermissionType.ALL, result.type)
        assertTrue(result.granted)
        assertFalse(result.denied)
    }

    @Test
    fun testHandlePermissionResult_someDenied() {
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
        val grantResults = intArrayOf(PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_DENIED)

        val result = permissionHelper.handlePermissionResult(
            PermissionHelper.ALL_PERMISSIONS_REQUEST,
            permissions,
            grantResults
        )

        assertEquals(PermissionType.ALL, result.type)
        assertFalse(result.granted)
        assertTrue(result.denied)
    }

    @Test
    fun testGetMissingPermissions() {
        mockStatic(ContextCompat::class.java).use { mockedContextCompat ->
            // Mock camera permission denied, others granted
            mockedContextCompat.`when`<Int> {
                ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.CAMERA)
            }.thenReturn(PackageManager.PERMISSION_DENIED)

            mockedContextCompat.`when`<Int> {
                ContextCompat.checkSelfPermission(mockActivity, any())
            }.thenReturn(PackageManager.PERMISSION_GRANTED)

            val missingPermissions = permissionHelper.getMissingPermissions()
            assertTrue(missingPermissions.contains(Manifest.permission.CAMERA))
        }
    }
}