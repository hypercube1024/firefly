package test.utils.json.compiler;

import static org.hamcrest.Matchers.is;

import org.junit.Assert;
import org.junit.Test;

import test.utils.json.Group;
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
}
