package com.fireflysource.common.track

import com.fireflysource.common.coroutine.launchSingle
import com.fireflysource.common.func.Callback
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.sys.Result
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

/**
 * @author Pengtao Qiu
 */
class FixedTimeLeakDetector<T>(
    private val scheduler: ScheduledExecutorService,
    private val initialDelay: Long,
    private val delay: Long,
    private val releaseTimeout: Long,
    private val unit: TimeUnit,
    private val noLeakCallback: Callback
) : AbstractLifeCycle() {

    init {
        start()
    }

    private val registeredMap = IdentityHashMap<T, TrackedObject>()

    fun register(obj: T, leakCallback: Consumer<T>) = launchSingle {
        val trackedObject = TrackedObject()
        trackedObject.leakCallback = leakCallback
        trackedObject.registeredTime = System.currentTimeMillis()
        registeredMap[obj] = trackedObject
    }

    fun clear(obj: T) = launchSingle {
        registeredMap.remove(obj)
    }

    private fun checkLeak() {
        launchSingle {
            var leaked = false
            for ((obj, trackedObject) in registeredMap) {
                val releaseTimeoutMillis = unit.toMillis(releaseTimeout)
                val currentTime = System.currentTimeMillis()
                if (currentTime - trackedObject.registeredTime >= releaseTimeoutMillis) {
                    leaked = true
                    trackedObject.leakCallback.accept(obj)
                }
            }
            if (!leaked) {
                noLeakCallback.call()
            }

            if (!isStopped) {
                scheduler.schedule(this@FixedTimeLeakDetector::checkLeak, delay, unit)
            }
        }
    }

    override fun init() {
        scheduler.schedule(this::checkLeak, initialDelay, unit)
    }

    override fun destroy() {}

    private inner class TrackedObject {
        var registeredTime: Long = 0
        var leakCallback: Consumer<T> = Result.emptyConsumer()
    }

}