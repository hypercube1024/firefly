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
	* [Contact information](#contact-information)

# Firefly Framework

Firefly framework is a high performance full-stack java web framework. It helps you create a java web application __*Easy*__ and __*Quickly*__. It provides MVC framework with HTTP Server and many other useful components for developing web applications. That means you can easy deploy your web without any other java web containers, in short , it's containerless. It taps into the fullest potential of hardware using __*SEDA*__ architecture, a highly customizable thread model.  

## Getting start

Running firefly is very easy, now you can download the dependency from Apache Central Repository, the pom is:

{% highlight xml %}
<dependency>
  <groupId>com.fireflysource</groupId>
  <artifactId>firefly-common</artifactId>
  <version>3.0.2</version>
</dependency>
<dependency>
  <groupId>com.fireflysource</groupId>
  <artifactId>firefly-template</artifactId>
  <version>3.0.2</version>
</dependency>
<dependency>
  <groupId>com.fireflysource</groupId>
  <artifactId>firefly-nettool</artifactId>
  <version>3.0.2</version>
</dependency>
<dependency>
  <groupId>com.fireflysource</groupId>
  <artifactId>firefly</artifactId>
  <version>3.0.2</version>
</dependency>
{% endhighlight %}

There are two ways to start a firefly application, building by maven, or just run it on eclipse simply.

### Building by maven
1. Clone firefly source code from Github.
2. Find the demo project 'firefly-demo', modify the log path in firefly-log.properties, you can find it in 'firefly-demo/src/main/resources', in this case, you __*Must*__ modify these two rows to your own location

{% highlight properties %}
firefly-system=INFO,/Users/qiupengtao/develop/logs
firefly-access=INFO,/Users/qiupengtao/develop/logs
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

##Contact information
E-mail: qptkk@163.com  
QQ Group: 126079579