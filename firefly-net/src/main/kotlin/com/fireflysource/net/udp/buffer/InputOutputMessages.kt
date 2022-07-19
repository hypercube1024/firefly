package com.fireflysource.net.udp.buffer

import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

sealed interface InputMessage

@JvmInline
value class InputBuffer(val bufferFuture: CompletableFuture<ByteBuffer>) : InputMessage

object CancelSelectionKey : InputMessage

object InvalidSelectionKey : InputMessage