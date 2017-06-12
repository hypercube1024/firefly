package test.utils.lang;

import com.firefly.utils.lang.GenericTypeReference;
import com.firefly.utils.lang.bean.FieldGenericTypeBind;
import com.firefly.utils.lang.bean.MethodGenericTypeBind;
import com.firefly.utils.lang.bean.MethodType;
import com.firefly.utils.lang.bean.PropertyAccess;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.firefly.utils.BeanUtils.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static test.utils.lang.TestGenericTypeReference.Foo;
import static test.utils.lang.TestGenericTypeReference.Request;

/**
 * @author Pengtao Qiu
 */
public class TestBeanUtils {

    public static class Store<T, R> {
        private List<T> products;
        private R manager;

        public List<T> getProducts() {
            return products;
        }

        public void setProducts(List<T> products) {
            this.products = products;
        }

        public R getManager() {
            return manager;
        }

        public void setManager(R manager) {
            this.manager = manager;
        }
    }

    public static class Person<T> {
        public String name;
        private Map<String, T> info;

        public Map<String, T> getInfo() {
            return info;
        }

        public void setInfo(Map<String, T> info) {
            this.info = info;
        }
    }

    @Test
    public void testFieldType() throws Exception {
        Map<String, FieldGenericTypeBind> map = getGenericBeanFields(
                new GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>>() {
                });

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
        Map<String, FieldGenericTypeBind> map = getGenericBeanFields(
                new GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>.Bar<List<String>>>() {
                });

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

        GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>.Par> parRef =
                new GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>.Par>() {
                };
        map = getGenericBeanFields(parRef);
        FieldGenericTypeBind par = map.get("par");
        Class<?> clazz = (Class<?>) par.getType();
        System.out.println(clazz.getTypeName());
        Assert.assertThat(clazz.getTypeName(), is("java.lang.String"));
    }

    @Test
    public void testOwnType() {
        GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>.Par.Sar<Integer>> sarRef =
                new GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>.Par.Sar<Integer>>() {
                };
        Map<String, MethodGenericTypeBind> getterMap = getGenericBeanGetterMethods(sarRef);
        MethodGenericTypeBind list = getterMap.get("list");
        System.out.println(list.getType().getTypeName());
        Assert.assertThat(list.getType().getTypeName(), is("java.util.List<java.util.Map<java.lang.String, test.utils.lang.TestGenericTypeReference$Foo>>"));

        MethodGenericTypeBind sar = getterMap.get("sar");
        Assert.assertThat(sar.getType().getTypeName(), is("java.lang.Integer"));
    }

    @Test
    public void testOverrideOwnType() {
        Map<String, FieldGenericTypeBind> map = getGenericBeanFields(
                new GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>.Car<List<Integer>>>() {
                });
        FieldGenericTypeBind car = map.get("car");
        ParameterizedType parameterizedType = (ParameterizedType) car.getType();
        System.out.println(parameterizedType.getTypeName());
        System.out.println(parameterizedType.getOwnerType());
        Assert.assertThat(parameterizedType.getTypeName(), is("java.util.List<java.lang.Integer>"));
        Assert.assertThat(parameterizedType.getOwnerType(), nullValue());
    }

    @Test
    public void testMethodType() {
        Map<String, MethodGenericTypeBind> getterMap = getGenericBeanGetterMethods(
                new GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>>() {
                });
        MethodGenericTypeBind extInfo = getterMap.get("extInfo");
        ParameterizedType parameterizedType = (ParameterizedType) extInfo.getType();
        System.out.println(parameterizedType.getTypeName());
        Assert.assertThat(parameterizedType.getTypeName(), is("java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.util.List<test.utils.lang.TestGenericTypeReference$Foo>>>"));
        Assert.assertThat(extInfo.getMethodType(), is(MethodType.GETTER));

        Map<String, MethodGenericTypeBind> setterMap = getGenericBeanSetterMethods(
                new GenericTypeReference<Request<Map<String, Foo>, String, Map<String, List<Foo>>>>() {
                });
        extInfo = setterMap.get("extInfo");
        parameterizedType = (ParameterizedType) extInfo.getType();
        System.out.println(parameterizedType.getTypeName());
        Assert.assertThat(parameterizedType.getTypeName(), is("java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.util.List<test.utils.lang.TestGenericTypeReference$Foo>>>"));
        Assert.assertThat(extInfo.getMethodType(), is(MethodType.SETTER));
    }

    @Test
    public void testGenericArray() {
        GenericTypeReference<Request<Foo[][], String, List<Foo>[]>> arrayRef =
                new GenericTypeReference<Request<Foo[][], String, List<Foo>[]>>() {
                };
        Map<String, MethodGenericTypeBind> setterMap = getGenericBeanSetterMethods(arrayRef);
        MethodGenericTypeBind data = setterMap.get("data");
        System.out.println(data.getType().getTypeName());
        Class<?> clazz = (Class<?>) data.getType();
        Assert.assertThat(clazz.isArray(), is(true));
        Assert.assertThat(clazz.getComponentType() == Foo[].class, is(true));
        Assert.assertThat(getComponentType(data.getType()) == Foo[].class, is(true));

        setterMap = getGenericBeanSetterMethods(arrayRef);
        MethodGenericTypeBind extInfo = setterMap.get("extInfo");
        System.out.println(extInfo.getType().getClass());
        ParameterizedType parameterizedType = (ParameterizedType) extInfo.getType();
        System.out.println(parameterizedType.getActualTypeArguments()[1].getClass());
        GenericArrayType genericArrayType = (GenericArrayType) parameterizedType.getActualTypeArguments()[1];
        parameterizedType = (ParameterizedType) genericArrayType.getGenericComponentType();
        Assert.assertThat(parameterizedType.getRawType() == List.class, is(true));
        Assert.assertThat(parameterizedType.getActualTypeArguments()[0] == Foo.class, is(true));
        Assert.assertThat(getComponentType(genericArrayType) == List.class, is(true));

        setterMap = getGenericBeanSetterMethods(arrayRef);
        MethodGenericTypeBind array = setterMap.get("array");
        System.out.println(array.getType().getClass());
        genericArrayType = (GenericArrayType) array.getType();
        System.out.println(genericArrayType.getTypeName());
        Assert.assertThat(genericArrayType.getTypeName(), is("test.utils.lang.TestGenericTypeReference$Foo[][][]"));
        Assert.assertThat(genericArrayType.getGenericComponentType().getTypeName(), is("test.utils.lang.TestGenericTypeReference$Foo[][]"));
        Assert.assertThat(isArray(array.getType()), is(true));
        Assert.assertThat(getComponentType(genericArrayType) == Foo[][].class, is(true));
    }

    @Test
    public void testComponentType() {
        GenericTypeReference<Person<Foo>[][][][]> personRef = new GenericTypeReference<Person<Foo>[][][][]>() {
        };
        System.out.println(personRef.getType());
        System.out.println(personRef.getType().getClass());
        Assert.assertThat(extractGenericArrayClass((GenericArrayType) personRef.getType()) == Person[][][].class, is(true));
    }

    @Test
    public void testExtractClass() {
        GenericTypeReference<Person<Foo>> personRef = new GenericTypeReference<Person<Foo>>() {
        };
        Assert.assertThat(extractClass(personRef.getType()) == Person.class, is(true));

        GenericTypeReference<Foo> fooRef = new GenericTypeReference<Foo>() {
        };
        Assert.assertThat(extractClass(fooRef.getType()) == Foo.class, is(true));

        GenericTypeReference<Person<String>[]> personRef2 = new GenericTypeReference<Person<String>[]>() {
        };
        Assert.assertThat(extractClass(personRef2.getType()) == Person.class, is(true));

        GenericTypeReference<Person<String>[][]> personRef3 = new GenericTypeReference<Person<String>[][]>() {
        };
        Assert.assertThat(extractClass(personRef3.getType()) == Person[].class, is(true));
    }

    @Test
    public void testGenericBeanAccess() {
        Store<Map<String, String>, Person<String>> storeInstance = new Store<>();
        Map<String, PropertyAccess> store = getBeanAccess(new GenericTypeReference<Store<Map<String, String>, Person<String>>>() {
        });

        PropertyAccess products = store.get("products");
        System.out.println(products.getType().getTypeName());
        Assert.assertThat(products.extractClass() == List.class, is(true));

        List<Map<String, String>> productList = new ArrayList<>();
        Map<String, String> product = new HashMap<>();
        product.put("name", "bike");
        product.put("type", "vehicle");
        product.put("price", "1000.00");
        productList.add(product);

        product = new HashMap<>();
        product.put("name", "car");
        product.put("type", "vehicle");
        product.put("price", "500000.00");
        productList.add(product);

        products.setValue(storeInstance, productList);

        System.out.println(storeInstance.getProducts().size());
        Assert.assertThat(storeInstance.getProducts().size(), is(2));

        List<Map<String, String>> productList2 = products.getValue(storeInstance);
        Assert.assertThat(productList2.size(), is(2));
        Assert.assertThat(productList2.get(0).get("name"), is("bike"));


        PropertyAccess manager = store.get("manager");
        Assert.assertThat(manager.extractClass() == Person.class, is(true));

        Person<String> person = new Person<>();
        person.name = "Pengtao Qiu";
        person.setInfo(new HashMap<>());
        person.getInfo().put("favorite food", "Sauteed beef shreds with chilli");
        person.getInfo().put("keyboard", "Keyclick mechanical keyboard");
        manager.setValue(storeInstance, person);
        Assert.assertThat(storeInstance.getManager().name, is("Pengtao Qiu"));
        Assert.assertThat(storeInstance.getManager().getInfo().get("keyboard"), is("Keyclick mechanical keyboard"));
    }

    @Test
    public void testClassBeanAccess() {
        Store<String, Person<String>> storeInstance = new Store<>();
        Map<String, PropertyAccess> store = getBeanAccess(storeInstance.getClass());

        PropertyAccess manager = store.get("manager");
        Assert.assertThat(manager.getType().getTypeName(), is("R"));

        Map<String, PropertyAccess> foo = getBeanAccess(Foo.class);
        PropertyAccess f = foo.get("foo");
        Assert.assertThat(f.getType().getTypeName(), is("java.lang.String"));
    }
}
