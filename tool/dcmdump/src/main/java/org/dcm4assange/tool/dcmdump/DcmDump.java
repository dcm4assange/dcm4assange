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
        try (DicomInputStream dis = new DicomInputStream(Files.newInputStream(file))) {
            dis.withPreambleHandler(this::onPreamble)
                    .withDicomElementHandler(this::onElement)
                    .withItemHandler(this::onItem)
                    .withFragmentHandler(this::onFragment)
                    .readDataSet();
        }
        return 0;
    }

    private void onPreamble(DicomInputStream dis) throws IOException {
        System.out.println(dis.promptPreambleTo(sb.append("0: "), cols));
    }

    private boolean onElement(DicomInputStream dis, long pos, DicomElement dcmElm)
            throws IOException {
        int tag = dcmElm.tag();
        int valueLength = dcmElm.valueLength();
        sb.setLength(0);
        sb.append(pos).append(": ");
        if (!(dcmElm.vr() == VR.SQ || valueLength == -1))
            dis.fillCache(dis.streamPosition() + Math.min(valueLength, cols * 2));
        dcmElm.promptTo(sb, cols);
        System.out.println(sb);
        boolean keep = tag == Tag.TransferSyntaxUID
                || tag == Tag.SpecificCharacterSet
                || TagUtils.isPrivateCreator(tag);
        if (keep) {
            dcmElm.dicomObject().add(dcmElm);
        }
        int headerLength = (int) (dis.streamPosition() - pos);
        if (dcmElm.vr() == VR.SQ) {
            dis.parseItems(dcmElm, valueLength);
        } else if (valueLength == -1) {
            dis.parseFragments(dcmElm);
        } else {
            if (!keep && valueLength > limit) {
                dis.skip(pos, headerLength + valueLength);
            }
            dis.seek(dis.streamPosition() + valueLength);
        }
        return true;
    }

    private boolean onItem(DicomInputStream dis, long pos, DicomElement dcmElm, DicomElement itemHeader)
            throws IOException {
        return promptItem(dis, pos, dcmElm, itemHeader, this::promptItem);
    }

    private boolean onFragment(DicomInputStream dis, long pos, DicomElement dcmElm, DicomElement itemHeader)
            throws IOException {
        return promptItem(dis, pos, dcmElm, itemHeader, this::promptFragment);
    }

    private boolean promptItem(DicomInputStream dis, long pos, DicomElement dcmElm, DicomElement itemHeader,
                               ItemPrompter itemPrompter) throws IOException {
        sb.setLength(0);
        sb.append(pos).append(": ");
        if (itemHeader.tag() == Tag.Item) {
            itemPrompter.accept(dis, pos, dcmElm, itemHeader);
        } else {
            itemHeader.promptTo(sb, cols);
            System.out.println(sb);
            dis.seek(dis.streamPosition() + itemHeader.valueLength());
        }
        return true;
    }

    @FunctionalInterface
    private interface ItemPrompter {
        void accept(DicomInputStream dis, long pos, DicomElement dcmElm, DicomElement itemHeader) throws IOException;
    }

    private void promptItem(DicomInputStream dis, long pos, DicomElement dcmElm, DicomElement itemHeader)
            throws IOException {
        int itemLength = itemHeader.valueLength();
        dcmElm.promptLevelTo(sb)
                .append("(FFFE,E000) #").append(itemLength)
                .append(" Item #").append(dcmElm.numberOfItems() + 1);
        System.out.println(sb);
        dis.parse(dcmElm.addItem(), itemLength);
    }

    private void promptFragment(DicomInputStream dis, long pos, DicomElement dcmElm, DicomElement itemHeader)
            throws IOException {
        itemHeader.promptTo(sb, cols);
        System.out.println(sb);
        int itemLengh = itemHeader.valueLength();
        if (itemLengh > limit) {
            dis.skip(pos, 8 + itemLengh);
        }
        dis.seek(dis.streamPosition() + itemLengh);
    }
}
