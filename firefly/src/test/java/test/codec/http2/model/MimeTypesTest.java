package test.codec.http2.model;

import com.firefly.codec.http2.model.AcceptMIMEType;
import com.firefly.codec.http2.model.MimeTypes;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class MimeTypesTest {
    @Test
    public void testGetMimeByExtension_Gzip() {
        assertMimeTypeByExtension("application/gzip", "test.gz");
    }

    @Test
    public void testGetMimeByExtension_Png() {
        assertMimeTypeByExtension("image/png", "test.png");
        assertMimeTypeByExtension("image/png", "TEST.PNG");
        assertMimeTypeByExtension("image/png", "Test.Png");
    }

    @Test
    public void testGetMimeByExtension_Png_MultiDot() {
        assertMimeTypeByExtension("image/png", "org.eclipse.jetty.Logo.png");
    }

    @Test
    public void testGetMimeByExtension_Png_DeepPath() {
        assertMimeTypeByExtension("image/png", "/org/eclipse/jetty/Logo.png");
    }

    @Test
    public void testGetMimeByExtension_Text() {
        assertMimeTypeByExtension("text/plain", "test.txt");
        assertMimeTypeByExtension("text/plain", "TEST.TXT");
    }

    @Test
    public void testGetMimeByExtension_NoExtension() {
        MimeTypes mimetypes = new MimeTypes();
        String contentType = mimetypes.getMimeByExtension("README");
        assertNull(contentType);
    }

    private void assertMimeTypeByExtension(String expectedMimeType, String filename) {
        MimeTypes mimetypes = new MimeTypes();
        String contentType = mimetypes.getMimeByExtension(filename);
        String prefix = "MimeTypes.getMimeByExtension(" + filename + ")";
        assertNotNull(prefix, contentType);
        assertEquals(prefix, expectedMimeType, contentType);
    }

    private void assertCharsetFromContentType(String contentType, String expectedCharset) {
        assertThat("getCharsetFromContentType(\"" + contentType + "\")",
                MimeTypes.getCharsetFromContentType(contentType), is(expectedCharset));
    }

    @Test
    public void testCharsetFromContentType() {
        assertCharsetFromContentType("foo/bar;charset=abc;some=else", "abc");
        assertCharsetFromContentType("foo/bar;charset=abc", "abc");
        assertCharsetFromContentType("foo/bar ; charset = abc", "abc");
        assertCharsetFromContentType("foo/bar ; charset = abc ; some=else", "abc");
        assertCharsetFromContentType("foo/bar;other=param;charset=abc;some=else", "abc");
        assertCharsetFromContentType("foo/bar;other=param;charset=abc", "abc");
        assertCharsetFromContentType("foo/bar other = param ; charset = abc", "abc");
        assertCharsetFromContentType("foo/bar other = param ; charset = abc ; some=else", "abc");
        assertCharsetFromContentType("foo/bar other = param ; charset = abc", "abc");
        assertCharsetFromContentType("foo/bar other = param ; charset = \"abc\" ; some=else", "abc");
        assertCharsetFromContentType("foo/bar", null);
        assertCharsetFromContentType("foo/bar;charset=uTf8", "utf-8");
        assertCharsetFromContentType("foo/bar;other=\"charset=abc\";charset=uTf8", "utf-8");
        assertCharsetFromContentType("application/pdf; charset=UTF-8", "utf-8");
        assertCharsetFromContentType("application/pdf;; charset=UTF-8", "utf-8");
        assertCharsetFromContentType("application/pdf;;; charset=UTF-8", "utf-8");
        assertCharsetFromContentType("application/pdf;;;; charset=UTF-8", "utf-8");
        assertCharsetFromContentType("text/html;charset=utf-8", "utf-8");
    }

    @Test
    public void testContentTypeWithoutCharset() {
        assertEquals("foo/bar;some=else", MimeTypes.getContentTypeWithoutCharset("foo/bar;charset=abc;some=else"));
        assertEquals("foo/bar", MimeTypes.getContentTypeWithoutCharset("foo/bar;charset=abc"));
        assertEquals("foo/bar", MimeTypes.getContentTypeWithoutCharset("foo/bar ; charset = abc"));
        assertEquals("foo/bar;some=else", MimeTypes.getContentTypeWithoutCharset("foo/bar ; charset = abc ; some=else"));
        assertEquals("foo/bar;other=param;some=else", MimeTypes.getContentTypeWithoutCharset("foo/bar;other=param;charset=abc;some=else"));
        assertEquals("foo/bar;other=param", MimeTypes.getContentTypeWithoutCharset("foo/bar;other=param;charset=abc"));
        assertEquals("foo/bar ; other = param", MimeTypes.getContentTypeWithoutCharset("foo/bar ; other = param ; charset = abc"));
        assertEquals("foo/bar ; other = param;some=else", MimeTypes.getContentTypeWithoutCharset("foo/bar ; other = param ; charset = abc ; some=else"));
        assertEquals("foo/bar ; other = param", MimeTypes.getContentTypeWithoutCharset("foo/bar ; other = param ; charset = abc"));
        assertEquals("foo/bar ; other = param;some=else", MimeTypes.getContentTypeWithoutCharset("foo/bar ; other = param ; charset = \"abc\" ; some=else"));
        assertEquals("foo/bar", MimeTypes.getContentTypeWithoutCharset("foo/bar"));
        assertEquals("foo/bar", MimeTypes.getContentTypeWithoutCharset("foo/bar;charset=uTf8"));
        assertEquals("foo/bar;other=\"charset=abc\"", MimeTypes.getContentTypeWithoutCharset("foo/bar;other=\"charset=abc\";charset=uTf8"));
        assertEquals("text/html", MimeTypes.getContentTypeWithoutCharset("text/html;charset=utf-8"));
    }

    @Test
    public void testAcceptMimeTypes() {
        List<AcceptMIMEType> list = MimeTypes.parseAcceptMIMETypes("text/plain; q=0.9, text/html");
        Assert.assertThat(list.size(), is(2));
        Assert.assertThat(list.get(0).getParentType(), is("text"));
        Assert.assertThat(list.get(0).getChildType(), is("plain"));
        Assert.assertThat(list.get(0).getQuality(), is(0.9F));
        Assert.assertThat(list.get(1).getParentType(), is("text"));
        Assert.assertThat(list.get(1).getChildType(), is("html"));
        Assert.assertThat(list.get(1).getQuality(), is(1.0F));

        list = MimeTypes.parseAcceptMIMETypes("text/plain, text/html");
        Assert.assertThat(list.size(), is(2));
        Assert.assertThat(list.get(0).getParentType(), is("text"));
        Assert.assertThat(list.get(0).getChildType(), is("plain"));
        Assert.assertThat(list.get(1).getParentType(), is("text"));
        Assert.assertThat(list.get(1).getChildType(), is("html"));

        list = MimeTypes.parseAcceptMIMETypes("text/plain");
        Assert.assertThat(list.size(), is(1));
        Assert.assertThat(list.get(0).getParentType(), is("text"));
        Assert.assertThat(list.get(0).getChildType(), is("plain"));
    }
}
