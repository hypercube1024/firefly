package com.firefly.kotlin.ext.db

import com.firefly.db.*
import com.firefly.kotlin.ext.log.Log
import com.firefly.utils.Assert
import kotlinx.coroutines.experimental.future.await
import org.apache.commons.dbutils.BeanProcessor
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

/**
 * @author Pengtao Qiu
 */
private val log = Log.getLogger("firefly-system")

class AsynchronousJDBCHelper(val jdbcHelper: JDBCHelper, val transactionalManager: TransactionalManager) {

    constructor(dataSource: DataSource) : this(dataSource, null, ThreadLocalTransactionalManager(dataSource))

    constructor(dataSource: DataSource,
                metricReporterFactory: MetricReporterFactory?,
                transactionalManager: TransactionalManager) : this(JDBCHelper(dataSource, metricReporterFactory), transactionalManager)

    suspend fun <T> queryForSingleColumn(sql: String, vararg params: Any) = executeSQL { connection, helper ->
        helper.queryForSingleColumn<T>(connection, sql, *params)
    }

    suspend inline fun <reified T> queryForObject(sql: String, vararg params: Any) = executeSQL { connection, helper ->
        helper.queryForObject<T>(connection, sql, T::class.java, *params)
    }

    suspend inline fun <reified T> queryForObject(sql: String, beanProcessor: BeanProcessor, vararg params: Any) = executeSQL { connection, helper ->
        helper.queryForObject<T>(connection, sql, T::class.java, beanProcessor, *params)
    }

    suspend inline fun <reified T> queryById(id: Any) = executeSQL { connection, helper ->
        helper.queryById<T>(connection, T::class.java, id)
    }

    suspend inline fun <K, reified V> queryForBeanMap(sql: String, vararg params: Any): Map<K, V> = executeSQL { connection, helper ->
        helper.queryForBeanMap<K, V>(connection, sql, V::class.java, *params)
    } ?: mapOf()

    suspend inline fun <K, reified V> queryForBeanMap(sql: String, beanProcessor: BeanProcessor, vararg params: Any): Map<K, V> = executeSQL { connection, helper ->
        val t = V::class.java
        val columnName = helper.defaultBeanProcessor.getIdColumnName(t)
        Assert.notNull(columnName)
        helper.queryForBeanMap<K, V>(connection, sql, t, columnName, beanProcessor, *params)
    } ?: mapOf()

    suspend inline fun <reified T> queryForList(sql: String, vararg params: Any): List<T> = executeSQL { connection, helper ->
        helper.queryForList<T>(connection, sql, T::class.java, *params)
    } ?: listOf()

    suspend inline fun <reified T> queryForList(sql: String, beanProcessor: BeanProcessor, vararg params: Any): List<T> = executeSQL { connection, helper ->
        helper.queryForList<T>(connection, sql, T::class.java, beanProcessor, *params)
    } ?: listOf()

    suspend fun update(sql: String, vararg params: Any) = executeSQL { connection, helper ->
        helper.update(connection, sql, *params)
    } ?: -1

    suspend fun updateObject(obj: Any) = executeSQL { connection, helper ->
        helper.updateObject(connection, obj)
    } ?: -1

    suspend fun <T> insert(sql: String, vararg params: Any) = executeSQL { connection, helper ->
        helper.insert<T>(connection, sql, *params)
    }

    suspend fun <T> insertObject(obj: Any) = executeSQL { connection, helper ->
        helper.insertObject<T>(connection, obj)
    }

    suspend inline fun <reified T> deleteById(id: Any) = executeSQL { connection, helper ->
        helper.deleteById(connection, T::class.java, id)
    } ?: -1

    suspend fun batch(sql: String, params: Array<Array<Any>>) = executeSQL { connection, helper ->
        try {
            helper.runner.batch(connection, sql, params)
        } catch (e: Exception) {
            log.error("batch exception", e)
            throw DBException(e)
        }
    }

    suspend fun <T> executeTransaction

    suspend fun <T> executeSQL(func: (Connection, JDBCHelper) -> T): T? {
        if (transactionalManager.isTransactionBegin) {
            transactionalManager.beginTransaction()
            try {
                val ret = jdbcHelper.async(transactionalManager.connection, func).await()
                transactionalManager.commit()
                return ret
            } catch (t: Throwable) {
                transactionalManager.rollback()
                log.error("the transaction exception", t)
                return null
            } finally {
                transactionalManager.endTransaction()
            }
        } else {
            try {
                transactionalManager.connection.use { connection ->
                    jdbcHelper.setAutoCommit(connection, true)
                    return jdbcHelper.async(connection, func).await()
                }
            } catch (e: SQLException) {
                log.error("execute SQL exception", e)
                throw DBException(e)
            }

        }
    }

}
