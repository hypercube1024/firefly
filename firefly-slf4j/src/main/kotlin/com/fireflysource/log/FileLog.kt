package com.fireflysource.log

import com.fireflysource.log.LogConfigParser.*
import com.fireflysource.log.internal.utils.TimeUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.CancellationException


/**
 * @author Pengtao Qiu
 */
class FileLog : Log {

    companion object {
        private val stackTrace =
            java.lang.Boolean.getBoolean("com.fireflysource.log.FileLog.debugMode")

        val executor = newSingleThreadExecutor()
        private val logThread: CoroutineDispatcher = executor.asCoroutineDispatcher()
        private val fileLogThreadScope: CoroutineScope = CoroutineScope(logThread + CoroutineName("FireflyFileLogThread"))

        class FinalizableExecutorService(private val executor: ExecutorService) : ExecutorService by executor {
            protected fun finalize() {
                executor.shutdown()
            }
        }

        private fun newSingleThreadExecutor(): ExecutorService {
            val executor = ThreadPoolExecutor(
                1, 1, 0, TimeUnit.MILLISECONDS, LinkedTransferQueue()
            ) { runnable -> Thread(runnable, "firefly-log-thread") }
            return FinalizableExecutorService(executor)
        }

        private val stopLogMessage = LogItem()
    }

    var level: LogLevel = LogLevel.fromName(DEFAULT_LOG_LEVEL)
    var path: String = DEFAULT_LOG_DIRECTORY.absolutePath
    var logName: String = DEFAULT_LOG_NAME
    var consoleOutput: Boolean = false
    var fileOutput: Boolean = true
    var maxFileSize: Long = DEFAULT_MAX_FILE_SIZE
    var logFormatter: LogFormatter = DEFAULT_LOG_FORMATTER
    var logNameFormatter: LogNameFormatter = DEFAULT_LOG_NAME_FORMATTER
    var logFilter: LogFilter = DEFAULT_LOG_FILTER
    var maxSplitTime: MaxSplitTimeEnum = MaxSplitTimeEnum.DAY
    var charset: Charset = DEFAULT_CHARSET

    private val mdc: MappedDiagnosticContext = MappedDiagnosticContextFactory.getInstance().mappedDiagnosticContext

    private val output = LogOutputStream()
    private val channel = Channel<LogItem>(UNLIMITED)
    private val consumerJob = fileLogThreadScope.launch(logThread) {
        recvLogItemLoop@ while (true) {
            val logItem = channel.receive()
            if (logItem === stopLogMessage) {
                break@recvLogItemLoop
            }

            logFilter.filter(logItem)
            if (consoleOutput) {
                println(logFormatter.format(logItem))
            }

            if (fileOutput) {
                output.write(logFormatter.format(logItem), logItem.date)
            }
        }
        println("File log $logName is closed.")
    }


    private inner class LogOutputStream {
        private var fileOutputStream: FileOutputStream? = null
        private var writtenSize: Long = 0
        private var lastWrittenTime: LocalDateTime? = null

        private fun getLogName(localDateTime: LocalDateTime): String {
            return logNameFormatter.format(name, localDateTime)
        }

        private fun getLogBakName(localDateTime: LocalDateTime, index: Int): String {
            return logNameFormatter.formatBak(name, localDateTime, index)
        }

        private fun getLogBakName(localDateTime: LocalDateTime): String {
            var index = 0
            var bakName = getLogBakName(localDateTime, index)
            while (Files.exists(Paths.get(path, bakName))) {
                index++
                bakName = getLogBakName(localDateTime, index)
            }
            return bakName
        }

        private fun isNotSplitByTime(newLocalDateTime: LocalDateTime): Boolean {
            val lastTime = lastWrittenTime
            requireNotNull(lastTime)
            when (maxSplitTime) {
                MaxSplitTimeEnum.DAY -> {
                    return (lastTime.year == newLocalDateTime.year
                            && lastTime.month == newLocalDateTime.month
                            && lastTime.dayOfMonth == newLocalDateTime.dayOfMonth)
                }
                MaxSplitTimeEnum.HOUR -> {
                    return (lastTime.year == newLocalDateTime.year
                            && lastTime.month == newLocalDateTime.month
                            && lastTime.dayOfMonth == newLocalDateTime.dayOfMonth
                            && lastTime.hour == newLocalDateTime.hour)
                }
                MaxSplitTimeEnum.MINUTE -> {
                    return (lastTime.year == newLocalDateTime.year
                            && lastTime.month == newLocalDateTime.month
                            && lastTime.dayOfMonth == newLocalDateTime.dayOfMonth
                            && lastTime.hour == newLocalDateTime.hour
                            && lastTime.minute == newLocalDateTime.minute)
                }
            }
        }

        private fun getFileLastModifiedTime(logPath: Path): LocalDateTime {
            val lastTime = lastWrittenTime
            return if (lastTime == null) {
                val fileTime = Files.getLastModifiedTime(logPath)
                val time = LocalDateTime.from(fileTime.toInstant().atZone(ZoneId.systemDefault()))
                lastWrittenTime = time
                time
            } else lastTime
        }

        private fun getOutput(newDate: Date, currentLogSize: Long): OutputStream {
            initializeOutputStream(newDate, currentLogSize)
            val output = fileOutputStream
            requireNotNull(output)
            return output
        }

        private fun initializeOutputStream(newDate: Date, currentLogSize: Long) {
            val newLocalDateTime = TimeUtils.toLocalDateTime(newDate)
            val logName = getLogName(newLocalDateTime)
            val logPath = Paths.get(path, logName)

            if (Files.exists(logPath)) {
                val lastModifiedTime = getFileLastModifiedTime(logPath)

                if (isNotSplitByTime(newLocalDateTime)) {
                    if (maxFileSize > 0) {
                        if (writtenSize == 0L) {
                            writtenSize = Files.size(logPath)
                        }
                        if (currentLogSize + writtenSize > maxFileSize) {
                            createLogOutputAndBackupOldLog(logName, logPath, lastModifiedTime)
                        } else createLogOutputIfNull(logName)
                    } else createLogOutputIfNull(logName)
                } else createLogOutputAndBackupOldLog(logName, logPath, lastModifiedTime)
            } else createLogOutputIfNull(logName)
        }

        private fun createLogOutputAndBackupOldLog(
            logName: String,
            logPath: Path,
            fileLastModifiedDateTime: LocalDateTime
        ) {
            close()
            Files.move(logPath, Paths.get(path, getLogBakName(fileLastModifiedDateTime)))
            fileOutputStream = FileOutputStream(File(path, logName), true)
        }

        private fun createLogOutputIfNull(logName: String) {
            if (fileOutputStream == null) {
                fileOutputStream = FileOutputStream(File(path, logName), true)
            }
        }

        fun write(str: String, date: Date) {
            val text = (str + Log.CL).toByteArray(charset)
            try {
                val output = getOutput(date, text.size.toLong())
                output.write(text)
                writtenSize += text.size
                lastWrittenTime = TimeUtils.toLocalDateTime(date)
            } catch (e: IOException) {
                System.err.println("write log exception, " + e.message)
            }
        }

        fun close() {
            val output = fileOutputStream
            if (output != null) {
                try {
                    output.close()
                    writtenSize = 0
                } catch (e: IOException) {
                    System.err.println("close log writer exception, " + e.message)
                }
            }
        }

    }

    override fun getName(): String = logName

    override fun isTraceEnabled(): Boolean = level.isEnabled(LogLevel.TRACE)

    override fun trace(str: String?) {
        if (isTraceEnabled) {
            write(str, LogLevel.TRACE.getName(), null, null)
        }
    }

    override fun trace(str: String?, objs: Array<Any>?) {
        if (isTraceEnabled) {
            write(str, LogLevel.TRACE.getName(), null, objs)
        }
    }

    override fun trace(str: String?, throwable: Throwable?, objs: Array<Any>?) {
        if (isTraceEnabled) {
            write(str, LogLevel.TRACE.getName(), throwable, objs)
        }
    }

    override fun isDebugEnabled(): Boolean = level.isEnabled(LogLevel.DEBUG)

    override fun debug(str: String?) {
        if (isDebugEnabled) {
            write(str, LogLevel.DEBUG.getName(), null, null)
        }
    }

    override fun debug(str: String?, objs: Array<Any>?) {
        if (isDebugEnabled) {
            write(str, LogLevel.DEBUG.getName(), null, objs)
        }
    }

    override fun debug(str: String?, throwable: Throwable?, objs: Array<Any>?) {
        if (isDebugEnabled) {
            write(str, LogLevel.DEBUG.getName(), throwable, objs)
        }
    }

    override fun isInfoEnabled(): Boolean = level.isEnabled(LogLevel.INFO)

    override fun info(str: String?) {
        if (isInfoEnabled) {
            write(str, LogLevel.INFO.getName(), null, null)
        }
    }

    override fun info(str: String?, objs: Array<Any>?) {
        if (isInfoEnabled) {
            write(str, LogLevel.INFO.getName(), null, objs)
        }
    }

    override fun info(str: String?, throwable: Throwable?, objs: Array<Any>?) {
        if (isInfoEnabled) {
            write(str, LogLevel.INFO.getName(), throwable, objs)
        }
    }

    override fun isWarnEnabled(): Boolean = level.isEnabled(LogLevel.WARN)

    override fun warn(str: String?) {
        if (isWarnEnabled) {
            write(str, LogLevel.WARN.getName(), null, null)
        }
    }

    override fun warn(str: String?, objs: Array<Any>?) {
        if (isWarnEnabled) {
            write(str, LogLevel.WARN.getName(), null, objs)
        }
    }

    override fun warn(str: String?, throwable: Throwable?, objs: Array<Any>?) {
        if (isWarnEnabled) {
            write(str, LogLevel.WARN.getName(), throwable, objs)
        }
    }

    override fun isErrorEnabled(): Boolean = level.isEnabled(LogLevel.ERROR)

    override fun error(str: String?) {
        if (isErrorEnabled) {
            write(str, LogLevel.ERROR.getName(), null, null)
        }
    }

    override fun error(str: String?, objs: Array<Any>?) {
        if (isErrorEnabled) {
            write(str, LogLevel.ERROR.getName(), null, objs)
        }
    }

    override fun error(str: String?, throwable: Throwable?, objs: Array<Any>?) {
        if (isErrorEnabled) {
            write(str, LogLevel.ERROR.getName(), throwable, objs)
        }
    }

    override fun close() = runBlocking {
        try {
            channel.trySend(stopLogMessage)
            consumerJob.cancel(CancellationException("Cancel file log exception."))
            consumerJob.join()
        } finally {
            output.close()
        }
    }

    private fun write(content: String?, level: String, throwable: Throwable?, objs: Array<Any>?) {
        val item = LogItem()
        item.level = level
        item.name = name
        item.content = content
        item.objs = objs
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
        val stackTraceElement = arr[4]
        return if (stackTraceElement != null) {
            if (stackTraceElement.className == "com.fireflysource.log.ClassNameLogWrap")
                arr[6]
            else stackTraceElement
        } else stackTraceElement
    }

    private fun write(logItem: LogItem) {
        channel.trySend(logItem)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileLog

        if (logName != other.logName) return false

        return true
    }

    override fun hashCode(): Int {
        return logName.hashCode()
    }

    override fun toString(): String {
        return "FileLog{" +
                "level=" + level +
                ", path='" + path + '\''.toString() +
                ", name='" + name + '\''.toString() +
                ", consoleOutput=" + consoleOutput +
                ", fileOutput=" + fileOutput +
                ", maxFileSize=" + maxFileSize +
                ", charset=" + charset +
                ", maxSplitTime=" + maxSplitTime.value +
                '}'.toString()
    }

}