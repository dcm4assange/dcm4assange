// This file is generated from element dictionary template files included in
// David Clunie's Dicom3tools Software (https://www.dclunie.com/dicom3tools.html)

module org.dcm4assange.elmdict.papyrus {
    exports org.dcm4assange.elmdict.papyrus;

    requires org.dcm4assange.core;

    provides org.dcm4assange.ElementDictionary with
            org.dcm4assange.elmdict.papyrus.PAPYRUS,
            org.dcm4assange.elmdict.papyrus.PAPYRUS_3_0;
}
