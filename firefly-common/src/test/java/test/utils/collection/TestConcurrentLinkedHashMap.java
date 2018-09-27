package test.utils.collection;

import com.firefly.utils.collection.ConcurrentLinkedHashMap;
import com.firefly.utils.collection.ConcurrentLinkedHashMap.MapEventListener;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.is;

public class TestConcurrentLinkedHashMap {
    static MapEventListener<String, String> listener = new MapEventListener<String, String>() {

        @Override
        public String onRemoveEntry(String key, String value) {
            return value;
        }

        @Override
        public String onGetEntry(String key, String value) {
            return value;
        }

        @Override
        public String onPutEntry(String key, String value, String previousValue) {
            return previousValue;
        }

        @Override
        public boolean onEliminateEntry(String key, String value) {
//			System.out.println(key + "|" + value);
            return true;
        }
    };

    @Test
    public void testMaxEntries() {

        ConcurrentLinkedHashMap<String, String> map = new ConcurrentLinkedHashMap<>(true, 33, listener);
        System.out.println(map.getConcurrencyLevel() + "|" + map.getSegmentMask() + "|" + map.getSegmentShift());
        for (int i = 0; i < 100; i++) {
            map.put("" + i, "v" + i);
        }
        System.out.println(map.size());
        Assert.assertThat(map.size(), is(32));

    }

    @Test
    public void testLeastRecentlyUsed() {
        ConcurrentLinkedHashMap<String, String> map = new ConcurrentLinkedHashMap<>(true, 33, listener);
        System.out.println(map.getConcurrencyLevel() + "|" + map.getSegmentMask() + "|" + map.getSegmentShift());
        for (int i = 0; i < 100; i++) {
            if (i > 75)
                map.get("75");
            map.put("" + i, "v" + i);
        }
        Assert.assertThat(map.get("75"), is("v75"));
    }

    @Test
    public void testConcurrentLevel() {
        ConcurrentLinkedHashMap<String, String> map = new ConcurrentLinkedHashMap<>(true, 200, listener, 200);
        Assert.assertThat(map.getConcurrencyLevel(), is(256));
    }

    public static void main(String[] args) {
        ConcurrentLinkedHashMap<String, String> map = new ConcurrentLinkedHashMap<>(true, 33, listener);
        System.out.println(map.getConcurrencyLevel() + "|" + map.getSegmentMask() + "|" + map.getSegmentShift());
        for (int i = 0; i < 100; i++) {
            map.put("key" + i, "v" + i);
        }
        System.out.println(map.toString());
        for (int i = 0; i < 100; i++) {
            System.out.println(map.get("key" + i));
        }
        System.out.println(map.get("key97"));
        System.out.println(map.get("key97"));
        System.out.println(map.get("key97"));
        System.out.println(map.get("key97"));
        System.out.println(map.get("key97"));
    }
}
