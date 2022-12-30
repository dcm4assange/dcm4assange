// This file is generated from element dictionary template files included in
// David Clunie's Dicom3tools Software (https://www.dclunie.com/dicom3tools.html)

module org.dcm4assange.elmdict.spi {
    exports org.dcm4assange.elmdict.spi;

    requires org.dcm4assange.core;

    provides org.dcm4assange.ElementDictionary with
            org.dcm4assange.elmdict.spi.SPI,
            org.dcm4assange.elmdict.spi.SPI_Release_1,
            org.dcm4assange.elmdict.spi.SPI_RELEASE_1;
}
