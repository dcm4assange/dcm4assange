/*
 * Copyright 2021 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.dcm4assange.net;

import org.dcm4assange.conf.model.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Dec 2021
 */
public class TCPListener implements Runnable {
    static final Logger LOG = LoggerFactory.getLogger(TCPListener.class);
    private final DeviceRuntime runtime;
    private final Connection conn;
    private final ServerSocket ssock;

    protected TCPListener(DeviceRuntime runtime, Connection conn) throws IOException {
        this.runtime = runtime;
        this.conn = conn;
        this.ssock = new ServerSocket();
    }

    @Override
    public void run() {
        InetSocketAddress endpoint = new InetSocketAddress(conn.bindAddress(), conn.getPort());
        try {
            LOG.info("Start TCP Listener on {}", endpoint);
            ssock.bind(endpoint, conn.getBacklog());
            while (!ssock.isClosed()) {
                LOG.debug("Wait for connection on {}", endpoint);
                runtime.onAccepted(conn, ssock.accept());
            }
        } catch (Throwable e) {
            if (!ssock.isClosed()) // ignore exception caused by close()
                LOG.error("Exception on listing on {}:", endpoint, e);
        }
        LOG.info("Stop TCP Listener on {}", endpoint);
    }
}
