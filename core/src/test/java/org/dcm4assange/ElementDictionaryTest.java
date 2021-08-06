package org.dcm4assange;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Aug 2021
 */
public class ElementDictionaryTest {

    @Test
    public void vrOf() {
        assertEquals(VR.UL, ElementDictionary.vrOf(Tag.CommandGroupLength));
        assertEquals(VR.UL, ElementDictionary.vrOf(Tag.FileMetaInformationGroupLength));
        assertEquals(VR.UL, ElementDictionary.vrOf(0x00080000));
        assertEquals(VR.UN, ElementDictionary.vrOf(0x00080002));
        assertEquals(VR.LO, ElementDictionary.vrOf(0x00090010));
        assertEquals(VR.UN, ElementDictionary.vrOf(0x00091010));
        assertEquals(VR.US, ElementDictionary.vrOf(Tag.RowsForNthOrderCoefficients | 0xF0));
        assertEquals(VR.US, ElementDictionary.vrOf(Tag.EscapeTriplet | 0xCAF0));
        assertEquals(VR.CS, ElementDictionary.vrOf(Tag.SourceImageIDs | 0xFE));
        assertEquals(VR.US, ElementDictionary.vrOf(Tag.ZonalMap | 0xCAFE));
        assertEquals(VR.OW, ElementDictionary.vrOf(Tag.CurveData | 0x00CA0000));
        assertEquals(VR.OW, ElementDictionary.vrOf(Tag.OverlayData | 0x00CA0000));
        assertEquals(VR.OW, ElementDictionary.vrOf(Tag.VariablePixelData | 0x00CA0000));
        assertEquals(VR.NONE, ElementDictionary.vrOf(Tag.Item));
    }

    @Test
    public void keywordOf() {
        assertEquals("CommandGroupLength", ElementDictionary.keywordOf(Tag.CommandGroupLength));
        assertEquals("FileMetaInformationGroupLength", ElementDictionary.keywordOf(Tag.FileMetaInformationGroupLength));
        assertEquals("GroupLength", ElementDictionary.keywordOf(0x00080000));
        assertEquals("", ElementDictionary.keywordOf(0x00080002));
        assertEquals("PrivateCreatorID", ElementDictionary.keywordOf(0x00090010));
        assertEquals("", ElementDictionary.keywordOf(0x00091010));
        assertEquals("SourceImageIDs", ElementDictionary.keywordOf(Tag.SourceImageIDs | 0xFE));
        assertEquals("RowsForNthOrderCoefficients", ElementDictionary.keywordOf(Tag.RowsForNthOrderCoefficients | 0xF0));
        assertEquals("EscapeTriplet", ElementDictionary.keywordOf(Tag.EscapeTriplet | 0xCAF0));
        assertEquals("ZonalMap", ElementDictionary.keywordOf(Tag.ZonalMap | 0xCAFE));
        assertEquals("CurveData", ElementDictionary.keywordOf(Tag.CurveData | 0x00CA0000));
        assertEquals("OverlayData", ElementDictionary.keywordOf(Tag.OverlayData | 0x00CA0000));
        assertEquals("VariablePixelData", ElementDictionary.keywordOf(Tag.VariablePixelData | 0x00CA0000));
        assertEquals("Item", ElementDictionary.keywordOf(Tag.Item));
    }

    @Test
    public void tagForKeyword() {
        assertEquals(Tag.CommandGroupLength, ElementDictionary.tagForKeyword("CommandGroupLength"));
        assertEquals(Tag.FileMetaInformationGroupLength, ElementDictionary.tagForKeyword("FileMetaInformationGroupLength"));
        assertEquals(Tag.SourceImageIDs, ElementDictionary.tagForKeyword("SourceImageIDs"));
        assertEquals(Tag.RowsForNthOrderCoefficients, ElementDictionary.tagForKeyword("RowsForNthOrderCoefficients"));
        assertEquals(Tag.EscapeTriplet, ElementDictionary.tagForKeyword("EscapeTriplet"));
        assertEquals(Tag.ZonalMap, ElementDictionary.tagForKeyword("ZonalMap"));
        assertEquals(Tag.CurveData, ElementDictionary.tagForKeyword("CurveData"));
        assertEquals(Tag.OverlayData, ElementDictionary.tagForKeyword("OverlayData"));
        assertEquals(Tag.VariablePixelData, ElementDictionary.tagForKeyword("VariablePixelData"));
        assertEquals(Tag.Item, ElementDictionary.tagForKeyword("Item"));
    }
}