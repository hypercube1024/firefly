package com.firefly.example.kotlin.test

import com.firefly.example.kotlin.coffee.store.ktCtx
import com.firefly.example.kotlin.coffee.store.utils.DBUtils
import com.firefly.kotlin.ext.common.CoroutineLocal
import com.firefly.kotlin.ext.context.getBean
import com.firefly.kotlin.ext.db.AsyncHttpContextTransactionalManager
import com.firefly.server.http2.router.RoutingContext
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.launch
import org.junit.Before
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Pengtao Qiu
 */
open class TestBase {

    protected val coroutineLocal = ktCtx.getBean<CoroutineLocal<RoutingContext>>("httpCoroutineLocal")
    protected val db = ktCtx.getBean<AsyncHttpContextTransactionalManager>()

    @Before
    fun before() {
        val dbUtils = ktCtx.getBean<DBUtils>()
        dbUtils.createTables()
        dbUtils.initializeData()
        println("init test data")
    }

    protected suspend fun newTransaction(action: suspend () -> Unit): Job {
        val map = ConcurrentHashMap<String, Any>()
        val ctx = mock(RoutingContext::class.java)
        `when`(ctx.getAttribute(db.transactionKey)).thenReturn(db.sqlClient.connection.await())
        `when`(ctx.attributes).thenReturn(map)
        return GlobalScope.launch(coroutineLocal.createContext(ctx)) {
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