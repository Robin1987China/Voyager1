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

package io.voyager1.plugin;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.*;

/**
 * <a href="https://stackoverflow.com/questions/33998477/turn-ssl-verification-off-for-jgit-clone-command/35587504">https://stackoverflow.com/questions/33998477/turn-ssl-verification-off-for-jgit-clone-command/35587504</a>
 *
 * @see ChainingCredentialsProvider
 * @see HttpTransport
 * @see TransportHttp#trustInsecureSslConnection(Throwable)
 * @since 2023/1/29
 */
public class SslVerifyUsernamePasswordCredentialsProvider extends UsernamePasswordCredentialsProvider {

    public SslVerifyUsernamePasswordCredentialsProvider(String username, String password) {
        super(username, password);
    }

    @Override
    public boolean supports(CredentialItem... items) {
        for (CredentialItem item : items) {
            if ((item instanceof CredentialItem.YesNoType)) {
                return true;
            }
        }
        return super.supports(items);
    }

    @Override
    public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
        for (CredentialItem item : items) {
            if (item instanceof CredentialItem.YesNoType) {
                ((CredentialItem.YesNoType) item).setValue(true);
                return true;
            }
        }
        return super.get(uri, items);
    }
}
