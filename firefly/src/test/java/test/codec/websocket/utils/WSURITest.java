package test.codec.websocket.utils;

import com.firefly.codec.websocket.utils.WSURI;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.is;

public class WSURITest {
    private void assertURI(URI actual, URI expected) {
        System.out.println(actual.getPort() + ", " + expected.getPort());
        Assert.assertThat(actual.toASCIIString(), is(expected.toASCIIString()));
    }

    @Test
    public void testHttpsToHttps() throws URISyntaxException {
        assertURI(WSURI.toHttp(URI.create("https://localhost/")), URI.create("https://localhost/"));
    }

    @Test
    public void testHttpsToWss() throws URISyntaxException {
        assertURI(WSURI.toWebsocket(URI.create("https://localhost/")), URI.create("wss://localhost/"));
    }

    @Test
    public void testHttpToHttp() throws URISyntaxException {
        assertURI(WSURI.toHttp(URI.create("http://localhost/")), URI.create("http://localhost/"));
    }

    @Test
    public void testHttpToWs() throws URISyntaxException {
        assertURI(WSURI.toWebsocket(URI.create("http://localhost/")), URI.create("ws://localhost/"));
        assertURI(WSURI.toWebsocket(URI.create("http://localhost:8080/deeper/")), URI.create("ws://localhost:8080/deeper/"));
        assertURI(WSURI.toWebsocket("http://localhost/"), URI.create("ws://localhost/"));
        assertURI(WSURI.toWebsocket("http://localhost/", null), URI.create("ws://localhost/"));
        assertURI(WSURI.toWebsocket("http://localhost/", "a=b"), URI.create("ws://localhost/?a=b"));
    }

    @Test
    public void testWssToHttps() throws URISyntaxException {
        assertURI(WSURI.toHttp(URI.create("wss://localhost/")), URI.create("https://localhost/"));
    }

    @Test
    public void testWssToWss() throws URISyntaxException {
        assertURI(WSURI.toWebsocket(URI.create("wss://localhost/")), URI.create("wss://localhost/"));
    }

    @Test
    public void testWsToHttp() throws URISyntaxException {
        assertURI(WSURI.toHttp(URI.create("ws://localhost/")), URI.create("http://localhost/"));
    }

    @Test
    public void testWsToWs() throws URISyntaxException {
        assertURI(WSURI.toWebsocket(URI.create("ws://localhost/")), URI.create("ws://localhost/"));
    }

}
