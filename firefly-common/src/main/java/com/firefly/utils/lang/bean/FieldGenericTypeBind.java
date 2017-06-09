package com.firefly.utils.lang.bean;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * @author Pengtao Qiu
 */
public class FieldGenericTypeBind extends BeanTypeBind {

    private Field field;

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldGenericTypeBind that = (FieldGenericTypeBind) o;
        return Objects.equals(field, that.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field);
    }
}
