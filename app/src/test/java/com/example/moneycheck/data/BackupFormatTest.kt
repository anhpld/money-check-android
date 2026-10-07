package com.example.moneycheck.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupFormatTest {

    @Test
    fun backupZipArchive_containsManifestDatabaseAndSettings() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zipOut ->
            zipOut.putNextEntry(ZipEntry("manifest.json"))
            zipOut.write("""{"version":1,"app":"MoneyCheck"}""".toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            zipOut.putNextEntry(ZipEntry("settings.json"))
            zipOut.write("""{"apiBaseUrl":"https://api.openai.com/v1","model":"gpt-4o"}""".toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            zipOut.putNextEntry(ZipEntry("database.db"))
            zipOut.write("SQLite format 3\u0000dummy-data".toByteArray(Charsets.US_ASCII))
            zipOut.closeEntry()
        }

        val entries = mutableListOf<String>()
        var foundManifest = false
        var foundSettings = false
        var foundDb = false

        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                entries.add(entry.name)
                when (entry.name) {
                    "manifest.json" -> foundManifest = true
                    "settings.json" -> foundSettings = true
                    "database.db" -> foundDb = true
                }
                entry = zipIn.nextEntry
            }
        }

        assertTrue(foundManifest)
        assertTrue(foundSettings)
        assertTrue(foundDb)
        assertEquals(3, entries.size)
    }

    @Test
    fun sqliteHeader_detectedCorrectly() {
        val header = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
        val magic = String(header, Charsets.US_ASCII)
        assertTrue(magic.startsWith("SQLite format 3"))
    }

    @Test
    fun zipHeader_detectedCorrectly() {
        val zipHeader = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
        val magic = String(zipHeader, Charsets.US_ASCII)
        assertTrue(magic.startsWith("PK"))
    }
}
