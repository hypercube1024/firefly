/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.firefly.net.tcp.secure.openssl.nativelib;

import java.nio.ByteBuffer;

/**
 * A PEM encoded value.
 *
 * @see PemEncoded
 */
class PemValue implements PemEncoded {

    private final ByteBuffer content;

    private final boolean sensitive;

    public PemValue(ByteBuffer content, boolean sensitive) {
        this.content = ObjectUtil.checkNotNull(content, "content");
        this.sensitive = sensitive;
    }

    @Override
    public boolean isSensitive() {
        return sensitive;
    }

    @Override
    public ByteBuffer content() {
        return content;
    }

    @Override
    public PemValue copy() {
        ByteBuffer tmp = ByteBuffer.allocateDirect(content.remaining());
        tmp.put(content.duplicate()).flip();
        return replace(tmp);
    }

    @Override
    public PemValue duplicate() {
        return replace(content.duplicate());
    }

    @Override
    public PemValue replace(ByteBuffer content) {
        return new PemValue(content, sensitive);
    }
}
