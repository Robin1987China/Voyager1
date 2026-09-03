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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * HMac 摘要器，"" {@code .crypto.digest.HMac}。
 */
public class HMac {

    private final Mac mac;

    HMac(String algorithm, byte[] key) {
        try {
            this.mac = Mac.getInstance(algorithm);
            this.mac.init(new SecretKeySpec(key, algorithm));
        } catch (Exception e) {
            throw new RuntimeException("初始化 HMac 失败", e);
        }
    }

    public byte[] digest(byte[] data) {
        return mac.doFinal(data);
    }

    public byte[] digest(String data) {
        return digest(data.getBytes(StandardCharsets.UTF_8));
    }

    public String digestHex(byte[] data) {
        return HexUtil.encodeHexStr(digest(data));
    }

    public String digestHex(String data) {
        return HexUtil.encodeHexStr(digest(data));
    }
}
