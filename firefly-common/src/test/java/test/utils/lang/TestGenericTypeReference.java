package test.utils.lang;

import com.firefly.utils.ReflectUtils;
import com.firefly.utils.lang.GenericTypeReference;
import com.firefly.utils.lang.bean.FieldGenericTypeBind;
import org.junit.Assert;
import org.junit.Test;

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

        public class Bar<U> {
            public Map<R, S> maps;
            public U bar;
        }

        public class Car<R> {
            public R car;
        }

        public class Par {
            public R par;
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
    public void test3() throws Exception {
        Map<String, FieldGenericTypeBind> map = ReflectUtils.getGenericBeanFields(
                new GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>>() {
                }, null);

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
        Map<String, FieldGenericTypeBind> map = ReflectUtils.getGenericBeanFields(
                new GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>.Bar<List<String>>>() {
                }, null);

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
    }

}
