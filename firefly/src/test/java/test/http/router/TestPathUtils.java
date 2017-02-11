package test.http.router;

import com.firefly.server.http2.router.utils.PathUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

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
}
