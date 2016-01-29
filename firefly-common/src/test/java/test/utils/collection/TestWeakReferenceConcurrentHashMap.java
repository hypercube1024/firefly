package test.utils.collection;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.utils.collection.ConcurrentAutomaticClearMap;
import com.firefly.utils.collection.WeakReferenceConcurrentHashMap;

public class TestWeakReferenceConcurrentHashMap {

	@Test
	public void test() {
		ConcurrentAutomaticClearMap<String, String> map = new WeakReferenceConcurrentHashMap<>();

		long i = 0;
		while (true) {
			i = i + 1;
			map.put("key" + i, UUID.randomUUID().toString());
//			System.out.println(map.get("key1") + "|" + map.size());
			if (map.get("key1") == null)
				break;
		}
		System.out.println(i);
		System.out.println(map.size());
//		System.out.println(map.entrySet());
//		System.out.println(map.size());
		Assert.assertTrue(i >= map.size());
	}

}
