package com.firefly.example.http.hello;

import com.firefly.$;
import com.firefly.utils.exception.CommonRuntimeException;

/**
 * @author Pengtao Qiu
 */
public class ErrorHandlerDemo {
    public static void main(String[] args) {
        $.httpServer().router().get("/error")
         .handler(ctx -> {
             throw new CommonRuntimeException("perhaps some errors happen");
         }).listen("localhost", 8080);
    }
}
