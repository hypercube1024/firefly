package test.utils.lang;

import com.firefly.utils.lang.GenericTypeReference;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;

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

    public static class SubReq extends Request<String, Integer, String> {

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

}
