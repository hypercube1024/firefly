package test.http.router.handler;

import com.firefly.$;
import com.firefly.utils.RandomUtils;
import org.junit.Before;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractHTTPHandlerTest {
    protected final String host = "localhost";
    protected static int port = 8000;
    protected String uri;

    @Before
    public void init() {
        port = (int) RandomUtils.random(3000, 65534);
        uri = $.uri.newURIBuilder("http", host, port).toString();
        System.out.println(uri);
    }
}
