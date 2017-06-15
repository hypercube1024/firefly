package com.firefly.utils.lang;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * This generic abstract class is used for obtaining full generics type information
 * by sub-classing.
 * <p>
 * Class is based on ideas from
 * <a href="http://gafter.blogspot.com/2006/12/super-type-tokens.html"
 * >http://gafter.blogspot.com/2006/12/super-type-tokens.html</a>,
 * Additional idea (from a suggestion made in comments of the article)
 * is to require bogus implementation of <code>Comparable</code>
 * (any such generic interface would do, as long as it forces a method
 * with generic type to be implemented).
 * to ensure that a Type argument is indeed given.
 * <p>
 * Usage is by sub-classing: here is one way to instantiate reference
 * to generic type <code>List&lt;Integer&gt;</code>:
 * <pre>
 *  GenericTypeReference ref = new GenericTypeReference&lt;List&lt;Integer&gt;&gt;() { };
 * </pre>
 * which can be passed to methods that accept GenericTypeReference.
 */
public abstract class GenericTypeReference<T> implements Comparable<GenericTypeReference<T>> {

    protected final Type type;

    protected GenericTypeReference() {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof Class<?>) { // sanity check, should never happen
            throw new IllegalArgumentException("Internal error: GenericTypeReference constructed without actual type information");
        }

        /* 22-Dec-2008, tatu: Not sure if this case is safe -- I suspect
         *   it is possible to make it fail?
         *   But let's deal with specific
         *   case when we know an actual use case, and thereby suitable
         *   workarounds for valid case(s) and/or error to throw
         *   on invalid one(s).
         */
        type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    public Type getType() {
        return type;
    }

    /**
     * The only reason we define this method (and require implementation
     * of <code>Comparable</code>) is to prevent constructing a
     * reference without type information.
     */
    @Override
    public int compareTo(GenericTypeReference<T> o) {
        return 0;
    }
    // just need an implementation, not a good one... hence ^^^
}
