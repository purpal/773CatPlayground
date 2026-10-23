package com.purpal.catplayground.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionsTest {

    @Test
    fun comparesSemanticVersions() {
        assertTrue(Versions.isNewer("v0.4.0", "0.3.0"))
        assertTrue(Versions.isNewer("0.10.0", "0.9.9"))
        assertTrue(Versions.isNewer("1.0.0", "0.99.0"))
        assertFalse(Versions.isNewer("v0.3.0", "0.3.0"))
        assertFalse(Versions.isNewer("0.2.9", "0.3.0"))
    }

    @Test
    fun turnsReleasePleaseNotesIntoBullets() {
        val body = """
            ## [0.3.0](https://github.com/purpal/773CatPlayground/compare/v0.2.0...v0.3.0) (2026-10-23)


            ### Features

            * league-style victory screen and faster pouring ([2e81269](https://github.com/purpal/773CatPlayground/commit/2e81269ad4a26b63ea07174d5dd9e77ca95cd03a))
        """.trimIndent()
        assertEquals("• league-style victory screen and faster pouring", Versions.releaseNotes(body))
    }
}
