package com.fireflysource.common.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

abstract public class CompletableFutures {
    /**
     * Returns a new stage that, when this stage completes
     * exceptionally, is executed with this stage's exception as the
     * argument to the supplied function.  Otherwise, if this stage
     * completes normally, then the returned stage also completes
     * normally with the same value.
     *
     * <p>This differs from
     * {@link java.util.concurrent.CompletionStage#exceptionally(java.util.function.Function)}
     * in that the function should return a {@link java.util.concurrent.CompletionStage} rather than
     * the value directly.
     *
     * @param stage the {@link CompletionStage} to compose
     * @param fn    the function computes the value of the
     *              returned {@link CompletionStage} if this stage completed
     *              exceptionally
     * @param <T>   the type of the input stage's value.
     * @return the new {@link CompletionStage}
     */
    public static <T> CompletionStage<T> exceptionallyCompose(CompletionStage<T> stage, Function<Throwable, ? extends CompletionStage<T>> fn) {
        return dereference(wrap(stage).exceptionally(fn));
    }

    public static <T> CompletionStage<T> dereference(CompletionStage<? extends CompletionStage<T>> stage) {
        return stage.thenCompose(Function.identity());
    }

    private static <T> CompletionStage<CompletionStage<T>> wrap(CompletionStage<T> future) {
        return future.thenApply(CompletableFuture::completedFuture);
    }

    public static <T> CompletableFuture<T> completeExceptionally(Throwable t) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(t);
        return future;
    }
}
