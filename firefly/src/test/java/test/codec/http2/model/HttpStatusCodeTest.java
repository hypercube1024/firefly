package test.codec.http2.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.codec.http2.model.HttpStatus;

public class HttpStatusCodeTest {
    @Test
    public void testInvalidGetCode() {
        assertNull("Invalid code: 800", HttpStatus.getCode(800));
        assertNull("Invalid code: 190", HttpStatus.getCode(190));
    }

    @Test
    public void testHttpMethod() {
        assertEquals("GET", HttpMethod.GET.toString());
    }
}
