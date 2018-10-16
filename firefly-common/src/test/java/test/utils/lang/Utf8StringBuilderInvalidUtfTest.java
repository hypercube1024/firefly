package test.utils.lang;

import com.firefly.utils.lang.TypeUtils;
import com.firefly.utils.lang.Utf8Appendable.NotUtf8Exception;
import com.firefly.utils.lang.Utf8StringBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Test various invalid UTF8 byte sequences.
 */
@RunWith(Parameterized.class)
public class Utf8StringBuilderInvalidUtfTest {
    @Parameters
    public static Collection<Object[]> data() {
        List<Object[]> data = new ArrayList<>();
        data.add(new String[]{"c0af"});
        data.add(new String[]{"EDA080"});
        data.add(new String[]{"f08080af"});
        data.add(new String[]{"f8808080af"});
        data.add(new String[]{"e080af"});
        data.add(new String[]{"F4908080"});
        data.add(new String[]{"fbbfbfbfbf"});
        data.add(new String[]{"10FFFF"});
        data.add(new String[]{"CeBaE1BdB9Cf83CeBcCeB5EdA080656469746564"});
        // use of UTF-16 High Surrogates (in codepoint form)
        data.add(new String[]{"da07"});
        data.add(new String[]{"d807"});
        // decoded UTF-16 High Surrogate "\ud807" (in UTF-8 form)

        data.add(new String[]{"EDA087"});
        return data;
    }

    private byte[] bytes;

    public Utf8StringBuilderInvalidUtfTest(String rawhex) {
        bytes = TypeUtils.fromHexString(rawhex);
        System.out.printf("Utf8StringBuilderInvalidUtfTest[] (%s)%n", TypeUtils.toHexString(bytes));
    }

    @Test(expected = NotUtf8Exception.class)
    public void testInvalidUTF8() {
        Utf8StringBuilder buffer = new Utf8StringBuilder();
        buffer.append(bytes, 0, bytes.length);
    }
}
