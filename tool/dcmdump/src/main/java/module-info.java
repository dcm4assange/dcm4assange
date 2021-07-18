/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since May 2021
 */
module org.dcm4assange.tool.dcmdump {
    requires org.dcm4assange.core;
    requires info.picocli;

    opens org.dcm4assange.tool.dcmdump to info.picocli;
}