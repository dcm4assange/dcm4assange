/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Jul 2021
 */
module org.dcm4assange.elmdict.elscint {
    exports org.dcm4assange.elmdict.elscint;

    requires org.dcm4assange.core;

    provides org.dcm4assange.ElementDictionary with org.dcm4assange.elmdict.elscint.Elscint;
}
