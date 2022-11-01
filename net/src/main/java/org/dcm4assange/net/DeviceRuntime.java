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

import org.dcm4assange.DicomObject;
import org.dcm4assange.conf.model.ApplicationEntity;
import org.dcm4assange.conf.model.Connection;
import org.dcm4assange.conf.model.Device;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Jan 2022
 */
public class DeviceRuntime {
    final Device device;
    final ExecutorService executorService;
    final ScheduledExecutorService scheduledExecutorService;
    final TCPConnectionMonitor monitor;
    final NegotiateUserIdentity negotiateUserIdentity;
    final DimseHandler dimseHandler;

    public DeviceRuntime(Device device, DimseHandler dimseHandler) {
        this(device, dimseHandler,
                Executors.newCachedThreadPool(),
                Executors.newSingleThreadScheduledExecutor(),
                TCPConnectionMonitor.DEFAULT,
                NegotiateUserIdentity.DEFAULT);
    }

    public DeviceRuntime(Device device,
                         DimseHandler dimseHandler, ExecutorService executorService,
                         ScheduledExecutorService scheduledExecutorService,
                         TCPConnectionMonitor monitor,
                         NegotiateUserIdentity negotiateUserIdentity) {
        this.device = Objects.requireNonNull(device);
        this.dimseHandler = Objects.requireNonNull(dimseHandler);
        this.executorService = Objects.requireNonNull(executorService);
        this.scheduledExecutorService = Objects.requireNonNull(scheduledExecutorService);
        this.monitor = Objects.requireNonNull(monitor);
        this.negotiateUserIdentity = negotiateUserIdentity;
    }

    public void bindConnections() throws IOException {
        for (Connection conn : device.getConnections())
            if (conn.isInstalled() && conn.isListening())
                executorService.execute(new TCPListener(this, conn));
    }

    public Socket connect(Connection localConn, Connection remoteConn) throws IOException {
        Socket s = new Socket();
        s.bind(new InetSocketAddress(localConn.clientBindAddress(), 0));
        s.connect(new InetSocketAddress(remoteConn.getHostname(), remoteConn.getPort()), localConn.getConnectTimeout());
        monitor.onConnectionEstablished(localConn, remoteConn, s);
        return s;
    }

    public CompletableFuture<Association> openAssociation(
            ApplicationEntity ae, Connection localConn, Connection remoteConn, AAssociate.RQ rq)
            throws IOException {
        Association as = Association.open(this, ae, localConn, connect(localConn, remoteConn), rq);
        executorService.execute(as);
        return as.aaacReceived;
    }

    void onAccepted(Connection conn, Socket sock) {
        monitor.onConnectionAccepted(conn, sock);
        try {
            executorService.execute(Association.accept(this, conn, sock));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void closeSocketDelayed(Connection conn, Socket sock) {
        scheduledExecutorService.schedule(() -> closeSocket(conn, sock),
                conn.getSocketCloseDelay(), TimeUnit.MILLISECONDS);
    }

    void closeSocket(Connection conn, Socket sock) {
        monitor.onConnectionClose(conn, sock);
        try {
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void onDimseRQ(Association as, Byte pcid, Dimse dimse, DicomObject commandSet, InputStream dataStream)
            throws IOException {
        dimseHandler.accept(as, pcid, dimse, commandSet, dataStream);
    }
}
