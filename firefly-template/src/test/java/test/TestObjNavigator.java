package test;

import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.template.Model;
import com.firefly.template.support.ObjectNavigator;


public class TestObjNavigator {
	
	public void testMap() {
		Map<Integer, Map<String, String>> map = new HashMap<Integer, Map<String, String>>();
		Map<String, String> m1 = new HashMap<String, String>();
		m1.put("key1", "key1");
		map.put(4, m1);
		Model model = new ModelMock();
		model.put("testMap", map);
		ObjectNavigator o = ObjectNavigator.getInstance();
		Assert.assertThat(o.find(model, "testMap[4]['key1']").toString(), equalTo("key1"));
	}
	
	@Test
	public void testRoot() {
		Map<String, Object> map2 = new HashMap<String, Object>();
		map2.put("ccc", "ddd");
		map2.put("eee", "fff");
		
		int[] arr = {111, 222, 333};
		
		List<String> list = new ArrayList<String>();
		list.add("list111");
		list.add("list222");
		list.add("list333");
		
		Model model = new ModelMock();
		model.put("a", "fffff");
		model.put("b", map2);
		model.put("arr", arr);
		model.put("list", list);
		
		ObjectNavigator o = ObjectNavigator.getInstance();
		Assert.assertThat(o.find(model, "a").toString(), equalTo("fffff"));
		Assert.assertThat(o.find(model, "b['ccc']").toString(), equalTo("ddd"));
		Assert.assertThat(o.find(model, "b['eee']").toString(), equalTo("fff"));
		Assert.assertThat(o.find(model, "b[\"ccc\"]").toString(), equalTo("ddd"));
		Assert.assertThat((Integer)o.find(model, "arr[2]"), equalTo(333));
		Assert.assertThat(o.find(model, "list[2]").toString(), equalTo("list333"));
	}
	
	@Test
	public void testObject() {
		Foo foo = new Foo();
		Bar bar = new Bar();
		bar.setInfo("bar1");
		bar.setSerialNumber(33L);
		bar.setPrice(3.30);
		foo.setBar(bar);
		
		
		Map<String, Bar> fooMap = new HashMap<String, Bar>();
		bar = new Bar();
		bar.setInfo("bar2");
		bar.setSerialNumber(23L);
		bar.setPrice(2.30);
		fooMap.put("bar2", bar);
		foo.setMap(fooMap);
		
		Map<String, Object> map2 = new HashMap<String, Object>();
		
		Map<String, Object> map3 = new HashMap<String, Object>();
		map3.put("hello", "world");
		map3.put("hello3", "world3");
		map3.put("arr", Arrays.asList(5,6,7));
		
		Map<String, Object> map4 = new HashMap<String, Object>();
		map4.put("hello4", map3);
		
		map2.put("map3", map3);
		map2.put("map4", map4);
		foo.setMap2(map2);
		
		Model model = new ModelMock();
		model.put("foo", foo);
		
		ObjectNavigator o = ObjectNavigator.getInstance();
		Assert.assertThat(String.valueOf(o.find(model, "foo.bar.info")), equalTo("bar1"));
		Assert.assertThat(String.valueOf(o.find(model, "foo.bar.serialNumber")), equalTo("33"));
		Assert.assertThat(String.valueOf(o.find(model, "foo.bar.price")), equalTo("3.3"));
		Assert.assertThat(String.valueOf(o.find(model, "foo.numbers[2]")), equalTo("5"));
		Assert.assertThat(String.valueOf(o.find(model, "foo.map['bar2'].price")), equalTo("2.3"));
		Assert.assertThat(String.valueOf(o.find(model, "foo.map2['map4']['hello4']['hello3']")), equalTo("world3"));
		Assert.assertThat(String.valueOf(o.find(model, "foo.map2['map4']['hello4']['arr'][2]")), equalTo("7"));
		Assert.assertThat(o.find(model, "foo.map['bar5']"), nullValue());
		Assert.assertThat(o.find(model, "ok"), nullValue());
	}
	
	public static void main(String[] args) {
		ObjectNavigator o = ObjectNavigator.getInstance();
		
		Foo foo = new Foo();
		Bar bar = new Bar();
		bar.setInfo("bar1");
		bar.setSerialNumber(33L);
		bar.setPrice(3.30);
		foo.setBar(bar);
		
		
		Map<String, Bar> fooMap = new HashMap<String, Bar>();
		bar = new Bar();
		bar.setInfo("bar2");
		bar.setSerialNumber(23L);
		bar.setPrice(2.30);
		fooMap.put("bar2", bar);
		foo.setMap(fooMap);
		
		Map<String, Object> map2 = new HashMap<String, Object>();
		
		Map<String, Object> map3 = new HashMap<String, Object>();
		map3.put("hello", "world");
		map3.put("hello3", "world3");
		map3.put("arr", Arrays.asList(5,6,7));
		
		Map<String, Object> map4 = new HashMap<String, Object>();
		map4.put("hello4", map3);
		
		map2.put("map3", map3);
		map2.put("map4", map4);
		foo.setMap2(map2);
		
		Model model = new ModelMock();
		model.put("foo", foo);
		
		System.out.println(o.find(model, "foo.bar.info"));
		System.out.println(o.find(model, "foo.bar.info"));
		System.out.println(o.find(model, "foo.bar.serialNumber"));
		System.out.println(o.find(model, "foo.bar.price"));
		System.out.println(o.find(model, "foo.numbers[2]"));
		System.out.println(o.find(model, "foo.bags[2]"));
		System.out.println(o.find(model, "foo.map['bar2']"));
		System.out.println(o.find(model, "foo.map['bar2'].price"));
		System.out.println(o.find(model, "foo.map['bar4']"));
		System.out.println(o.find(model, "user.name"));
		System.out.println(o.find(model, "foo.map2['map4']['hello4']['hello3']"));
		System.out.println(o.find(model, "foo.map2['map4']['hello4']['arr'][2]"));
		
//		long start = System.currentTimeMillequalTo();
//		for (int i = 0; i < 1000000; i++) {
//			o.find(model, "foo.numbers[2]");
//		}
//		long end = System.currentTimeMillequalTo() - start;
//		System.out.println(o.find(model, "foo.numbers[2]") + "|" + end);
//		
//		start = System.currentTimeMillequalTo();
//		for (int i = 0; i < 1000000; i++) {
//			o.find(model, "foo.bags[3]");
//		}
//		end = System.currentTimeMillequalTo() - start;
//		System.out.println(o.find(model, "foo.bags[3]") + "|" + end);
//		List<Object> list = null;
//		for(Object obj : (Collection<?>)list) {
//			
//		}
		
	}
}
