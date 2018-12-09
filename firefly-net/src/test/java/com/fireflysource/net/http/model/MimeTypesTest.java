package com.fireflysource.net.http.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MimeTypesTest {

    @Test
    void testGetMimeByExtension_Gzip() {
        assertMimeTypeByExtension("application/gzip", "test.gz");
    }

    @Test
    void testGetMimeByExtension_Png() {
        assertMimeTypeByExtension("image/png", "test.png");
        assertMimeTypeByExtension("image/png", "TEST.PNG");
        assertMimeTypeByExtension("image/png", "Test.Png");
    }

    @Test
    void testGetMimeByExtension_Png_MultiDot() {
        assertMimeTypeByExtension("image/png", "com.fireflysource.Logo.png");
    }

    @Test
    void testGetMimeByExtension_Png_DeepPath() {
        assertMimeTypeByExtension("image/png", "/com/fireflysource/Logo.png");
    }

    @Test
    void testGetMimeByExtension_Text() {
        assertMimeTypeByExtension("text/plain", "test.txt");
        assertMimeTypeByExtension("text/plain", "TEST.TXT");
    }

    @Test
    void testGetMimeByExtension_NoExtension() {
        MimeTypes mimetypes = new MimeTypes();
        String contentType = mimetypes.getMimeByExtension("README");
        assertNull(contentType);
    }

    private void assertMimeTypeByExtension(String expectedMimeType, String filename) {
        MimeTypes mimetypes = new MimeTypes();
        String contentType = mimetypes.getMimeByExtension(filename);
        assertNotNull(contentType);
        assertEquals(expectedMimeType, contentType);
    }

    private void assertCharsetFromContentType(String contentType, String expectedCharset) {
        assertEquals(expectedCharset, MimeTypes.getCharsetFromContentType(contentType));
    }

    @Test
    void testCharsetFromContentType() {
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
    void testContentTypeWithoutCharset() {
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
    void testAcceptMimeTypes() {
        List<AcceptMIMEType> list = MimeTypes.parseAcceptMIMETypes("text/plain; q=0.9, text/html");
        assertEquals(2, list.size());
        assertEquals("text", list.get(0).getParentType());
        assertEquals("html", list.get(0).getChildType());
        assertEquals(1.0F, list.get(0).getQuality());
        assertEquals("text", list.get(1).getParentType());
        assertEquals("plain", list.get(1).getChildType());
        assertEquals(0.9F, list.get(1).getQuality());

        list = MimeTypes.parseAcceptMIMETypes("text/plain, text/html");
        assertEquals(2, list.size());
        assertEquals("text", list.get(0).getParentType());
        assertEquals("plain", list.get(0).getChildType());
        assertEquals("text", list.get(1).getParentType());
        assertEquals("html", list.get(1).getChildType());

        list = MimeTypes.parseAcceptMIMETypes("text/plain");
        assertEquals(1, list.size());
        assertEquals("text", list.get(0).getParentType());
        assertEquals("plain", list.get(0).getChildType());

        list = MimeTypes.parseAcceptMIMETypes("*/*; q=0.8, text/plain; q=0.9, text/html, */json");
        assertEquals(4, list.size());

        assertEquals("text", list.get(0).getParentType());
        assertEquals("html", list.get(0).getChildType());
        assertEquals(1.0F, list.get(0).getQuality());
        assertEquals(AcceptMIMEMatchType.EXACT, list.get(0).getMatchType());

        assertEquals("*", list.get(1).getParentType());
        assertEquals("json", list.get(1).getChildType());
        assertEquals(1.0F, list.get(1).getQuality());
        assertEquals(AcceptMIMEMatchType.CHILD, list.get(1).getMatchType());

        assertEquals("text", list.get(2).getParentType());
        assertEquals("plain", list.get(2).getChildType());
        assertEquals(0.9F, list.get(2).getQuality());
        assertEquals(AcceptMIMEMatchType.EXACT, list.get(2).getMatchType());

        assertEquals("*", list.get(3).getParentType());
        assertEquals("*", list.get(3).getChildType());
        assertEquals(0.8F, list.get(3).getQuality());
        assertEquals(AcceptMIMEMatchType.ALL, list.get(3).getMatchType());
    }
}
