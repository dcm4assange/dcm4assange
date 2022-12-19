/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Jul 2021
 */
module org.dcm4assange.elmdict.toshiba {
    exports org.dcm4assange.elmdict.toshiba;

    requires org.dcm4assange.core;

    provides org.dcm4assange.ElementDictionary with
            org.dcm4assange.elmdict.toshiba.CanonMecCT3,
            org.dcm4assange.elmdict.toshiba.PMTFInformationData,
            org.dcm4assange.elmdict.toshiba.ToshibaComaplHeader,
            org.dcm4assange.elmdict.toshiba.ToshibaComaplOOG,
            org.dcm4assange.elmdict.toshiba.ToshibaEncryptedSRData,
            org.dcm4assange.elmdict.toshiba.ToshibaMDWHeader,
            org.dcm4assange.elmdict.toshiba.ToshibaMDWNonImage,
            org.dcm4assange.elmdict.toshiba.ToshibaMec,
            org.dcm4assange.elmdict.toshiba.ToshibaMecCT1,
            org.dcm4assange.elmdict.toshiba.ToshibaMecCT3,
            org.dcm4assange.elmdict.toshiba.ToshibaMecMR3,
            org.dcm4assange.elmdict.toshiba.ToshibaMecOT3,
            org.dcm4assange.elmdict.toshiba.ToshibaMecXA3,
            org.dcm4assange.elmdict.toshiba.ToshibaSR;
}
