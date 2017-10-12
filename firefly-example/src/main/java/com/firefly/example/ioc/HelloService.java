package com.firefly.example.ioc;

/**
 * @author Pengtao Qiu
 */
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
