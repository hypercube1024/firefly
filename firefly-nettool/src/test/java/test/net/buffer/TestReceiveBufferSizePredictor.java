package test.net.buffer;

import com.firefly.net.BufferSizePredictor;
import com.firefly.net.buffer.AdaptiveBufferSizePredictor;
import com.firefly.net.buffer.FixedBufferSizePredictor;
import com.firefly.utils.io.BufferUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.*;

public class TestReceiveBufferSizePredictor {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    @Test
    public void testAdaptive() {
        BufferSizePredictor receiveBufferSizePredictor = new AdaptiveBufferSizePredictor();
        receiveBufferSizePredictor.previousReceivedBufferSize(960);
        receiveBufferSizePredictor.previousReceivedBufferSize(960);
        receiveBufferSizePredictor.previousReceivedBufferSize(960);
        log.debug("current buf size: " + receiveBufferSizePredictor.nextBufferSize());
        Assert.assertThat(receiveBufferSizePredictor.nextBufferSize(), is(1024));

        receiveBufferSizePredictor.previousReceivedBufferSize(1025);
        receiveBufferSizePredictor.previousReceivedBufferSize(1300);
        log.debug("current buf size: " + receiveBufferSizePredictor.nextBufferSize());
        Assert.assertThat(receiveBufferSizePredictor.nextBufferSize(), greaterThan(1024));

        receiveBufferSizePredictor.previousReceivedBufferSize(4000);
        log.debug("current buf size: " + receiveBufferSizePredictor.nextBufferSize());
        Assert.assertThat(receiveBufferSizePredictor.nextBufferSize(), greaterThan(2000));

        receiveBufferSizePredictor.previousReceivedBufferSize(500);
        Assert.assertThat(receiveBufferSizePredictor.nextBufferSize(), greaterThan(2000));
        receiveBufferSizePredictor.previousReceivedBufferSize(1000);
        Assert.assertThat(receiveBufferSizePredictor.nextBufferSize(), lessThan(2000));
    }

    @Test
    public void testFix() {
        BufferSizePredictor receiveBufferSizePredictor = new FixedBufferSizePredictor(1024 * 8);
        receiveBufferSizePredictor.previousReceivedBufferSize(960);
        Assert.assertThat(receiveBufferSizePredictor.nextBufferSize(), is(1024 * 8));
        receiveBufferSizePredictor.previousReceivedBufferSize(40000);
        Assert.assertThat(receiveBufferSizePredictor.nextBufferSize(), is(1024 * 8));
    }

    public static void main(String[] args) {
        BufferSizePredictor receiveBufferSizePredictor = new AdaptiveBufferSizePredictor();
        receiveBufferSizePredictor.previousReceivedBufferSize(960);
        System.out.println(BufferUtils.normalizeBufferSize(receiveBufferSizePredictor.nextBufferSize()));
        receiveBufferSizePredictor.previousReceivedBufferSize(1024);
        System.out.println(BufferUtils.normalizeBufferSize(receiveBufferSizePredictor.nextBufferSize()));
        receiveBufferSizePredictor.previousReceivedBufferSize(1024);
        System.out.println(BufferUtils.normalizeBufferSize(receiveBufferSizePredictor.nextBufferSize()));
        receiveBufferSizePredictor.previousReceivedBufferSize(1024);
        System.out.println(BufferUtils.normalizeBufferSize(receiveBufferSizePredictor.nextBufferSize()));
        receiveBufferSizePredictor.previousReceivedBufferSize(1024);
        System.out.println(BufferUtils.normalizeBufferSize(receiveBufferSizePredictor.nextBufferSize()));
        receiveBufferSizePredictor.previousReceivedBufferSize(2048);
        System.out.println(BufferUtils.normalizeBufferSize(receiveBufferSizePredictor.nextBufferSize()));
        receiveBufferSizePredictor.previousReceivedBufferSize(2048);
        System.out.println(BufferUtils.normalizeBufferSize(receiveBufferSizePredictor.nextBufferSize()));
        receiveBufferSizePredictor.previousReceivedBufferSize(2048);
        System.out.println(BufferUtils.normalizeBufferSize(receiveBufferSizePredictor.nextBufferSize()));
        receiveBufferSizePredictor.previousReceivedBufferSize(2048);
        System.out.println(BufferUtils.normalizeBufferSize(receiveBufferSizePredictor.nextBufferSize()));
        receiveBufferSizePredictor.previousReceivedBufferSize(2048);
        System.out.println(BufferUtils.normalizeBufferSize(receiveBufferSizePredictor.nextBufferSize()));
        receiveBufferSizePredictor.previousReceivedBufferSize(3072);
        System.out.println(BufferUtils.normalizeBufferSize(receiveBufferSizePredictor.nextBufferSize()));

//		receiveBufferSizePredictor.previousReceiveBufferSize(4000);
//		System.out.println(receiveBufferSizePredictor.nextReceiveBufferSize());
//		receiveBufferSizePredictor.previousReceiveBufferSize(960);
//		System.out.println(receiveBufferSizePredictor.nextReceiveBufferSize());
//		receiveBufferSizePredictor.previousReceiveBufferSize(50);
//		receiveBufferSizePredictor.previousReceiveBufferSize(50);
//		receiveBufferSizePredictor.previousReceiveBufferSize(50);
//		System.out.println(receiveBufferSizePredictor.nextReceiveBufferSize());
    }

}
