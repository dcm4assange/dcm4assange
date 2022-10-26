package org.dcm4assange.conf.model;

import org.dcm4assange.util.ArrayUtils;
import org.dcm4assange.util.StringUtils;

import java.util.*;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since May 2019
 */
public class ApplicationEntity {

    private volatile String aeTitle = "*";
    private volatile String description;
    private volatile String[] applicationClusters = {};
    private volatile String[] supportedCharacterSets = {};
    private volatile String[] preferredCalledAETs = {};
    private volatile String[] preferredCallingAETs = {};
    private volatile String[] acceptedCallingAETs = {};
    private volatile boolean acceptor = true;
    private volatile boolean initiator = true;
    private volatile Boolean installed;
    private volatile Device device;

    private final List<Connection> conns = new ArrayList<>();
    private final List<TransferCapability> tcs = new ArrayList<>();

    public Device getDevice() {
        return device;
    }

    public ApplicationEntity setDevice(Device device) {
        if (this.device != device) {
            if (this.device != null && device != null)
                throw new IllegalStateException("ApplicationEntity already contained by " + device);
            if (device != null)
                conns.forEach(conn -> conn.setDevice(device));
            this.device = device;
        }
        return this;
    }

    public String getAETitle() {
        return aeTitle;
    }

    public ApplicationEntity setAETitle(String title) {
        this.aeTitle = StringUtils.requireNonBlank(title);
        return this;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public ApplicationEntity setDescription(String description) {
        this.description = StringUtils.trimAndNullifyEmpty(description);
        return this;
    }

    public List<String> getApplicationClusters() {
        return List.of(applicationClusters);
    }

    public ApplicationEntity setApplicationClusters(String... applicationClusters) {
        this.applicationClusters = ArrayUtils.requireNonNull(applicationClusters);
        return this;
    }

    public List<String> getSupportedCharacterSets() {
        return List.of(supportedCharacterSets);
    }

    public ApplicationEntity setSupportedCharacterSets(String... supportedCharacterSets) {
        this.supportedCharacterSets = ArrayUtils.requireNonNull(supportedCharacterSets);
        return this;
    }

    public List<String> getPreferredCalledAETs() {
        return List.of(preferredCalledAETs);
    }

    public ApplicationEntity preferredCalledAETs(String... preferredCalledAETs) {
        this.preferredCalledAETs = ArrayUtils.requireNonNull(preferredCalledAETs);
        return this;
    }

    public List<String> setPreferredCallingAETs() {
        return List.of(preferredCallingAETs);
    }

    public ApplicationEntity preferredCallingAETs(String... preferredCallingAETs) {
        this.preferredCallingAETs = ArrayUtils.requireNonNull(preferredCallingAETs);
        return this;
    }

    public List<String> getAcceptedCallingAETs() {
        return List.of(acceptedCallingAETs);
    }

    public ApplicationEntity setAcceptedCallingAETs(String... acceptedCallingAETs) {
        this.acceptedCallingAETs = ArrayUtils.requireNonNull(preferredCallingAETs);
        return this;
    }

    public boolean isAcceptedCallingAET(String aeTitle) {
        return acceptedCallingAETs.length == 0 || ArrayUtils.contains(acceptedCallingAETs, aeTitle);
    }

    public boolean isAssociationAcceptor() {
        return acceptor;
    }

    public ApplicationEntity setAssociationAcceptor(boolean acceptor) {
        this.acceptor = acceptor;
        return this;
    }

    public boolean isAssociationInitiator() {
        return initiator;
    }

    public ApplicationEntity setAssociationInitiator(boolean initiator) {
        this.initiator = initiator;
        return this;
    }

    public Optional<Boolean> getInstalled() {
        return Optional.ofNullable(installed);
    }

    public ApplicationEntity setInstalled(Boolean installed) {
        this.installed = installed;
        return this;
    }

    public boolean isInstalled() {
        return (device == null || device.isInstalled()) && installed != Boolean.FALSE;
    }

    public List<Connection> getConnections() {
        return Collections.unmodifiableList(conns);
    }

    public ApplicationEntity removeConnection(Connection conn) {
        conns.remove(Objects.requireNonNull(conn));
        return this;
    }

    public ApplicationEntity clearConnections() {
        conns.clear();
        return this;
    }

    public ApplicationEntity addConnection(Connection conn) {
        if (device == null) {
            throw new IllegalStateException("No associated Device");
        }
        if (device != conn.getDevice()) {
            throw new IllegalStateException("Connection not contained by associated Device");
        }
        conns.add(conn.setDevice(device));
        return this;
    }

    public boolean hasConnection(Connection conn) {
        return conns.contains(conn);
    }

    public List<TransferCapability> getTransferCapabilities() {
        return Collections.unmodifiableList(tcs);
    }

    public Optional<TransferCapability> getTransferCapabilityOrDefault(
            TransferCapability.Role role, String abstractSyntax) {
        return getTransferCapability(role, abstractSyntax).or(() -> getDefaultTransferCapability(role));
    }

    public Optional<TransferCapability> getDefaultTransferCapability(TransferCapability.Role role) {
        return getTransferCapability(role, "*");
    }

    public Optional<TransferCapability> getTransferCapability(TransferCapability.Role role, String abstractSyntax) {
        for (TransferCapability tc : tcs) {
            if (tc.getRole().equals(role) && tc.getSOPClass().equals(abstractSyntax))
                return Optional.of(tc);
        }
        return Optional.empty();
    }

    public ApplicationEntity removeTransferCapability(TransferCapability tc) {
        tcs.remove(Objects.requireNonNull(tc));
        return this;
    }

    public ApplicationEntity addTransferCapability(TransferCapability tc) {
        tcs.add(Objects.requireNonNull(tc));
        return this;
    }
}
