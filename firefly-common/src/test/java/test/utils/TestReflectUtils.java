package test.utils;

import org.junit.Assert;
import org.junit.Test;
import com.firefly.utils.ReflectUtils;
import static org.hamcrest.Matchers.*;

public class TestReflectUtils {

	@Test
	public void testGetterMethod() {
		Assert.assertThat(ReflectUtils.getGetterMethod(Foo.class, "name")
				.getName(), is("getName"));
		Assert.assertThat(ReflectUtils.getGetterMethod(Foo.class, "failure")
				.getName(), is("isFailure"));
	}
	
	public static void main(String[] args) {
		System.out.println(ReflectUtils.getGetterMethod(Foo.class, "name").getName());
	}

	public static class Foo {
		private boolean failure;
		private String name;
		

		public boolean isFailure() {
			return failure;
		}

		public void setFailure(boolean failure) {
			this.failure = failure;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
