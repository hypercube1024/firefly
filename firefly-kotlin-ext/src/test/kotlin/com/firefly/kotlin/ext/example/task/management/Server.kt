package com.firefly.kotlin.ext.example.task.management

import com.firefly.codec.http2.model.HttpMethod
import com.firefly.kotlin.ext.common.CoroutineLocal
import com.firefly.kotlin.ext.context.Context
import com.firefly.kotlin.ext.db.AsyncTransactionalJDBCHelper
import com.firefly.kotlin.ext.example.task.management.dao.UserDao
import com.firefly.kotlin.ext.example.task.management.model.User
import com.firefly.kotlin.ext.example.task.management.service.UserService
import com.firefly.kotlin.ext.example.task.management.vo.Request
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.kotlin.ext.http.getPathParameter
import com.firefly.kotlin.ext.http.writeJson
import com.firefly.kotlin.ext.log.Log
import com.firefly.server.http2.router.RoutingContext
import kotlinx.coroutines.experimental.runBlocking
import java.util.*

/**
 * @author Pengtao Qiu
 */
private val log = Log.getLogger { }

fun main(args: Array<String>) {
    initData()
    applicationRun("localhost", 8080)
}

fun applicationRun(host: String, port: Int) {
    HttpServer(Context.getBean<CoroutineLocal<RoutingContext>>()) {
        router {
            httpMethod = HttpMethod.GET
            path = "/user/:id"

            asyncHandler {
                val userService = Context.getBean<UserService>()
                val response = userService.listUsers(Request("test", listOf(getPathParameter("id").toLong())))
                writeJson(response).end()
            }
        }

    }.listen(host, port)
}

fun initData(): Unit = runBlocking {
    val jdbcHelper = Context.getBean<AsyncTransactionalJDBCHelper>()
    jdbcHelper.update("drop schema if exists test")
    jdbcHelper.update("create schema test")
    jdbcHelper.update("set mode MySQL")

    val createUserTable = """
            CREATE TABLE `test`.`user` (
            `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
            `name` VARCHAR(64) NOT NULL,
            `create_time` DATETIME NOT NULL,
            `update_time` DATETIME NOT NULL
            )"""
    jdbcHelper.update(createUserTable)

    val userDao = Context.getBean<UserDao>()
    val id = userDao.insert(User(0L, "admin", Date(), Date()))
    log.info("insert user $id")
}