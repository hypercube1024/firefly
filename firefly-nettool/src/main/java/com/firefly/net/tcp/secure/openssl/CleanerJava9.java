package com.firefly.net.tcp.secure.openssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * Provide a way to clean a ByteBuffer on Java9+.
 */
final class CleanerJava9 implements Cleaner {
    private static final Logger logger = LoggerFactory.getLogger("firefly-system");

    private static final Method INVOKE_CLEANER;

    static {
        final Method method;
        final Throwable error;
        if (PlatformDependent0.hasUnsafe()) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(1);
            Object maybeInvokeMethod;
            try {
                // See https://bugs.openjdk.java.net/browse/JDK-8171377
                Method m = PlatformDependent0.UNSAFE.getClass().getDeclaredMethod("invokeCleaner", ByteBuffer.class);
                m.invoke(PlatformDependent0.UNSAFE, buffer);
                maybeInvokeMethod = m;
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                maybeInvokeMethod = e;
            }
            if (maybeInvokeMethod instanceof Throwable) {
                method = null;
                error = (Throwable) maybeInvokeMethod;
            } else {
                method = (Method) maybeInvokeMethod;
                error = null;
            }
        } else {
            method = null;
            error = new UnsupportedOperationException("sun.misc.Unsafe unavailable");
        }
        if (error == null) {
            logger.debug("java.nio.ByteBuffer.cleaner(): available");
        } else {
            logger.debug("java.nio.ByteBuffer.cleaner(): unavailable", error);
        }
        INVOKE_CLEANER = method;
    }

    static boolean isSupported() {
        return INVOKE_CLEANER != null;
    }

    @Override
    public void freeDirectBuffer(ByteBuffer buffer) {
        try {
            INVOKE_CLEANER.invoke(PlatformDependent0.UNSAFE, buffer);
        } catch (Throwable cause) {
            PlatformDependent0.throwException(cause);
        }
    }
}
