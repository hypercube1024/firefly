package com.firefly.utils.concurrent;

import com.firefly.utils.exception.CommonRuntimeException;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

/**
 * @author Pengtao Qiu
 */
public class BlockingTask {

    public static <T> T callInManagedBlock(final Supplier<T> supplier) {
        final SupplierManagedBlock<T> managedBlock = new SupplierManagedBlock<>(supplier);
        try {
            ForkJoinPool.managedBlock(managedBlock);
        } catch (InterruptedException e) {
            throw new CommonRuntimeException(e);
        }
        return managedBlock.getResult();
    }

    private static class SupplierManagedBlock<T> implements ForkJoinPool.ManagedBlocker {
        private final Supplier<T> supplier;
        private T result;
        private boolean done = false;

        private SupplierManagedBlock(final Supplier<T> supplier) {
            this.supplier = supplier;
        }

        @Override
        public boolean block() {
            result = supplier.get();
            done = true;
            return true;
        }

        @Override
        public boolean isReleasable() {
            return done;
        }

        public T getResult() {
            return result;
        }
    }
}
