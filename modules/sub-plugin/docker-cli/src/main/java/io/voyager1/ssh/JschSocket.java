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

package io.voyager1.ssh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Optional;

@Slf4j
class JschSocket extends Socket {

    private final Session session;

    private Channel channel;
    private InputStream inputStream;
    private OutputStream outputStream;
    private final boolean useSudo;

    JschSocket(Session session, boolean useSudo) {
        this.session = session;
        this.useSudo = useSudo;
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        connect(0);
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        connect(timeout);
    }

    @Override
    public boolean isConnected() {
        return channel.isConnected();
    }

    @Override
    public boolean isClosed() {
        return channel != null && channel.isClosed();
    }

    private void connect(int timeout) throws IOException {
        try {
            // only 18.09 and up
            channel = session.openChannel("exec");
            String command;
            if (useSudo) {
                command = "sudo docker system dial-stdio";
            } else {
                command = "docker system dial-stdio";
            }
            ((ChannelExec) channel).setCommand(command);
            log.debug("Using dialer command【{}】", command);
            inputStream = channel.getInputStream();
            outputStream = channel.getOutputStream();

            channel.connect(timeout);

        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        Optional.ofNullable(channel).ifPresent(Channel::disconnect);
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

}
