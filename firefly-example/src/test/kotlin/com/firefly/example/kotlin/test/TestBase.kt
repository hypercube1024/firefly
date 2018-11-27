package com.firefly.example.kotlin.test

import com.firefly.example.kotlin.coffee.store.ktCtx
import com.firefly.example.kotlin.coffee.store.utils.DBUtils
import com.firefly.kotlin.ext.common.CoroutineLocal
import com.firefly.kotlin.ext.common.CoroutineLocalContext
import com.firefly.kotlin.ext.context.getBean
import com.firefly.kotlin.ext.db.AsyncCoroutineContextTransactionalManager
import com.firefly.server.http2.router.RoutingContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import org.junit.Before
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Pengtao Qiu
 */
open class TestBase {
    protected val db = ktCtx.getBean<AsyncCoroutineContextTransactionalManager>()

    @Before
    fun before() {
        val dbUtils = ktCtx.getBean<DBUtils>()
        dbUtils.createTables()
        dbUtils.initializeData()
        println("init test data")
    }

    protected suspend fun newTransaction(action: suspend () -> Unit): Job {
        return GlobalScope.launch(CoroutineLocalContext.inheritParentElement()) {
            db.beginTransaction()
            try {
                action.invoke()
                db.commitAndEndTransaction()
            } catch (e: Exception) {
                e.printStackTrace()
                db.rollbackAndEndTransaction()
            }
        }
    }
}