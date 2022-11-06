package org.dcm4assange.conf.model;

import org.dcm4assange.util.StringUtils;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since May 2019
 */
public class Connection {
    public static final int NO_TIMEOUT = 0;
    public static final int SYNCHRONOUS_MODE = 1;
    public static final int NOT_LISTENING = -1;
    public static final int DEF_BACKLOG = 50;
    public static final int DEF_SOCKETDELAY = 50;
    public static final int DEF_ABORT_TIMEOUT = 1000;
    public static final int DEF_BUFFERSIZE = 0;

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
    private int sendPDULength;
    private int receivePDULength;
    private int maxOpsPerformed = SYNCHRONOUS_MODE;
    private int maxOpsInvoked = SYNCHRONOUS_MODE;
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

    public String bindAddress() {
        return bindAddress != null ? bindAddress : hostname;
    }

    public String getClientBindAddress() {
        return clientBindAddress;
    }

    public Connection setClientBindAddress(String clientBindAddress) {
        this.clientBindAddress = clientBindAddress;
        return this;
    }

    public String clientBindAddress() {
        return clientBindAddress != null ? clientBindAddress : hostname;
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
        return maxOpsPerformed != SYNCHRONOUS_MODE || maxOpsInvoked != SYNCHRONOUS_MODE;
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
