package com.fireflysource.net.http.common;

public class ProxyAuthentication {

    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "ProxyAuthentication{" +
                "username=******'" + '\'' +
                ", password='******" + '\'' +
                '}';
    }
}
