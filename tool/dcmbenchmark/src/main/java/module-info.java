/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Dec 2022
 */
module org.dcm4assange.tool.dcmbenchmark {
    requires org.dcm4assange.core;
    requires info.picocli;

    opens org.dcm4assange.tool.dcmbenchmark to info.picocli;
}