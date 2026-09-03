/*
 * Copyright (c) 2026 Voyager1
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.voyager1.util;

import java.security.MessageDigest;

/**
 * 摘要器，配合 {@link DigestUtil#sha256()} 使用。
 */
public class Digester {

    private final MessageDigest digest;

    Digester(MessageDigest digest) {
        this.digest = digest;
    }

    public byte[] digest(byte[] data) {
        return digest.digest(data);
    }

    public byte[] digest(String data) {
        return digest.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public String digestHex(byte[] data) {
        return HexUtil.encodeHexStr(digest(data));
    }

    public String digestHex(String data) {
        return HexUtil.encodeHexStr(digest(data));
    }
}
