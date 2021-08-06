/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Jul 2021
 */
module org.dcm4assange.elmdict.camtron {
    exports org.dcm4assange.elmdict.camtron;

    requires org.dcm4assange.core;

    provides org.dcm4assange.ElementDictionary with
            org.dcm4assange.elmdict.camtron.Camtronics,
            org.dcm4assange.elmdict.camtron.CamtronicsImageLevelData,
            org.dcm4assange.elmdict.camtron.CamtronicsIP,
            org.dcm4assange.elmdict.camtron.CamtronicsQCA;
}
