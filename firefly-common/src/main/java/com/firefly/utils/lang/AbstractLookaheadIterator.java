package com.firefly.utils.lang;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractLookaheadIterator<T> implements Iterator<T> {
    /**
     * The predetermined "next" object retrieved from the wrapped iterator, can be null.
     */
    protected T next;

    /**
     * Implement the hasNext policy of this iterator.
     * Returns true of the getNext() policy returns a new item.
     */
    public boolean hasNext() {
        if (next != null) {
            return true;
        }

        // we haven't done it already, so go find the next thing...
        return doesHaveNext() && getNext();

    }

    /**
     * by default we can return true, since our logic does not rely on hasNext() - it prefetches the next
     */
    protected boolean doesHaveNext() {
        return true;
    }

    /**
     * Fetch the next item
     *
     * @return false if the next item is null.
     */
    protected boolean getNext() {
        return (next = loadNext()) != null;
    }

    /**
     * Subclasses implement the 'get next item' functionality by implementing this method. Implementations return null when they have no more.
     *
     * @return Null if there is no next.
     */
    protected abstract T loadNext();

    /**
     * Return the next item from the wrapped iterator.
     */
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        T result = next;
        next = null;
        return result;
    }

    /**
     * Not implemented.
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
