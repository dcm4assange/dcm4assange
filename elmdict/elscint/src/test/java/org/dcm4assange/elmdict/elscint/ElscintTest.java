package org.dcm4assange.elmdict.elscint;

import org.dcm4assange.ElementDictionary;
import org.dcm4assange.VR;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Aug 2021
 */
public class ElscintTest {

    @Test
    public void vrOf() {
        assertEquals(VR.CS, ElementDictionary.vrOf(Elscint.PRIVATE_CREATOR, Elscint.Tag.AngularSamplingDensity | 0x1000));
        assertEquals(VR.FL, ElementDictionary.vrOf(Elscint.PRIVATE_CREATOR, 0x01F1100E));
        assertEquals(VR.UN, ElementDictionary.vrOf(Elscint.PRIVATE_CREATOR, 0x01F1100F));
    }

    @Test
    public void keywordOf() {
        assertEquals("AngularSamplingDensity",
                ElementDictionary.keywordOf(Elscint.PRIVATE_CREATOR, Elscint.Tag.AngularSamplingDensity | 0x1000));
        assertEquals("", ElementDictionary.keywordOf(Elscint.PRIVATE_CREATOR, 0x01F1100E));
        assertEquals("", ElementDictionary.keywordOf(Elscint.PRIVATE_CREATOR, 0x01F1100F));
    }

    @Test
    public void tagForKeyword() {
        assertEquals(Elscint.Tag.AngularSamplingDensity,
                ElementDictionary.tagForKeyword(Elscint.PRIVATE_CREATOR, "AngularSamplingDensity"));
    }
}