package com.firefly.example.kotlin.coffee.store

import com.firefly.annotation.Inject
import com.firefly.example.kotlin.coffee.store.router.RouterInstaller
import com.firefly.example.kotlin.coffee.store.utils.DBUtils
import com.firefly.kotlin.ext.context.Context
import com.firefly.kotlin.ext.context.getBean
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.kotlin.ext.log.KtLogger
import com.firefly.kotlin.ext.log.info
import com.firefly.utils.lang.AbstractLifeCycle

/**
 * @author Pengtao Qiu
 */

val ktCtx = Context.create("kotlin-example.xml")

class AppMain : AbstractLifeCycle() {

    private val log = KtLogger.getLogger { }

    @Inject
    private lateinit var server: HttpServer

    @Inject
    private lateinit var dbUtils: DBUtils

    @Inject
    private lateinit var projectConfig: ProjectConfig

    override fun init() {
        dbUtils.createTables()
        dbUtils.initializeData()

        ktCtx.getBeans(RouterInstaller::class.java).sorted().forEach {
            log.info { "install routers [${it::class.qualifiedName}]" }
            it.install()
        }
//        server.server.configuration.isSecureConnectionEnabled = true
        server.listen(projectConfig.host, projectConfig.port)
    }

    override fun destroy() {
        server.stop()
    }
}

fun main(args: Array<String>) {
    ktCtx.getBean<AppMain>().start()
}