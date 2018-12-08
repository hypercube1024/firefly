package com.fireflysource.common.collection.array;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility methods for Array manipulation
 */
public class ArrayUtils implements Cloneable, Serializable {

    private static final long serialVersionUID = 2252854725258539040L;

    /**
     * An empty immutable <code>Object</code> array.
     */
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    /**
     * An empty immutable <code>Class</code> array.
     */
    public static final Class[] EMPTY_CLASS_ARRAY = new Class[0];
    /**
     * An empty immutable <code>String</code> array.
     */
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    /**
     * An empty immutable <code>long</code> array.
     */
    public static final long[] EMPTY_LONG_ARRAY = new long[0];
    /**
     * An empty immutable <code>Long</code> array.
     */
    public static final Long[] EMPTY_LONG_OBJECT_ARRAY = new Long[0];
    /**
     * An empty immutable <code>int</code> array.
     */
    public static final int[] EMPTY_INT_ARRAY = new int[0];
    /**
     * An empty immutable <code>Integer</code> array.
     */
    public static final Integer[] EMPTY_INTEGER_OBJECT_ARRAY = new Integer[0];
    /**
     * An empty immutable <code>short</code> array.
     */
    public static final short[] EMPTY_SHORT_ARRAY = new short[0];
    /**
     * An empty immutable <code>Short</code> array.
     */
    public static final Short[] EMPTY_SHORT_OBJECT_ARRAY = new Short[0];
    /**
     * An empty immutable <code>byte</code> array.
     */
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    /**
     * An empty immutable <code>Byte</code> array.
     */
    public static final Byte[] EMPTY_BYTE_OBJECT_ARRAY = new Byte[0];
    /**
     * An empty immutable <code>double</code> array.
     */
    public static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
    /**
     * An empty immutable <code>Double</code> array.
     */
    public static final Double[] EMPTY_DOUBLE_OBJECT_ARRAY = new Double[0];
    /**
     * An empty immutable <code>float</code> array.
     */
    public static final float[] EMPTY_FLOAT_ARRAY = new float[0];
    /**
     * An empty immutable <code>Float</code> array.
     */
    public static final Float[] EMPTY_FLOAT_OBJECT_ARRAY = new Float[0];
    /**
     * An empty immutable <code>boolean</code> array.
     */
    public static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
    /**
     * An empty immutable <code>Boolean</code> array.
     */
    public static final Boolean[] EMPTY_BOOLEAN_OBJECT_ARRAY = new Boolean[0];
    /**
     * An empty immutable <code>char</code> array.
     */
    public static final char[] EMPTY_CHAR_ARRAY = new char[0];
    /**
     * An empty immutable <code>Character</code> array.
     */
    public static final Character[] EMPTY_CHARACTER_OBJECT_ARRAY = new Character[0];

    public static <T> T[] removeFromArray(T[] array, Object item) {
        if (item == null || array == null)
            return array;
        for (int i = array.length; i-- > 0; ) {
            if (item.equals(array[i])) {
                Class<?> c = array.getClass().getComponentType();
                @SuppressWarnings("unchecked")
                T[] na = (T[]) Array.newInstance(c, Array.getLength(array) - 1);
                if (i > 0)
                    System.arraycopy(array, 0, na, 0, i);
                if (i + 1 < array.length)
                    System.arraycopy(array, i + 1, na, i, array.length - (i + 1));
                return na;
            }
        }
        return array;
    }

    /**
     * Add element to an array
     *
     * @param array The array to add to (or null)
     * @param item  The item to add
     * @param type  The type of the array (in case of null array)
     * @param <T>   the array entry type
     * @return new array with contents of array plus item
     */
    public static <T> T[] addToArray(T[] array, T item, Class<?> type) {
        if (array == null) {
            if (type == null && item != null) {
                type = item.getClass();
            }
            @SuppressWarnings("unchecked")
            T[] na = (T[]) Array.newInstance(type, 1);
            na[0] = item;
            return na;
        } else {
            T[] na = Arrays.copyOf(array, array.length + 1);
            na[array.length] = item;
            return na;
        }
    }

    /**
     * Add element to the start of an array
     *
     * @param array The array to add to (or null)
     * @param item  The item to add
     * @param type  The type of the array (in case of null array)
     * @param <T>   the array entry type
     * @return new array with contents of array plus item
     */
    public static <T> T[] prependToArray(T item, T[] array, Class<?> type) {
        if (array == null) {
            if (type == null && item != null) {
                type = item.getClass();
            }
            @SuppressWarnings("unchecked")
            T[] na = (T[]) Array.newInstance(type, 1);
            na[0] = item;
            return na;
        } else {
            Class<?> c = array.getClass().getComponentType();
            @SuppressWarnings("unchecked")
            T[] na = (T[]) Array.newInstance(c, Array.getLength(array) + 1);
            System.arraycopy(array, 0, na, 1, array.length);
            na[0] = item;
            return na;
        }
    }

    /**
     * @param array Any array of object
     * @param <E>   the array entry type
     * @return A new <i>modifiable</i> list initialised with the elements from
     * <code>array</code>.
     */
    public static <E> List<E> asMutableList(E[] array) {
        if (array == null || array.length == 0)
            return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(array));
    }

    public static <T> T[] removeNulls(T[] array) {
        for (T t : array) {
            if (t == null) {
                List<T> list = new ArrayList<>();
                for (T t2 : array) {
                    if (t2 != null) {
                        list.add(t2);
                    }
                }
                return list.toArray(Arrays.copyOf(array, list.size()));
            }
        }
        return array;
    }

    // nullToEmpty
    //-----------------------------------------------------------------------

    /**
     * <p>Defensive programming technique to change a <code>null</code>
     * reference to an empty one.</p>
     * <p>
     * <p>This method returns an empty array for a <code>null</code> input array.</p>
     * <p>
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty <code>public static</code> references in this class.</p>
     *
     * @param array the array to check for <code>null</code> or empty
     * @return the same array, <code>public static</code> empty array if <code>null</code> or empty input
     * @since 2.5
     */
    public static Object[] nullToEmpty(Object[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_OBJECT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a <code>null</code>
     * reference to an empty one.</p>
     * <p>
     * <p>This method returns an empty array for a <code>null</code> input array.</p>
     * <p>
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty <code>public static</code> references in this class.</p>
     *
     * @param array the array to check for <code>null</code> or empty
     * @return the same array, <code>public static</code> empty array if <code>null</code> or empty input
     * @since 2.5
     */
    public static String[] nullToEmpty(String[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_STRING_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a <code>null</code>
     * reference to an empty one.</p>
     * <p>
     * <p>This method returns an empty array for a <code>null</code> input array.</p>
     * <p>
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty <code>public static</code> references in this class.</p>
     *
     * @param array the array to check for <code>null</code> or empty
     * @return the same array, <code>public static</code> empty array if <code>null</code> or empty input
     * @since 2.5
     */
    public static long[] nullToEmpty(long[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_LONG_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a <code>null</code>
     * reference to an empty one.</p>
     * <p>
     * <p>This method returns an empty array for a <code>null</code> input array.</p>
     * <p>
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty <code>public static</code> references in this class.</p>
     *
     * @param array the array to check for <code>null</code> or empty
     * @return the same array, <code>public static</code> empty array if <code>null</code> or empty input
     * @since 2.5
     */
    public static int[] nullToEmpty(int[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_INT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a <code>null</code>
     * reference to an empty one.</p>
     * <p>
     * <p>This method returns an empty array for a <code>null</code> input array.</p>
     * <p>
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty <code>public static</code> references in this class.</p>
     *
     * @param array the array to check for <code>null</code> or empty
     * @return the same array, <code>public static</code> empty array if <code>null</code> or empty input
     * @since 2.5
     */
    public static short[] nullToEmpty(short[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_SHORT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a <code>null</code>
     * reference to an empty one.</p>
     * <p>
     * <p>This method returns an empty array for a <code>null</code> input array.</p>
     * <p>
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty <code>public static</code> references in this class.</p>
     *
     * @param array the array to check for <code>null</code> or empty
     * @return the same array, <code>public static</code> empty array if <code>null</code> or empty input
     * @since 2.5
     */
    public static char[] nullToEmpty(char[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_CHAR_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a <code>null</code>
     * reference to an empty one.</p>
     * <p>
     * <p>This method returns an empty array for a <code>null</code> input array.</p>
     * <p>
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty <code>public static</code> references in this class.</p>
     *
     * @param array the array to check for <code>null</code> or empty
     * @return the same array, <code>public static</code> empty array if <code>null</code> or empty input
     * @since 2.5
     */
    public static byte[] nullToEmpty(byte[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a <code>null</code>
     * reference to an empty one.</p>
     * <p>
     * <p>This method returns an empty array for a <code>null</code> input array.</p>
     * <p>
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty <code>public static</code> references in this class.</p>
     *
     * @param array the array to check for <code>null</code> or empty
     * @return the same array, <code>public static</code> empty array if <code>null</code> or empty input
     * @since 2.5
     */
    public static double[] nullToEmpty(double[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_DOUBLE_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a <code>null</code>
     * reference to an empty one.</p>
     * <p>
     * <p>This method returns an empty array for a <code>null</code> input array.</p>
     * <p>
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty <code>public static</code> references in this class.</p>
     *
     * @param array the array to check for <code>null</code> or empty
     * @return the same array, <code>public static</code> empty array if <code>null</code> or empty input
     * @since 2.5
     */
    public static float[] nullToEmpty(float[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_FLOAT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a <code>null</code>
     * reference to an empty one.</p>
     * <p>
     * <p>This method returns an empty array for a <code>null</code> input array.</p>
     * <p>
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty <code>public static</code> references in this class.</p>
     *
     * @param array the array to check for <code>null</code> or empty
     * @return the same array, <code>public static</code> empty array if <code>null</code> or empty input
     * @since 2.5
     */
    public static boolean[] nullToEmpty(boolean[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_BOOLEAN_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a <code>null</code>
     * reference to an empty one.</p>
     * <p>
     * <p>This method returns an empty array for a <code>null</code> input array.</p>
     * <p>
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty <code>public static</code> references in this class.</p>
     *
     * @param array the array to check for <code>null</code> or empty
     * @return the same array, <code>public static</code> empty array if <code>null</code> or empty input
     * @since 2.5
     */
    public static Long[] nullToEmpty(Long[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_LONG_OBJECT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a <code>null</code>
     * reference to an empty one.</p>
     * <p>
     * <p>This method returns an empty array for a <code>null</code> input array.</p>
     * <p>
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty <code>public static</code> references in this class.</p>
     *
     * @param array the array to check for <code>null</code> or empty
     * @return the same array, <code>public static</code> empty array if <code>null</code> or empty input
     * @since 2.5
     */
    public static Integer[] nullToEmpty(Integer[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_INTEGER_OBJECT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a <code>null</code>
     * reference to an empty one.</p>
     * <p>
     * <p>This method returns an empty array for a <code>null</code> input array.</p>
     * <p>
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty <code>public static</code> references in this class.</p>
     *
     * @param array the array to check for <code>null</code> or empty
     * @return the same array, <code>public static</code> empty array if <code>null</code> or empty input
     * @since 2.5
     */
    public static Short[] nullToEmpty(Short[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_SHORT_OBJECT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a <code>null</code>
     * reference to an empty one.</p>
     * <p>
     * <p>This method returns an empty array for a <code>null</code> input array.</p>
     * <p>
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty <code>public static</code> references in this class.</p>
     *
     * @param array the array to check for <code>null</code> or empty
     * @return the same array, <code>public static</code> empty array if <code>null</code> or empty input
     * @since 2.5
     */
    public static Character[] nullToEmpty(Character[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_CHARACTER_OBJECT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a <code>null</code>
     * reference to an empty one.</p>
     * <p>
     * <p>This method returns an empty array for a <code>null</code> input array.</p>
     * <p>
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty <code>public static</code> references in this class.</p>
     *
     * @param array the array to check for <code>null</code> or empty
     * @return the same array, <code>public static</code> empty array if <code>null</code> or empty input
     * @since 2.5
     */
    public static Byte[] nullToEmpty(Byte[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_BYTE_OBJECT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a <code>null</code>
     * reference to an empty one.</p>
     * <p>
     * <p>This method returns an empty array for a <code>null</code> input array.</p>
     * <p>
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty <code>public static</code> references in this class.</p>
     *
     * @param array the array to check for <code>null</code> or empty
     * @return the same array, <code>public static</code> empty array if <code>null</code> or empty input
     * @since 2.5
     */
    public static Double[] nullToEmpty(Double[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_DOUBLE_OBJECT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a <code>null</code>
     * reference to an empty one.</p>
     * <p>
     * <p>This method returns an empty array for a <code>null</code> input array.</p>
     * <p>
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty <code>public static</code> references in this class.</p>
     *
     * @param array the array to check for <code>null</code> or empty
     * @return the same array, <code>public static</code> empty array if <code>null</code> or empty input
     * @since 2.5
     */
    public static Float[] nullToEmpty(Float[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_FLOAT_OBJECT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a <code>null</code>
     * reference to an empty one.</p>
     * <p>
     * <p>This method returns an empty array for a <code>null</code> input array.</p>
     * <p>
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty <code>public static</code> references in this class.</p>
     *
     * @param array the array to check for <code>null</code> or empty
     * @return the same array, <code>public static</code> empty array if <code>null</code> or empty input
     * @since 2.5
     */
    public static Boolean[] nullToEmpty(Boolean[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_BOOLEAN_OBJECT_ARRAY;
        }
        return array;
    }

    // Primitive/Object array converters
    // ----------------------------------------------------------------------

    // Character array converters
    // ----------------------------------------------------------------------

    /**
     * <p>Converts an array of object Characters to primitives.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array a <code>Character</code> array, may be <code>null</code>
     * @return a <code>char</code> array, <code>null</code> if null array input
     * @throws NullPointerException if array content is <code>null</code>
     */
    public static char[] toPrimitive(Character[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_CHAR_ARRAY;
        }
        final char[] result = new char[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    /**
     * <p>Converts an array of object Character to primitives handling <code>null</code>.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array        a <code>Character</code> array, may be <code>null</code>
     * @param valueForNull the value to insert if <code>null</code> found
     * @return a <code>char</code> array, <code>null</code> if null array input
     */
    public static char[] toPrimitive(Character[] array, char valueForNull) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_CHAR_ARRAY;
        }
        final char[] result = new char[array.length];
        for (int i = 0; i < array.length; i++) {
            Character b = array[i];
            result[i] = (b == null ? valueForNull : b);
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive chars to objects.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array a <code>char</code> array
     * @return a <code>Character</code> array, <code>null</code> if null array input
     */
    public static Character[] toObject(char[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_CHARACTER_OBJECT_ARRAY;
        }
        final Character[] result = new Character[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    // Long array converters
    // ----------------------------------------------------------------------

    /**
     * <p>Converts an array of object Longs to primitives.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array a <code>Long</code> array, may be <code>null</code>
     * @return a <code>long</code> array, <code>null</code> if null array input
     * @throws NullPointerException if array content is <code>null</code>
     */
    public static long[] toPrimitive(Long[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_LONG_ARRAY;
        }
        final long[] result = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    /**
     * <p>Converts an array of object Long to primitives handling <code>null</code>.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array        a <code>Long</code> array, may be <code>null</code>
     * @param valueForNull the value to insert if <code>null</code> found
     * @return a <code>long</code> array, <code>null</code> if null array input
     */
    public static long[] toPrimitive(Long[] array, long valueForNull) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_LONG_ARRAY;
        }
        final long[] result = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            Long b = array[i];
            result[i] = (b == null ? valueForNull : b);
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive longs to objects.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array a <code>long</code> array
     * @return a <code>Long</code> array, <code>null</code> if null array input
     */
    public static Long[] toObject(long[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_LONG_OBJECT_ARRAY;
        }
        final Long[] result = new Long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    // Int array converters
    // ----------------------------------------------------------------------

    /**
     * <p>Converts an array of object Integers to primitives.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array a <code>Integer</code> array, may be <code>null</code>
     * @return an <code>int</code> array, <code>null</code> if null array input
     * @throws NullPointerException if array content is <code>null</code>
     */
    public static int[] toPrimitive(Integer[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_INT_ARRAY;
        }
        final int[] result = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    /**
     * <p>Converts an array of object Integer to primitives handling <code>null</code>.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array        a <code>Integer</code> array, may be <code>null</code>
     * @param valueForNull the value to insert if <code>null</code> found
     * @return an <code>int</code> array, <code>null</code> if null array input
     */
    public static int[] toPrimitive(Integer[] array, int valueForNull) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_INT_ARRAY;
        }
        final int[] result = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            Integer b = array[i];
            result[i] = (b == null ? valueForNull : b);
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive ints to objects.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array an <code>int</code> array
     * @return an <code>Integer</code> array, <code>null</code> if null array input
     */
    public static Integer[] toObject(int[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_INTEGER_OBJECT_ARRAY;
        }
        final Integer[] result = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    // Short array converters
    // ----------------------------------------------------------------------

    /**
     * <p>Converts an array of object Shorts to primitives.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array a <code>Short</code> array, may be <code>null</code>
     * @return a <code>byte</code> array, <code>null</code> if null array input
     * @throws NullPointerException if array content is <code>null</code>
     */
    public static short[] toPrimitive(Short[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_SHORT_ARRAY;
        }
        final short[] result = new short[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    /**
     * <p>Converts an array of object Short to primitives handling <code>null</code>.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array        a <code>Short</code> array, may be <code>null</code>
     * @param valueForNull the value to insert if <code>null</code> found
     * @return a <code>byte</code> array, <code>null</code> if null array input
     */
    public static short[] toPrimitive(Short[] array, short valueForNull) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_SHORT_ARRAY;
        }
        final short[] result = new short[array.length];
        for (int i = 0; i < array.length; i++) {
            Short b = array[i];
            result[i] = (b == null ? valueForNull : b);
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive shorts to objects.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array a <code>short</code> array
     * @return a <code>Short</code> array, <code>null</code> if null array input
     */
    public static Short[] toObject(short[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_SHORT_OBJECT_ARRAY;
        }
        final Short[] result = new Short[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    // Byte array converters
    // ----------------------------------------------------------------------

    /**
     * <p>Converts an array of object Bytes to primitives.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array a <code>Byte</code> array, may be <code>null</code>
     * @return a <code>byte</code> array, <code>null</code> if null array input
     * @throws NullPointerException if array content is <code>null</code>
     */
    public static byte[] toPrimitive(Byte[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        final byte[] result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    /**
     * <p>Converts an array of object Bytes to primitives handling <code>null</code>.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array        a <code>Byte</code> array, may be <code>null</code>
     * @param valueForNull the value to insert if <code>null</code> found
     * @return a <code>byte</code> array, <code>null</code> if null array input
     */
    public static byte[] toPrimitive(Byte[] array, byte valueForNull) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        final byte[] result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            Byte b = array[i];
            result[i] = (b == null ? valueForNull : b);
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive bytes to objects.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array a <code>byte</code> array
     * @return a <code>Byte</code> array, <code>null</code> if null array input
     */
    public static Byte[] toObject(byte[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_BYTE_OBJECT_ARRAY;
        }
        final Byte[] result = new Byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    // Double array converters
    // ----------------------------------------------------------------------

    /**
     * <p>Converts an array of object Doubles to primitives.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array a <code>Double</code> array, may be <code>null</code>
     * @return a <code>double</code> array, <code>null</code> if null array input
     * @throws NullPointerException if array content is <code>null</code>
     */
    public static double[] toPrimitive(Double[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_DOUBLE_ARRAY;
        }
        final double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    /**
     * <p>Converts an array of object Doubles to primitives handling <code>null</code>.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array        a <code>Double</code> array, may be <code>null</code>
     * @param valueForNull the value to insert if <code>null</code> found
     * @return a <code>double</code> array, <code>null</code> if null array input
     */
    public static double[] toPrimitive(Double[] array, double valueForNull) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_DOUBLE_ARRAY;
        }
        final double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            Double b = array[i];
            result[i] = (b == null ? valueForNull : b);
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive doubles to objects.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array a <code>double</code> array
     * @return a <code>Double</code> array, <code>null</code> if null array input
     */
    public static Double[] toObject(double[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_DOUBLE_OBJECT_ARRAY;
        }
        final Double[] result = new Double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    //   Float array converters
    // ----------------------------------------------------------------------

    /**
     * <p>Converts an array of object Floats to primitives.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array a <code>Float</code> array, may be <code>null</code>
     * @return a <code>float</code> array, <code>null</code> if null array input
     * @throws NullPointerException if array content is <code>null</code>
     */
    public static float[] toPrimitive(Float[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_FLOAT_ARRAY;
        }
        final float[] result = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    /**
     * <p>Converts an array of object Floats to primitives handling <code>null</code>.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array        a <code>Float</code> array, may be <code>null</code>
     * @param valueForNull the value to insert if <code>null</code> found
     * @return a <code>float</code> array, <code>null</code> if null array input
     */
    public static float[] toPrimitive(Float[] array, float valueForNull) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_FLOAT_ARRAY;
        }
        final float[] result = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            Float b = array[i];
            result[i] = (b == null ? valueForNull : b);
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive floats to objects.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array a <code>float</code> array
     * @return a <code>Float</code> array, <code>null</code> if null array input
     */
    public static Float[] toObject(float[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_FLOAT_OBJECT_ARRAY;
        }
        final Float[] result = new Float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    // Boolean array converters
    // ----------------------------------------------------------------------

    /**
     * <p>Converts an array of object Booleans to primitives.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array a <code>Boolean</code> array, may be <code>null</code>
     * @return a <code>boolean</code> array, <code>null</code> if null array input
     * @throws NullPointerException if array content is <code>null</code>
     */
    public static boolean[] toPrimitive(Boolean[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_BOOLEAN_ARRAY;
        }
        final boolean[] result = new boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    /**
     * <p>Converts an array of object Booleans to primitives handling <code>null</code>.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array        a <code>Boolean</code> array, may be <code>null</code>
     * @param valueForNull the value to insert if <code>null</code> found
     * @return a <code>boolean</code> array, <code>null</code> if null array input
     */
    public static boolean[] toPrimitive(Boolean[] array, boolean valueForNull) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_BOOLEAN_ARRAY;
        }
        final boolean[] result = new boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            Boolean b = array[i];
            result[i] = (b == null ? valueForNull : b);
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive booleans to objects.</p>
     * <p>
     * <p>This method returns <code>null</code> for a <code>null</code> input array.</p>
     *
     * @param array a <code>boolean</code> array
     * @return a <code>Boolean</code> array, <code>null</code> if null array input
     */
    public static Boolean[] toObject(boolean[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_BOOLEAN_OBJECT_ARRAY;
        }
        final Boolean[] result = new Boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = (array[i] ? Boolean.TRUE : Boolean.FALSE);
        }
        return result;
    }

}
