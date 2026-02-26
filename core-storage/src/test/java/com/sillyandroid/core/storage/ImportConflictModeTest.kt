package com.sillyandroid.core.storage

import com.sillyandroid.core.model.ImportConflictMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportConflictModeTest {

    @Test
    fun `rename mode should keep both presets with different names`() {
        AppRepository.setImportConflictMode(ImportConflictMode.Rename)

        val json = """
            {
              "name": "SamePreset",
              "model": "gpt-4o-mini",
              "temperature": 0.7
            }
        """.trimIndent().toByteArray()

        AppRepository.importByName("preset-a.json", json)
        AppRepository.importByName("preset-b.json", json)

        val names = AppRepository.presets.value.map { it.name }.filter { it.startsWith("SamePreset") }
        // at least two entries for rename mode
        assertEquals(true, names.size >= 2)
    }

    @Test
    fun `overwrite mode should keep single preset name`() {
        AppRepository.setImportConflictMode(ImportConflictMode.Overwrite)

        val json = """
            {
              "name": "OverwritePreset",
              "model": "gpt-4o-mini",
              "temperature": 0.5
            }
        """.trimIndent().toByteArray()

        AppRepository.importByName("preset-overwrite-a.json", json)
        AppRepository.importByName("preset-overwrite-b.json", json)

        val count = AppRepository.presets.value.count { it.name == "OverwritePreset" }
        assertEquals(1, count)
    }
}
