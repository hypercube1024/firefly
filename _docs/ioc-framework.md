---

category : docs
layout: document
title: Inversion of control

---


# Basic concepts
IOC is also known as dependency injection (DI) which concerns itself with decoupling dependencies between different layers or components through shared abstractions.

In object-oriented programming, DI is a combination of factory pattern, service locator pattern, strategy design pattern and singleton pattern.

# Container overview
The interface `com.firefly.core.ApplicationContext` represents IOC container and is responsible for managing the lifecycle of components. The component configuration is represented in XML or Java Annotation. It allows you to express the objects that compose your application and the rich interdependencies between such objects. The following example demonstrates its basic features.  

Create `hello.xml` to the classpath.
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

Create interface `com.firefly.example.ioc.HelloService` and implements it
```java
public interface HelloService {
    void print();
}

public class HelloServiceImpl implements HelloService {

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
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

# Dependency injection
Dependency injection (DI) is a process whereby objects define their dependencies, that is, the other objects they work with, only through constructor arguments or properties that are set on the object instance after it is constructed. The container then injects those dependencies when it creates the bean. DI principle is more effective to decouple components and their dependencies. Your components become easier to test when the dependencies are on interfaces or abstract base classes, which allow for stub or mock implementations to be used in unit tests. As such, it does not affect the other related components when you replace the different implementation of components.

Configure component using Java Annotation and inject `HelloService` to `FooService`
```java
public interface HelloService {
    void print();
}

@Component
public class FooServiceImpl implements FooService {

    @Inject
    private HelloService helloService;

    @Override
    public void say(String message) {
        System.out.println(message);
        helloService.print();
    }
}
```

Add `component-scan` in `hello.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.fireflysource.com/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.fireflysource.com/beans http://www.fireflysource.com/beans.xsd">

    <component-scan base-package="com.firefly.example.ioc"/>

    <bean id="helloService" class="com.firefly.example.ioc.HelloServiceImpl">
        <property name="message" value="Hello IOC"/>
    </bean>

</beans>
```

Run it.
```java
public class IOCMain {
    public static final ApplicationContext ctx = $.createApplicationContext("hello-ioc.xml");

    public static void main(String[] args) {
        ctx.getBean(FooService.class).say("foo");
    }
}
```

The console shows
```
foo
Hello IOC
```
