package com.fireflysource.net.http.common.v2.hpack;


import com.fireflysource.common.object.TypeUtils;
import com.fireflysource.net.http.common.model.HttpField;
import com.fireflysource.net.http.common.model.HttpHeader;
import com.fireflysource.net.http.common.model.HttpScheme;
import com.fireflysource.net.http.common.model.MetaData;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Iterator;

import static com.fireflysource.net.http.common.v2.hpack.HpackException.CompressionException;
import static com.fireflysource.net.http.common.v2.hpack.HpackException.StreamException;
import static org.junit.jupiter.api.Assertions.*;


class HpackDecoderTest {

    @Test
    void testDecodeD_3() throws Exception {
        HpackDecoder decoder = new HpackDecoder(4096, 8192);

        // First request
        String encoded = "828684410f7777772e6578616d706c652e636f6d";
        ByteBuffer buffer = ByteBuffer.wrap(TypeUtils.fromHexString(encoded));

        MetaData.Request request = (MetaData.Request) decoder.decode(buffer);

        assertEquals("GET", request.getMethod());
        assertEquals(HttpScheme.HTTP.getValue(), request.getURI().getScheme());
        assertEquals("/", request.getURI().getPath());
        assertEquals("www.example.com", request.getURI().getHost());
        assertFalse(request.iterator().hasNext());

        // Second request
        encoded = "828684be58086e6f2d6361636865";
        buffer = ByteBuffer.wrap(TypeUtils.fromHexString(encoded));

        request = (MetaData.Request) decoder.decode(buffer);

        assertEquals("GET", request.getMethod());
        assertEquals(HttpScheme.HTTP.getValue(), request.getURI().getScheme());
        assertEquals("/", request.getURI().getPath());
        assertEquals("www.example.com", request.getURI().getHost());
        Iterator<HttpField> iterator = request.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(new HttpField("cache-control", "no-cache"), iterator.next());
        assertFalse(iterator.hasNext());

        // Third request
        encoded = "828785bf400a637573746f6d2d6b65790c637573746f6d2d76616c7565";
        buffer = ByteBuffer.wrap(TypeUtils.fromHexString(encoded));

        request = (MetaData.Request) decoder.decode(buffer);

        assertEquals("GET", request.getMethod());
        assertEquals(HttpScheme.HTTPS.getValue(), request.getURI().getScheme());
        assertEquals("/index.html", request.getURI().getPath());
        assertEquals("www.example.com", request.getURI().getHost());
        iterator = request.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(new HttpField("custom-key", "custom-value"), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void testDecodeD_4() throws Exception {
        HpackDecoder decoder = new HpackDecoder(4096, 8192);

        // First request
        String encoded = "828684418cf1e3c2e5f23a6ba0ab90f4ff";
        ByteBuffer buffer = ByteBuffer.wrap(TypeUtils.fromHexString(encoded));

        MetaData.Request request = (MetaData.Request) decoder.decode(buffer);

        assertEquals("GET", request.getMethod());
        assertEquals(HttpScheme.HTTP.getValue(), request.getURI().getScheme());
        assertEquals("/", request.getURI().getPath());
        assertEquals("www.example.com", request.getURI().getHost());
        assertFalse(request.iterator().hasNext());

        // Second request
        encoded = "828684be5886a8eb10649cbf";
        buffer = ByteBuffer.wrap(TypeUtils.fromHexString(encoded));

        request = (MetaData.Request) decoder.decode(buffer);

        assertEquals("GET", request.getMethod());
        assertEquals(HttpScheme.HTTP.getValue(), request.getURI().getScheme());
        assertEquals("/", request.getURI().getPath());
        assertEquals("www.example.com", request.getURI().getHost());
        Iterator<HttpField> iterator = request.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(new HttpField("cache-control", "no-cache"), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void testDecodeWithArrayOffset() throws Exception {
        String value = "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==";

        HpackDecoder decoder = new HpackDecoder(4096, 8192);
        String encoded = "8682418cF1E3C2E5F23a6bA0Ab90F4Ff841f0822426173696320515778685a475270626a70766347567549484e6c633246745a513d3d";
        byte[] bytes = TypeUtils.fromHexString(encoded);
        byte[] array = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, array, 1, bytes.length);
        ByteBuffer buffer = ByteBuffer.wrap(array, 1, bytes.length).slice();

        MetaData.Request request = (MetaData.Request) decoder.decode(buffer);

        assertEquals("GET", request.getMethod());
        assertEquals(HttpScheme.HTTP.getValue(), request.getURI().getScheme());
        assertEquals("/", request.getURI().getPath());
        assertEquals("www.example.com", request.getURI().getHost());
        assertEquals(1, request.getFields().size());
        HttpField field = request.iterator().next();
        assertEquals(HttpHeader.AUTHORIZATION, field.getHeader());
        assertEquals(value, field.getValue());
    }

    @Test
    void testDecodeHuffmanWithArrayOffset() throws Exception {
        HpackDecoder decoder = new HpackDecoder(4096, 8192);

        String encoded = "8286418cf1e3c2e5f23a6ba0ab90f4ff84";
        byte[] bytes = TypeUtils.fromHexString(encoded);
        byte[] array = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, array, 1, bytes.length);
        ByteBuffer buffer = ByteBuffer.wrap(array, 1, bytes.length).slice();

        MetaData.Request request = (MetaData.Request) decoder.decode(buffer);

        assertEquals("GET", request.getMethod());
        assertEquals(HttpScheme.HTTP.getValue(), request.getURI().getScheme());
        assertEquals("/", request.getURI().getPath());
        assertEquals("www.example.com", request.getURI().getHost());
        assertFalse(request.iterator().hasNext());
    }

    @Test
    void testNghttpx() throws Exception {
        // Response encoded by nghttpx
        String encoded = "886196C361Be940b6a65B6850400B8A00571972e080a62D1Bf5f87497cA589D34d1f9a0f0d0234327690Aa69D29aFcA954D3A5358980Ae112e0f7c880aE152A9A74a6bF3";
        ByteBuffer buffer = ByteBuffer.wrap(TypeUtils.fromHexString(encoded));

        HpackDecoder decoder = new HpackDecoder(4096, 8192);
        MetaData.Response response = (MetaData.Response) decoder.decode(buffer);

        assertEquals(200, response.getStatus());
        assertEquals(6, response.getFields().size());
        assertTrue(response.getFields().contains(new HttpField(HttpHeader.DATE, "Fri, 15 Jul 2016 02:36:20 GMT")));
        assertTrue(response.getFields().contains(new HttpField(HttpHeader.CONTENT_TYPE, "text/html")));
        assertTrue(response.getFields().contains(new HttpField(HttpHeader.CONTENT_ENCODING, "")));
        assertTrue(response.getFields().contains(new HttpField(HttpHeader.CONTENT_LENGTH, "42")));
        assertTrue(response.getFields().contains(new HttpField(HttpHeader.SERVER, "nghttpx nghttp2/1.12.0")));
        assertTrue(response.getFields().contains(new HttpField(HttpHeader.VIA, "1.1 nghttpx")));
    }

    @Test
    void testResize() throws Exception {
        String encoded = "203f136687A0E41d139d090760881c6490B2Cd39Ba7f";
        ByteBuffer buffer = ByteBuffer.wrap(TypeUtils.fromHexString(encoded));
        HpackDecoder decoder = new HpackDecoder(4096, 8192);
        MetaData metaData = decoder.decode(buffer);
        assertEquals("localhost0", metaData.getFields().get(HttpHeader.HOST));
        assertEquals("abcdefghij", metaData.getFields().get(HttpHeader.COOKIE));
        assertEquals(50, decoder.getHpackContext().getMaxDynamicTableSize());
        assertEquals(1, decoder.getHpackContext().size());


    }

    @Test
    void testBadResize() throws Exception {
        /*
        4. Dynamic Table Management
        4.2. Maximum Table Size
          × 1: Sends a dynamic table size update at the end of header block
            -> The endpoint MUST treat this as a decoding error.
               Expected: GOAWAY Frame (Error Code: COMPRESSION_ERROR)
                         Connection closed
        */

        String encoded = "203f136687A0E41d139d090760881c6490B2Cd39Ba7f20";
        ByteBuffer buffer = ByteBuffer.wrap(TypeUtils.fromHexString(encoded));
        HpackDecoder decoder = new HpackDecoder(4096, 8192);
        try {
            decoder.decode(buffer);
            fail();
        } catch (CompressionException e) {
            assertTrue(e.getMessage().contains("Dynamic table resize after fields"));
        }
    }

    @Test
    void testTooBigToIndex() throws Exception {
        String encoded = "3f610f17FfEc02Df3990A190A0D4Ee5b3d2940Ec98Aa4a62D127D29e273a0aA20dEcAa190a503b262d8a2671D4A2672a927aA874988a2471D05510750c951139EdA2452a3a548cAa1aA90bE4B228342864A9E0D450A5474a92992a1aA513395448E3A0Aa17B96cFe3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f14E7Cf9f3e7cF9F3E7Cf9f3e7cF9F3E7Cf9f3e7cF9F3E7Cf9f3e7cF9F3E7Cf9f3e7cF9F3E7Cf9f3e7cF9F3E7Cf9f3e7cF9F3E7Cf9f3e7cF9F3E7Cf9f3e7cF9F3E7Cf9f3e7cF9F3E7Cf9f3e7cF9F3E7Cf9f3e7cF9F3E7Cf9f3e7cF9F3E7Cf9f3e7cF9F3E7Cf9f3e7cF9F3E7Cf9f3e7cF9F3E7Cf9f3e7cF9F353F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F7F54f";
        ByteBuffer buffer = ByteBuffer.wrap(TypeUtils.fromHexString(encoded));

        HpackDecoder decoder = new HpackDecoder(128, 8192);
        MetaData metaData = decoder.decode(buffer);

        assertEquals(0, decoder.getHpackContext().getDynamicTableSize());
        assertTrue(metaData.getFields().get("host").startsWith("This is a very large field"));
    }

    @Test
    void testUnknownIndex() throws Exception {
        String encoded = "BE";
        ByteBuffer buffer = ByteBuffer.wrap(TypeUtils.fromHexString(encoded));

        HpackDecoder decoder = new HpackDecoder(128, 8192);
        try {
            decoder.decode(buffer);
            fail();
        } catch (HpackException.SessionException e) {
            assertTrue(e.getMessage().startsWith("Unknown index"));
        }

    }

    /* 8.1.2.1. Pseudo-Header Fields */
    @Test
    void test8_1_2_1_PsuedoHeaderFields() throws Exception {
        // 1:Sends a HEADERS frame that contains a unknown pseudo-header field
        MetaDataBuilder mdb = new MetaDataBuilder(4096);
        mdb.emit(new HttpField(":unknown", "value"));
        try {
            mdb.build();
            fail();
        } catch (StreamException ex) {
            assertTrue(ex.getMessage().contains("Unknown pseudo header"));
        }

        // 2: Sends a HEADERS frame that contains the pseudo-header field defined for response
        mdb = new MetaDataBuilder(4096);
        mdb.emit(new HttpField(HttpHeader.C_SCHEME, "http"));
        mdb.emit(new HttpField(HttpHeader.C_METHOD, "GET"));
        mdb.emit(new HttpField(HttpHeader.C_PATH, "/path"));
        mdb.emit(new HttpField(HttpHeader.C_STATUS, "100"));
        try {
            mdb.build();
            fail();
        } catch (StreamException ex) {
            assertTrue(ex.getMessage().contains("Request and Response headers"));
        }

        // 3: Sends a HEADERS frame that contains a pseudo-header field as trailers

        // 4: Sends a HEADERS frame that contains a pseudo-header field that appears in a header block after a regular header field
        mdb = new MetaDataBuilder(4096);
        mdb.emit(new HttpField(HttpHeader.C_SCHEME, "http"));
        mdb.emit(new HttpField(HttpHeader.C_METHOD, "GET"));
        mdb.emit(new HttpField(HttpHeader.C_PATH, "/path"));
        mdb.emit(new HttpField("Accept", "No Compromise"));
        mdb.emit(new HttpField(HttpHeader.C_AUTHORITY, "localhost"));
        try {
            mdb.build();
            fail();
        } catch (StreamException ex) {
            assertTrue(ex.getMessage().contains("Pseudo header :authority after fields"));
        }
    }

    @Test
    void test8_1_2_2_ConnectionSpecificHeaderFields() throws Exception {
        MetaDataBuilder mdb;

        // 1: Sends a HEADERS frame that contains the connection-specific header field
        mdb = new MetaDataBuilder(4096);
        mdb.emit(new HttpField(HttpHeader.CONNECTION, "value"));
        try {
            mdb.build();
            fail();
        } catch (StreamException ex) {
            assertTrue(ex.getMessage().contains("Connection specific field 'Connection'"));
        }

        // 2: Sends a HEADERS frame that contains the TE header field with any value other than "trailers"
        mdb = new MetaDataBuilder(4096);
        mdb.emit(new HttpField(HttpHeader.TE, "not_trailers"));
        try {
            mdb.build();
            fail();
        } catch (StreamException ex) {
            assertTrue(ex.getMessage().contains("Unsupported TE value 'not_trailers'"));
        }


        mdb = new MetaDataBuilder(4096);
        mdb.emit(new HttpField(HttpHeader.CONNECTION, "TE"));
        mdb.emit(new HttpField(HttpHeader.TE, "trailers"));
        assertNotNull(mdb.build());
    }


    @Test
    void test8_1_2_3_RequestPseudoHeaderFields() throws Exception {
        MetaDataBuilder mdb;

        mdb = new MetaDataBuilder(4096);
        mdb.emit(new HttpField(HttpHeader.C_METHOD, "GET"));
        mdb.emit(new HttpField(HttpHeader.C_SCHEME, "http"));
        mdb.emit(new HttpField(HttpHeader.C_AUTHORITY, "localhost:8080"));
        mdb.emit(new HttpField(HttpHeader.C_PATH, "/"));
        assertTrue(mdb.build() instanceof MetaData.Request);


        // 1: Sends a HEADERS frame with empty ":path" pseudo-header field
        mdb = new MetaDataBuilder(4096);
        mdb = new MetaDataBuilder(4096);
        mdb.emit(new HttpField(HttpHeader.C_METHOD, "GET"));
        mdb.emit(new HttpField(HttpHeader.C_SCHEME, "http"));
        mdb.emit(new HttpField(HttpHeader.C_AUTHORITY, "localhost:8080"));
        mdb.emit(new HttpField(HttpHeader.C_PATH, ""));
        try {
            mdb.build();
            fail();
        } catch (StreamException ex) {
            assertTrue(ex.getMessage().contains("No Path"));
        }

        // 2: Sends a HEADERS frame that omits ":method" pseudo-header field
        mdb = new MetaDataBuilder(4096);
        mdb.emit(new HttpField(HttpHeader.C_SCHEME, "http"));
        mdb.emit(new HttpField(HttpHeader.C_AUTHORITY, "localhost:8080"));
        mdb.emit(new HttpField(HttpHeader.C_PATH, "/"));
        try {
            mdb.build();
            fail();
        } catch (StreamException ex) {
            assertTrue(ex.getMessage().contains("No Method"));
        }


        // 3: Sends a HEADERS frame that omits ":scheme" pseudo-header field
        mdb = new MetaDataBuilder(4096);
        mdb.emit(new HttpField(HttpHeader.C_METHOD, "GET"));
        mdb.emit(new HttpField(HttpHeader.C_AUTHORITY, "localhost:8080"));
        mdb.emit(new HttpField(HttpHeader.C_PATH, "/"));
        try {
            mdb.build();
            fail();
        } catch (StreamException ex) {
            assertTrue(ex.getMessage().contains("No Scheme"));
        }

        // 4: Sends a HEADERS frame that omits ":path" pseudo-header field
        mdb = new MetaDataBuilder(4096);
        mdb.emit(new HttpField(HttpHeader.C_METHOD, "GET"));
        mdb.emit(new HttpField(HttpHeader.C_SCHEME, "http"));
        mdb.emit(new HttpField(HttpHeader.C_AUTHORITY, "localhost:8080"));
        try {
            mdb.build();
            fail();
        } catch (StreamException ex) {
            assertTrue(ex.getMessage().contains("No Path"));
        }

        // 5: Sends a HEADERS frame with duplicated ":method" pseudo-header field
        mdb = new MetaDataBuilder(4096);
        mdb.emit(new HttpField(HttpHeader.C_METHOD, "GET"));
        mdb.emit(new HttpField(HttpHeader.C_METHOD, "GET"));
        mdb.emit(new HttpField(HttpHeader.C_SCHEME, "http"));
        mdb.emit(new HttpField(HttpHeader.C_AUTHORITY, "localhost:8080"));
        mdb.emit(new HttpField(HttpHeader.C_PATH, "/"));
        try {
            mdb.build();
            fail();
        } catch (StreamException ex) {
            assertTrue(ex.getMessage().contains("Duplicate"));
        }

        // 6: Sends a HEADERS frame with duplicated ":scheme" pseudo-header field
        mdb = new MetaDataBuilder(4096);
        mdb.emit(new HttpField(HttpHeader.C_METHOD, "GET"));
        mdb.emit(new HttpField(HttpHeader.C_SCHEME, "http"));
        mdb.emit(new HttpField(HttpHeader.C_SCHEME, "http"));
        mdb.emit(new HttpField(HttpHeader.C_AUTHORITY, "localhost:8080"));
        mdb.emit(new HttpField(HttpHeader.C_PATH, "/"));
        try {
            mdb.build();
            fail();
        } catch (StreamException ex) {
            assertTrue(ex.getMessage().contains("Duplicate"));
        }
    }


    @Test
    void testHuffmanEncodedStandard() throws Exception {
        HpackDecoder decoder = new HpackDecoder(4096, 8192);

        String encoded = "82868441" + "83" + "49509F";
        ByteBuffer buffer = ByteBuffer.wrap(TypeUtils.fromHexString(encoded));

        MetaData.Request request = (MetaData.Request) decoder.decode(buffer);

        assertEquals("GET", request.getMethod());
        assertEquals(HttpScheme.HTTP.getValue(), request.getURI().getScheme());
        assertEquals("/", request.getURI().getPath());
        assertEquals("test", request.getURI().getHost());
        assertFalse(request.iterator().hasNext());
    }


    /* 5.2.1: Sends a Huffman-encoded string literal representation with padding longer than 7 bits */
    @Test
    void testHuffmanEncodedExtraPadding() throws Exception {
        HpackDecoder decoder = new HpackDecoder(4096, 8192);

        String encoded = "82868441" + "84" + "49509FFF";
        ByteBuffer buffer = ByteBuffer.wrap(TypeUtils.fromHexString(encoded));

        try {
            decoder.decode(buffer);
            fail();
        } catch (CompressionException ex) {
            assertTrue(ex.getMessage().contains("Bad termination"));
        }
    }


    /* 5.2.2: Sends a Huffman-encoded string literal representation padded by zero */
    @Test
    void testHuffmanEncodedZeroPadding() throws Exception {
        HpackDecoder decoder = new HpackDecoder(4096, 8192);

        String encoded = "82868441" + "83" + "495090";
        ByteBuffer buffer = ByteBuffer.wrap(TypeUtils.fromHexString(encoded));

        try {
            decoder.decode(buffer);
            fail();
        } catch (CompressionException ex) {
            assertTrue(ex.getMessage().contains("Incorrect padding"));
        }
    }


    /* 5.2.3: Sends a Huffman-encoded string literal representation containing the EOS symbol */
    @Test
    void testHuffmanEncodedWithEOS() throws Exception {
        HpackDecoder decoder = new HpackDecoder(4096, 8192);

        String encoded = "82868441" + "87" + "497FFFFFFF427F";
        ByteBuffer buffer = ByteBuffer.wrap(TypeUtils.fromHexString(encoded));

        try {
            decoder.decode(buffer);
            fail();
        } catch (CompressionException ex) {
            assertTrue(ex.getMessage().contains("EOS in content"));
        }
    }


    @Test
    void testHuffmanEncodedOneIncompleteOctet() throws Exception {
        HpackDecoder decoder = new HpackDecoder(4096, 8192);

        String encoded = "82868441" + "81" + "FE";
        ByteBuffer buffer = ByteBuffer.wrap(TypeUtils.fromHexString(encoded));

        try {
            decoder.decode(buffer);
            fail();
        } catch (CompressionException ex) {
            assertTrue(ex.getMessage().contains("Bad termination"));
        }
    }


    @Test
    void testHuffmanEncodedTwoIncompleteOctet() throws Exception {
        HpackDecoder decoder = new HpackDecoder(4096, 8192);

        String encoded = "82868441" + "82" + "FFFE";
        ByteBuffer buffer = ByteBuffer.wrap(TypeUtils.fromHexString(encoded));

        try {
            decoder.decode(buffer);
            fail();
        } catch (CompressionException ex) {
            assertTrue(ex.getMessage().contains("Bad termination"));
        }
    }
}
