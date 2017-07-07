package com.firefly.kotlin.ext.common

import com.firefly.kotlin.ext.annotation.NoArg
import org.junit.Test
import kotlin.test.assertEquals

/**
 * @author Pengtao Qiu
 */
class TestCommonExtension {

    @Test
    fun testJsonParser() {
        val str = """
        {
            "age":20,
            "data":["data1","data2"],
            "name":"Fuck"
        }
        """

        println(str)
        val foo: Foo<List<String>> = Json.parse(str)
        println("parsed result -> ${foo.name}, ${foo.age}, ${foo.data}")
        assertEquals("Fuck", foo.name)
        assertEquals(20, foo.age)
        assertEquals(2, foo.data?.size)
    }

    @Test
    fun testNameResolver() {
        val name = KotlinNameResolver.name { }
        println(name)
        assertEquals("com.firefly.kotlin.ext.common.TestCommonExtension", name)
    }
}

@NoArg
class Foo<T>(var name: String, var age: Int, var data: T?)