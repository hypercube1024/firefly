package com.fireflysource.common.io;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

public interface AsyncCloseable extends Closeable {

    CompletableFuture<Void> closeAsync();

}
