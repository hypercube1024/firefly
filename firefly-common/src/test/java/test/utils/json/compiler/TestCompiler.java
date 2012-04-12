package test.utils.json.compiler;

import static org.hamcrest.Matchers.*;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import test.utils.json.CollectionObj;
import test.utils.json.Group;
import test.utils.json.SimpleObj;

import com.firefly.utils.json.Parser;
import com.firefly.utils.json.compiler.DecodeCompiler;
import com.firefly.utils.json.compiler.EncodeCompiler;
import com.firefly.utils.json.parser.CollectionParser;
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
		ParserMetaInfo[] pp = DecodeCompiler.compile(CollectionObj.class);
		ParserMetaInfo p = pp[0];
		Assert.assertThat(p.getType().getName(), is(List.class.getName()));
		
		Parser parser = p.getParser();
		if(parser instanceof CollectionParser) {
			CollectionParser colp = (CollectionParser) parser;
			p = colp.getElementMetaInfo();
			Assert.assertThat(p.getType().getName(), is(List.class.getName()));
		}
		
		parser = p.getParser();
		if(parser instanceof CollectionParser) {
			CollectionParser colp = (CollectionParser) parser;
			p = colp.getElementMetaInfo();
			Assert.assertThat(p.getType().getName(), is(SimpleObj.class.getName()));
		}
	}
	
	public static void main(String[] args) {
		ParserMetaInfo[] pp = DecodeCompiler.compile(CollectionObj.class);
		ParserMetaInfo p = pp[0];
		test(p);
		System.out.println(List.class);
	}
	
	public static void test(ParserMetaInfo p) {
		System.out.println(p.getType());
		Parser parser = p.getParser();
		if(parser instanceof CollectionParser) {
			CollectionParser colp = (CollectionParser) parser;
			test(colp.getElementMetaInfo());
		}
	}
}
