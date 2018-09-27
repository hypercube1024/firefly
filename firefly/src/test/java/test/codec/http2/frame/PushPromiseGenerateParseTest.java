package test.codec.http2.frame;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.encode.HeaderGenerator;
import com.firefly.codec.http2.encode.PushPromiseGenerator;
import com.firefly.codec.http2.frame.PushPromiseFrame;
import com.firefly.codec.http2.hpack.HpackEncoder;
import com.firefly.codec.http2.model.*;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class PushPromiseGenerateParseTest {

    @Test
    public void testGenerateParse() throws Exception {
        PushPromiseGenerator generator = new PushPromiseGenerator(new HeaderGenerator(), new HpackEncoder());

        final List<PushPromiseFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onPushPromise(PushPromiseFrame frame) {
                frames.add(frame);
            }
        }, 4096, 8192);

        int streamId = 13;
        int promisedStreamId = 17;
        HttpFields fields = new HttpFields();
        fields.put("Accept", "text/html");
        fields.put("User-Agent", "Jetty");
        MetaData.Request metaData = new MetaData.Request("GET", HttpScheme.HTTP,
                new HostPortHttpField("localhost:8080"), "/path", HttpVersion.HTTP_2, fields);

        // Iterate a few times to be sure generator and parser are properly
        // reset.
        for (int i = 0; i < 2; ++i) {
            List<ByteBuffer> list = generator.generatePushPromise(streamId, promisedStreamId, metaData);

            frames.clear();
            for (ByteBuffer buffer : list) {
                while (buffer.hasRemaining()) {
                    parser.parse(buffer);
                }
            }

            Assert.assertEquals(1, frames.size());
            PushPromiseFrame frame = frames.get(0);
            Assert.assertEquals(streamId, frame.getStreamId());
            Assert.assertEquals(promisedStreamId, frame.getPromisedStreamId());
            MetaData.Request request = (MetaData.Request) frame.getMetaData();
            Assert.assertEquals(metaData.getMethod(), request.getMethod());
            Assert.assertEquals(metaData.getURI(), request.getURI());
            for (int j = 0; j < fields.size(); ++j) {
                HttpField field = fields.getField(j);
                Assert.assertTrue(request.getFields().contains(field));
            }
        }
    }

    @Test
    public void testGenerateParseOneByteAtATime() throws Exception {
        PushPromiseGenerator generator = new PushPromiseGenerator(new HeaderGenerator(), new HpackEncoder());

        final List<PushPromiseFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onPushPromise(PushPromiseFrame frame) {
                frames.add(frame);
            }
        }, 4096, 8192);

        int streamId = 13;
        int promisedStreamId = 17;
        HttpFields fields = new HttpFields();
        fields.put("Accept", "text/html");
        fields.put("User-Agent", "Jetty");
        MetaData.Request metaData = new MetaData.Request("GET", HttpScheme.HTTP,
                new HostPortHttpField("localhost:8080"), "/path", HttpVersion.HTTP_2, fields);

        // Iterate a few times to be sure generator and parser are properly
        // reset.
        for (int i = 0; i < 2; ++i) {
            List<ByteBuffer> list = generator.generatePushPromise(streamId, promisedStreamId, metaData);

            frames.clear();
            for (ByteBuffer buffer : list) {
                while (buffer.hasRemaining()) {
                    parser.parse(ByteBuffer.wrap(new byte[]{buffer.get()}));
                }
            }

            Assert.assertEquals(1, frames.size());
            PushPromiseFrame frame = frames.get(0);
            Assert.assertEquals(streamId, frame.getStreamId());
            Assert.assertEquals(promisedStreamId, frame.getPromisedStreamId());
            MetaData.Request request = (MetaData.Request) frame.getMetaData();
            Assert.assertEquals(metaData.getMethod(), request.getMethod());
            Assert.assertEquals(metaData.getURI(), request.getURI());
            for (int j = 0; j < fields.size(); ++j) {
                HttpField field = fields.getField(j);
                Assert.assertTrue(request.getFields().contains(field));
            }
        }
    }
}
