/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Dec 2022
 */
module org.dcm4assange.elmdict.spi {
    exports org.dcm4assange.elmdict.spi;

    requires org.dcm4assange.core;

    provides org.dcm4assange.ElementDictionary with
            org.dcm4assange.elmdict.spi.SPI,
            org.dcm4assange.elmdict.spi.SPIRelease,
            org.dcm4assange.elmdict.spi.SPIRELEASE;
}
