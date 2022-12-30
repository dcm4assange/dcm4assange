// This file is generated from element dictionary template files included in
// David Clunie's Dicom3tools Software (https://www.dclunie.com/dicom3tools.html)

module org.dcm4assange.elmdict.picker {
    exports org.dcm4assange.elmdict.picker;

    requires org.dcm4assange.core;

    provides org.dcm4assange.ElementDictionary with
            org.dcm4assange.elmdict.picker.Picker_MR_Private_Group,
            org.dcm4assange.elmdict.picker.Picker_NM_Private_Group;
}
