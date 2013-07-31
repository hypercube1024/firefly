package test.utils.collection;

import org.junit.Test;
import org.junit.Assert;

import static org.hamcrest.Matchers.*;
import com.firefly.utils.collection.ConsistentHash;

public class TestConsistentHash {

	@Test
	public void test() {
		ConsistentHash<Integer> h = new ConsistentHash<Integer>(new ConsistentHash.HashFunction() {
			@Override
			public int hash(Object o) {
				return o.hashCode();
			}

			@Override
			public int hashWithVirtualNodeIndex(Object o, int index) {
				return o.hashCode() + index * 10000;
			}
		});
		h.add(3);
		h.add(10);
		h.add(20);

//		System.out.println(h.get(1508844));
//		System.out.println(h.get(15));
		
		Assert.assertThat(h.get(1508844), is(3));
		Assert.assertThat(h.get(15), is(20));
	}
}
