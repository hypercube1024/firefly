package com.firefly.utils.function;

/**
 * @author Pengtao Qiu
 */
public interface YCombinator {
    interface RecursiveFunction<F> extends Func1<RecursiveFunction<F>, F> {
    }

    static <A, B> Func1<A, B> Y(Func1<Func1<A, B>, Func1<A, B>> f) {
        RecursiveFunction<Func1<A, B>> r = w -> f.call(x -> w.call(w).call(x));
        return r.call(r);
    }
}
