---

category : docs
layout: post
title: Firefly Guide

---

**Table of Contents**

* [Firefly Framework](#firefly-framework)
	* [Getting start](#getting-start)
		* [Building by maven](#building-by-maven)
		* [Running on eclipse](#running-on-eclipse)
	* [Component context](#component-context)
		* [Controller](#controller)
			* [URL binding](#url-binding)
			* [URL parameters binding](#url-parameter-binding)
			* [View render](#view-render)
			* [Custom view](#custom-view)
			* [File uploading](#file-uploading)
			* [Asynchronous context](#asynchronous-context)
		* [Interceptor](#interceptor)
		* [Component](#component)
			* [Injecting a component based on XML](#injecting-a-component-based-on-xml)
			* [Injecting a component based on annotation](#injecting-a-component-based-on-annotation)
	* [HTTP server](#http-server)
	* [Template](#template)
		* [Object navigation](#object-navigation)
		* [If else and elseif](#If-else-and-elseif)
		* [Eval](#eval)
		* [For](#for)
		* [Switch case](#switch-case)
		* [Include](#include)
		* [Set](#Set)
		* [Custom function](#custom-function)
	* [Performance](#performance)
	* [Contact information](#contact-information)

# Firefly Framework

Firefly framework is a high performance full-stack java web framework. It helps you create a java web application __*Easy*__ and __*Quickly*__. It provides MVC framework with HTTP Server and many other useful components for developing web applications. That means you can easy deploy your web without any other java web containers, in short , it's containerless. It taps into the fullest potential of hardware using __*SEDA*__ architecture, a highly customizable thread model.  

## Getting start

Running firefly is very easy, now you can download the dependency from Apache Central Repository, the pom is:

{% highlight xml %}
<dependency>
  <groupId>com.fireflysource</groupId>
  <artifactId>firefly-common</artifactId>
  <version>3.0.3</version>
</dependency>
<dependency>
  <groupId>com.fireflysource</groupId>
  <artifactId>firefly-template</artifactId>
  <version>3.0.3</version>
</dependency>
<dependency>
  <groupId>com.fireflysource</groupId>
  <artifactId>firefly-nettool</artifactId>
  <version>3.0.3</version>
</dependency>
<dependency>
  <groupId>com.fireflysource</groupId>
  <artifactId>firefly</artifactId>
  <version>3.0.3</version>
</dependency>
{% endhighlight %}

There are two ways to start a firefly application, building by maven, or just run it on eclipse simply.

### Building by maven
1. Clone firefly source code from Github.
2. Find the demo project 'firefly-demo', modify the log path in firefly-log.properties, you can find it in 'firefly-demo/src/main/resources', in this case, you __*Must*__ modify these three rows to your own location

{% highlight properties %}
firefly-system=INFO,/Users/qiupengtao/develop/logs
firefly-access=INFO,/Users/qiupengtao/develop/logs
demo-log=INFO,/Users/qiupengtao/develop/logs
{% endhighlight %}

When you have finished these operations above-mentioned, run maven command 'mvn test' and 'mvn compile', then run the class 'App' and visit the URL http://localhost:8080/index in your browser, you will see the 'Hello World'.  


### Running on eclipse

1. Clone firefly source code from Github.
2. Open Eclipse IDE and import the demo project - 'firefly-benchmark'
3. Modify the log path in firefly-log.properties, you can find it in 'firefly-benchmark/src', in this case, you __*Must*__ modify these two rows to your own location

{% highlight properties %}
firefly-system=INFO,/Users/qiupengtao/develop/logs
firefly-access=INFO,/Users/qiupengtao/develop/logs
{% endhighlight %}

When you have finished these operations above-mentioned, run the class 'Bootstrap' and visit the URL http://localhost:8080/ in your browser, you will see the 'Hello World'.  


Notice: you __*Must*__ use JDK in your IDE environment __*NOT*__ JRE, because the firefly depends on Java Compiler API that isn't in JRE.

## Component context

Firefly has three kinds of components - Interceptor, Controller and Component which managed by application context. These components are singleton. You can declare them based on annotation or XML.

### Controller

#### URL binding

Controller is used to process HTTP request. Every controller can bind one or more HTTP URLs. The first, we can declare a controller like this: (all examples can be found in project 'firefly-demo')

{% highlight java %}
@Controller
public class IndexController {

	@RequestMapping(value = "/index")
	public View index(HttpServletRequest request, HttpServletResponse response) {
		request.setAttribute("info", new String[]{"hello firefly", "test"});
		return new TemplateView("/index.html");
	}
	
	@RequestMapping(value = "/document/?/?")
	public View document(HttpServletRequest request, 
					@PathVariable String[] args) {
		request.setAttribute("info", args);
		return new TemplateView("/index.html");
	}
}
{% endhighlight %}

1. The method index binds to the URI "/index", when you visit URL "http://host:port/index", method index will be called.
2. The method document binds to a URI pattern "/document/?/?". That the parameter is annotated by @PathVariable will receive the matched part of URI. For example, when you visit "http://host:port/document/22/xxx", the args value is ["22", "xxx"].

#### URL parameter binding
Firefly supports the HTTP parameters bind to a java bean automatically. It's a very useful for posting a form that contains many fields.
{% highlight java %}
public class GoodsInformation {

	private String title;
	private String introduction;
	private double price;
	private int stockNumber;
	private Integer status;
	...
}
{% endhighlight %}
{% highlight java %}
@Controller
public class GoodsController {
	
	@RequestMapping(value = "/goods/edit")
	public View editGoods(HttpServletRequest request) {
		return new TemplateView("/goods/edit.html");
	}

	@RequestMapping(value = "/goods/post", method=HttpMethod.POST)
	public View postGoods(@HttpParam GoodsInformation goodsInformation) {
		return new TextView("ok: " + goodsInformation);
	}
}
{% endhighlight %}

As you see, the method parameter of controller supports four types: HttpServletRequest, HttpServletResponse, @PathVariable annotated String[] and @HttpParam annotated java bean. You can use these four types arbitrarily, don't care the parameter order and number. Even no one parameter is allowed.  

In above case, when you submit a HTTP post request, firefly will bind all the same names between HTTP parameters and GoodsInformation fields.

#### View render
The controller method returns a View interface (com.firefly.mvc.web.View) that is used to do HTTP response. If you return null or the controller method's return value is void, you need do response using HttpServletResponse. Firefly has five kinds of implementation classes of View interface, TemplateView, TextView, JsonView, RedirectView, StaticFileView. You can also develop yourself View interface implementation.

Example JsonView:
{% highlight java %}
@RequestMapping(value = "/goods/get")
public View getJson(HttpServletRequest request) {
	String title = request.getParameter("title");
	log.info("get json title -> {}", title);
	if(title == null) {
		Map<String, String> ret = new HashMap<String, String>();
		ret.put("result", "error");
		return new JsonView(ret);
	}
	return new JsonView(map.get(title));
}
{% endhighlight %}

JsonView's constructor receives a Object and serializes it to json format automatically.  


Example StaticFileView:
{% highlight java %}
@RequestMapping(value = "/goods/introductions")
public View getFile() {
	return new StaticFileView("/file-test.html");
}
{% endhighlight %}
StaticFileView outputs a static file in server home.  

The RedirectView do a 302 response. The TextView is used to output plain texts and the TemplateView is used to render Firefly template, you can find examples in previous sections.

#### Custom view
The View.render method is a callback method when the Firefly HTTP Server do a response. So you just implement that method, if you want to develop a custom view. You can develop some custom views for different kinds of pages. It helps you greatly reduce duplicated codes.

{% highlight java %}
@RequestMapping(value = "/goods/information")
public View getGoodsInformation() {
	return new View(){

	@Override
	public void render(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		GoodsInformation information = new GoodsInformation();
		information.setTitle("Firefly documents");
		information.setPrice(999.9);
		information.setStockNumber(1);
		
		Writer writer = response.getWriter();
		try {
			writer.write(information.toString());
		} finally {
			writer.close();
		}
		
	}};
}
{% endhighlight %}

As you see, Firefly receives whatever implementations of View interface.

#### File uploading
Firefly uploads file as same as the other Java EE Server which supports Servlet 3.0. It uses HttpServletRequest API simply.

{% highlight java %}
@RequestMapping(value = "/goods/upload", method=HttpMethod.POST)
public View uploadGoodsInformation(HttpServletRequest request) {
	try {
		Part part = request.getPart("grid");
		part.write("/Users/qiupengtao/goods/" + part.getName());
	} catch (Throwable e) {
		log.error("uploading failure!", e);
	}
	return new TextView("uploading success!");	
}
{% endhighlight %}

Firefly HTTP Server implements Part interface completely, the details you can see <a href="http://docs.oracle.com/javaee/7/api/javax/servlet/http/Part.html" target="_blank">Part API document</a>.

#### Asynchronous context
Firefly supports the server end asynchronous handling. If you don't want to do response in current thread, you need start an asynchronous context, and then, do response in another thread. It's appropriate for invoking some remote interfaces asynchronously, such as you start a asynchronous HTTP request in a controller, you can do response in the callback method, not blocking current thread.

{% highlight java %}
@RequestMapping(value = "/goods/asyncNotice")
public View getInformationAsynchronously(HttpServletRequest request, HttpServletResponse response) {
	PrintWriter writer = null;
	try {
		writer = response.getWriter();
	} catch (IOException e) {
		log.error("get writer failure", e);
	}
	writer.println("start asynchronous notice");
	writer.flush();
	request.startAsync();
	request.getAsyncContext().addListener(new TestAsyncListener());
	request.getAsyncContext().start(new TestRunnable(request.getAsyncContext(), "asynchronous notice of goods information"));
	return null;
}

private class TestRunnable implements Runnable {
	private AsyncContext context;
	private String name;

	public TestRunnable(AsyncContext context, String name) {
		this.context = context;
		this.name = name;
	}
	
	@Override
	public void run() {
		
		ServletResponse res = context.getResponse();
		PrintWriter writer = null;
		try {
			writer = res.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		writer.println(name + " ......");
		writer.flush();
		try {
			Thread.sleep(5000);
			
			writer.println(name + ": received a async message");
			writer.flush();
			context.complete();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
}

...
{% endhighlight %}

More details of above sample you can find in GoodsController.java of 'firefly-demo'. When you run 'telnet localhost 8080' and send a HTTP request to '/goods/asyncNotice', the screen will print 'start asynchronous notice', and after 5 seconds, the screen will print the rest of texts.

{% highlight bash %}
~$ telnet localhost 8080
Trying 127.0.0.1...
Connected to localhost.
Escape character is '^]'.
GET /goods/asyncNotice HTTP/1.1
Host: 127.0.0.1

HTTP/1.1 200 OK
Transfer-Encoding: chunked
Server: Firefly/3.0
Connection: keep-alive
Date: Sat, 8 Nov 2014 12:04:27 GMT

1a
start asynchronous notice

30
asynchronous notice of goods information ......

43
asynchronous notice of goods information: received a async message

17
async context complete

0

Connection closed by foreign host.
{% endhighlight %}

If you invoke 'writer.flush()' method, Firefly HTTP server will do a response using HTTP chunked encoding forcedly. 

### Interceptor
The interceptor is used to intercept HTTP requests. You can do some operations before or after the controller is invoked.

{% highlight java %}
@Interceptor(uri = "/goods/*", order=0)
public class GoodsInformationInterceptorStep1 {

	private static Log log = LogFactory.getInstance().getLog("demo-log");
	
	public View dispose(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
		log.info("befor goods information step 1");
		View view = chain.doNext(request, response, chain);
		log.info("after goods information step 1");
		return view;
	}
}

@Interceptor(uri = "/goods/*", order=1)
public class GoodsInformationInterceptorStep2 {

	private static Log log = LogFactory.getInstance().getLog("demo-log");
	
	public View dispose(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
		log.info("befor goods information step 2");
		View view = chain.doNext(request, response, chain);
		log.info("after goods information step 2");
		return view;
	}
}
{% endhighlight %}

The interceptor uses a URL pattern to match the HTTP request. And the order specifies the invoking order. In the above sample, it prints:

{% highlight text %}
INFO 2014-11-08 20:52:15	befor goods information step 1
INFO 2014-11-08 20:52:15	befor goods information step 2
INFO 2014-11-08 20:52:15	goods information controller
INFO 2014-11-08 20:52:15	after goods information step 2
INFO 2014-11-08 20:52:15	after goods information step 1
{% endhighlight %}

### Component
Usually, the component is used to process business logic. You can inject a component to another component, controller or interceptor.
The Firefly framework need a root configuration file - firefly.xml, also you can specify another file name using Config.setConfigFileName.
In the root configuration file, you can import other configuration files. Usually, we configure basic settings in the root file and do some additional settings in other files.

{% highlight xml %}
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.fireflysource.com/beans"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://www.fireflysource.com/beans http://www.fireflysource.com/beans.xsd">
	
	<import resource="fruits.xml"/>
	
	<component-scan base-package="com.fireflysource"/>
	<mvc view-path="/template" view-encoding="UTF-8"/>
	
</beans>
{% endhighlight %}

1. `<import/>` node sets the path of a configuration file.
2. `<component-scan/>` node sets the scan path of components.
3. `<mvc/>` node sets the path of template files and the character encoding.

The configuration details can be found in the <a href="http://www.fireflysource.com/beans.xsd" target="_blank">schema file</a>.




#### Injecting a component based on XML
{% highlight java %}
public class Fruit {

	private String title;
	private String season;
	private String category;
	private double price;
	
	// ... setter and getter method
}
{% endhighlight %}


{% highlight java %}
public class FruitServiceImpl implements FruitService {
	
	private Map<String, Fruit> map;
	
	public FruitServiceImpl(){}
	
	public FruitServiceImpl(Map<String, Fruit> map) {
		this.map = map;
	}

	@Override
	public Fruit getFruitByTitle(String title) {
		return map.get(title);
	}

}
{% endhighlight %}

{% highlight xml %}
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.fireflysource.com/beans"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://www.fireflysource.com/beans http://www.fireflysource.com/beans.xsd">

	<bean id="foodRepository" 
	class="com.fireflysource.demo.service.impl.FruitServiceImpl">
		<constructor>
			<argument type="java.util.Map">
				<map>
					<entry>
						<key>apple</key>
						<value><ref bean="apple"/></value>
					</entry>
					<entry>
						<key>orange</key>
						<value><ref bean="orange"/></value>
					</entry>
					<entry>
						<key>kiwi</key>
						<value><ref bean="kiwi"/></value>
					</entry>
				</map>
			</argument>
		</constructor>
	</bean>
	
	<bean id="apple" class="com.fireflysource.demo.model.Fruit">
		<property name="title" value="apple" />
		<property name="season" value="full year" />
		<property name="category" value="mobile phone" />
		<property name="price" value="4999.0" />
	</bean>
	
	<bean id="orange" class="com.fireflysource.demo.model.Fruit">
		<property name="title" value="orange" />
		<property name="season" value="the third season" />
		<property name="category" value="citrus" />
		<property name="price" value="1.1" />
	</bean>
	
	<bean id="kiwi" class="com.fireflysource.demo.model.Fruit">
		<property name="title" value="kiwi" />
		<property name="season" value="the third season" />
		<property name="category" value="Actinidia chinensis" />
		<property name="price" value="20.33" />
	</bean>
	
</beans>
{% endhighlight %}

Firefly supports the constructor and setter method injecting in XML files. You can inject an object which is List, Map, Array, Primitive types, String or any types to another object. The configuration details can be found in the <a href="http://www.fireflysource.com/beans.xsd" target="_blank">schema file</a>.

#### Injecting a component based on annotation
{% highlight java %}
@Controller
public class FruitController {
	
	@Inject
	private FruitService fruitService;
	
	@RequestMapping(value = "/fruit/?")
	public View getFruit(@PathVariable String[] args) {
		String title = args[0];
		return new JsonView(fruitService.getFruitByTitle(title));
	}
}
{% endhighlight %}

You can use the `@Inject` to inject a component simply. The `@Inject` targets contain method, constructor and field. For example:

{% highlight java %}
@Component
public class FruitServiceWrap {
	private FruitService fruitService;
	
	@Inject
	public void setFruitService(FruitService fruitService) {
		this.fruitService = fruitService;
	}
	
}
{% endhighlight %}

In this case, when you visit http://localhost:8080/fruit/apple, the browser will show:
{% highlight json %}
{
	category: "mobile phone",
	price: 4999,
	season: "full year",
	title: "apple"
}
{% endhighlight %}

This is a classic web application architecture. The controller handles HTTP request, and the business logic is in the service.

## HTTP server
You don't need any other web server for startup. It just runs `ServerBootstrap.start(config)` in main method.

{% highlight java %}
public class App {
	
	public static void main(String[] args) throws Throwable {
		String projectHome = new File(App.class.getResource("/").toURI()).getParentFile().getParent();
		String serverHome = new File(projectHome, "/page").getAbsolutePath();
		
		System.out.println(projectHome);
		System.out.println(serverHome);
		Config config = new Config();
		config.setHost("localhost");
		config.setPort(8080);
		config.setServerHome(serverHome);
		ServerBootstrap.start(config);
	}
}
{% endhighlight %}

The HTTP configuration details:

<table class="table table-hover">
<tr>
	<th>Arguments</th>
	<th>Descriptions</th>
</tr>
<tr>
	<td>configFileName</td>
	<td>The root configuration file name, the default is firefly.xml.</td>
</tr>
<tr>
	<td>encoding</td>
	<td>The application character encoding, the default is UTF-8.</td>
</tr>
<tr>
	<td>errorPage</td>
	<td>The custom error page, you can bind the HTTP error codes to your pages, for example: {404, "/error/e404.html"}.</td>
</tr>
<tr>
	<td>maxRequestLineLength</td>
	<td>The max length of the HTTP request line, the default is 8kb.</td>
</tr>
<tr>
	<td>maxRequestHeadLength</td>
	<td>The max length of the HTTP head, the default is 16kb.</td>
</tr>
<tr>
	<td>maxRangeNum</td>
	<td>The max number of HTTP range sections, the default is 8.</td>
</tr>
<tr>
	<td>writeBufferSize</td>
	<td>The response buffer size, the default is 8kb.</td>
</tr>
<tr>
	<td>maxConnections</td>
	<td>The max TCP connection number, the default is 2000.</td>
</tr>
<tr>
	<td>maxConnectionTimeout</td>
	<td>The max TCP connection idle time, the default is 10s.</td>
</tr>
<tr>
	<td>corePoolSize</td>
	<td>The number of threads to keep in the pool, even if they are idle, the default is CPU number * 2.</td>
</tr>
<tr>
	<td>maximumPoolSize</td>
	<td>The maximum number of threads to allow in the pool, the default is 128.</td>
</tr>
<tr>
	<td>poolQueueSize</td>
	<td>The queue to use for holding tasks before they are executed.  This queue will hold only the Runnable tasks submitted by the  execute method. The default queue size is 50000.</td>
</tr>
<tr>
	<td>poolKeepAliveTime</td>
	<td>When the number of threads is greater than the core, this is the maximum time that excess idle threads will wait for new tasks before terminating. The default is 30s.</td>
</tr>
<tr>
	<td>poolWaitTimeout</td>
	<td>The maximum time of waiting Runnable tasks in the queue, the default is 3s.</td>
</tr>
<tr>
	<td>maxUploadLength</td>
	<td>The maximum size of posting a file, the default is 50mb.</td>
</tr>
<tr>
	<td>httpBodyThreshold</td>
	<td>The maximum size of HTTP body in the memory, if the HTTP body size exceeds this value, it will be saved in a temporary file on the hard disk, the default is 4mb.</td>
</tr>
<tr>
	<td>keepAlive</td>
	<td>Enable HTTP connections keep alive, the default is true.</td>
</tr>
<tr>
	<td>enableThreadPool</td>
	<td>Enable business logic thread pool, if you set this parameter is false, it will improve the throughput of the server, but the request will be executed serially. The default is false.</td>
</tr>
<tr>
	<td>asynchronousContextTimeout</td>
	<td>The maximum time of running a asynchronous task. The default is 6s.</td>
</tr>
<tr>
	<td>serverHome</td>
	<td>The root directory of HTTP server. The URI '/' will visit this directory.</td>
</tr>
<tr>
	<td>host</td>
	<td>The IP or domain name of this server.</td>
</tr>
<tr>
	<td>port</td>
	<td>The tcp port of this server.</td>
</tr>
<tr>
	<td>secure</td>
	<td>Enable the HTTPS connections, the default is false.</td>
</tr>
<tr>
	<td>credentialPath</td>
	<td>The credential path, if you don't set this value, it will use the firefly default SSL credential.</td>
</tr>
<tr>
	<td>keystorePassword</td>
	<td>The password used to check the integrity of the keystore, the password used to unlock the keystore.</td>
</tr>
<tr>
	<td>keyPassword</td>
	<td>The password for recovering keys in the keystore.</td>
</tr>
<tr>
	<td>sessionIdName</td>
	<td>The HTTP session id name, the default is jsessionid.</td>
</tr>
<tr>
	<td>maxSessionInactiveInterval</td>
	<td>The maximum idle time of the HTTP session, the default is 10 minutes.</td>
</tr>
</table>

## Template
Firefly template is used to rendering HTML pages. You can write it in the HTML comments. It is compatible for any HTML editor.
When the Firefly server starts up, the template will be compiled to java byte code, so it has higner performance than the other template languages which interpreted execution.

### Object navigation

{% highlight html %}
<!DOCTYPE html>
<html>
<body>
<div>
<div>${len(users)}</div>
<div>${u.name}</div>
<div>${array[3]}</div>
<div>${testMap['ret']['title']}</div>
</div>
</body>
</html>
{% endhighlight %}

 * `${u.name}` It invokes user.getName() and prints that value.
 * `${len(users)}` It invokes a custom function len() to get List.size and prints it. 
 * `${array[3]}` It prints an array or list element that index is 3.
 * `${testMap['ret']['title']}` It prints a map, testMap.get("ret").get("title").
 
### If else and elseif

{% highlight html %}
<!DOCTYPE html>
<html>
<body>
        <div>
        <!-- #if ${login} -->
        Welcome ${user.name}
        <!-- #else -->
        can not access
        <!-- #end -->
        </div>

        <!-- #if ${user.age} > 15 + 3 -->
        <div>age more than 18</div>
        <!-- #end -->

        <!-- #if 15*2<=${user.age}  -->
        <div>age less then 30</div>
        <!-- #end -->

        <div>${testFunction(3, "hello", user.age)}</div>
        <div>${testFunction2()}</div>

        <div>
        <!-- #if "Pengtao Qiu" == ${user.name} -->
                master come
        <!-- #elseif ${user.name} == "Bob" -->
                joke come
        <!-- #elseif ${user.name} == "Jim" -->
                Jim come
        <!-- #else -->
                small potato come
        <!-- #end -->
        </div>
</body>
</html>
{% endhighlight %}

### Eval

{% highlight html %}
<div><!-- #eval 3.0 + 3 * 5.0 / 2.0 --></div>
{% endhighlight %}

### For

{% highlight html %}
<!DOCTYPE html>
<html>
<body>
<div>
<!-- #for i : ${intArr} -->
${i} &nbsp;&nbsp;
<!-- #end -->
</div>

<div>
<div>${len(users)}</div>
<table style="table-layout: fixed;">
        <thead style="text-align: center;">
        <tr><th>name</th><th>age</th></tr>
        </thead>
        <tbody>
        <!-- #for u : ${users} -->
        <tr><td>${u.name}|||${len(u.name)}</td><td>${u.age}</td></tr>
        <!-- #end -->
        </tbody>
</table>
</div>
</body>
</html>
{% endhighlight %}

### Switch case

{% highlight html %}
<!DOCTYPE html>
<html>
<body>
<div>
<!-- #switch ${stage} -->
<!-- #case 1 -->
        stage1
<!-- #case 2 -->
        stage2
<!-- #default -->
        stage-default
<!-- #end -->
</div>

</body>
</html>
{% endhighlight %}


### Include

{% highlight html %}
<!DOCTYPE html>
<html>
<head>
<!-- #include /common/head.html?title=TestInclude -->
</head>
<body>
<!-- #include /common/top.html -->
<!-- #include /common/top.html?title=thisIsOnePage -->
</body>
</html>
{% endhighlight %}

### Set

{% highlight html %}
<!DOCTYPE html>
<html>
<body>
<!-- #set msg=welcom&price=4.5&testName=${name} -->
<div>
${msg}&nbsp;&nbsp;${testName}
</div>
<div>
apple's price: ${price}
</div>

</body>
</html>
{% endhighlight %}

### Custom function

You can write a function in java and invoke it in template. For example:

{% highlight java %}
Function function = new Function(){
        @Override
        public void render(Model model, OutputStream out, Object... obj) {
                Integer i = (Integer)obj[0];
                String str = (String)obj[1];
                String o = String.valueOf(obj[2]);

                try {
                        out.write((i + "|" + str + "|" + o).getBytes("UTF-8"));
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }
};

FunctionRegistry.add("testFunction", function);
{% endhighlight %}
And then, you invoke it in template like this : `${testFunction(3, "hello", user.age)}`.  

Firefly has some default functions, you can find them in package com.firefly.template.function.



## Performance

Environment:

 * MAC OS X 10.10
 * JVM arguments: -XX:+AggressiveOpts -XX:+UseParallelGC -XX:+UseParallelOldGC -Xmx1024m -Xms1024m
 * CPU: Intel Core i5 2.3GHz
 * RAM: 8G

Test case:
{% highlight text %}
./ab -k -n100000 -c100 $URL
{% endhighlight %}

{% highlight text %}
Document Path:          /index
Document Length:        2736 bytes

Concurrency Level:      100
Time taken for tests:   3.096 seconds
Complete requests:      100000
Failed requests:        0
Keep-Alive requests:    100000
Total transferred:      289800000 bytes
HTML transferred:       273600000 bytes
Requests per second:    32294.88 [#/sec] (mean)
Time per request:       3.096 [ms] (mean)
Time per request:       0.031 [ms] (mean, across all concurrent requests)
Transfer rate:          91397.04 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    0   0.1      0       5
Processing:     0    3   4.5      2      92
Waiting:        0    3   4.5      2      92
Total:          0    3   4.5      2      92

Percentage of the requests served within a certain time (ms)
  50%      2
  66%      3
  75%      3
  80%      4
  90%      5
  95%      6
  98%     12
  99%     19
 100%     92 (longest request)
{% endhighlight %} 


##Contact information
E-mail: qptkk@163.com  
QQ Group: 126079579