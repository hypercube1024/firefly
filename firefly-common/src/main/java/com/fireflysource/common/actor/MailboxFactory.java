package com.fireflysource.common.actor;

import org.jctools.queues.MpscLinkedQueue;
import org.jctools.queues.SpscLinkedQueue;

import java.util.Queue;

abstract public class MailboxFactory {

    public static <T> Mailbox<T, AbstractActor.SystemMessage> createMailbox() {
        return new AbstractActor.MailboxImpl<>(new MpscLinkedQueue<>(), new SpscLinkedQueue<>());
    }

    public static <T> Mailbox<T, AbstractActor.SystemMessage> createMailbox(Queue<T> userMessageQueue, Queue<AbstractActor.SystemMessage> systemMessageQueue) {
        return new AbstractActor.MailboxImpl<>(userMessageQueue, systemMessageQueue);
    }
}
