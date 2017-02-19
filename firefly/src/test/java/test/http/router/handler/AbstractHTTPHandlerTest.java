package test.http.router.handler;

import com.firefly.$;
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
        port++;
        uri = $.uri.newURIBuilder("http", host, port).toString();
        System.out.println(uri);
    }
}
