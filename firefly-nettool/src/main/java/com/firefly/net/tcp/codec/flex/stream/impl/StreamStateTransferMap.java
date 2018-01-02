package com.firefly.net.tcp.codec.flex.stream.impl;

import com.firefly.net.tcp.codec.flex.stream.Stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.firefly.net.tcp.codec.flex.stream.impl.StreamStateTransferMap.Op.RECV_ES;
import static com.firefly.net.tcp.codec.flex.stream.impl.StreamStateTransferMap.Op.SEND_ES;

/**
 * @author Pengtao Qiu
 */
public class StreamStateTransferMap {

    private static final List<Node> rules = new ArrayList<>();

    static {
        Node newStream = new Node();
        newStream.op = SEND_ES;
        newStream.from = Stream.State.OPEN;
        newStream.to = Stream.State.LOCALLY_CLOSED;
        rules.add(newStream);

        newStream = new Node();
        newStream.op = RECV_ES;
        newStream.from = Stream.State.OPEN;
        newStream.to = Stream.State.REMOTELY_CLOSED;
        rules.add(newStream);

        Node locallyClosedStream = new Node();
        locallyClosedStream.op = RECV_ES;
        locallyClosedStream.from = Stream.State.LOCALLY_CLOSED;
        locallyClosedStream.to = Stream.State.CLOSED;
        rules.add(locallyClosedStream);

        Node remotelyClosed = new Node();
        remotelyClosed.op = SEND_ES;
        remotelyClosed.from = Stream.State.REMOTELY_CLOSED;
        remotelyClosed.to = Stream.State.CLOSED;
        rules.add(remotelyClosed);
    }

    public static Stream.State getNextState(Stream.State currentState, Op op) {
        return rules.stream()
                    .filter(node -> node.op == op && node.from == currentState)
                    .map(node -> node.to)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("The stream state error. " + currentState + ", " + op));
    }

    public enum Op {
        SEND_ES, RECV_ES
    }

    private static class Node {
        Op op;
        Stream.State from;
        Stream.State to;
    }


}
