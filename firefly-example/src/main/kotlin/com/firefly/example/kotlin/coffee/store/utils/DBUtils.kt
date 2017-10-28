package com.firefly.example.kotlin.coffee.store.utils

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.db.init.ScriptUtils.executeSqlScript
import com.firefly.kotlin.ext.log.Log
import com.firefly.kotlin.ext.log.error
import com.firefly.utils.io.Resource
import java.util.stream.Collectors
import javax.sql.DataSource

/**
 * @author Pengtao Qiu
 */
private val logger = Log.getLogger { }

@Component
class DBUtils {

    @Inject
    lateinit var dataSource: DataSource

    @Inject
    lateinit var resourceUtils: ResourceUtils

    val schemaScript: Resource
        get() = resourceUtils.resource("/dbScript/coffee_store_schema.sql")

    val initDataScript: Resource
        get() = resourceUtils.resource("/dbScript/coffee_store_init_data.sql")

    fun createTables() {
        executeScript(schemaScript)
    }

    fun initializeData() {
        executeScript(initDataScript)
    }

    fun executeScript(resource: Resource) = try {
        dataSource.connection.use { executeSqlScript(it, resource) }
    } catch (e: Exception) {
        logger.error({ "execute SQL exception" }, e)
    }

    companion object {
        fun toWildcard(list: List<*>): String {
            return list.parallelStream().map { "?" }.collect(Collectors.joining(","))
        }
    }

}