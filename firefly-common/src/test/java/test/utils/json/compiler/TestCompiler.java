package test.utils.json.compiler;

import static org.hamcrest.Matchers.*;

import org.junit.Assert;
import org.junit.Test;

import test.utils.json.Group;
import test.utils.json.User;

import com.firefly.utils.json.compiler.DecodeCompiler;
import com.firefly.utils.json.compiler.EncodeCompiler;
import com.firefly.utils.json.support.ParserMetaInfo;
import com.firefly.utils.json.support.SerializerMetaInfo;

public class TestCompiler {
	
	@Test
	public void test() {
		SerializerMetaInfo[] s = EncodeCompiler.compile(Group.class);
		ParserMetaInfo[] p = DecodeCompiler.compile(Group.class);
		
		for (int i = 0; i < p.length; i++) {
			Assert.assertThat(p[i].getPropertyNameString(), is(s[i].getPropertyNameString()));
			System.out.println(p[i].getPropertyNameString());
		}
	}
	
	@Test
	public void test2() {
		ParserMetaInfo[] p = DecodeCompiler.compile(Group.class);
		Assert.assertThat(p[3].getActualTypeArguments().length, is(1));
		Assert.assertThat(p[3].getActualTypeArguments()[0] == User.class, is(true));
	}
	
	public static void main(String[] args) {
		
	}
}
