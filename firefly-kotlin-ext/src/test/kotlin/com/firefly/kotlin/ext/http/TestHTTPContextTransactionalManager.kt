package com.firefly.kotlin.ext.http

import com.firefly.codec.http2.model.HttpStatus
import com.firefly.kotlin.ext.common.firefly
import com.firefly.kotlin.ext.example.task.management.initData
import com.firefly.kotlin.ext.example.task.management.model.Project
import com.firefly.kotlin.ext.example.task.management.model.User
import com.firefly.kotlin.ext.example.task.management.server
import com.firefly.kotlin.ext.example.task.management.vo.ProjectEditor
import com.firefly.kotlin.ext.example.task.management.vo.ProjectResult
import com.firefly.kotlin.ext.example.task.management.vo.Request
import com.firefly.kotlin.ext.example.task.management.vo.Response
import com.firefly.utils.RandomUtils
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

/**
 * @author Pengtao Qiu
 */
class TestHTTPContextTransactionalManager {

    @Test
    fun test(): Unit = runBlocking {
        initData()
        val host = "localhost"
        val port = RandomUtils.random(3000, 65534).toInt()
        server.listen(host, port)

        val url = "http://$host:$port"
        val client = firefly.httpClient()

        val r0 = client.post("$url/user/create")
                .jsonBody(Request("test", User(0, "testUser2")))
                .asyncSubmit().getJsonBody<Response<Long>>()
        assertEquals(2L, r0.data)

        val r1 = client.get("$url/user/${r0.data}")
                .asyncSubmit().getJsonBody<Response<User>>()
        assertEquals("testUser2", r1.data?.name)

        val r2 = client.post("$url/user/createSimulateRollback")
                .jsonBody(Request("test", User(0, "testUser3")))
                .asyncSubmit()
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR_500, r2.status)

        val r3 = client.get("$url/user/3")
                .asyncSubmit().getJsonBody<Response<User>>()
        println(r3.data)
        assertEquals(true, r3.data == null)

        val r4 = client.post("$url/project/create")
                .jsonBody(Request("test", ProjectEditor(
                        Project(0, "project1", "project1 desc"),
                        listOf(1, 2)))).asyncSubmit().getJsonBody<Response<Long>>()
        assertEquals(1L, r4.data)

        val r5 = client.get("$url/project/${r4.data}")
                .asyncSubmit().getJsonBody<Response<ProjectResult>>()
        assertEquals(2, r5.data?.users?.size)
        assertEquals("project1", r5.data?.project?.name)
    }
}