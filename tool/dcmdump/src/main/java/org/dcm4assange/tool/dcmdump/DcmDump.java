package org.dcm4assange.tool.dcmdump;

import org.dcm4assange.*;
import org.dcm4assange.util.TagUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since May 2021
 */
@CommandLine.Command(
        name = "dcmdump",
        mixinStandardHelpOptions = true,
        versionProvider = DcmDump.VersionProvider.class,
        descriptionHeading = "%n",
        description = "The dcmdump utility dumps the contents of a DICOM file (file format or raw data set) " +
                "to standard output in textual form.",
        parameterListHeading = "%nParameters:%n",
        optionListHeading = "%nOptions:%n",
        showDefaultValues = true,
        footerHeading = "%nExample:%n",
        footer = { "$ dcmdump image.dcm", "Dump DICOM file image.dcm to standard output." }
)
public class DcmDump implements Callable<Integer> {
    static class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[]{ DcmDump.class.getModule().getDescriptor().rawVersion().orElse("7") };
        }
    }

    @CommandLine.Parameters(description = "DICOM input file to be dumped.")
    Path file;

    @CommandLine.Option(names = { "-w", "--width" }, description = "Set output width to <cols>.")
    int cols = 80;

    @CommandLine.Option(names = { "-a", "--alloc" }, description = "Set limit of length of values kept in memory.")
    int limit = 1024;

    private final StringBuilder sb = new StringBuilder();

    public static void main(String[] args) {
        new CommandLine(new DcmDump()).execute(args);
    }

    @Override
    public Integer call() throws Exception {
        try (DicomInputStream2 dis = new DicomInputStream2(Files.newInputStream(file))) {
            dis.withPreambleHandler(this::onPreamble)
                    .withDicomElementHandler(this::onElement)
                    .withItemHandler(this::onItem)
                    .withFragmentHandler(this::onFragment)
                    .withParseItemsEager(true)
                    .readDataSet();
        }
        return 0;
    }

    private void onPreamble(DicomInputStream2 dis) throws IOException {
        System.out.println(dis.promptPreambleTo(sb.append("0: "), cols));
    }

    private boolean onElement(DicomInputStream2 dis, DicomObject2 dcmobj, long header)
            throws IOException {
        long pos = DicomInputStream2.header2position(header);
        int tag = dis.header2tag(header);
        VR vr = VR.fromHeader(header);
        int headerLength = DicomObject2.header2headerLength(header);
        int valueLength = dis.header2valueLength(header);
        sb.setLength(0);
        sb.append(pos).append(": ");
        if (!(vr == VR.SQ || valueLength == -1))
            dis.fillCache(dis.streamPosition() + Math.min(valueLength, cols * 2));
        dcmobj.promptElementTo(header, null, sb, cols);
        System.out.println(sb);
        boolean keep = tag == Tag.TransferSyntaxUID
                || tag == Tag.SpecificCharacterSet
                || TagUtils.isPrivateCreator(tag);
        if (keep) {
            dcmobj.add(header, null);
        }
        if (vr == VR.SQ) {
            dis.parseItems(new Sequence(dcmobj, tag), valueLength);
        } else if (valueLength == -1) {
            dis.parseFragments(new Fragments(dcmobj, tag));
        } else {
            long uvalueLength = valueLength & 0xffffffffL;
            if (!keep && uvalueLength > limit) {
                dis.skip(pos, headerLength + uvalueLength, null);
            }
            dis.seek(pos + headerLength + uvalueLength);
        }
        return true;
    }

    private boolean onItem(DicomInputStream2 dis, Sequence seq, long header) throws IOException {
        if (promptItem(false, dis, header, seq.getDicomObject(), seq.size() + 1)) {
            dis.onItem(seq, header);
        }
        return true;
    }

    private boolean onFragment(DicomInputStream2 dis, Fragments frags, long header) throws IOException {
        if (promptItem(true, dis, header, frags.getDicomObject(), frags.size() + 1)) {
            long pos = DicomInputStream2.header2position(header);
            long uitemlen = dis.header2valueLength(header) & 0xffffffffL;
            if (uitemlen > limit) {
                dis.skip(pos, 8 + uitemlen, null);
            }
            dis.seek(pos + 8 + uitemlen);
        }
        return true;
    }

    private boolean promptItem(boolean fragment, DicomInputStream2 dis, long header, DicomObject2 dcmobj, int no) {
        long pos = DicomInputStream2.header2position(header);
        int tag = dis.header2tag(header);
        int itemLength = dis.header2valueLength(header);
        sb.setLength(0);
        sb.append(pos).append(": ");
        boolean item = tag == Tag.Item;
        if (item) {
            if (fragment) {
                dcmobj.promptFragmentTo(header, null, sb, cols);
            } else {
                dcmobj.promptLevelTo(sb)
                        .append("(FFFE,E000) #").append(itemLength)
                        .append(" Item #").append(no);
            }
        } else {
            dcmobj.promptElementTo(header, null, sb, cols);
            dis.seek(dis.streamPosition() + itemLength);
        }
        System.out.println(sb);
        return item;
    }
}
