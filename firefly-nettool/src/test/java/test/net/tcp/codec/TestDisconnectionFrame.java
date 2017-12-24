package test.net.tcp.codec;

import com.firefly.net.tcp.codec.decode.FrameParser;
import com.firefly.net.tcp.codec.encode.FrameGenerator;
import com.firefly.net.tcp.codec.protocol.DisconnectionFrame;
import com.firefly.net.tcp.codec.protocol.ErrorCode;
import com.firefly.net.tcp.codec.protocol.FrameType;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestDisconnectionFrame {

    @Test
    public void test() {
        DisconnectionFrame disconnectionFrame = new DisconnectionFrame(ErrorCode.INTERNAL.getValue(),
                ErrorCode.INTERNAL.getDescription().getBytes(StandardCharsets.UTF_8));
        ByteBuffer buffer = FrameGenerator.generate(disconnectionFrame);

        FrameParser parser = new FrameParser();
        parser.complete(frame -> {
            Assert.assertThat(frame.getType(), is(FrameType.DISCONNECTION));
            DisconnectionFrame disconnection = (DisconnectionFrame) frame;

            Assert.assertThat(disconnection.getCode(), is(ErrorCode.INTERNAL.getValue()));
            Assert.assertThat(new String(disconnection.getData(), StandardCharsets.UTF_8),
                    is(ErrorCode.INTERNAL.getDescription()));

            System.out.println(disconnection);
        }).exception(Throwable::printStackTrace);
        parser.receive(buffer);

    }
}
