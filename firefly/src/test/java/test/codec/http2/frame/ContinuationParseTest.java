package test.codec.http2.frame;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.encode.HeaderGenerator;
import com.firefly.codec.http2.encode.HeadersGenerator;
import com.firefly.codec.http2.frame.Flags;
import com.firefly.codec.http2.frame.FrameType;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.PriorityFrame;
import com.firefly.codec.http2.hpack.HpackEncoder;
import com.firefly.codec.http2.model.*;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ContinuationParseTest {
    @Test
    public void testParseOneByteAtATime() throws Exception {
        HeadersGenerator generator = new HeadersGenerator(new HeaderGenerator(), new HpackEncoder());

        final List<HeadersFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onHeaders(HeadersFrame frame) {
                frames.add(frame);
            }

            @Override
            public void onConnectionFailure(int error, String reason) {
                frames.add(new HeadersFrame(null, null, false));
            }
        }, 4096, 8192);

        // Iterate a few times to be sure the parser is properly reset.
        for (int i = 0; i < 2; ++i) {
            int streamId = 13;
            HttpFields fields = new HttpFields();
            fields.put("Accept", "text/html");
            fields.put("User-Agent", "Jetty");
            MetaData.Request metaData = new MetaData.Request("GET", HttpScheme.HTTP,
                    new HostPortHttpField("localhost:8080"), "/path", HttpVersion.HTTP_2, fields);

            List<ByteBuffer> byteBuffers = generator.generateHeaders(streamId, metaData, null, true);
            Assert.assertEquals(2, byteBuffers.size());

            ByteBuffer headersBody = byteBuffers.remove(1);
            int start = headersBody.position();
            int length = headersBody.remaining();
            int oneThird = length / 3;
            int lastThird = length - 2 * oneThird;

            // Adjust the length of the HEADERS frame.
            ByteBuffer headersHeader = byteBuffers.get(0);
            headersHeader.put(0, (byte) ((oneThird >>> 16) & 0xFF));
            headersHeader.put(1, (byte) ((oneThird >>> 8) & 0xFF));
            headersHeader.put(2, (byte) (oneThird & 0xFF));

            // Remove the END_HEADERS flag from the HEADERS header.
            headersHeader.put(4, (byte) (headersHeader.get(4) & ~Flags.END_HEADERS));

            // New HEADERS body.
            headersBody.position(start);
            headersBody.limit(start + oneThird);
            byteBuffers.add(headersBody.slice());

            // Split the rest of the HEADERS body into CONTINUATION frames.
            // First CONTINUATION header.
            byte[] continuationHeader1 = new byte[9];
            continuationHeader1[0] = (byte) ((oneThird >>> 16) & 0xFF);
            continuationHeader1[1] = (byte) ((oneThird >>> 8) & 0xFF);
            continuationHeader1[2] = (byte) (oneThird & 0xFF);
            continuationHeader1[3] = (byte) FrameType.CONTINUATION.getType();
            continuationHeader1[4] = Flags.NONE;
            continuationHeader1[5] = 0x00;
            continuationHeader1[6] = 0x00;
            continuationHeader1[7] = 0x00;
            continuationHeader1[8] = (byte) streamId;
            byteBuffers.add(ByteBuffer.wrap(continuationHeader1));
            // First CONTINUATION body.
            headersBody.position(start + oneThird);
            headersBody.limit(start + 2 * oneThird);
            byteBuffers.add(headersBody.slice());
            // Second CONTINUATION header.
            byte[] continuationHeader2 = new byte[9];
            continuationHeader2[0] = (byte) ((lastThird >>> 16) & 0xFF);
            continuationHeader2[1] = (byte) ((lastThird >>> 8) & 0xFF);
            continuationHeader2[2] = (byte) (lastThird & 0xFF);
            continuationHeader2[3] = (byte) FrameType.CONTINUATION.getType();
            continuationHeader2[4] = Flags.END_HEADERS;
            continuationHeader2[5] = 0x00;
            continuationHeader2[6] = 0x00;
            continuationHeader2[7] = 0x00;
            continuationHeader2[8] = (byte) streamId;
            byteBuffers.add(ByteBuffer.wrap(continuationHeader2));
            headersBody.position(start + 2 * oneThird);
            headersBody.limit(start + length);
            byteBuffers.add(headersBody.slice());

            frames.clear();
            for (ByteBuffer buffer : byteBuffers) {
                while (buffer.hasRemaining()) {
                    parser.parse(ByteBuffer.wrap(new byte[]{buffer.get()}));
                }
            }

            Assert.assertEquals(1, frames.size());
            HeadersFrame frame = frames.get(0);
            Assert.assertEquals(streamId, frame.getStreamId());
            Assert.assertTrue(frame.isEndStream());
            MetaData.Request request = (MetaData.Request) frame.getMetaData();
            Assert.assertEquals(metaData.getMethod(), request.getMethod());
            Assert.assertEquals(metaData.getURI(), request.getURI());
            for (int j = 0; j < fields.size(); ++j) {
                HttpField field = fields.getField(j);
                Assert.assertTrue(request.getFields().contains(field));
            }
            PriorityFrame priority = frame.getPriority();
            Assert.assertNull(priority);
        }
    }
}
