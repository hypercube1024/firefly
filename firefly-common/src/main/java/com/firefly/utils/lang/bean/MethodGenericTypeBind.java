package com.firefly.utils.lang.bean;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author Pengtao Qiu
 */
public class MethodGenericTypeBind extends BeanTypeBind {

    private Method method;

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public MethodType getMethodType() {
        if (method.getName().startsWith("set")) {
            return MethodType.SETTER;
        } else {
            return MethodType.GETTER;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodGenericTypeBind that = (MethodGenericTypeBind) o;
        return Objects.equals(method, that.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method);
    }
}
