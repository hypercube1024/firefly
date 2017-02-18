package test.http.router.handler.body;

import com.firefly.$;
import org.junit.Before;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractHTTPHandlerTest {
    final String host = "localhost";
    static int port = 8000;
    String uri;

    @Before
    public void init() {
        port++;
        uri = $.uri.newURIBuilder("http", host, port).toString();
        System.out.println(uri);
    }
}
