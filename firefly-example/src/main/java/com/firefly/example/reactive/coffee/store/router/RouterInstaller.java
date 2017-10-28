package com.firefly.example.reactive.coffee.store.router;

/**
 * @author Pengtao Qiu
 */
public interface RouterInstaller extends Comparable<RouterInstaller> {

    void install();

    default int compareTo(RouterInstaller o) {
        return order().compareTo(o.order());
    }

    default Integer order() {
        return Integer.MAX_VALUE;
    }
}
