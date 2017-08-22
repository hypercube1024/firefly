package com.firefly.kotlin.ext.example.task.management

import com.firefly.codec.http2.model.HttpMethod
import com.firefly.codec.http2.model.HttpStatus
import com.firefly.db.SQLClient
import com.firefly.kotlin.ext.common.CoroutineLocal
import com.firefly.kotlin.ext.context.Context
import com.firefly.kotlin.ext.db.AsyncTransactionalManager
import com.firefly.kotlin.ext.db.asyncUpdate
import com.firefly.kotlin.ext.db.execSQL
import com.firefly.kotlin.ext.example.task.management.dao.UserDao
import com.firefly.kotlin.ext.example.task.management.model.User
import com.firefly.kotlin.ext.example.task.management.service.ProjectService
import com.firefly.kotlin.ext.example.task.management.service.UserService
import com.firefly.kotlin.ext.example.task.management.vo.ProjectEditor
import com.firefly.kotlin.ext.example.task.management.vo.Request
import com.firefly.kotlin.ext.example.task.management.vo.Response
import com.firefly.kotlin.ext.http.*
import com.firefly.kotlin.ext.log.Log
import com.firefly.server.http2.router.RoutingContext
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.runBlocking
import java.util.*
import kotlin.coroutines.experimental.CoroutineContext

/**
 * @author Pengtao Qiu
 */
private val log = Log.getLogger { }

fun main(args: Array<String>) {
    initData()
    server.listen("localhost", 8080)
}

val server = HttpServer(Context.getBean<CoroutineLocal<RoutingContext>>()) {

    val transactionalManager = Context.getBean<AsyncTransactionalManager>()
    val projectService = Context.getBean<ProjectService>()
    val userService = Context.getBean<UserService>()

    fun RouterBlock.asyncTransactionalHandler(handler: suspend RoutingContext.(context: CoroutineContext) -> Unit) = asyncHandler {
        try {
            handler.invoke(this, it)
            asyncSucceed(Unit)
        } catch (x: Throwable) {
            asyncFail<Unit>(x)
        }
    }

    router {
        // transactional interceptor
        httpMethods = listOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)
        paths = listOf("/*create*", "/*update*", "/*delete*")

        asyncHandler {
            transactionalManager.beginTransaction()
            asyncNext<Unit>(succeeded = {
                try {
                    transactionalManager.commitAndEndTransaction()
                } catch (e: Exception) {
                    log.error("commit and end transaction exception", e)
                } finally {
                    end()
                }
            }, failed = {
                try {
                    statusLine {
                        status = HttpStatus.INTERNAL_SERVER_ERROR_500
                    }
                    transactionalManager.rollbackAndEndTransaction()
                    log.error("transactional request exception", it)
                    writeJson(Response(500, "server exception", it?.message))
                } catch (e: Exception) {
                    log.error("rollback and end transaction exception", e)
                } finally {
                    end()
                }
            })
        }
    }

    router {
        httpMethod = HttpMethod.GET
        path = "/user/:id"

        asyncHandler {
            val response = userService.getUser(Request("test", getPathParameter("id").toLong()))
            writeJson(response).end()
        }
    }

    router {
        httpMethod = HttpMethod.POST
        path = "/user/create"
        consumes = "application/json"

        asyncTransactionalHandler {
            val request = getJsonBody<Request<User>>()
            log.info("create user request $request")
            val response = userService.insert(request)
            writeJson(response)
        }
    }

    router {
        httpMethod = HttpMethod.POST
        path = "/user/createSimulateRollback"

        asyncTransactionalHandler {
            val request = getJsonBody<Request<User>>()
            log.info("create user request $request")
            val response = userService.insert(request)
            throw IllegalStateException("rollback $response")
        }
    }

    router {
        httpMethod = HttpMethod.GET
        path = "/project/:id"

        asyncHandler {
            val response = projectService.getProject(Request("test", getPathParameter("id").toLong()))
            writeJson(response).end()
        }
    }

    router {
        httpMethod = HttpMethod.POST
        path = "/project/create"

        asyncTransactionalHandler {
            val request = getJsonBody<Request<ProjectEditor>>()
            log.info("create project request $request")
            val response = projectService.createProject(request)
            writeJson(response)
        }
    }

}

fun initData(): Unit = runBlocking {
    val dbClient = Context.getBean<SQLClient>()
    dbClient.connection.await().execSQL {
        it.asyncUpdate("drop schema if exists test")
        it.asyncUpdate("create schema test")
        it.asyncUpdate("set mode MySQL")

        val userTable = """
            CREATE TABLE `test`.`user` (
            `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
            `name` VARCHAR(64) NOT NULL,
            `create_time` DATETIME NOT NULL,
            `update_time` DATETIME NOT NULL
            )"""
        it.asyncUpdate(userTable)

        val projectTable = """
            CREATE TABLE `test`.`project` (
            `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
            `name` VARCHAR(64) NOT NULL,
            `create_time` DATETIME NOT NULL,
            `update_time` DATETIME NOT NULL,
            `description` VARCHAR(128) NOT NULL
            )"""
        it.asyncUpdate(projectTable)

        val projectUser = """
            CREATE TABLE `test`.`project_user` (
            `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
            `user_id` BIGINT NOT NULL,
            `project_id` BIGINT NOT NULL
            )"""
        it.asyncUpdate(projectUser)

        val userDao = Context.getBean<UserDao>()
        val id = userDao.insert(User(0L, "admin", Date(), Date()))
        log.info("insert user $id")
    }
    Unit
}