package test.net.tcp.codec.flex;

import com.firefly.net.tcp.codec.flex.stream.Stream;
import org.junit.Assert;
import org.junit.Test;

import static com.firefly.net.tcp.codec.flex.stream.impl.StreamStateTransferMap.Op;
import static com.firefly.net.tcp.codec.flex.stream.impl.StreamStateTransferMap.getNextState;
import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestStreamStateTransferMap {

    @Test
    public void test() {
        Stream.State next = getNextState(Stream.State.OPEN, Op.SEND_ES);
        Assert.assertThat(next, is(Stream.State.LOCALLY_CLOSED));
        System.out.println(next);

        next = getNextState(Stream.State.LOCALLY_CLOSED, Op.RECV_ES);
        Assert.assertThat(next, is(Stream.State.CLOSED));
        System.out.println(next);

        next = getNextState(Stream.State.OPEN, Op.RECV_ES);
        Assert.assertThat(next, is(Stream.State.REMOTELY_CLOSED));
        System.out.println(next);

        next = getNextState(Stream.State.REMOTELY_CLOSED, Op.SEND_ES);
        Assert.assertThat(next, is(Stream.State.CLOSED));
        System.out.println(next);
    }
}
