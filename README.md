# Firefly Framework

Firefly framework is a high performance full-stack java web framework. It helps you create a java web application __*Easy*__ and __*Quickly*__. It provides MVC framework with HTTP Server and many other useful components for developing web applications. That means you can easy deploy your web without any other java web containers, in short , it's containerless. It taps into the fullest potential of hardware using __*SEDA*__ architecture, a highly customizable thread model.  

## Getting start

Running firefly is very easy, now you can download the dependency from Apache Central Repository, the pom is:

```xml
	<dependency>
      <groupId>com.fireflysource</groupId>
      <artifactId>firefly-common</artifactId>
      <version>3.0.4.5</version>
    </dependency>
    <dependency>
      <groupId>com.fireflysource</groupId>
      <artifactId>firefly-template</artifactId>
      <version>3.0.4.5</version>
    </dependency>
    <dependency>
      <groupId>com.fireflysource</groupId>
      <artifactId>firefly-nettool</artifactId>
      <version>3.0.4.5</version>
    </dependency>
    <dependency>
      <groupId>com.fireflysource</groupId>
      <artifactId>firefly</artifactId>
      <version>3.0.4.5</version>
    </dependency>
```

There are two ways to start a firefly application, building by maven, or just run it on eclipse simply.

### Building by maven
1. Clone firefly source code from Github.
2. Find the demo project 'firefly-demo', modify the log path in firefly-log.properties, you can find it in 'firefly-demo/src/main/resources', in this case, you __*Must*__ modify these two rows to your own location

```
firefly-system=INFO,/Users/qiupengtao/develop/logs
firefly-access=INFO,/Users/qiupengtao/develop/logs
```
When you have finished these operations above-mentioned, run maven command 'mvn test' and 'mvn compile', then run the class 'App' and visit the URL http://localhost:8080/index in your browser, you will see the 'Hello World'.  


### Running on eclipse

1. Clone firefly source code from Github.
2. Open Eclipse IDE and import the demo project - 'firefly-benchmark'
3. Modify the log path in firefly-log.properties, you can find it in 'firefly-benchmark/src', in this case, you __*Must*__ modify these two rows to your own location

```
firefly-system=INFO,/Users/qiupengtao/develop/logs
firefly-access=INFO,/Users/qiupengtao/develop/logs
```
When you have finished these operations above-mentioned, run the class 'Bootstrap' and visit the URL http://localhost:8080/ in your browser, you will see the 'Hello World'.  


Notice: you __*Must*__ use JDK in your IDE environment __*NOT*__ JRE, because the firefly depends on Java Compiler API that doesn't exist in JRE.    

More details you can find in [Wiki](https://github.com/hypercube1024/firefly/wiki/Guide) or [guide document](http://www.fireflysource.com/docs/firefly-guide.html)

##Contact information
E-mail: qptkk@163.com  
QQ Group: 126079579
