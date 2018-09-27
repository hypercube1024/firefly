package com.firefly.utils.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class CollectionTools {

    public static <F, S, T> Map<F, S> transform(Collection<T> from, MultiReturnFunction<F, S, T> function) {
        if (from == null || from.size() == 0)
            return null;

        Map<F, S> map = new HashMap<F, S>();
        for (T t : from) {
            Pair<F, S> pair = function.apply(t);
            map.put(pair.first, pair.second);
        }
        return map;
    }

    public static <F, T> List<F> transform(Collection<T> from, SingleReturnFunction<F, T> function) {
        if (from == null || from.size() == 0)
            return null;

        List<F> collection = new ArrayList<F>();
        for (T t : from) {
            collection.add(function.apply(t));
        }
        return collection;
    }
}
