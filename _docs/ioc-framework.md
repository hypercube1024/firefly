---

category : docs
layout: document
title: IOC framework

---


# Basic concepts
IOC is also known as dependency injection (DI) which concerns itself with decoupling dependencies between different layers or components through shared abstractions.

In object-oriented programming, DI is a combination of factory pattern, service locator pattern, strategy design pattern and singleton pattern.

# Container overview
The interface `com.firefly.core.ApplicationContext` represents IOC container and is responsible for managing the lifecycle of components. The following example demonstrates its basic features.  

Add `hello.xml` to the classpath.
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.fireflysource.com/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.fireflysource.com/beans http://www.fireflysource.com/beans.xsd">

    <bean id="helloService" class="com.firefly.example.ioc.HelloService">
        <property name="message" value="Hello IOC"/>
    </bean>

</beans>
```

Create class `com.firefly.example.ioc.HelloService`
```java
public class HelloService {

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void print() {
        System.out.println(message);
    }
}
```

Instantiate `ApplicationContext` using factory method
```java
public class IOCMain {
    public static final ApplicationContext ctx = $.createApplicationContext("hello-ioc.xml");

    public static void main(String[] args) {
        ctx.getBean(HelloService.class).print();
    }
}
```

Run it. The console shows
```
Hello IOC
```
