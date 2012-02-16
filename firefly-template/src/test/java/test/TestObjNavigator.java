package test;

import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.template.Model;
import com.firefly.template.support.ObjectNavigator;


public class TestObjNavigator {
	
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
		Assert.assertThat(o.find(model, "a").toString(), is("fffff"));
		Assert.assertThat(o.find(model, "b['ccc']").toString(), is("ddd"));
		Assert.assertThat(o.find(model, "b['eee']").toString(), is("fff"));
		Assert.assertThat(o.find(model, "b[\"ccc\"]").toString(), is("ddd"));
		Assert.assertThat((Integer)o.find(model, "arr[2]"), is(333));
		Assert.assertThat(o.find(model, "list[2]").toString(), is("list333"));
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
		
		Model model = new ModelMock();
		model.put("foo", foo);
		
		ObjectNavigator o = ObjectNavigator.getInstance();
		Assert.assertThat(String.valueOf(o.find(model, "foo.bar.info")), is("bar1"));
		Assert.assertThat(String.valueOf(o.find(model, "foo.bar.serialNumber")), is("33"));
		Assert.assertThat(String.valueOf(o.find(model, "foo.bar.price")), is("3.3"));
		Assert.assertThat(String.valueOf(o.find(model, "foo.numbers[2]")), is("5"));
		Assert.assertThat(String.valueOf(o.find(model, "foo.map['bar2'].price")), is("2.3"));
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
		
//		long start = System.currentTimeMillis();
//		for (int i = 0; i < 1000000; i++) {
//			o.find(model, "foo.numbers[2]");
//		}
//		long end = System.currentTimeMillis() - start;
//		System.out.println(o.find(model, "foo.numbers[2]") + "|" + end);
//		
//		start = System.currentTimeMillis();
//		for (int i = 0; i < 1000000; i++) {
//			o.find(model, "foo.bags[3]");
//		}
//		end = System.currentTimeMillis() - start;
//		System.out.println(o.find(model, "foo.bags[3]") + "|" + end);
//		List<Object> list = null;
//		for(Object obj : (Collection<?>)list) {
//			
//		}
		
	}
}
