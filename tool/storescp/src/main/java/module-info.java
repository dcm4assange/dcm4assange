/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since May 2021
 */
module org.dcm4assange.tool.storescp {
    requires org.dcm4assange.conf.model;
    requires org.dcm4assange.core;
    requires org.dcm4assange.net;
    requires org.slf4j;
    requires info.picocli;

    opens org.dcm4assange.tool.storescp to info.picocli;
}