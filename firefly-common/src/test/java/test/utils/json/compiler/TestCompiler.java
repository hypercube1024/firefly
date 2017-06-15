package test.utils.json.compiler;

import com.firefly.utils.json.Parser;
import com.firefly.utils.json.compiler.DecodeCompiler;
import com.firefly.utils.json.compiler.EncodeCompiler;
import com.firefly.utils.json.parser.CollectionParser;
import com.firefly.utils.json.support.ParserMetaInfo;
import com.firefly.utils.json.support.SerializerMetaInfo;
import org.junit.Assert;
import org.junit.Test;
import test.utils.json.CollectionObj;
import test.utils.json.Group;
import test.utils.json.SimpleObj;

import java.util.ArrayList;
import java.util.LinkedList;

import static org.hamcrest.Matchers.is;

public class TestCompiler {

    @Test
    public void test() {
        SerializerMetaInfo[] s = EncodeCompiler.compile(Group.class);
        ParserMetaInfo[] p = DecodeCompiler.compile(Group.class, Group.class);

        for (int i = 0; i < p.length; i++) {
            Assert.assertThat(p[i].getPropertyNameString(), is(s[i].getPropertyNameString()));
            System.out.println(p[i].getPropertyNameString());
        }
    }

    @Test
    public void test2() {
        ParserMetaInfo[] pp = DecodeCompiler.compile(CollectionObj.class, CollectionObj.class);
        ParserMetaInfo p = pp[0];
        Assert.assertThat(p.getExtractedType().getName(), is(ArrayList.class.getName()));

        Parser parser = p.getParser();
        if (parser instanceof CollectionParser) {
            CollectionParser colp = (CollectionParser) parser;
            p = colp.getElementMetaInfo();
            Assert.assertThat(p.getExtractedType().getName(), is(LinkedList.class.getName()));
        }

        parser = p.getParser();
        if (parser instanceof CollectionParser) {
            CollectionParser colp = (CollectionParser) parser;
            p = colp.getElementMetaInfo();
            Assert.assertThat(p.getExtractedType().getName(), is(SimpleObj.class.getName()));
        }
    }

}
