package com.fireflysource.log

import com.fireflysource.log.LogConfigParser.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.Closeable
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


/**
 * @author Pengtao Qiu
 */
class FileLog : Log, Closeable {

    var level: LogLevel = LogLevel.fromName(DEFAULT_LOG_LEVEL)
    var path: String = DEFAULT_LOG_DIRECTORY.absolutePath
    var logName: String = DEFAULT_LOG_NAME
    var consoleOutput: Boolean = false
    var fileOutput: Boolean = true
    var maxFileSize: Long = DEFAULT_MAX_FILE_SIZE
    var logFormatter: LogFormatter = DefaultLogFormatter()
    var logNameFormatter: LogNameFormatter = DefaultLogNameFormatter()
    var logFilter: LogFilter = DefaultLogFilter()
    var maxSplitTime: MaxSplitTimeEnum = MaxSplitTimeEnum.DAY
    var charset: Charset = DEFAULT_CHARSET

    private val mdc: MappedDiagnosticContext = MappedDiagnosticContextFactory.getInstance().mappedDiagnosticContext
    private val consumerJob = GlobalScope.launch(consumerThread) {
        fun initFileOutputStream() {

        }

        while (!channel.isClosedForReceive) {
            val logItem = channel.receive()
            // TODO
        }

    }

    companion object {
        private val stackTrace =
            java.lang.Boolean.getBoolean("com.fireflysource.log.com.fireflysource.log.FileLog.debugMode")

        private val singleThreadQueueSize = Integer.getInteger(
            "com.fireflysource.log.com.fireflysource.log.FileLog.singleThreadQueueSize",
            20000
                                                              )
        private val producerThread: CoroutineDispatcher by lazy {
            val threadId = AtomicInteger()
            ThreadPoolExecutor(
                1, 1,
                0, TimeUnit.MILLISECONDS,
                ArrayBlockingQueue<Runnable>(singleThreadQueueSize)
                              ) { r ->
                Thread(r, "firefly-file-log-producer-thread-pool-" + threadId.getAndIncrement())
            }.asCoroutineDispatcher()
        }

        private val consumerThread: CoroutineDispatcher by lazy {
            val threadId = AtomicInteger()
            ThreadPoolExecutor(
                1, 1,
                0, TimeUnit.MILLISECONDS,
                ArrayBlockingQueue<Runnable>(singleThreadQueueSize)
                              ) { r ->
                Thread(r, "firefly-file-log-consumer-thread-pool-" + threadId.getAndIncrement())
            }.asCoroutineDispatcher()
        }

        private val channel = Channel<LogItem>()
    }

    override fun getName(): String = logName

    override fun isTraceEnabled(): Boolean = level.isEnabled(LogLevel.TRACE)

    override fun trace(str: String?) {
        if (isTraceEnabled) {
            write(str, LogLevel.TRACE.getName(), null)
        }
    }

    override fun trace(str: String?, vararg objs: Any?) {
        if (isTraceEnabled) {
            write(str, LogLevel.TRACE.getName(), null, objs)
        }
    }

    override fun trace(str: String?, throwable: Throwable?, vararg objs: Any?) {
        if (isTraceEnabled) {
            write(str, LogLevel.TRACE.getName(), throwable, objs)
        }
    }

    override fun isDebugEnabled(): Boolean = level.isEnabled(LogLevel.DEBUG)

    override fun debug(str: String?) {
        if (isDebugEnabled) {
            write(str, LogLevel.DEBUG.getName(), null)
        }
    }

    override fun debug(str: String?, vararg objs: Any?) {
        if (isDebugEnabled) {
            write(str, LogLevel.DEBUG.getName(), null, objs)
        }
    }

    override fun debug(str: String?, throwable: Throwable?, vararg objs: Any?) {
        if (isDebugEnabled) {
            write(str, LogLevel.DEBUG.getName(), throwable, objs)
        }
    }

    override fun isInfoEnabled(): Boolean = level.isEnabled(LogLevel.INFO)

    override fun info(str: String?) {
        if (isInfoEnabled) {
            write(str, LogLevel.INFO.getName(), null)
        }
    }

    override fun info(str: String?, vararg objs: Any?) {
        if (isInfoEnabled) {
            write(str, LogLevel.INFO.getName(), null, objs)
        }
    }

    override fun info(str: String?, throwable: Throwable?, vararg objs: Any?) {
        if (isInfoEnabled) {
            write(str, LogLevel.INFO.getName(), throwable, objs)
        }
    }

    override fun isWarnEnabled(): Boolean = level.isEnabled(LogLevel.WARN)

    override fun warn(str: String?) {
        if (isWarnEnabled) {
            write(str, LogLevel.WARN.getName(), null)
        }
    }

    override fun warn(str: String?, vararg objs: Any?) {
        if (isWarnEnabled) {
            write(str, LogLevel.WARN.getName(), null, objs)
        }
    }

    override fun warn(str: String?, throwable: Throwable?, vararg objs: Any?) {
        if (isWarnEnabled) {
            write(str, LogLevel.WARN.getName(), throwable, objs)
        }
    }

    override fun isErrorEnabled(): Boolean = level.isEnabled(LogLevel.ERROR)

    override fun error(str: String?) {
        if (isErrorEnabled) {
            write(str, LogLevel.ERROR.getName(), null)
        }
    }

    override fun error(str: String?, vararg objs: Any?) {
        if (isErrorEnabled) {
            write(str, LogLevel.ERROR.getName(), null, objs)
        }
    }

    override fun error(str: String?, throwable: Throwable?, vararg objs: Any?) {
        if (isErrorEnabled) {
            write(str, LogLevel.ERROR.getName(), throwable, objs)
        }
    }

    override fun close() = runBlocking {
        channel.close()
        consumerJob.join()
    }

    private fun write(content: String?, level: String, throwable: Throwable?, vararg objects: Any) {
        val item = LogItem()
        item.level = level
        item.name = name
        item.content = content
        item.objs = objects
        item.throwable = throwable
        item.date = Date()
        item.mdcData = mdc.copyOfContextMap
        item.className = ClassNameLogWrap.name.get()
        item.threadName = Thread.currentThread().name
        if (stackTrace) {
            item.stackTraceElement = getStackTraceElement()
        }
        write(item)
    }

    private fun getStackTraceElement(): StackTraceElement? {
        val arr = Thread.currentThread().stackTrace
        val s = arr[4]
        return if (s != null) {
            if (s.className == "com.fireflysource.log.ClassNameLogWrap") {
                arr[6]
            } else {
                s
            }
        } else {
            s
        }
    }

    private fun write(logItem: LogItem) {
        GlobalScope.launch(producerThread) {
            channel.send(logItem)
        }
    }
}