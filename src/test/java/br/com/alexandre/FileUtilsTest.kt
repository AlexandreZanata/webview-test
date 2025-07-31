package br.com.alexandre.utils

import org.junit.Test
import org.junit.Assert.*

class FileUtilsTest {

    @Test
    fun testGetMimeType_pdf() {
        val mimeType = FileUtils.getMimeType("document.pdf")
        assertEquals("application/pdf", mimeType)
    }

    @Test
    fun testGetMimeType_jpg() {
        val mimeType = FileUtils.getMimeType("image.jpg")
        assertEquals("image/jpeg", mimeType)
    }

    @Test
    fun testGetMimeType_unknown() {
        val mimeType = FileUtils.getMimeType("unknown.xyz")
        assertEquals("application/octet-stream", mimeType)
    }

    @Test
    fun testGetFileExtension_withExtension() {
        val extension = FileUtils.getFileExtension("https://example.com/file.pdf")
        assertEquals("pdf", extension)
    }

    @Test
    fun testGetFileExtension_withQuery() {
        val extension = FileUtils.getFileExtension("https://example.com/file.pdf?param=value")
        assertEquals("pdf", extension)
    }

    @Test
    fun testGetFileExtension_noExtension() {
        val extension = FileUtils.getFileExtension("https://example.com/file")
        assertEquals("", extension)
    }

    @Test
    fun testIsFileTypeAllowed_pdf() {
        assertTrue(FileUtils.isFileTypeAllowed("application/pdf"))
    }

    @Test
    fun testIsFileTypeAllowed_notAllowed() {
        assertFalse(FileUtils.isFileTypeAllowed("application/x-executable"))
    }

    @Test
    fun testIsDownloadableFile_pdf() {
        assertTrue(FileUtils.isDownloadableFile("https://example.com/document.pdf"))
    }

    @Test
    fun testIsDownloadableFile_image() {
        assertFalse(FileUtils.isDownloadableFile("https://example.com/image.jpg"))
    }

    @Test
    fun testCreateUniqueFileName() {
        val original = "document.pdf"
        val unique = FileUtils.createUniqueFileName(original)
        
        assertTrue(unique.contains("document"))
        assertTrue(unique.endsWith(".pdf"))
        assertTrue(unique.contains("_"))
    }

    @Test
    fun testGetSafeFileName() {
        val unsafeFileName = "file with spaces & special chars!.pdf"
        val safeFileName = FileUtils.getSafeFileName(unsafeFileName)
        
        assertFalse(safeFileName.contains(" "))
        assertFalse(safeFileName.contains("&"))
        assertFalse(safeFileName.contains("!"))
        assertTrue(safeFileName.contains("_"))
    }

    @Test
    fun testFormatFileSize_bytes() {
        assertEquals("512 B", FileUtils.formatFileSize(512))
    }

    @Test
    fun testFormatFileSize_kb() {
        assertEquals("1.0 KB", FileUtils.formatFileSize(1024))
    }

    @Test
    fun testFormatFileSize_mb() {
        assertEquals("1.0 MB", FileUtils.formatFileSize(1024 * 1024))
    }

    @Test
    fun testFormatFileSize_gb() {
        assertEquals("1.0 GB", FileUtils.formatFileSize(1024 * 1024 * 1024))
    }
}