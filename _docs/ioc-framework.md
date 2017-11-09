---

category : docs
layout: document
title: Inversion of control

---
<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Basic concepts](#basic-concepts)
- [Container overview](#container-overview)
- [Dependency injection](#dependency-injection)
	- [Java Annotation based configuration metadata](#java-annotation-based-configuration-metadata)
	- [XML based configuration metadata](#xml-based-configuration-metadata)
- [Lifecycle callbacks](#lifecycle-callbacks)
- [Proxy chain](#proxy-chain)

<!-- /TOC -->

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

## Java Annotation based configuration metadata
Configure component using Java Annotation and inject `HelloService` to `FooService`
```java
public interface HelloService {
    void print();
}

@Component("fooService")
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

## XML based configuration metadata
Create interface `com.firefly.example.ioc.BarService` and implements it
```java
public interface BarService {

    List<String> getFoods();

    Map<String, Double> getFoodPrices();

    String getBarName();

    FooService getFooService();

}

public class BarServiceImpl implements BarService {

    private FooService fooService;
    private List<String> foods;
    private Map<String, Double> foodPrices;
    private String barName;

    @Override
    public List<String> getFoods() {
        return foods;
    }

    public void setFoods(List<String> foods) {
        this.foods = foods;
    }

    @Override
    public Map<String, Double> getFoodPrices() {
        return foodPrices;
    }

    public void setFoodPrices(Map<String, Double> foodPrices) {
        this.foodPrices = foodPrices;
    }

    public void setBarName(String barName) {
        this.barName = barName;
    }

    @Override
    public String getBarName() {
        return barName;
    }

    @Override
    public FooService getFooService() {
        return fooService;
    }

    public void setFooService(FooService fooService) {
        this.fooService = fooService;
    }

}
```

Add bean `barService` in `hello.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.fireflysource.com/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.fireflysource.com/beans http://www.fireflysource.com/beans.xsd">

    <component-scan base-package="com.firefly.example.ioc"/>

    <bean id="helloService" class="com.firefly.example.ioc.HelloServiceImpl">
        <property name="message" value="Hello IOC"/>
    </bean>

    <bean id="barService" class="com.firefly.example.ioc.BarServiceImpl">
        <property name="fooService" ref="fooService" />
        <property name="foods" >
            <list>
                <value>fish</value>
                <value>Cola</value>
                <value>whisky</value>
            </list>
        </property>
        <property name="foodPrices">
            <map>
                <entry key="fish" value="15.00"/>
                <entry key="Cola" value="2.5" />
                <entry key="whisky" value="20.99" />
            </map>
        </property>
        <property name="barName" value="BarRR"/>
    </bean>

</beans>
```
XML based injection supports the Map, List, String, reference type, primitive types and their wrap types.  

Run it.
```java
public class IOCMain {
    public static final ApplicationContext ctx = $.createApplicationContext("hello-ioc.xml");

    public static void main(String[] args) {
        BarService barService = ctx.getBean(BarService.class);
        System.out.println(barService.getBarName());
        System.out.println(barService.getFoodPrices());
        System.out.println(barService.getFoods());
        barService.getFooService().say("It's OK");
    }
}
```
The console shows
```
BarRR
{Cola=2.5, whisky=20.99, fish=15.0}
[fish, Cola, whisky]
It's OK
Hello IOC
```

# Lifecycle callbacks
To interact with the container’s management of the bean lifecycle, you can use `@InitialMethod` and `@DestroyedMethod` to specify lifecycle callbacks. For example:
```java
@Component
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

    @InitialMethod
    public void init() {
        System.out.println("init HelloService");
    }

    @DestroyedMethod
    public void destroy() {
        System.out.println("destroy HelloService");
    }
}
```
When the components assemble completely the container will call the methods which have annotation `@InitialMethod`. If the process is killed or the stop method of ApplicationContext is invoked the container will call the methods which have annotation `@DestroyedMethod`.

Run it
```java
public class IOCMain {
    public static final ApplicationContext ctx = $.createApplicationContext("hello-ioc.xml");

    public static void main(String[] args) {
        ctx.getBean(HelloService.class).print();
    }
}
```

The console shows
```
init HelloService
Hello IOC
```

When you kill the process, the console will show
```
destroy HelloService
```

Also, you can configure lifecycle callbacks using XML. Add `init()` and `destroy()` methods to the `BarServiceImpl`
```java
public void init() {
    System.out.println("init BarService");
}

public void destroy() {
    System.out.println("destroy BarService");
}
```

Add configurations in `hello.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.fireflysource.com/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.fireflysource.com/beans http://www.fireflysource.com/beans.xsd">

    <component-scan base-package="com.firefly.example.ioc"/>

    <bean id="helloService" class="com.firefly.example.ioc.HelloServiceImpl">
        <property name="message" value="Hello IOC"/>
    </bean>

    <bean id="barService" class="com.firefly.example.ioc.BarServiceImpl" init-method="init" destroy-method="destroy">
        <property name="fooService" ref="fooService"/>
        <property name="foods">
            <list>
                <value>fish</value>
                <value>Cola</value>
                <value>whisky</value>
            </list>
        </property>
        <property name="foodPrices">
            <map>
                <entry key="fish" value="15.00"/>
                <entry key="Cola" value="2.5"/>
                <entry key="whisky" value="20.99"/>
            </map>
        </property>
        <property name="barName" value="BarRR"/>
    </bean>

</beans>
```

# Proxy chain
The proxy design pattern is one of the twenty-three well-known GoF design patterns that describe how to solve reusable design problems to design flexible object-oriented software, that is, objects that are easier to change, test, and reuse.  
What problems can the Proxy design pattern solve?  
* The access to an object should be controlled.
* Provide additional functionality when accessing an object.

Now we create a proxy chain which contains `LogProxy` and `AuthenticationProxy`.
```java
@Component("logProxy")
public class LogProxy implements ClassProxy {

    @Override
    public Object intercept(MethodProxy handler, Object originalInstance, Object[] args) {
        System.out.println("log entry " + handler.method().getName());
        Object ret = handler.invoke(originalInstance, args);
        System.out.println("log exit " + handler.method().getName());
        return ret;
    }
}

@Component("authenticationProxy")
public class AuthenticationProxy implements ClassProxy {

    @Override
    public Object intercept(MethodProxy handler, Object originalInstance, Object[] args) {
        System.out.println("authentication start " + handler.method().getName());
        Object ret = handler.invoke(originalInstance, args);
        System.out.println("authentication exit " + handler.method().getName());
        return ret;
    }
}
```
In this case, it just prints some texts to show how does it work. The `MethodProxy` instance is a reference of original method.

Add a new annotation `ProxyChain` that sets proxy order of the chain.
```java
@Proxy(proxyClass = LogProxy.class)
@Proxy(proxyClass = AuthenticationProxy.class)
@Target( { ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProxyChain {
}
```
The proxy order is inverted order of codes. In this case, the execution order is `AuthenticationProxy -> LogProxy -> OriginalService`.

Add proxy chain for `HelloService`
```java
@ProxyChain
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

    @InitialMethod
    public void init() {
        System.out.println("init HelloService");
    }

    @DestroyedMethod
    public void destroy() {
        System.out.println("destroy HelloService");
    }
}
```

Run it
```java
public class IOCMain {
    public static final ApplicationContext ctx = $.createApplicationContext("hello-ioc.xml");

    public static void main(String[] args) {
        ctx.getBean(HelloService.class).print();
    }
}
```

The console shows
```
authentication start print
log entry print
Hello IOC
log exit print
authentication exit print
```
