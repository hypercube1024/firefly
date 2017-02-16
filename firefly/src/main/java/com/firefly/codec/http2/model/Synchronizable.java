package com.firefly.codec.http2.model;

/**
 * <p>Implementations of this interface expose a lock object
 * via {@link #getLock()} so that callers can synchronize
 * externally on that lock:</p>
 * <pre>
 * if (iterator instanceof Synchronizable)
 * {
 *     Object element = null;
 *     synchronized (((Synchronizable)iterator).getLock())
 *     {
 *         if (iterator.hasNext())
 *             element = iterator.next();
 *     }
 * }
 * </pre>
 * <p>In the example above, the calls to {@code hasNext()} and
 * {@code next()} are performed "atomically".</p>
 */
public interface Synchronizable {
    /**
     * @return the lock object to synchronize on
     */
    Object getLock();
}
