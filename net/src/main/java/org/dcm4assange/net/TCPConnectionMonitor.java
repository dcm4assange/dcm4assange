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

import java.net.Socket;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Dec 2021
 */
public interface TCPConnectionMonitor {
    Logger LOG = LoggerFactory.getLogger(TCPConnectionMonitor.class);
    TCPConnectionMonitor DEFAULT = new TCPConnectionMonitor(){};

    default void onConnectionEstablished(Connection conn, Connection remoteConn, Socket s) {
        LOG.warn("Connection established {}", s);
    }

    default void onConnectionFailed(Connection conn, Connection remoteConn, Socket s, Throwable e) {
        LOG.warn("Connection failed {} - ",s , e);
    }

    default void onConnectionRejectedBlacklisted(Connection conn, Socket s) {
        LOG.info("Reject blacklisted connection {}", s);
    }

    default void onConnectionRejected(Connection conn, Socket s, Throwable e) {
        LOG.warn("Reject connection {} - ",s , e);
    }

    default void onConnectionAccepted(Connection conn, Socket s) {
        LOG.info("Accept connection {}", s);
    }

    default void onConnectionClose(Connection conn, Socket s) {
        LOG.info("Close connection {}", s);
    }
}
