package test.utils.json;

import static org.hamcrest.Matchers.*;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.utils.json.Json;

public class TestParser {
	@Test
	public void test() {
		SimpleObj i = new SimpleObj();
		i.setAge(10);
		i.setId(33442);
		i.setNumber(30);
		i.setName("PengtaoQiu\nAlvin");
		i.setType((short)-33);
		i.setWeight(55.47f);
		i.setHeight(170.5);
		String jsonStr = Json.toJson(i);
		System.out.println(jsonStr);
		
		SimpleObj i2 = Json.toObject(jsonStr, SimpleObj.class);
		Assert.assertThat(i2.getAge(), is(10));
		Assert.assertThat(i2.getId(), is(33442));
		Assert.assertThat(i2.getNumber(), is(30));
		Assert.assertThat(i2.getDate(), is(0L));
		Assert.assertThat(i2.getName(), is("PengtaoQiu\nAlvin"));
		Assert.assertThat(i2.getType(), is((short)-33));
		Assert.assertThat(i2.getHeight(), is(170.5));
		Assert.assertThat(i2.getWeight(), is(55.47f));
	}
	
	public static void main(String[] args) {
		SimpleObj i = new SimpleObj();
		i.setAge(10);
		i.setId(33442);
		i.setNumber(30);
		i.setName("PengtaoQiu\nAlvin");
		String jsonStr = Json.toJson(i);
		System.out.println(jsonStr);
		
		SimpleObj i2 = Json.toObject(jsonStr, SimpleObj.class);
		System.out.println(i2.getAge());
		System.out.println(i2.getId());
		System.out.println(i2.getNumber());
		System.out.println(i2.getName());
	}
}
