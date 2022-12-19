/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Dec 2022
 */
module org.dcm4assange.elmdict.papyrus {
    exports org.dcm4assange.elmdict.papyrus;

    requires org.dcm4assange.core;

    provides org.dcm4assange.ElementDictionary with
            org.dcm4assange.elmdict.papyrus.Papyrus,
            org.dcm4assange.elmdict.papyrus.Papyrus30;
}
