package test.utils.lang;

import com.firefly.utils.ReflectUtils;
import com.firefly.utils.lang.GenericTypeReference;
import com.firefly.utils.lang.bean.FieldGenericTypeBind;
import com.firefly.utils.lang.bean.MethodGenericTypeBind;
import com.firefly.utils.lang.bean.MethodType;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Pengtao Qiu
 */
public class TestGenericTypeReference {

    public static class Request<T, R, S> {
        private T data;
        private String name;
        private Map<R, S> extInfo;
        private List<String> msgs;
        public Map<R, S> attrs;
        private T[] array;

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<R, S> getExtInfo() {
            return extInfo;
        }

        public void setExtInfo(Map<R, S> extInfo) {
            this.extInfo = extInfo;
        }

        public List<String> getMsgs() {
            return msgs;
        }

        public void setMsgs(List<String> msgs) {
            this.msgs = msgs;
        }

        public T[] getArray() {
            return array;
        }

        public void setArray(T[] array) {
            this.array = array;
        }

        public class Bar<U> {
            public Map<R, S> maps;
            public U bar;
        }

        public class Car<R> {
            public R car;
        }

        public class Par {
            public R par;

            public class Sar<U> {
                private U sar;
                private List<T> list;

                public U getSar() {
                    return sar;
                }

                public void setSar(U sar) {
                    this.sar = sar;
                }

                public List<T> getList() {
                    return list;
                }

                public void setList(List<T> list) {
                    this.list = list;
                }
            }
        }
    }

    public static class Foo {
        private String foo;

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }
    }

    public static GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>> reqRef = new GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>>() {
    };
    public static GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>.Bar<List<String>>> barRef = new GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>.Bar<List<String>>>() {
    };
    public static GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>.Car<List<Integer>>> carRef = new GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>.Car<List<Integer>>>() {
    };
    public static GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>.Par> parRef = new GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>.Par>() {
    };
    public static GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>.Par.Sar<Integer>> sarRef = new GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>.Par.Sar<Integer>>() {
    };
    public static GenericTypeReference<Request<Foo[][], String, List<Foo>[]>> arrayRef = new GenericTypeReference<Request<Foo[][], String, List<Foo>[]>>() {
    };

    @Test
    public void test() throws Exception {
        ParameterizedType type = (ParameterizedType) new GenericTypeReference<Request<Map<String, Foo>, String, Integer>>() {
        }.getType();

        Assert.assertThat(type.getActualTypeArguments()[0] instanceof ParameterizedType, is(true));
        Assert.assertThat(type.getActualTypeArguments()[1] instanceof Class<?>, is(true));
        Assert.assertThat(type.getRawType() == Request.class, is(true));

        Class<?> rawType = (Class<?>) type.getRawType();
        System.out.println("raw type -> " + rawType + "|" + type);

        Map<String, Type> genericNameTypeMap = new HashMap<>();
        TypeVariable[] typeVariables = rawType.getTypeParameters();
        Type[] types = type.getActualTypeArguments();
        for (int i = 0; i < types.length; i++) {
            System.out.println(typeVariables[i].getName() + "|" + types[i] + "|" + types[i].getClass());
            genericNameTypeMap.put(typeVariables[i].getName(), types[i]);
        }

        Assert.assertThat(genericNameTypeMap.get("S") == Integer.class, is(true));

        System.out.println("----------------------------");
        System.out.println(rawType.getDeclaredField("data").getGenericType());
        System.out.println(rawType.getDeclaredField("extInfo").getGenericType());
        System.out.println(rawType.getDeclaredField("name").getGenericType());
        System.out.println(rawType.getDeclaredField("msgs").getGenericType());
        Assert.assertThat(rawType.getDeclaredField("extInfo").getGenericType() instanceof ParameterizedType, is(true));
        Assert.assertThat(rawType.getDeclaredField("msgs").getGenericType() instanceof ParameterizedType, is(true));

        ParameterizedType extInfoType = (ParameterizedType) rawType.getDeclaredField("extInfo").getGenericType();
        System.out.println(extInfoType.getActualTypeArguments()[0].getTypeName());
        Assert.assertThat(extInfoType.getActualTypeArguments()[0].getTypeName(), is("R"));

        ParameterizedType msgsType = (ParameterizedType) rawType.getDeclaredField("msgs").getGenericType();
        System.out.println(msgsType.getActualTypeArguments()[0].getTypeName());

        System.out.println("----------------------------");
    }

    @Test
    public void test2() throws Exception {
        Type type = new GenericTypeReference<Foo>() {
        }.getType();
        System.out.println(type);
        Assert.assertThat(type == Foo.class, is(true));
    }

    @Test
    public void testFieldType() throws Exception {
        Map<String, FieldGenericTypeBind> map = ReflectUtils.getGenericBeanFields(reqRef);

        FieldGenericTypeBind extInfo = map.get("attrs");
        System.out.println(extInfo.getField().getName());
        System.out.println(extInfo.getField().getGenericType().getTypeName() + "|" + extInfo.getField().getGenericType().getClass());
        System.out.println(extInfo.getType().getTypeName() + "|" + extInfo.getType().getClass());
        Assert.assertThat(extInfo.getType().getTypeName(), is("java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.util.List<test.utils.lang.TestGenericTypeReference$Foo>>>"));


        ParameterizedType parameterizedType = (ParameterizedType) extInfo.getType();
        System.out.println(parameterizedType.getOwnerType());
        Assert.assertThat(parameterizedType.getOwnerType(), nullValue());

        parameterizedType = (ParameterizedType) extInfo.getField().getGenericType();
        System.out.println(parameterizedType.getOwnerType());
        Assert.assertThat(parameterizedType.getOwnerType(), nullValue());
    }

    @Test
    public void testNestedClass() throws Exception {
        Map<String, FieldGenericTypeBind> map = ReflectUtils.getGenericBeanFields(barRef);

        FieldGenericTypeBind barMaps = map.get("maps");
        ParameterizedType parameterizedType = (ParameterizedType) barMaps.getType();
        System.out.println(parameterizedType.getTypeName());
        System.out.println(parameterizedType.getOwnerType());
        Assert.assertThat(parameterizedType.getTypeName(), is("java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.util.List<test.utils.lang.TestGenericTypeReference$Foo>>>"));
        Assert.assertThat(parameterizedType.getOwnerType(), nullValue());

        FieldGenericTypeBind bar = map.get("bar");
        parameterizedType = (ParameterizedType) bar.getType();
        System.out.println(parameterizedType.getTypeName());
        System.out.println(parameterizedType.getOwnerType());
        Assert.assertThat(parameterizedType.getTypeName(), is("java.util.List<java.lang.String>"));
        Assert.assertThat(parameterizedType.getOwnerType(), nullValue());




        map = ReflectUtils.getGenericBeanFields(parRef);
        FieldGenericTypeBind par = map.get("par");
        Class<?> clazz = (Class<?>) par.getType();
        System.out.println(clazz.getTypeName());
        Assert.assertThat(clazz.getTypeName(), is("java.lang.String"));


    }

    @Test
    public void testOwnType() {
        Map<String, MethodGenericTypeBind> getterMap = ReflectUtils.getGenericBeanGetterMethods(sarRef);
        MethodGenericTypeBind list = getterMap.get("list");
        System.out.println(list.getType().getTypeName());
        Assert.assertThat(list.getType().getTypeName(), is("java.util.List<java.util.Map<java.lang.String, test.utils.lang.TestGenericTypeReference$Foo>>"));

        MethodGenericTypeBind sar = getterMap.get("sar");
        Assert.assertThat(sar.getType().getTypeName(), is("java.lang.Integer"));
    }

    @Test
    public void testOverrideOwnType() {
        Map<String, FieldGenericTypeBind> map = ReflectUtils.getGenericBeanFields(carRef);
        FieldGenericTypeBind car = map.get("car");
        ParameterizedType parameterizedType = (ParameterizedType) car.getType();
        System.out.println(parameterizedType.getTypeName());
        System.out.println(parameterizedType.getOwnerType());
        Assert.assertThat(parameterizedType.getTypeName(), is("java.util.List<java.lang.Integer>"));
        Assert.assertThat(parameterizedType.getOwnerType(), nullValue());
    }

    @Test
    public void testMethodType() {
        Map<String, MethodGenericTypeBind> getterMap = ReflectUtils.getGenericBeanGetterMethods(reqRef);
        MethodGenericTypeBind extInfo = getterMap.get("extInfo");
        ParameterizedType parameterizedType = (ParameterizedType) extInfo.getType();
        System.out.println(parameterizedType.getTypeName());
        Assert.assertThat(parameterizedType.getTypeName(), is("java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.util.List<test.utils.lang.TestGenericTypeReference$Foo>>>"));
        Assert.assertThat(extInfo.getMethodType(), is(MethodType.GETTER));

        Map<String, MethodGenericTypeBind> setterMap = ReflectUtils.getGenericBeanSetterMethods(reqRef);
        extInfo = setterMap.get("extInfo");
        parameterizedType = (ParameterizedType) extInfo.getType();
        System.out.println(parameterizedType.getTypeName());
        Assert.assertThat(parameterizedType.getTypeName(), is("java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.util.List<test.utils.lang.TestGenericTypeReference$Foo>>>"));
        Assert.assertThat(extInfo.getMethodType(), is(MethodType.SETTER));
    }

    @Test
    public void testGenericArray() {
        Map<String, MethodGenericTypeBind> setterMap = ReflectUtils.getGenericBeanSetterMethods(arrayRef);
        MethodGenericTypeBind data = setterMap.get("data");
        System.out.println(data.getType().getClass());
        System.out.println(data.getType().getTypeName());
        Class<?> clazz = (Class<?>) data.getType();
        Assert.assertThat(clazz.isArray(), is(true));
        Assert.assertThat(clazz.getComponentType() == Foo[].class, is(true));

        MethodGenericTypeBind extInfo = setterMap.get("extInfo");
        System.out.println(extInfo.getType().getClass());
        ParameterizedType parameterizedType = (ParameterizedType) extInfo.getType();
        System.out.println(parameterizedType.getActualTypeArguments()[1].getClass());
        GenericArrayType genericArrayType = (GenericArrayType) parameterizedType.getActualTypeArguments()[1];
        System.out.println(genericArrayType.getGenericComponentType().getTypeName());
        System.out.println(genericArrayType.getGenericComponentType().getClass());
        parameterizedType = (ParameterizedType) genericArrayType.getGenericComponentType();
        Assert.assertThat(parameterizedType.getRawType() == List.class, is(true));
        Assert.assertThat(parameterizedType.getActualTypeArguments()[0] == Foo.class, is(true));

        MethodGenericTypeBind array = setterMap.get("array");
        System.out.println(array.getType().getClass());
        genericArrayType = (GenericArrayType) array.getType();
        System.out.println(genericArrayType.getTypeName());
        System.out.println(genericArrayType.getClass());
        Assert.assertThat(genericArrayType.getTypeName(), is("test.utils.lang.TestGenericTypeReference$Foo[][][]"));
        System.out.println(genericArrayType.getGenericComponentType().getTypeName());
        System.out.println(genericArrayType.getGenericComponentType().getClass());
        Assert.assertThat(genericArrayType.getGenericComponentType().getTypeName(), is("test.utils.lang.TestGenericTypeReference$Foo[][]"));
    }

}
