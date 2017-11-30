package test.codec.http2.encode;

import org.junit.Assert;
import org.junit.Test;

import static com.firefly.codec.http2.encode.PredefinedHTTP1Response.CONTINUE_100_BYTES;
import static com.firefly.codec.http2.encode.PredefinedHTTP1Response.H2C_BYTES;
import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestPredefinedHTTP1Response {

    @Test
    public void testH2c() {
        System.out.println(H2C_BYTES.length);
        Assert.assertThat(H2C_BYTES.length, is(71));
        System.out.println(new String(H2C_BYTES));
        Assert.assertThat(new String(H2C_BYTES), is(
                "HTTP/1.1 101 Switching Protocols\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Upgrade: h2c\r\n\r\n"));
    }

    @Test
    public void continue100() {
        System.out.println(CONTINUE_100_BYTES.length);
        Assert.assertThat(CONTINUE_100_BYTES.length, is(25));
        System.out.println(new String(CONTINUE_100_BYTES));
        Assert.assertThat(new String(CONTINUE_100_BYTES), is("HTTP/1.1 100 Continue\r\n\r\n"));
    }
}
