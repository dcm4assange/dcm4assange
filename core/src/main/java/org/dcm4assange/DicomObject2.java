package org.dcm4assange;

public class DicomObject2 {
    private static final long PARSED = 0xc000000000000000L;
    private static final long POSITION = 0x000fffffffffffffL;

    private final MemoryCache.DicomInput dicomInput;
    private long[] elmRefs;
    private int size;

    DicomObject2(MemoryCache.DicomInput dicomInput, int capacity) {
        this.dicomInput = dicomInput;
        this.elmRefs = new long[capacity];
    }

    private int tagOf(long elmRef) {
        return (elmRef & PARSED) == 0 ? (int) elmRef : dicomInput.tagAt(elmRef & POSITION);
    }

    private VR vrOf(long elmRef) {
        return VR.get(((int) (elmRef >> 52)) & 0x3ff);
    }
}
