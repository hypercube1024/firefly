package com.firefly.kotlin.ext

import com.firefly.kotlin.ext.annotation.NoArg
import com.firefly.kotlin.ext.common.Json
import org.junit.Test
import kotlin.test.assertEquals

/**
 * @author Pengtao Qiu
 */
class TestFireflyCommonWrap {

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
        assertEquals(foo.name, "Fuck")
        assertEquals(foo.age, 20)
        assertEquals(foo.data?.size, 2)
    }
}

@NoArg
class Foo<T>(var name: String, var age: Int, var data: T?)