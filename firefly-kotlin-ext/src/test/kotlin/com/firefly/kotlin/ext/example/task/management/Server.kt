package com.firefly.kotlin.ext.example.task.management

import com.firefly.codec.http2.model.HttpMethod
import com.firefly.kotlin.ext.common.CoroutineLocal
import com.firefly.kotlin.ext.context.Context
import com.firefly.kotlin.ext.db.AsyncHttpContextTransactionalManager
import com.firefly.kotlin.ext.db.AsyncTransactionalJDBCHelper
import com.firefly.kotlin.ext.example.task.management.dao.UserDao
import com.firefly.kotlin.ext.example.task.management.model.User
import com.firefly.kotlin.ext.example.task.management.service.UserService
import com.firefly.kotlin.ext.example.task.management.vo.Request
import com.firefly.kotlin.ext.example.task.management.vo.Response
import com.firefly.kotlin.ext.http.*
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
    server.listen("localhost", 8080)
}

val server = HttpServer(Context.getBean<CoroutineLocal<RoutingContext>>()) {

    val transactionalManager = Context.getBean<AsyncHttpContextTransactionalManager>()

    router {
        // transactional interceptor
        httpMethods = listOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)
        paths = listOf("/*create*", "/*update*", "/*delete*")

        asyncHandler {
            transactionalManager.asyncBeginTransaction()
            promise<Unit>(succeeded = {
                try {
                    transactionalManager.commit()
                } finally {
                    transactionalManager.endTransaction()
                    end()
                }
            }, failed = {
                try {
                    transactionalManager.rollback()
                    log.error("transactional request exception", it)
                    writeJson(Response(500, "server exception", it?.message))
                } finally {
                    transactionalManager.endTransaction()
                    end()
                }
            })
            next()
        }
    }

    router {
        httpMethod = HttpMethod.GET
        path = "/user/:id"

        asyncHandler {
            val userService = Context.getBean<UserService>()
            val response = userService.listUsers(Request("test", listOf(getPathParameter("id").toLong())))
            writeJson(response).end()
        }
    }

    router {
        httpMethod = HttpMethod.POST
        path = "/user/create"
        consumes = "application/json"

        asyncHandler {
            try {
                val userService = Context.getBean<UserService>()
                val request = getJsonBody<Request<User>>()
                log.info("create user request $request")
                val response = userService.insert(request)
                writeJson(response).succeed(Unit)
            } catch (x: Throwable) {
                fail<Unit>(x)
            }
        }
    }

    router {
        httpMethod = HttpMethod.POST
        path = "/user/createSimulateRollback"

        asyncHandler {
            try {
                val userService = Context.getBean<UserService>()
                val request = getJsonBody<Request<User>>()
                log.info("create user request $request")
                val response = userService.insert(request)
                throw IllegalStateException("rollback $response")
            } catch (x: Throwable) {
                fail<Unit>(x)
            }
        }
    }
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