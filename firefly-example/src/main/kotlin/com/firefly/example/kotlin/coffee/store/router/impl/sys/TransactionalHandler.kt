package com.firefly.example.kotlin.coffee.store.router.impl.sys

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.codec.http2.model.HttpStatus
import com.firefly.kotlin.ext.db.AsyncTransactionalManager
import com.firefly.kotlin.ext.http.AsyncHandler
import com.firefly.kotlin.ext.http.asyncFail
import com.firefly.kotlin.ext.http.asyncNext
import com.firefly.kotlin.ext.http.asyncSucceed
import com.firefly.kotlin.ext.log.Log
import com.firefly.server.http2.router.RoutingContext

/**
 * @author Pengtao Qiu
 */
@Component("transactionalHandler")
class TransactionalHandler : AsyncHandler {

    private val log = Log.getLogger { }

    @Inject
    private lateinit var db: AsyncTransactionalManager

    suspend override fun handle(ctx: RoutingContext) {
        log.info("begin transaction -> ${ctx.uri}")
        db.beginTransaction()
        ctx.asyncNext<Unit>(succeeded = {
            try {
                db.commitAndEndTransaction()
                ctx.asyncSucceed(Unit)
            } catch (e: Exception) {
                log.error("commit and end transaction exception", e)
                ctx.asyncFail<Unit>(e)
            }
        }, failed = {
            ctx.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500)
            try {
                log.error("transactional request exception", it)
                db.rollbackAndEndTransaction()
                ctx.asyncFail<Unit>(it)
            } catch (e: Exception) {
                log.error("rollback and end transaction exception", e)
                ctx.asyncFail<Unit>(e)
            }
        })
    }

}