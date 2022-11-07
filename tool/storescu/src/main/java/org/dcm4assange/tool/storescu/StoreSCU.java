package org.dcm4assange.tool.storescu;

import org.dcm4assange.*;
import org.dcm4assange.conf.model.ApplicationEntity;
import org.dcm4assange.conf.model.Connection;
import org.dcm4assange.conf.model.Device;
import org.dcm4assange.net.AAssociate;
import org.dcm4assange.net.Association;
import org.dcm4assange.net.DeviceRuntime;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Dec 2019
 */
@CommandLine.Command(
        name = "storescu",
        mixinStandardHelpOptions = true,
        versionProvider = StoreSCU.ModuleVersionProvider.class,
        descriptionHeading = "%n",
        description = "The storescu application implements a Service Class User (SCU) for the Verification and Storage SOP Class.",
        parameterListHeading = "%nParameters:%n",
        optionListHeading = "%nOptions:%n",
        showDefaultValues = true,
        footerHeading = "%nExample:%n",
        footer = { "$ storescu --called DCM4CHEE localhost 11112 image.dcm",
                "Sends image.dcm to Application Entity DCM4CHEE listening on port 11112 at localhost." }
)
public class StoreSCU implements Callable<Integer> {

    static class ModuleVersionProvider implements CommandLine.IVersionProvider {
        public String[] getVersion() {
            return new String[]{StoreSCU.class.getModule().getDescriptor().rawVersion().orElse("7")};
        }
    }

    @CommandLine.Parameters(
            description = "Hostname of DICOM peer AE.",
            index = "0")
    String peer;

    @CommandLine.Parameters(
            description = "TCP/IP port number of peer AE.",
            showDefaultValue = CommandLine.Help.Visibility.NEVER,
            index = "1")
    int port;

    @CommandLine.Parameters(
            description = { "DICOM file or directory to be transmitted.",
                    "If absent, verifies the communication by sending a C-ECHO RQ."},
            index = "2..*")
    List<Path> file;

    @CommandLine.Option(names = "--calling", paramLabel = "<aetitle>",
            description = "Set my calling AE title.")
    String calling = "STORESCU";

    @CommandLine.Option(names = "--called", paramLabel = "<aetitle>",
            description = "Set called AE title of peer AE.")
    String called = "STORESCP";

    @CommandLine.Option(names = "--not-async",
            description = "Do not use asynchronous mode; equivalent to --max-ops-invoked=1 and --max-ops-performed=1.")
    boolean notAsync;

    @CommandLine.Option(names = "--max-ops-invoked", paramLabel = "<no>",
            description = "Maximum number of outstanding operations invoked asynchronously, 0 = unlimited.")
    int maxOpsInvoked;

    @CommandLine.Option(names = "--max-ops-performed", paramLabel = "<no>",
            description = "Maximum number of outstanding operations performed asynchronously, 0 = unlimited.")
    int maxOpsPerformed;

    @CommandLine.Option(names = "--max-pdulen-rcv", paramLabel = "<size>",
            description = "Maximum length of received P-DATA-TF PDUs, 0 = unlimited.")
    int receivePduLength;

    @CommandLine.Option(names = "--max-pdulen-snd", paramLabel = "<size>",
            description = "Maximum length of sent P-DATA-TF PDUs.")
    int sendPduLength = Connection.DEF_SEND_PDU_LENGTH;

    @CommandLine.Option(names = "--sosnd-buffer", paramLabel = "<size>",
            description = "Set SO_SNDBUF socket option to specified value, 0 = use default.")
    int sndBufferSize;

    @CommandLine.Option(names = "--sorcv-buffer", paramLabel = "<size>",
            description = "Set SO_RCVBUF socket option to specified value, 0 = use default.")
    int rcvBufferSize;

    @CommandLine.Option(names = "--tcp-delay",
            description = "Set TCP_NODELAY socket option to false, true by default.")
    boolean tcpDelay;

    @CommandLine.Option(names = "--not-pack-pdv", paramLabel = "<size>",
            description = {"Send only one PDV in one P-Data-TF PDU.",
                    "Pack command and data PDV in one P-DATA-TF PDU by default." })
    boolean notPackPDV;

    private final List<FileInfo> fileInfos = new ArrayList<>();

    public static void main(String[] args) {
        new CommandLine(new StoreSCU()).execute(args);
    }

    @Override
    public Integer call() throws Exception {
        Connection remoteConn = new Connection()
                .setHostname(peer)
                .setPort(port);
        Connection localConn = new Connection()
                .setMaxOpsInvoked(notAsync ? 1 : maxOpsInvoked)
                .setMaxOpsInvoked(notAsync ? 1 : maxOpsPerformed)
                .setSendPDULength(sendPduLength)
                .setReceivePDULength(receivePduLength)
                .setSendBufferSize(sndBufferSize)
                .setReceiveBufferSize(rcvBufferSize)
                .setTcpNoDelay(!tcpDelay)
                .setPackPDV(!notPackPDV);
        ApplicationEntity ae = new ApplicationEntity().setAETitle(calling);
        Device device = new Device().setDeviceName(calling).addConnection(localConn).addApplicationEntity(ae);
        DeviceRuntime runtime = new DeviceRuntime(device, null);
        AAssociate.RQ rq = new AAssociate.RQ(calling, called);
        rq.setAsyncOpsWindow(localConn);
        rq.setMaxPDULength(localConn);
        if (file == null) {
            rq.addPresentationContext(UID.Verification, UID.ImplicitVRLittleEndian);
            Association as = runtime.openAssociation(ae, localConn, remoteConn, rq).join();
            as.cecho();
            as.release().join();
        } else {
            for (Path path : file) {
                try (Stream<Path> walk = Files.walk(path)) {
                    walk.filter(file -> Files.isRegularFile(file)).forEach(this::scanFile);
                }
            }
            fileInfos.forEach(info ->
                    rq.findOrAddPresentationContext(info.sopClassUID, info.transferSyntax));
            long t1 = System.currentTimeMillis();
            Association as = runtime.openAssociation(ae, localConn, remoteConn, rq).join();
            long t2 = System.currentTimeMillis();
            System.out.format("Opened DICOM association in %d ms%n", t2 - t1);
            long totLength = 0;
            for (FileInfo fileInfo : fileInfos) {
                as.cstore(fileInfo.sopClassUID, fileInfo.sopInstanceUID, fileInfo, fileInfo.transferSyntax);
                totLength += fileInfo.length;
            }
            as.release().join();
            long t3 = System.currentTimeMillis();
            long dt = t3 - t2;
            System.out.format("Sent %d objects (%f MB) in %d ms (%f MB/s)%n",
                    fileInfos.size(), totLength / 1000000.f, dt, totLength / (dt * 1000.f));
        }
        return 0;
    }

    private void scanFile(Path path) {
        try (DicomInputStream dis = new DicomInputStream(Files.newInputStream(path))) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.path = path;
            fileInfo.length = Files.size(path);
            DicomObject fmi = dis.readFileMetaInformation();
            if (fmi != null) {
                fileInfo.sopClassUID = fmi.getStringOrElseThrow(Tag.MediaStorageSOPClassUID);
                fileInfo.sopInstanceUID = fmi.getStringOrElseThrow(Tag.MediaStorageSOPInstanceUID);
                fileInfo.transferSyntax = fmi.getStringOrElseThrow(Tag.TransferSyntaxUID);
                fileInfo.position = dis.streamPosition();
                fileInfo.length -= fileInfo.position;
            } else {
                dis.withDicomElementHandler(fileInfo::onElement).readDataSet();
            }
            fileInfos.add(fileInfo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class FileInfo implements Association.DataWriter {
        Path path;
        String sopClassUID;
        String sopInstanceUID;
        String transferSyntax;
        long position;
        long length;

        private boolean onElement(DicomInputStream dis, DicomObject dcmobj, long header)
                throws IOException {
            int tag = dis.header2tag(header);
            VR vr = VR.fromHeader(header);
            int valueLength = dis.header2valueLength(header);
            long valueEnd = dis.streamPosition() + (valueLength & 0xffffffffL);
            dis.fillCache(valueEnd);
            switch(tag) {
                case Tag.SOPClassUID:
                    sopClassUID = new String(dis.readNBytes(valueLength)).trim();
                    return true;
                case Tag.SOPInstanceUID:
                    sopInstanceUID = new String(dis.readNBytes(valueLength)).trim();
                    transferSyntax = dis.encoding().transferSyntaxUID;
                    return false;
            }
            if (vr == VR.SQ) {
                dis.parseItems(dcmobj.newSequence(tag), valueLength);
            } else if (valueLength == -1) {
                dis.parseFragments(dcmobj.newFragments(tag, header));
            } else {
                dis.seek(valueEnd);
            }
            return true;
        }

        @Override
        public void writeTo(OutputStream out, String tsuid) throws IOException {
            try (InputStream in = Files.newInputStream(path)) {
                in.skipNBytes(position);
                in.transferTo(out);
            }
        }
    }
}
