package test.utils.lang;

import com.firefly.utils.lang.GenericTypeReference;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
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
        ParameterizedType type = (ParameterizedType) new GenericTypeReference<Request<Foo, String, Integer>>() {
        }.getType();

        System.out.println(type.getOwnerType().getTypeName());
        System.out.println("getActualTypeArguments -> " + type.getActualTypeArguments().length);
        Arrays.stream(type.getActualTypeArguments()).forEach(t -> System.out.println(t.getTypeName()));
        Assert.assertThat(type.getRawType() == Request.class, is(true));

        Class<?> rawType = (Class<?>) type.getRawType();
        Arrays.stream(rawType.getTypeParameters()).forEach(v -> System.out.println(v.getName()));

        System.out.println("----------------------------");
        System.out.println(rawType.getDeclaredField("data").getGenericType().getTypeName());
        System.out.println(rawType.getDeclaredField("extInfo").getGenericType().getTypeName());
    }
}
