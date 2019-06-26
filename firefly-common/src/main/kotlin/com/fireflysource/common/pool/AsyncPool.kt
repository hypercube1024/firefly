package com.fireflysource.common.pool

/**
 * @author Pengtao Qiu
 */
interface AsyncPool<T> : Pool<T> {

    suspend fun getObject(): PooledObject<T>

}