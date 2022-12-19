/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Jul 2021
 */
module org.dcm4assange.elmdict.hitachi {
    exports org.dcm4assange.elmdict.hitachi;

    requires org.dcm4assange.core;

    provides org.dcm4assange.ElementDictionary with
            org.dcm4assange.elmdict.hitachi.Harmony10,
            org.dcm4assange.elmdict.hitachi.Harmony10C2,
            org.dcm4assange.elmdict.hitachi.Harmony10C3,
            org.dcm4assange.elmdict.hitachi.Harmony20;
}
