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
        assertEquals(VR.CS, ElementDictionary.vrOf(ELSCINT1.PRIVATE_CREATOR, ELSCINT1.Tag.AngularSamplingDensity | 0x1000));
        assertEquals(VR.FL, ElementDictionary.vrOf(ELSCINT1.PRIVATE_CREATOR, 0x01F1100E));
        assertEquals(VR.UN, ElementDictionary.vrOf(ELSCINT1.PRIVATE_CREATOR, 0x01F1100F));
    }

    @Test
    public void keywordOf() {
        assertEquals("AngularSamplingDensity",
                ElementDictionary.keywordOf(ELSCINT1.PRIVATE_CREATOR, ELSCINT1.Tag.AngularSamplingDensity | 0x1000));
        assertEquals("", ElementDictionary.keywordOf(ELSCINT1.PRIVATE_CREATOR, 0x01F1100E));
        assertEquals("", ElementDictionary.keywordOf(ELSCINT1.PRIVATE_CREATOR, 0x01F1100F));
    }

    @Test
    public void tagForKeyword() {
        assertEquals(ELSCINT1.Tag.AngularSamplingDensity,
                ElementDictionary.tagForKeyword(ELSCINT1.PRIVATE_CREATOR, "AngularSamplingDensity"));
    }
}