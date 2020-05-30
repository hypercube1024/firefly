package com.fireflysource.net.websocket.extension.compress;


import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.net.websocket.exception.MessageTooLargeException;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

public class ByteAccumulatorTest {

    @Test
    public void testCopyNormal() {
        ByteAccumulator accumulator = new ByteAccumulator(10_000);

        byte[] hello = "Hello".getBytes(UTF_8);
        byte[] space = " ".getBytes(UTF_8);
        byte[] world = "World".getBytes(UTF_8);

        accumulator.copyChunk(hello, 0, hello.length);
        accumulator.copyChunk(space, 0, space.length);
        accumulator.copyChunk(world, 0, world.length);

        assertEquals(hello.length + space.length + world.length, accumulator.getLength());

        ByteBuffer out = ByteBuffer.allocate(200);
        accumulator.transferTo(out);
        String result = BufferUtils.toUTF8String(out);
        assertEquals("Hello World", result);
    }

    @Test
    public void testTransferToNotEnoughSpace() {
        ByteAccumulator accumulator = new ByteAccumulator(10_000);

        byte[] hello = "Hello".getBytes(UTF_8);
        byte[] space = " ".getBytes(UTF_8);
        byte[] world = "World".getBytes(UTF_8);

        accumulator.copyChunk(hello, 0, hello.length);
        accumulator.copyChunk(space, 0, space.length);
        accumulator.copyChunk(world, 0, world.length);

        int length = hello.length + space.length + world.length;
        assertEquals(length, accumulator.getLength());

        ByteBuffer out = ByteBuffer.allocate(length - 2); // intentionally too small ByteBuffer
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> accumulator.transferTo(out));
        assertTrue(e.getMessage().contains("Not enough space in ByteBuffer"));
    }

    @Test
    public void testCopyChunkNotEnoughSpace() {
        byte[] hello = "Hello".getBytes(UTF_8);
        byte[] space = " ".getBytes(UTF_8);
        byte[] world = "World".getBytes(UTF_8);

        int length = hello.length + space.length + world.length;
        ByteAccumulator accumulator = new ByteAccumulator(length - 2); // intentionally too small of a max

        accumulator.copyChunk(hello, 0, hello.length);
        accumulator.copyChunk(space, 0, space.length);

        MessageTooLargeException e = assertThrows(MessageTooLargeException.class, () -> accumulator.copyChunk(world, 0, world.length));
        System.out.println(e.getMessage());
        assertTrue(e.getMessage().contains("too large for configured max"));
    }
}
