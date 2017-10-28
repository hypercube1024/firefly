---

category : docs
layout: document
title: Log configuration

---

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Overview](#overview)
- [Configuration](#configuration)
- [Log formatter](#log-formatter)
- [Lazy logger](#lazy-logger)

<!-- /TOC -->

# Overview
Firefly log implements slf4j APIs. The features:
* Asynchronous writing
* Timeout or max buffer size flush disk strategy
* Lazy logger
* MDC & custom formatter

# Configuration
Add maven dependency
```xml
<dependency>
    <groupId>com.fireflysource</groupId>
    <artifactId>firefly-slf4j</artifactId>
    <version>{{ site.data.global.releaseVersion }}</version>
</dependency>
```

Add `firefly-log.xml` to classpath
```xml
<?xml version="1.0" encoding="UTF-8"?>
<loggers xmlns="http://www.fireflysource.com/loggers"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.fireflysource.com/loggers http://www.fireflysource.com/loggers.xsd">
    <logger>
        <name>firefly-system</name>
        <level>INFO</level>
        <path>${log.path}</path>
        <formatter>com.firefly.example.common.ExampleLogFormatter</formatter>
    </logger>

    <logger>
        <name>firefly-monitor</name>
        <level>INFO</level>
        <path>${log.path}</path>
        <formatter>com.firefly.example.common.ExampleLogFormatter</formatter>
    </logger>

    <logger>
        <name>com.firefly.example.reactive</name>
        <level>INFO</level>
        <path>${log.path}</path>
        <!--<enable-console>true</enable-console>-->
        <max-file-size>300000</max-file-size>
        <charset>UTF-8</charset>
        <formatter>com.firefly.example.common.ExampleLogFormatter</formatter>
    </logger>

    <logger>
        <name>com.firefly.example.kotlin</name>
        <level>INFO</level>
        <path>${log.path}</path>
        <formatter>com.firefly.example.common.ExampleLogFormatter</formatter>
    </logger>
</loggers>
```
The `firefly-system` is the default logger. It records firefly framework and any other not specified log. The `firefly-monitor` records the firefly framework runtime performance metric.

The `<name>` node sets the logger name, it uses prefix to match logger instance. For example:
```java
private static final Logger logger = LoggerFactory.getLogger(Foo.class); //com.firefly.example.reactive.Foo

public void print() {
  logger.info("print foo")
}
```
The Foo class name is `com.firefly.example.reactive.Foo`, it matches the logger instance `com.firefly.example.reactive`. The logger will print records to `${log.path}/com.firefly.example.reactive.2017-10-04.txt`. The logger splits files daily. But you can also use `<max-file-size>` to set the max log file size. When the log file size exceeds the max size, Firefly logger will create a new file to save log records automatically.  

If `<enable-console>` node is true, the logger will print records to console.

# Log formatter
The `<formatter>` specifies a class that implements `LogFormatter`. For example:
```java
public class ExampleLogFormatter implements LogFormatter {

    @Override
    public String format(LogItem logItem) {
        String logStr = logItem.getLevel() + " " + SafeSimpleDateFormat.defaultDateFormat.format(logItem.getDate());

        if (!CollectionUtils.isEmpty(logItem.getMdcData())) {
            logStr += " " + logItem.getMdcData();
        }

        if (StringUtils.hasText(logItem.getClassName())) {
            String[] arr = $.string.split(logItem.getClassName(), '.');
            logStr += " " + arr[arr.length - 1];
        }

        if (StringUtils.hasText(logItem.getThreadName())) {
            logStr += " " + logItem.getThreadName();
        }

        if (logItem.getStackTraceElement() != null) {
            logStr += " " + logItem.getStackTraceElement();
        }

        logStr += " -> " + logItem.renderContentTemplate();
        return logStr;
    }
}
```
When the logger writes log records to a file or console, it will call the `LogFormatter.format` method. You can convert LogItem to a String using custom format.

# Lazy logger
When we want to print a large data object that helps us to debug programming in development stage, we need use the `logger.isDebugEnabled()` condition to avoid the resources are consumed excessively in the production environment. Just like:
```java
if (logger.isDebugEnabled()) {
    logger.getLogger().debug("debug dump data: ", dumpLargeData());
}
```

We set logger level is INFO in the production environment. The `dumpLargeData()` method will be not executed. But Firefly logger provides a simple API to do this. Just like:
```java
public class Slf4jImplDemo {

    private static final LazyLogger logger = LazyLogger.create();

    public static void main(String[] args) {
        // logger level is INFO
        logger.debug(() -> "debug dump data: " + dumpLargeData());
        if (logger.isDebugEnabled()) {
            logger.getLogger().debug("debug dump data: ", dumpLargeData());
        }
    }

    private static String dumpLargeData() {
        System.out.println("dump......");
        ThreadUtils.sleep(2000);
        return "large data";
    }
}
```
The lambda will be executed lazily. In this case, when the logger level is DEBUG, the lambda will be executed.
