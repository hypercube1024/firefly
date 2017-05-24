package test.http.router;

import com.firefly.server.http2.router.utils.PathUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestPathUtils {

    @Test
    public void test() {
        List<String> paths = PathUtils.split("/app/index/");
        Assert.assertThat(paths.size(), is(2));
        Assert.assertThat(paths.get(0), is("app"));
        Assert.assertThat(paths.get(1), is("index"));

        paths = PathUtils.split("/app/index");
        Assert.assertThat(paths.size(), is(2));
        Assert.assertThat(paths.get(0), is("app"));
        Assert.assertThat(paths.get(1), is("index"));
    }

    public static void main(String[] args) {
        String line = "This order was placed for QT3000! OK?";
        Pattern pattern = Pattern.compile("(.*?)(\\d+)(.*)");
        Matcher matcher = pattern.matcher(line);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(line);
        while (matcher.find()) {
            System.out.println(matcher.groupCount());
            System.out.println("group 1: " + matcher.group(1));
            System.out.println("group 2: " + matcher.group(2));
            System.out.println("group 3: " + matcher.group(3));
        }
    }
}
