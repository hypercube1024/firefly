package com.firefly.db.jdbc.utils;

/**
 * @author Pengtao Qiu
 */
public class PojoSourceCode {

    private String name;
    private String codes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCodes() {
        return codes;
    }

    public void setCodes(String codes) {
        this.codes = codes;
    }

    @Override
    public String toString() {
        return codes;
    }
}
