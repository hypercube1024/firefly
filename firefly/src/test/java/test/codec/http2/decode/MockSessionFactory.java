package test.codec.http2.decode;

import com.firefly.net.ByteBufferArrayOutputEntry;
import com.firefly.net.OutputEntry;
import com.firefly.net.Session;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.io.BufferUtils;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;

import static org.mockito.Mockito.*;

/**
 * @author Pengtao Qiu
 */
public class MockSessionFactory {

    public final LinkedList<ByteBuffer> output = new LinkedList<>();

    public Session create() {
        return mock(AbstractMockSession.class, withSettings().useConstructor(output).defaultAnswer(CALLS_REAL_METHODS));
    }

    abstract public static class AbstractMockSession implements Session {
        public Object attachment;
        public boolean isOpen = true;
        public final LinkedList<ByteBuffer> outboundData;

        public AbstractMockSession(LinkedList<ByteBuffer> outboundData) {
            this.outboundData = outboundData;
        }

        @Override
        public void attachObject(Object attachment) {
            this.attachment = attachment;
        }

        @Override
        public Object getAttachment() {
            return attachment;
        }

        @Override
        public void encode(Object message) {
            if (message instanceof ByteBufferArrayOutputEntry) {
                ByteBufferArrayOutputEntry outputEntry = (ByteBufferArrayOutputEntry) message;
                write(outputEntry);
            }
        }

        @Override
        public boolean isOpen() {
            return isOpen;
        }

        @Override
        public void write(ByteBuffer byteBuffer, Callback callback) {
            outboundData.offer(byteBuffer);
            byteBuffer.flip();
            callback.succeeded();
        }

        @Override
        public void write(ByteBuffer[] buffers, Callback callback) {
            for (ByteBuffer buffer : buffers) {
                outboundData.offer(buffer);
                buffer.flip();
            }
            callback.succeeded();

        }

        @Override
        public void write(Collection<ByteBuffer> buffers, Callback callback) {
            write(buffers.toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY), callback);
        }

        @Override
        public void write(OutputEntry<?> entry) {
            ByteBufferArrayOutputEntry outputEntry = (ByteBufferArrayOutputEntry) entry;
            write(outputEntry.getData(), outputEntry.getCallback());
        }

        @Override
        public void closeNow() {
            isOpen = false;
        }
    }
}
