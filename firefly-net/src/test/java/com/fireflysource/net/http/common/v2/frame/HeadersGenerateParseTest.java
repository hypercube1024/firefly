package com.fireflysource.net.http.common.v2.frame;

import com.fireflysource.net.http.common.model.*;
import com.fireflysource.net.http.common.v2.decoder.Parser;
import com.fireflysource.net.http.common.v2.encoder.FrameBytes;
import com.fireflysource.net.http.common.v2.encoder.HeaderGenerator;
import com.fireflysource.net.http.common.v2.encoder.HeadersGenerator;
import com.fireflysource.net.http.common.v2.hpack.HpackEncoder;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class HeadersGenerateParseTest {

    @Test
    void testGenerateTrailer() {
        HeadersGenerator generator = new HeadersGenerator(new HeaderGenerator(), new HpackEncoder());

        int streamId = 13;
        HttpFields fields = new HttpFields();
        fields.put("trailer1", "foo");
        fields.put("trailer2", "bar");
        MetaData.Request metaData = new MetaData.Request(fields);
        metaData.setOnlyTrailer(true);

        final List<HeadersFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onHeaders(HeadersFrame frame) {
                frames.add(frame);
            }
        }, 4096, 8192);
        parser.init(UnaryOperator.identity());

        FrameBytes frameBytes = generator.generateHeaders(streamId, metaData, null, true);

        frames.clear();
        for (ByteBuffer buffer : frameBytes.getByteBuffers()) {
            while (buffer.hasRemaining()) {
                parser.parse(buffer);
            }
        }

        assertEquals(1, frames.size());
        HeadersFrame frame = frames.get(0);
        assertEquals(streamId, frame.getStreamId());
        assertTrue(frame.isEndStream());
        assertEquals("foo", frame.getMetaData().getFields().get("trailer1"));
        assertEquals("bar", frame.getMetaData().getFields().get("trailer2"));
    }

    @Test
    void testGenerateParse() {
        HeadersGenerator generator = new HeadersGenerator(new HeaderGenerator(), new HpackEncoder());

        int streamId = 13;
        HttpFields fields = new HttpFields();
        fields.put("Accept", "text/html");
        fields.put("User-Agent", "Firefly");
        MetaData.Request metaData = new MetaData.Request("GET", HttpScheme.HTTP, new HostPortHttpField("localhost:8080"), "/path", HttpVersion.HTTP_2, fields);

        final List<HeadersFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onHeaders(HeadersFrame frame) {
                frames.add(frame);
            }
        }, 4096, 8192);
        parser.init(UnaryOperator.identity());

        // Iterate a few times to be sure generator and parser are properly reset.
        for (int i = 0; i < 2; ++i) {
            PriorityFrame priorityFrame = new PriorityFrame(streamId, 3 * streamId, 200, true);
            FrameBytes frameBytes = generator.generateHeaders(streamId, metaData, priorityFrame, true);

            frames.clear();
            for (ByteBuffer buffer : frameBytes.getByteBuffers()) {
                while (buffer.hasRemaining()) {
                    parser.parse(buffer);
                }
            }

            assertEquals(1, frames.size());
            HeadersFrame frame = frames.get(0);
            assertEquals(streamId, frame.getStreamId());
            assertTrue(frame.isEndStream());
            MetaData.Request request = (MetaData.Request) frame.getMetaData();
            assertEquals(metaData.getMethod(), request.getMethod());
            assertEquals(metaData.getURI(), request.getURI());
            for (int j = 0; j < fields.size(); ++j) {
                HttpField field = fields.getField(j);
                assertTrue(request.getFields().contains(field));
            }
            PriorityFrame priority = frame.getPriority();
            assertNotNull(priority);
            assertEquals(priorityFrame.getStreamId(), priority.getStreamId());
            assertEquals(priorityFrame.getParentStreamId(), priority.getParentStreamId());
            assertEquals(priorityFrame.getWeight(), priority.getWeight());
            assertEquals(priorityFrame.isExclusive(), priority.isExclusive());
        }
    }

    @Test
    void testGenerateParseOneByteAtATime() {
        HeadersGenerator generator = new HeadersGenerator(new HeaderGenerator(), new HpackEncoder());

        final List<HeadersFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onHeaders(HeadersFrame frame) {
                frames.add(frame);
            }
        }, 4096, 8192);
        parser.init(UnaryOperator.identity());

        // Iterate a few times to be sure generator and parser are properly reset.
        for (int i = 0; i < 2; ++i) {
            int streamId = 13;
            HttpFields fields = new HttpFields();
            fields.put("Accept", "text/html");
            fields.put("User-Agent", "Firefly");
            MetaData.Request metaData = new MetaData.Request("GET", HttpScheme.HTTP, new HostPortHttpField("localhost:8080"), "/path", HttpVersion.HTTP_2, fields);

            PriorityFrame priorityFrame = new PriorityFrame(streamId, 3 * streamId, 200, true);
            FrameBytes frameBytes = generator.generateHeaders(streamId, metaData, priorityFrame, true);

            frames.clear();
            for (ByteBuffer buffer : frameBytes.getByteBuffers()) {
                buffer = buffer.slice();
                while (buffer.hasRemaining()) {
                    parser.parse(ByteBuffer.wrap(new byte[]{buffer.get()}));
                }
            }

            assertEquals(1, frames.size());
            HeadersFrame frame = frames.get(0);
            assertEquals(streamId, frame.getStreamId());
            assertTrue(frame.isEndStream());
            MetaData.Request request = (MetaData.Request) frame.getMetaData();
            assertEquals(metaData.getMethod(), request.getMethod());
            assertEquals(metaData.getURI(), request.getURI());
            for (int j = 0; j < fields.size(); ++j) {
                HttpField field = fields.getField(j);
                assertTrue(request.getFields().contains(field));
            }
            PriorityFrame priority = frame.getPriority();
            assertNotNull(priority);
            assertEquals(priorityFrame.getStreamId(), priority.getStreamId());
            assertEquals(priorityFrame.getParentStreamId(), priority.getParentStreamId());
            assertEquals(priorityFrame.getWeight(), priority.getWeight());
            assertEquals(priorityFrame.isExclusive(), priority.isExclusive());
        }
    }
}
