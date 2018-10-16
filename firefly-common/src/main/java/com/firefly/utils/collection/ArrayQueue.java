package com.firefly.utils.collection;

import java.util.AbstractList;
import java.util.NoSuchElementException;
import java.util.Queue;

/* ------------------------------------------------------------ */

/**
 * Queue backed by circular array.
 * <p>
 * This partial Queue implementation (also with {@link #remove()} for stack
 * operation) is backed by a growable circular array.
 * </p>
 *
 * @param <E> the type of object the queue holds
 */
public class ArrayQueue<E> extends AbstractList<E> implements Queue<E> {
    public static final int DEFAULT_CAPACITY = 64;
    public static final int DEFAULT_GROWTH = 32;

    protected final Object _lock;
    protected final int _growCapacity;
    protected Object[] _elements;
    protected int _nextE;
    protected int _nextSlot;
    protected int _size;

    public ArrayQueue() {
        this(DEFAULT_CAPACITY, -1);
    }

    public ArrayQueue(Object lock) {
        this(DEFAULT_CAPACITY, -1, lock);
    }

    public ArrayQueue(int capacity) {
        this(capacity, -1);
    }

    public ArrayQueue(int initCapacity, int growBy) {
        this(initCapacity, growBy, null);
    }

    public ArrayQueue(int initCapacity, int growBy, Object lock) {
        _lock = lock == null ? this : lock;
        _growCapacity = growBy;
        _elements = new Object[initCapacity];
    }

    public Object lock() {
        return _lock;
    }

    public int getCapacity() {
        synchronized (_lock) {
            return _elements.length;
        }
    }

    /**
     * @return the next slot to be used
     */
    public int getNextSlotUnsafe() {
        return _nextSlot;
    }

    @Override
    public boolean add(E e) {
        if (!offer(e))
            throw new IllegalStateException("Full");
        return true;
    }

    public boolean offer(E e) {
        synchronized (_lock) {
            return enqueue(e);
        }
    }

    protected boolean enqueue(E e) {
        if (_size == _elements.length && !growUnsafe())
            return false;

        _size++;
        _elements[_nextSlot++] = e;
        if (_nextSlot == _elements.length)
            _nextSlot = 0;

        return true;
    }

    /**
     * Add without synchronization or bounds checking
     *
     * @param e the element to add
     * @see #add(Object)
     */
    public void addUnsafe(E e) {
        if (!enqueue(e))
            throw new IllegalStateException("Full");
    }

    public E element() {
        synchronized (_lock) {
            if (isEmpty())
                throw new NoSuchElementException();
            return at(_nextE);
        }
    }

    @SuppressWarnings("unchecked")
    private E at(int index) {
        return (E) _elements[index];
    }

    public E peek() {
        synchronized (_lock) {
            if (_size == 0)
                return null;
            return at(_nextE);
        }
    }

    public E peekUnsafe() {
        if (_size == 0)
            return null;
        return at(_nextE);
    }

    public E poll() {
        synchronized (_lock) {
            if (_size == 0)
                return null;
            return dequeue();
        }
    }

    public E pollUnsafe() {
        if (_size == 0)
            return null;
        return dequeue();
    }

    protected E dequeue() {
        E e = at(_nextE);
        _elements[_nextE] = null;
        _size--;
        if (++_nextE == _elements.length)
            _nextE = 0;
        return e;
    }

    public E remove() {
        synchronized (_lock) {
            if (_size == 0)
                throw new NoSuchElementException();
            return dequeue();
        }
    }

    @Override
    public void clear() {
        synchronized (_lock) {
            _size = 0;
            _nextE = 0;
            _nextSlot = 0;
        }
    }

    @Override
    public boolean isEmpty() {
        synchronized (_lock) {
            return _size == 0;
        }
    }

    @Override
    public int size() {
        synchronized (_lock) {
            return _size;
        }
    }

    public int sizeUnsafe() {
        return _size;
    }

    @Override
    public E get(int index) {
        synchronized (_lock) {
            if (index < 0 || index >= _size)
                throw new IndexOutOfBoundsException("!(" + 0 + "<" + index
                        + "<=" + _size + ")");
            return getUnsafe(index);
        }
    }

    /**
     * Get without synchronization or bounds checking.
     *
     * @param index index of the element to return
     * @return the element at the specified index
     * @see #get(int)
     */
    public E getUnsafe(int index) {
        int i = (_nextE + index) % _elements.length;
        return at(i);
    }

    @Override
    public E remove(int index) {
        synchronized (_lock) {
            if (index < 0 || index >= _size)
                throw new IndexOutOfBoundsException("!(" + 0 + "<" + index
                        + "<=" + _size + ")");

            int i = (_nextE + index) % _elements.length;
            E old = at(i);

            if (i < _nextSlot) {
                // 0                         _elements.length
                //       _nextE........._nextSlot
                System.arraycopy(_elements, i + 1, _elements, i, _nextSlot - i);
                _nextSlot--;
                _size--;
            } else {
                // 0                         _elements.length
                // ......_nextSlot   _nextE..........
                System.arraycopy(_elements, i + 1, _elements, i,
                        _elements.length - i - 1);
                if (_nextSlot > 0) {
                    _elements[_elements.length - 1] = _elements[0];
                    System.arraycopy(_elements, 1, _elements, 0, _nextSlot - 1);
                    _nextSlot--;
                } else
                    _nextSlot = _elements.length - 1;

                _size--;
            }

            return old;
        }
    }

    @Override
    public E set(int index, E element) {
        synchronized (_lock) {
            if (index < 0 || index >= _size)
                throw new IndexOutOfBoundsException("!(" + 0 + "<" + index
                        + "<=" + _size + ")");

            int i = _nextE + index;
            if (i >= _elements.length)
                i -= _elements.length;
            E old = at(i);
            _elements[i] = element;
            return old;
        }
    }

    @Override
    public void add(int index, E element) {
        synchronized (_lock) {
            if (index < 0 || index > _size)
                throw new IndexOutOfBoundsException("!(" + 0 + "<" + index
                        + "<=" + _size + ")");

            if (_size == _elements.length && !growUnsafe())
                throw new IllegalStateException("Full");

            if (index == _size) {
                add(element);
            } else {
                int i = _nextE + index;
                if (i >= _elements.length)
                    i -= _elements.length;

                _size++;
                _nextSlot++;
                if (_nextSlot == _elements.length)
                    _nextSlot = 0;

                if (i < _nextSlot) {
                    // 0                         _elements.length
                    //       _nextE.....i..._nextSlot
                    // 0                         _elements.length
                    // ..i..._nextSlot   _nextE..........
                    System.arraycopy(_elements, i, _elements, i + 1, _nextSlot
                            - i);
                    _elements[i] = element;
                } else {
                    // 0                         _elements.length
                    // ......_nextSlot   _nextE.....i....
                    if (_nextSlot > 0) {
                        System.arraycopy(_elements, 0, _elements, 1, _nextSlot);
                        _elements[0] = _elements[_elements.length - 1];
                    }

                    System.arraycopy(_elements, i, _elements, i + 1,
                            _elements.length - i - 1);
                    _elements[i] = element;
                }
            }
        }
    }

    protected void resizeUnsafe(int newCapacity) {
        newCapacity = Math.max(newCapacity, _size);
        Object[] elements = new Object[newCapacity];

        if (_size > 0) {
            if (_nextSlot > _nextE)
                System.arraycopy(_elements, _nextE, elements, 0, _size);
            else {
                int split = _elements.length - _nextE;
                System.arraycopy(_elements, _nextE, elements, 0, split);
                System.arraycopy(_elements, 0, elements, split, _nextSlot);
            }
        }
        _elements = elements;
        _nextE = 0;
        _nextSlot = _size;
    }

    protected boolean growUnsafe() {
        if (_growCapacity <= 0)
            return false;
        resizeUnsafe(_elements.length + _growCapacity);
        return true;
    }
}
