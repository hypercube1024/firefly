package com.firefly.utils.collection;

/***************************************************************************
 * File: HashedArrayTree.java
 * Author: Keith Schwarz (htiek@cs.stanford.edu)
 *
 * An implementation of the List abstraction backed by a hashed array tree
 * (HAT), a data structure supporting amortized O(1) lookup, append, and
 * last-element removal.  In this sense it is akin to a standard dynamic
 * array implementation.  However, a hashed array tree also has the advantage
 * that its memory overhead is only O(sqrt(n)) rather than the typical O(n)
 * found in dynamic arrays and their variants.
 *
 * Internally, the hashed array tree is implemented as an array of pointers
 * that optionally point to an array of elements.  The topmost array and each
 * element always have the same size, which is always a power of two.  In
 * this sense, the hashed array tree is essentially a two-dimensional array
 * of elements.  However, the advantage of the hashed array tree is that the
 * topmost array pointers are all initially null and only filled in when space
 * is needed.  This means that the maximum overhead of the structure is the
 * size of the topmost array, plus the number of unused elements in the current
 * block.  Here is one sample HAT:
 *
 *           [ ] [ ] [ ] [ ]
 *            |   |   |
 *            v   v   v
 *           [0] [4] [8]
 *           [1] [5] [9]
 *           [2] [6] [ ]
 *           [3] [7] [ ]
 *
 * Here, the topmost array of pointers has three pointers in use, each of which
 * point to an array of the corresponding number of elements.
 *
 * Whenever an element is added to a hashed array tree, one of three cases
 * must hold:
 *
 * 1. There is extra space at the end of the final subarray (for example, in
 *    the top picture).  In that case, the element is added to that position.
 * 2. The final subarray is full, but space for another subarray exists in the
 *    topmost array.  In that case, a new array is allocated and the element
 *    is added to that array.
 * 3. The final subarray is full and no new arrays remain open.  In that case,
 *    since the topmost array has size 2^n and each array has size 2^n, there
 *    must be a total of 2^(2n) elements in the hashed array tree.  We
 *    next double the size of the topmost array to 2^(n + 1), then allocate
 *    2^(n - 1) subarrays of size 2^(n + 1) elements for a total of 2^(2n)
 *    elements' worth of space.  The elements from the old HAT are then copied
 *    over, a new array is allocated for the new element, and it is added as
 *    the first element of that array.
 *
 * Let's now talk about the performance and memory usage of this structure.
 * First, we note that we can perform lookups in O(1), assuming that the
 * machine is transdichotomous (meaning that a single machine word can hold
 * the size of any array).  This can be done by breaking the input index in
 * half, then using the first half to choose which array to look into (the
 * "hashed" part of HAT) and the second half to choose which index to select.
 * This trick is similar to the trick used to represent a two-dimensional
 * array using a linear structure.
 *
 * Next, let's think about how much time it takes to do an append operation.
 * Each append when space remains takes O(1) to look up the proper position
 * in the hashed array tree for writing, but appends can be much more expensive
 * when the HAT needs to be resized.  Fortunately, this does not happen very
 * often.  Whenever the HAT doubles in size, its capacity grows from 2^(2n) to
 * 2^(2n + 2), meaning that four times as many elements must be inserted before
 * the next copy operation.  If we define a potential function as twice the 
 * number of filled-in elements in the HAT above half capacity (i.e. the
 * number of elements in the arrays in the second half), then we can prove an
 * amortized O(1) time for append.  Consider any series of appends.  If the
 * append does not expand the HAT, then it takes O(1) time and increases the
 * potential by 1/2.  If the append does expand the HAT, and the HAT's topmost
 * array has size 2^n, then there must be 2^(n-1) elements in the latter half
 * of the HAT, so the potential is 2^(2n).  The time required to move each
 * of the 2^(2n) elements is 2^(2n), and in the new HAT there are no elements
 * in the latter half of the array.  Consequently, the new potential is zero.
 * The actual time required to perform the append is thus 2^2n + O(1), and
 * the decrease in potential is -2^2n, so the amortized cost is O(1) as
 * expected.
 *
 * Last, let's talk about the cost to do a remove.  This is similar to the 
 * append case - we delete the last element of the last array, removing the
 * array from the topmost array if it becomes empty.  We also compact the
 * HAT if it becomes too sparse by shrinking from a HAT of size 2^(2n + 2) to
 * a HAT of size 2^(2n) if the HAT becomes one-eighth full.  A similar
 * potential method can be used to show that this operation runs in amortized
 * O(1).
 *
 * Finally, let's consider the memory overhead of the HAT.  For any HAT of
 * topmost array size 8 or more, since the HAT is always at least one-eighth
 * full, there must be at least one full array.  This array has size equal to
 * the size of the topmost array (call this k), and so if every array were to
 * be filled in to capacity, there would be a total of k arrays of size k,
 * for a total of k^2 elements.  Of this capacity, we know that at least an
 * eighth of them are filled in, so k^2 must be at most 8n, and so 
 * k = O(sqrt(n)).  To finish the analysis, the overhead of the structure is
 * at most the overhead of this top-level array, plus potentially k - 1
 * unused elements in some array.  This is a total of O(k) = O(sqrt(n))
 * overhead, which is what we originally desired.
 */

import java.util.*; // For AbstractList

@SuppressWarnings("unchecked")
public final class HashedArrayTree<T> extends AbstractList<T> {
    /* To simplify the implementation, we enforce that the size of the topmost
     * array never drops below 2.  This prevents weirdness when we try to
     * allocate 2^(n-1) arrays during a doubling and find that n = 0.
     */
    private static final int kMinArraySize = 2;

    /* The topmost array of elements; initially of size two. */
    private T[][] mArrays = (T[][]) new Object[kMinArraySize][];

    /* Number of elements, initially zero since the HAT is created empty. */
    private int mSize = 0;

    /* A constant containing lg2 of the topmost array size.  This enables some
     * cute bit-twiddling tricks to improve efficiency.
     */
    private int mLgSize = 1;

    /**
     * Returns the number of elements in the HashedArrayTree.
     *
     * @return The number of elements in the HashedArrayTree.
     */
    @Override
    public int size() {
        return mSize;
    }

    /**
     * Adds a new element to the HashedArrayTree.
     *
     * @param elem The element to add.
     * @return true
     */
    @Override
    public boolean add(T elem) {
        /* First, check if we're completely out of space.  If so, do a resize
         * to ensure we do indeed have room.
         */
        if (size() == mArrays.length * mArrays.length)
            grow();

        /* Compute the (arr, index) pair for the next position.  The next
         * position is at the location indicated by size(), but we know that
         * space exists from the previous call.
         */
        final int offset = computeOffset(size());
        final int index = computeIndex(size());

        /* Check if an array exists here.  If not, make one up. */
        if (mArrays[offset] == null)
            mArrays[offset] = (T[]) new Object[mArrays.length];

        /* Write the element to its location. */
        mArrays[offset][index] = elem;

        /* Update the element count. */
        ++mSize;

        /* Per the Collections contract, return true to signal a successful
         * add.
         */
        return true;
    }

    /**
     * Sets the element at the specified position to the indicated value.
     * If the index is out of bounds, throws an IndexOutOfBounds exception.
     *
     * @param index The index at which to set the value.
     * @param elem  The element to store at that position.
     * @return The value initially at that location.
     * @throws IndexOutOfBoundsException If index is invalid.
     */
    @Override
    public T set(int index, T elem) {
        /* Find out where to look. */
        final int offset = computeOffset(index);
        final int arrIndex = computeIndex(index);

        /* Cache the value there and write the new one. */
        T result = mArrays[offset][arrIndex];
        mArrays[offset][arrIndex] = elem;

        /* Hand back the old value. */
        return result;
    }

    /**
     * Returns the value of the element at the specified position.
     *
     * @param index The index at which to query.
     * @return The value of the element at that position.
     * @throws IndexOutOfBoundsException If the index is invalid.
     */
    @Override
    public T get(int index) {
        /* Check that this is a valid index. */
        if (index < 0 || index >= size())
            throw new IndexOutOfBoundsException("Index " + index + ", size " + size());

        /* Look up the element. */
        return mArrays[computeOffset(index)][computeIndex(index)];
    }

    /**
     * Adds the specified element at the position just before the specified
     * index.
     *
     * @param index The index just before which to insert.
     * @param elem  The value to insert
     * @throws IndexOutOfBoundsException if the index is invalid.
     */
    @Override
    public void add(int index, T elem) {
        /* Confirm the validity of the index. */
        if (index < 0 || index >= size())
            throw new IndexOutOfBoundsException("Index " + index + ", size " + size());

        /* Add a dummy element to ensure that everything resizes correctly.
         * There's no reason to repeat the logic.
         */
        add(null);

        /* Next, we need to shuffle down every element that appears after
         * the inserted element.  We'll do this using our own public interface.
         */
        for (int i = size(); i > index; ++i)
            set(i, get(i - 1));

        /* Finally, write the element. */
        set(index, elem);
    }

    /**
     * Removes the element at the specified position from the HashedArrayTree.
     *
     * @param index The index of the element to remove.
     * @return The value of the element at that position.
     * @throws IndexOutOfBoundsException If the index is invalid.
     */
    @Override
    public T remove(int index) {
        /* Cache the value at the indicated position; this also does the bounds
         * check.
         */
        T result = get(index);

        /* Use a naive shuffle-down algorithm to reposition elements after
         * the removed one.
         */
        for (int i = index + 1; i < size(); ++i)
            set(i - 1, get(i));

        /* Clobber the last element to play nicely with the garbage collector. */
        set(size() - 1, null);

        /* Decrement our size. */
        --mSize;

        /* If we are now at 1/8 total capacity, shrink the structure. */
        if (size() * 8 <= mArrays.length * mArrays.length)
            shrink();
            /* Otherwise, if the size is now an even multiple of the array size,
             * we can drop the very last array.  This is the array whose offset
             * is one after the end of the elements.
             */
        else if (size() % mArrays.length == 0)
            mArrays[computeOffset(size())] = null;

        return result;
    }

    /**
     * Given an index, returns the offset into the master array at which the
     * element with that index can be found.
     *
     * @return The index into the topmost array where the given element can
     * be found.
     */
    private int computeOffset(int index) {
        /* This can be computed by dividing the index by the index by the
         * topmost array.  However, if we want to be very clever, we can do
         * this efficiently by bit-shifting downard by the lg2 of the size
         * of the topmost array.
         */
        return index >> mLgSize;
    }

    /**
     * Given an index, returns the offset into the appropriate subarray in
     * which the element with that index can be found.
     *
     * @return The index into the subarray array where the given element can
     * be found.
     */
    private int computeIndex(int index) {
        /* This can be computed by modding the index by the index by the
         * topmost array.  But we can do this more efficiently with a different
         * tactic.  Since the array size is a perfect power of two, it must
         * look like this:
         *
         * 00..010..0
         *
         * Subtracting one yields
         *
         * 00..001..1
         *
         * ANDing this with the index produces the value we're looking for.
         */
        return index & (mArrays.length - 1);
    }

    /**
     * Grows the internal representation by doubling the size of the topmost
     * array and copying the appropriate number of elements over.
     */
    private void grow() {
        /* Double the size of the topmost array. */
        T[][] newArrays = (T[][]) new Object[mArrays.length * 2][];

        /* The new arrays each have size 2^(n + 1).  We need 2^(n - 1) of them
         * to hold the old elements.  Allocate those here and copy everything
         * over.
         */
        for (int i = 0; i < mArrays.length; i += 2) {
            /* Allocate the array. */
            newArrays[i / 2] = (T[]) new Object[newArrays.length];

            /* Use System.arraycopy to move everything over. */
            System.arraycopy(mArrays[i], 0, newArrays[i / 2], 0, mArrays.length);
            System.arraycopy(mArrays[i + 1], 0, newArrays[i / 2], mArrays.length, mArrays.length);

            /* Null out the old arrays to be nice to the GC during this
             * potentially stressful time.
             */
            mArrays[i] = mArrays[i + 1] = null;
        }

        /* Switch out this new array for the old. */
        mArrays = newArrays;

        /* Bump up lg2 of the size. */
        ++mLgSize;
    }

    /**
     * Decreases the size of the HAT by shrinking into a better fit.
     */
    private void shrink() {
        /* If the size of the topmost array is at its minimum, don't do
         * anything.  This doesn't change the asymptotic memory usage because
         * we only do this for small arrays.
         */
        if (mArrays.length == kMinArraySize) return;

        /* Otherwise, we currently have 2^(2n) / 8 = 2^(2n - 3) elements.
         * We're about to shrink into a grid of 2^(2n - 2) elements, and so
         * we'll fill in half of the elements.
         */
        T[][] newArrays = (T[][]) new Object[mArrays.length / 2][];

        /* Copy everything over.  We'll need half as many arrays as before. */
        for (int i = 0; i < newArrays.length / 2; ++i) {
            /* Create the arrays. */
            newArrays[i] = (T[]) new Object[newArrays.length];

            /* Move everything into it.  If this is an odd array, it comes
             * from the upper half of the old array; otherwise it comes from
             * the lower half.
             */
            System.arraycopy(mArrays[i / 2], (i % 2 == 0) ? 0 : newArrays.length,
                    newArrays[i], 0, newArrays.length);

            /* Play nice with the GC.  If this is an odd-numbered array, we
             * just copied over everything we needed and can clear out the
             * old array.
             */
            if (i % 2 == 1)
                mArrays[i / 2] = null;
        }

        /* Copy the arrays over. */
        mArrays = newArrays;

        /* Drop the lg2 of the size. */
        --mLgSize;
    }
}
