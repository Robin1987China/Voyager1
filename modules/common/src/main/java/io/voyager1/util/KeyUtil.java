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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 *  {@code io.voyager1.util.KeyUtil} 的兼容实现。
 *
 * <p>基于 JDK 自带的 KeyStore / CertificateFactory，仅覆盖代码库实际使用到的 API。</p>
 */
public final class KeyUtil {

    public static final String KEY_TYPE_JKS = "JKS";
    public static final String KEY_TYPE_PKCS12 = "PKCS12";
    public static final String CERT_TYPE_X509 = "X.509";

    private KeyUtil() {
    }

    /**
     * 从输入流读取 X.509 证书。
     */
    public static X509Certificate readX509Certificate(InputStream inputStream) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance(CERT_TYPE_X509);
            return (X509Certificate) certificateFactory.generateCertificate(inputStream);
        } catch (Exception e) {
            throw new IllegalArgumentException("读取 X.509 证书失败", e);
        }
    }

    /**
     * 从文件读取 X.509 证书。
     */
    public static X509Certificate readX509Certificate(File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            return readX509Certificate(inputStream);
        } catch (Exception e) {
            throw new IllegalArgumentException("读取 X.509 证书失败", e);
        }
    }

    /**
     * 读取 PKCS12 密钥库。
     */
    public static KeyStore readPKCS12KeyStore(File file, char[] password) {
        return readKeyStore(file, password, KEY_TYPE_PKCS12);
    }

    /**
     * 读取 JKS 密钥库。
     */
    public static KeyStore readJKSKeyStore(File file, char[] password) {
        return readKeyStore(file, password, KEY_TYPE_JKS);
    }

    private static KeyStore readKeyStore(File file, char[] password, String type) {
        try (InputStream inputStream = new FileInputStream(file)) {
            KeyStore keyStore = KeyStore.getInstance(type);
            keyStore.load(inputStream, password);
            return keyStore;
        } catch (Exception e) {
            throw new IllegalArgumentException("读取 " + type + " 密钥库失败", e);
        }
    }
}
