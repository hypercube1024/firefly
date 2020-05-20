package com.fireflysource.net.http.common.codec;

import com.fireflysource.net.http.common.exception.NotSupportContentEncoding;
import com.fireflysource.net.http.common.model.ContentEncoding;

import java.io.*;
import java.util.zip.*;

abstract public class ContentEncoded {

    public static byte[] decode(byte[] content, ContentEncoding contentEncoding) throws IOException {
        return decode(content, contentEncoding, 512);
    }

    public static byte[] decode(byte[] content, ContentEncoding contentEncoding, int bufferSize) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[bufferSize];
        try (ByteArrayInputStream in = new ByteArrayInputStream(content);
             InputStream decodingInputStream = createDecodingInputStream(in, contentEncoding, bufferSize)
        ) {
            while (true) {
                int len = decodingInputStream.read(buffer);
                if (len < 0) break;

                if (len > 0) {
                    out.write(buffer, 0, len);
                }
            }
        }
        return out.toByteArray();
    }

    public static byte[] encode(byte[] content, ContentEncoding contentEncoding) throws IOException {
        return encode(content, contentEncoding, 512);
    }

    public static byte[] encode(byte[] content, ContentEncoding contentEncoding, int bufferSize) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (OutputStream encodingOutputStream = createEncodingOutputStream(out, contentEncoding, bufferSize)) {
            encodingOutputStream.write(content);
        }
        return out.toByteArray();
    }

    public static InputStream createDecodingInputStream(InputStream in, ContentEncoding contentEncoding, int bufferSize) throws IOException {
        switch (contentEncoding) {
            case GZIP:
                return new GZIPInputStream(in, bufferSize);
            case DEFLATE:
                return new InflaterInputStream(in, new Inflater(), bufferSize);
            default:
                throw new NotSupportContentEncoding("Not support the content encoding");
        }
    }

    public static OutputStream createEncodingOutputStream(OutputStream out, ContentEncoding contentEncoding, int bufferSize) throws IOException {
        switch (contentEncoding) {
            case GZIP:
                return new GZIPOutputStream(out, bufferSize);
            case DEFLATE:
                return new DeflaterOutputStream(out, new Deflater(), bufferSize);
            default:
                throw new NotSupportContentEncoding("Not support the content encoding");
        }
    }


}
