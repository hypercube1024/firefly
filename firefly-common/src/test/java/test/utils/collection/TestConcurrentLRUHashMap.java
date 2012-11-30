package test.utils.collection;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import com.firefly.utils.collection.ConcurrentLRUHashMap;
import com.firefly.utils.collection.LRUMapEventListener;

public class TestConcurrentLRUHashMap {
	
	@Test
	public void test() {
		LRUMapEventListener<String, String> listener = new LRUMapEventListener<String, String>(){

			@Override
			public void eliminated(String key, String value) {
				Assert.assertThat((String)key, is("a2"));
				Assert.assertThat((String)value, is("hello2"));
			}

			@Override
			public String getNull(String key) {
				Assert.assertThat((String)key, is("a2"));
				return key + " is null";
			}};
		ConcurrentLRUHashMap<String, String> map = new ConcurrentLRUHashMap<String, String>(3, 0.75f, 1, listener);
		map.put("a1", "hello1");
		map.put("a2", "hello2");
		map.put("a3", "hello3");
		map.get("a1");
		map.put("a4", "hello4");
		
		Assert.assertThat(map.get("a1"), is("hello1"));
		Assert.assertThat(map.get("a2"), is("a2 is null"));
	}

}
