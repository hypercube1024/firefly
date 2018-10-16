package com.firefly.example.ioc;

import com.firefly.annotation.Proxy;

import java.lang.annotation.*;

/**
 * @author Pengtao Qiu
 */
@Proxy(proxyClass = LogProxy.class)
@Proxy(proxyClass = AuthenticationProxy.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProxyChain {
}
