package com.firefly.kotlin.ext.common

import org.junit.Test
import kotlin.test.assertEquals

/**
 * @author Pengtao Qiu
 */
class TestStringExtension {

    @Test
    fun testLevenshteinDistance() {
        val d = "abcde".levenshteinDistance("abc").toInt()
        assertEquals(2, d)

        val d1 = "apple tree".levenshteinDistance("banana tree").toInt()
        assertEquals(5, d1)
    }
}