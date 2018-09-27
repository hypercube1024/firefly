//
//  ========================================================================
//  Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package com.firefly.codec.http2.model;

import java.nio.ByteBuffer;

import com.firefly.utils.collection.ArrayTrie;
import com.firefly.utils.collection.Trie;
import com.firefly.utils.io.BufferUtils;

public enum HttpScheme {
    HTTP("http"), HTTPS("https"), WS("ws"), WSS("wss");

    public final static Trie<HttpScheme> CACHE = new ArrayTrie<HttpScheme>();

    static {
        for (HttpScheme version : HttpScheme.values())
            CACHE.put(version.asString(), version);
    }

    private final String string;
    private final ByteBuffer buffer;

    HttpScheme(String s) {
        string = s;
        buffer = BufferUtils.toBuffer(s);
    }

    public ByteBuffer asByteBuffer() {
        return buffer.asReadOnlyBuffer();
    }

    public boolean is(String s) {
        return s != null && string.equalsIgnoreCase(s);
    }

    public String asString() {
        return string;
    }

    @Override
    public String toString() {
        return string;
    }

}
