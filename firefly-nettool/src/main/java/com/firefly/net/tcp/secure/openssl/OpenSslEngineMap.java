package com.firefly.net.tcp.secure.openssl;

interface OpenSslEngineMap {

    /**
     * Remove the {@link OpenSslEngine} with the given {@code ssl} address and
     * return it.
     */
    ReferenceCountedOpenSslEngine remove(long ssl);

    /**
     * Add a {@link OpenSslEngine} to this {@link OpenSslEngineMap}.
     */
    void add(ReferenceCountedOpenSslEngine engine);

    /**
     * Get the {@link OpenSslEngine} for the given {@code ssl} address.
     */
    ReferenceCountedOpenSslEngine get(long ssl);
}
