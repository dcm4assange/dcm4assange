package org.dcm4assange.conf.model;

import org.dcm4assange.UID;
import org.dcm4assange.util.ArrayUtils;
import org.dcm4assange.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since May 2019
 */
public class TransferCapability {
    public enum Role { SCU, SCP }

    private String name;
    private String sopClass = UID.Verification;
    private Role role = Role.SCP;
    private String[] transferSyntaxes = { "*" };

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public TransferCapability setName(String name) {
        this.name = StringUtils.trimAndNullifyEmpty(name);
        return this;
    }

    public String getSOPClass() {
        return sopClass;
    }

    public TransferCapability setSOPClass(String sopClass) {
        this.sopClass = StringUtils.requireNonBlank(sopClass);
        return this;
    }

    public Role getRole() {
        return role;
    }

    public TransferCapability setRole(Role role) {
        this.role = Objects.requireNonNull(role);
        return this;
    }

    public String[] getTransferSyntaxes() {
        return transferSyntaxes.clone();
    }

    public TransferCapability setTransferSyntaxes(String... transferSyntaxes) {
        this.transferSyntaxes = ArrayUtils.requireNonNull(transferSyntaxes);
        return this;
    }

    public Optional<String> selectTransferSyntax(String... from) {
        if (from.length > 0) {
            for (String ts : transferSyntaxes) {
                if (ArrayUtils.contains(from, ts)) {
                    return Optional.of(ts);
                }
            }
            if (ArrayUtils.contains(transferSyntaxes, "*")) {
                return Optional.of(from[0]);
            }
        }
        return Optional.empty();
    }
}
