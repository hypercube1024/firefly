package com.firefly.kotlin.ext.context

import org.junit.Test
import kotlin.test.assertEquals

/**
 * @author Pengtao Qiu
 */
class TestFContext {

    @Test
    fun test() {
        val service = Context.getBean<FuckService>()
        assertEquals("fuck!~ aHa", service.fuck())

        val services = Context.getBeans<FuckService>()
        assertEquals(1, services.size)
        assertEquals("fuck!~ aHa", services.single().fuck())
    }

    @Test
    fun testInject() {
        val service = Context.getBean<PigService>()
        assertEquals("fuck!~ aHa", service.woo())
    }
}

