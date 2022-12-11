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

package org.dcm4assange.conf.model;

import org.dcm4assange.util.StringUtils;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import java.net.*;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since May 2019
 */
public class Connection {
    public static final int NO_TIMEOUT = 0;
    public static final int NOT_LISTENING = -1;
    public static final int DEF_BACKLOG = 50;
    public static final int DEF_SOCKETDELAY = 50;
    public static final int DEF_ABORT_TIMEOUT = 1000;
    public static final int DEF_SEND_PDU_LENGTH = 65536;

    public static final String TLS_RSA_WITH_NULL_SHA = "SSL_RSA_WITH_NULL_SHA";
    public static final String TLS_RSA_WITH_3DES_EDE_CBC_SHA = "SSL_RSA_WITH_3DES_EDE_CBC_SHA";
    public static final String TLS_RSA_WITH_AES_128_CBC_SHA = "TLS_RSA_WITH_AES_128_CBC_SHA";
    public static final String[] DEFAULT_TLS_PROTOCOLS =  { "TLSv1.2" };

    private volatile String commonName;
    private volatile String hostname = "localhost";
    private volatile String bindAddress;
    private volatile String clientBindAddress;
    private volatile int port = NOT_LISTENING;
    private volatile int backlog = DEF_BACKLOG;
    private volatile int connectTimeout;

    private volatile int socketCloseDelay = DEF_SOCKETDELAY;
    private volatile boolean packPDV = true;
    private volatile boolean tcpNoDelay = true;
    private volatile int sendBufferSize;
    private volatile int receiveBufferSize;
    private volatile int sendPDULength = DEF_SEND_PDU_LENGTH;
    private volatile int receivePDULength;
    private volatile int maxOpsPerformed;
    private volatile int maxOpsInvoked;
    private final SSLParameters sslParameters = new SSLParameters();
    private volatile String[] blacklist = {};
    private volatile Boolean installed;
    private volatile Device device;


    public Device getDevice() {
        return device;
    }

    Connection setDevice(Device device) {
        if (this.device != device) {
            if (this.device != null && device != null)
                throw new IllegalStateException("Connection already contained by " + device);
            this.device = device;
        }
        return this;
    }

    @Override
    public String toString() {
        return "Connection[" + hostname + ':' + port + ']';
    }

    public String getCommonName() {
        return commonName;
    }

    public Connection setCommonName(String name) {
        this.commonName = StringUtils.trimAndNullifyEmpty(name);
        return this;
    }

    public String getHostname() {
        return hostname;
    }

    public Connection setHostname(String hostname) {
        this.hostname = StringUtils.requireNonBlank(hostname);
        return this;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public Connection setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
        return this;
    }

    public InetSocketAddress bindSocketAddress() {
        return bindSocketAddress(bindAddress, port);
    }

    public String getClientBindAddress() {
        return clientBindAddress;
    }

    public Connection setClientBindAddress(String clientBindAddress) {
        this.clientBindAddress = clientBindAddress;
        return this;
    }

    public InetSocketAddress clientBindSocketAddress() {
        return bindSocketAddress(clientBindAddress, 0);
    }

    private static InetSocketAddress bindSocketAddress(String hostname, int port) {
        return hostname != null
                ? new InetSocketAddress(hostname, port)
                : new InetSocketAddress(port);
    }

    public int getPort() {
        return port;
    }

    public Connection setPort(int port) {
        if (port < NOT_LISTENING)
            throw new IllegalArgumentException("port < 0");
        this.port = port;
        return this;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        if (this.backlog == backlog)
            return;

        if (backlog < 1)
            throw new IllegalArgumentException("backlog: " + backlog);

        this.backlog = backlog;
    }

    public boolean isListening() {
        return port > NOT_LISTENING;
    }

    public String[] getTLSCipherSuites() {
        return sslParameters.getCipherSuites();
    }

    public Connection setTLSCipherSuites(String[] tlsCipherSuites) {
        sslParameters.setCipherSuites(tlsCipherSuites);
        return this;
    }

    public final boolean isTLS() {
        return getTLSCipherSuites() != null;
    }

    public String[] getBlacklist() {
        return blacklist.clone();
    }

    public void setBlacklist(String... blacklist) {
        this.blacklist = blacklist != null ? blacklist.clone() : StringUtils.EMPTY_STRINGS;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public Connection setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public int getSocketCloseDelay() {
        return socketCloseDelay;
    }

    public Connection setSocketCloseDelay(int socketCloseDelay) {
        this.socketCloseDelay = socketCloseDelay;
        return this;
    }

    public Boolean getInstalled() {
        return installed;
    }

    public Connection setInstalled(Boolean installed) {
        this.installed = installed;
        return this;
    }

    public boolean isInstalled() {
        return (device == null || device.isInstalled()) && (installed == null || installed);
    }

    public boolean isPackPDV() {
        return packPDV;
    }

    public Connection setPackPDV(boolean packPDV) {
        this.packPDV = packPDV;
        return this;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public Connection setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
        return this;
    }

    public int getSendBufferSize() {
        return sendBufferSize;
    }

    public Connection setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
        return this;
    }

    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    public Connection setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
        return this;
    }

    public final int getSendPDULength() {
        return sendPDULength;
    }

    public final Connection setSendPDULength(int sendPDULength) {
        this.sendPDULength = sendPDULength;
        return this;
    }

    public final int getReceivePDULength() {
        return receivePDULength;
    }

    public final Connection setReceivePDULength(int receivePDULength) {
        this.receivePDULength = receivePDULength;
        return this;
    }

    public final int getMaxOpsPerformed() {
        return maxOpsPerformed;
    }

    public final Connection setMaxOpsPerformed(int maxOpsPerformed) {
        this.maxOpsPerformed = maxOpsPerformed;
        return this;
    }

    public final int getMaxOpsInvoked() {
        return maxOpsInvoked;
    }

    public final Connection setMaxOpsInvoked(int maxOpsInvoked) {
        this.maxOpsInvoked = maxOpsInvoked;
        return this;
    }

    public boolean isAsynchronousMode() {
        return maxOpsPerformed != 1 || maxOpsInvoked != 1;
    }

    public boolean isBlackListed(InetAddress inetAddr) {
        for (String host : blacklist)
            try {
                for (InetAddress blackListed : InetAddress.getAllByName(host))
                    if (blackListed.equals(inetAddr)) return true;
            } catch (UnknownHostException ignore) {}
        return false;
    }

    public void init(ServerSocket ss) {
        if (ss instanceof SSLServerSocket ssl) {
            ssl.setSSLParameters(sslParameters);
        }
    }

    public void init(Socket ss) {

    }

    boolean match(Connection other) {
        return commonName != null
                ? commonName.equals(other.commonName)
                : hostname.equals(other.hostname) && port == other.port;
    }
}
