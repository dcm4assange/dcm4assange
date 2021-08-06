/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Jul 2021
 */
module org.dcm4assange.elmdict.agfa {
    exports org.dcm4assange.elmdict.agfa;

    requires org.dcm4assange.core;

    provides org.dcm4assange.ElementDictionary with
            org.dcm4assange.elmdict.agfa.Agfa,
            org.dcm4assange.elmdict.agfa.AgfaADC,
            org.dcm4assange.elmdict.agfa.AgfaDisplayableImage,
            org.dcm4assange.elmdict.agfa.AgfaHPState,
            org.dcm4assange.elmdict.agfa.AgfaKOSD,
            org.dcm4assange.elmdict.agfa.AgfaNX,
            org.dcm4assange.elmdict.agfa.AgfaPACS,
            org.dcm4assange.elmdict.agfa.AgfaXeroverse,
            org.dcm4assange.elmdict.agfa.AgilityOverlay,
            org.dcm4assange.elmdict.agfa.AgilityRuntime,
            org.dcm4assange.elmdict.agfa.MitraLinkedAttributes,
            org.dcm4assange.elmdict.agfa.MitraMarkup;
}
