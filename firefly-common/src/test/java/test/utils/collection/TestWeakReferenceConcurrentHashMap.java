package test.utils.collection;

import com.firefly.utils.collection.ConcurrentAutomaticClearMap;
import com.firefly.utils.collection.WeakReferenceConcurrentHashMap;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class TestWeakReferenceConcurrentHashMap {

    @Test
    public void test() {
        ConcurrentAutomaticClearMap<String, String> map = new WeakReferenceConcurrentHashMap<>();

        long i = 0;
        while (true) {
            i = i + 1;
            map.put("key" + i, UUID.randomUUID().toString());
            if (map.get("key1") == null) {
                break;
            }

            if (i > 100) {
                break;
            }
        }
        System.out.println(i);
        System.out.println(map.size());
//		System.out.println(map.entrySet());
//		System.out.println(map.size());
        Assert.assertTrue(i >= map.size());
    }

}
