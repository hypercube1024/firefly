# Firefly Framework

Firefly framework is a high performance full-stack java web framework. It helps you create a java web application __*Easy*__ and __*Quickly*__. It provides MVC framework with HTTP Server and many other useful components for developing web applications. That means you can easy deploy your web without any other java web containers, in short , it's containerless. It taps into the fullest potential of hardware using __*SEDA*__ architecture, a highly customizable thread model.  

## Getting Start

1. Clone firefly source from Github.
2. Open Eclipse IDE and import the demo project - 'firefly-benchmark'
3. Modify the log path in firefly-log.properties, you can find it in 'firefly-benchmark/src', in this case, you __*Must*__ modify these two rows to your own location

```
firefly-system=INFO,/Users/qiupengtao/develop/logs
firefly-access=INFO,/Users/qiupengtao/develop/logs
```
When you have finished these operations above-mentioned, run the class 'Bootstrap' and visit the URL http://localhost:8080/ in your browser, you will see the 'Hello World'.  

Notice: you __*Must*__ use JDK in your IDE environment __*NOT*__ JRE, because the firefly need invoke Java Compiler API that isn't in JRE.