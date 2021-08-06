/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Jul 2021
 */
module org.dcm4assange.elmdict.acuson {
    exports org.dcm4assange.elmdict.acuson;

    requires org.dcm4assange.core;

    provides org.dcm4assange.ElementDictionary with
            org.dcm4assange.elmdict.acuson.Acuson,
            org.dcm4assange.elmdict.acuson.Acuson0910,
            org.dcm4assange.elmdict.acuson.Acuson7f10,
            org.dcm4assange.elmdict.acuson.Acuson7ffe;
}
