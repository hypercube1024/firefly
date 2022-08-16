package com.fireflysource.net.udp.buffer

import com.fireflysource.net.udp.UdpConnection
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey
import java.util.concurrent.CompletableFuture

sealed interface InputMessage

@JvmInline
value class InputBuffer(val bufferFuture: CompletableFuture<ByteBuffer>) : InputMessage

object CancelSelectionKey : InputMessage

object InvalidSelectionKey : InputMessage

object UnregisterRead : InputMessage
@JvmInline
value class ReadComplete(val buffer: ByteBuffer): InputMessage

sealed interface NioWorkerMessage

data class RegisterRead(
    val datagramChannel: DatagramChannel,
    val udpConnection: UdpConnection,
    val future: CompletableFuture<SelectionKey>
) : NioWorkerMessage