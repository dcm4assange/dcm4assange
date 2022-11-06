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

package org.dcm4assange.tool.storescp;

import org.dcm4assange.DicomObject;
import org.dcm4assange.DicomOutputStream;
import org.dcm4assange.Tag;
import org.dcm4assange.conf.model.ApplicationEntity;
import org.dcm4assange.conf.model.Connection;
import org.dcm4assange.conf.model.Device;
import org.dcm4assange.conf.model.TransferCapability;
import org.dcm4assange.net.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Sep 2022
 */
@CommandLine.Command(
        name = "storescp",
        mixinStandardHelpOptions = true,
        versionProvider = StoreSCP.VersionProvider.class,
        descriptionHeading = "%n",
        description = "The storescp application implements a Service Class Provider (SCP) for the Storage Service Class.",
        parameterListHeading = "%nParameters:%n",
        optionListHeading = "%nOptions:%n",
        showDefaultValues = true,
        footerHeading = "%nExample:%n",
        footer = { "$ storescp 11112",
                "Starts server listening on port 11112" }
)
public class StoreSCP implements Callable<Integer> {
    static final Logger LOG = LoggerFactory.getLogger(StoreSCP.class);
    static class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[]{ StoreSCP.class.getModule().getDescriptor().rawVersion().orElse("7") };
        }
    }

    @CommandLine.Parameters(
            description = "tcp/ip port number to listen on",
            showDefaultValue = CommandLine.Help.Visibility.NEVER,
            index = "0")
    int port;

    @CommandLine.Parameters(
            description = "directory to which received DICOM Composite Objects are stored",
            arity = "0..1",
            index = "1")
    Path directory;

    @CommandLine.Option(names = "--called", paramLabel = "<aetitle>",
            description = "accepted called AE title")
    String called = "*";

    @CommandLine.Option(names = "--max-ops-invoked", paramLabel = "<no>",
            description = "maximum number of outstanding operations it allows the Association-requester " +
                    "to invoke asynchronously, 0 = unlimited")
    int maxOpsInvoked = 1;

    @CommandLine.Option(names = "--max-pdu-length", paramLabel = "<no>",
            description = "maximum length of received P-DATA-TF PDUs, 0 = unlimited")
    int maxPduLength = 0;

    public static void main(String[] args) {
        new CommandLine(new StoreSCP()).execute(args);
    }

    @Override
    public Integer call() throws Exception {
        if (directory != null) {
            Files.createDirectories(directory);
        }
        Connection conn = new Connection()
                .setPort(port)
                .setReceivePDULength(maxPduLength)
                .setMaxOpsPerformed(maxOpsInvoked);
        ApplicationEntity ae = new ApplicationEntity().setAETitle(called);
        ae.addTransferCapability(new TransferCapability()
                .setSOPClass("*")
                .setTransferSyntaxes("*")
                .setRole(TransferCapability.Role.SCP));
        Device device = new Device().addConnection(conn).addApplicationEntity(ae);
        DeviceRuntime runtime = new DeviceRuntime(device,
                new DicomServiceRegistry().setDefaultRQHandler(this::onDimseRQ));
        runtime.bindConnections();
        Thread.sleep(Long.MAX_VALUE);
        return 0;
    }

    private void onDimseRQ(Association as, Byte pcid, Dimse dimse, DicomObject commandSet, InputStream dataStream)
            throws IOException {
        if (dimse != Dimse.C_STORE_RQ) {
            throw new DicomServiceException(Status.UnrecognizedOperation);
        }
        if (directory == null) {
            while (dataStream.skip(Integer.MAX_VALUE) > 0);
        } else {
            Path file = directory.resolve(commandSet.getStringOrElseThrow(Tag.AffectedSOPInstanceUID));
            LOG.info("Start M-WRITE {}", file);
            try (DicomOutputStream dos = new DicomOutputStream(Files.newOutputStream(file))) {
                dos.writeFileMetaInformation(DicomObject.createFileMetaInformation(
                        commandSet.getStringOrElseThrow(Tag.AffectedSOPClassUID),
                        commandSet.getStringOrElseThrow(Tag.AffectedSOPInstanceUID),
                        as.getTransferSyntax(pcid),
                        true));
                dataStream.transferTo(dos);
            }
            LOG.info("Finished M-WRITE {}", file);
        }
        as.writeDimse(pcid, Dimse.C_STORE_RSP, dimse.mkRSP(commandSet));
    }
}
