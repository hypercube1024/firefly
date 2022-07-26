package com.fireflysource.net.udp.buffer

import com.fireflysource.net.udp.UdpConnection
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.CompletableFuture

sealed interface InputMessage

@JvmInline
value class InputBuffer(val bufferFuture: CompletableFuture<ByteBuffer>) : InputMessage

object CancelSelectionKey : InputMessage

object InvalidSelectionKey : InputMessage

object ReadComplete : InputMessage


sealed interface NioWorkerMessage

data class RegisterRead(
    val datagramChannel: DatagramChannel,
    val udpConnection: UdpConnection,
    val future: CompletableFuture<Void>
) : NioWorkerMessage