package test.utils.json;

import static org.hamcrest.Matchers.*;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.utils.json.Json;

public class TestParser {
	@Test
	public void test() {
		IntObj i = new IntObj();
		i.setAge(10);
		i.setId(33442);
		i.setNumber(30);
		String jsonStr = Json.toJson(i);
		System.out.println(jsonStr);
		
		IntObj i2 = Json.toObject(jsonStr, IntObj.class);
		Assert.assertThat(i2.getAge(), is(10));
		Assert.assertThat(i2.getId(), is(33442));
		Assert.assertThat(i2.getNumber(), is(30));
	}
	
	public static void main(String[] args) {
		IntObj i = new IntObj();
		i.setAge(10);
		i.setId(33442);
		i.setNumber(30);
		String jsonStr = Json.toJson(i);
		System.out.println(jsonStr);
		
		IntObj i2 = Json.toObject(jsonStr, IntObj.class);
		System.out.println(i2.getAge());
		System.out.println(i2.getId());
		System.out.println(i2.getNumber());
	}
}
