package com.firefly.kotlin.ext.db

import com.firefly.db.DBException
import com.firefly.db.JDBCHelper
import com.firefly.db.MetricReporterFactory
import com.firefly.kotlin.ext.log.Log
import com.firefly.utils.Assert
import kotlinx.coroutines.experimental.NonCancellable
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.run
import org.apache.commons.dbutils.BeanProcessor
import org.apache.commons.dbutils.ResultSetHandler
import java.sql.Connection
import javax.sql.DataSource

/**
 * Asynchronous JDBC helper executes SQL using coroutine
 *
 * @author Pengtao Qiu
 */
private val log = Log.getLogger("firefly-system")

class AsynchronousJDBCHelper(val jdbcHelper: JDBCHelper) {

    constructor(dataSource: DataSource) : this(dataSource, false, null)

    constructor(dataSource: DataSource,
                monitorEnable: Boolean,
                metricReporterFactory: MetricReporterFactory?) : this(JDBCHelper(dataSource, monitorEnable, metricReporterFactory))

    suspend fun <T> Connection.queryForSingleColumn(sql: String, vararg params: Any): T? = executeSQL(this) { connection, helper ->
        helper.queryForSingleColumn<T>(connection, sql, *params)
    }

    suspend inline fun <reified T> Connection.queryForObject(sql: String, vararg params: Any) = executeSQL(this) { connection, helper ->
        helper.queryForObject<T>(connection, sql, T::class.java, *params)
    }

    suspend inline fun <reified T> Connection.queryForObject(sql: String, beanProcessor: BeanProcessor, vararg params: Any) = executeSQL(this) { connection, helper ->
        helper.queryForObject<T>(connection, sql, T::class.java, beanProcessor, *params)
    }

    suspend inline fun <reified T> Connection.queryById(id: Any) = executeSQL(this) { connection, helper ->
        helper.queryById<T>(connection, T::class.java, id)
    }

    suspend inline fun <K, reified V> Connection.queryForBeanMap(sql: String, vararg params: Any): Map<K, V> = executeSQL(this) { connection, helper ->
        helper.queryForBeanMap<K, V>(connection, sql, V::class.java, *params)
    } ?: mapOf()

    suspend inline fun <K, reified V> Connection.queryForBeanMap(sql: String, beanProcessor: BeanProcessor, vararg params: Any): Map<K, V> = executeSQL(this) { connection, helper ->
        val t = V::class.java
        val columnName = helper.defaultBeanProcessor.getIdColumnName(t)
        Assert.notNull(columnName)
        helper.queryForBeanMap<K, V>(connection, sql, t, columnName, beanProcessor, *params)
    } ?: mapOf()

    suspend inline fun <reified T> Connection.queryForList(sql: String, vararg params: Any): List<T> = executeSQL(this) { connection, helper ->
        helper.queryForList<T>(connection, sql, T::class.java, *params)
    } ?: listOf()

    suspend inline fun <reified T> Connection.queryForList(sql: String, beanProcessor: BeanProcessor, vararg params: Any): List<T> = executeSQL(this) { connection, helper ->
        helper.queryForList<T>(connection, sql, T::class.java, beanProcessor, *params)
    } ?: listOf()

    suspend fun Connection.update(sql: String, vararg params: Any) = executeSQL(this) { connection, helper ->
        helper.update(connection, sql, *params)
    } ?: -1

    suspend fun <T> Connection.updateObject(obj: T) = executeSQL(this) { connection, helper ->
        helper.updateObject(connection, obj)
    } ?: -1

    suspend fun <R> Connection.insert(sql: String, vararg params: Any) = executeSQL(this) { connection, helper ->
        helper.insert<R>(connection, sql, *params)
    }

    suspend fun <T, R> Connection.insertObject(obj: T) = executeSQL(this) { connection, helper ->
        helper.insertObject<R>(connection, obj)
    }

    suspend inline fun <reified T, R> Connection.insertObjectBatch(list: List<T>, rsh: ResultSetHandler<R>) = executeSQL(this) { connection, helper ->
        helper.insertObjectBatch(connection, rsh, T::class.java, list)
    }

    suspend inline fun <reified T, reified R> Connection.insertObjectBatch(list: List<T>) = insertObjectBatch(list, ResultSetHandler<List<R>> { rs ->
        val ret = mutableListOf<R>()
        while (rs.next()) {
            val element = rs.getObject(1)
            if (element is R) {
                ret.add(element)
            }
        }
        ret
    })

    suspend inline fun <reified T> Connection.deleteById(id: Any) = executeSQL(this) { connection, helper ->
        helper.deleteById(connection, T::class.java, id)
    } ?: -1

    suspend fun Connection.batch(sql: String, params: Array<Array<Any>>) = executeSQL(this) { connection, helper ->
        try {
            helper.runner.batch(connection, sql, params)
        } catch (e: Exception) {
            log.error("batch exception", e)
            throw DBException(e)
        }
    }

    suspend fun <T> executeSQL(connection: Connection, func: (Connection, JDBCHelper) -> T): T? {
        return jdbcHelper.async(connection, func).await()
    }

    suspend fun getConnection(): Connection = jdbcHelper.asyncGetConnection().await()

    suspend fun <T> transaction(func: suspend AsynchronousJDBCHelper.(Connection) -> T?): T? {
        val connection = getConnection()
        try {
            connection.autoCommit = false
            return func.invoke(this, connection)
        } catch (e: Exception) {
            log.error("execute SQL exception", e)
            connection.rollback()
            return null
        } finally {
            run(NonCancellable) {
                connection.autoCommit = true
                connection.close()
            }
        }
    }

    suspend fun <T> executeSQL(func: suspend AsynchronousJDBCHelper.() -> T?): T? = func.invoke(this)

}