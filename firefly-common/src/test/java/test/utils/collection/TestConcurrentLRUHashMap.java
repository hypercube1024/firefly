package test.utils.collection;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import com.firefly.utils.collection.ConcurrentLRUHashMap;

public class TestConcurrentLRUHashMap {
	
	@Test
	public void test() {
		ConcurrentLRUHashMap<String, String> map = new ConcurrentLRUHashMap<String, String>(3, 0.75f, 1);
		map.put("a1", "hello1");
		map.put("a2", "hello2");
		map.put("a3", "hello3");
		map.get("a1");
		map.put("a4", "hello4");
		
		Assert.assertThat(map.get("a1"), is("hello1"));
		Assert.assertThat(map.get("a2"), nullValue());
	}

}
