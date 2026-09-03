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

import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.auth.ISVNProxyManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.io.SVNRepository;

import javax.net.ssl.TrustManager;
import java.util.Optional;

/**
 * svn 授权管理
 *
 * @since 2023/2/23
 */
public class AuthenticationManager implements ISVNAuthenticationManager {

    private final ISVNAuthenticationManager delegate;
    private final Integer timeout;

    public AuthenticationManager(ISVNAuthenticationManager delegate,
                                 Integer timeout) {
        this.delegate = delegate;
        this.timeout = timeout;
    }

    @Override
    public void setAuthenticationProvider(ISVNAuthenticationProvider provider) {
        delegate.setAuthenticationProvider(provider);
    }

    @Override
    public ISVNProxyManager getProxyManager(SVNURL url) throws SVNException {
        return delegate.getProxyManager(url);
    }

    @Override
    public TrustManager getTrustManager(SVNURL url) throws SVNException {
        return delegate.getTrustManager(url);
    }

    @Override
    public SVNAuthentication getFirstAuthentication(String kind, String realm, SVNURL url) throws SVNException {
        return delegate.getFirstAuthentication(kind, realm, url);
    }

    @Override
    public SVNAuthentication getNextAuthentication(String kind, String realm, SVNURL url) throws SVNException {
        return delegate.getNextAuthentication(kind, realm, url);
    }

    @Override
    public void acknowledgeAuthentication(boolean accepted, String kind, String realm, SVNErrorMessage errorMessage, SVNAuthentication authentication) throws SVNException {
        delegate.acknowledgeAuthentication(accepted, kind, realm, errorMessage, authentication);
    }

    @Override
    public void acknowledgeTrustManager(TrustManager manager) {
        delegate.acknowledgeTrustManager(manager);
    }

    @Override
    public boolean isAuthenticationForced() {
        return delegate.isAuthenticationForced();
    }

    @Override
    public int getReadTimeout(SVNRepository repository) {
        return Optional.ofNullable(timeout)
            .map(integer -> integer <= 0 ? null : integer)
            .map(integer -> integer * 1000)
            .orElseGet(() -> delegate.getReadTimeout(repository));
    }

    @Override
    public int getConnectTimeout(SVNRepository repository) {
        return Optional.ofNullable(timeout)
            .map(integer -> integer <= 0 ? null : integer)
            .map(integer -> integer * 1000)
            .orElseGet(() -> delegate.getConnectTimeout(repository));
    }
}
