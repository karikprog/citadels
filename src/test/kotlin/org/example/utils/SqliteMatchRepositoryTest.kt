package org.example.utils

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

class SqliteMatchRepositoryTest : MatchRepositoryTest() {

    @TempDir
    lateinit var tempDir: File

    override fun createRepo() = SqliteMatchRepository(File(tempDir, "test.db").absolutePath)

    @Test
    fun `creates database file on init`() {
        val dbFile = File(tempDir, "create-test.db")
        SqliteMatchRepository(dbFile.absolutePath)
        assertTrue(dbFile.exists())
    }
}
