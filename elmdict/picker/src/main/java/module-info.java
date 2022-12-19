/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Dec 2022
 */
module org.dcm4assange.elmdict.picker {
    exports org.dcm4assange.elmdict.picker;

    requires org.dcm4assange.core;

    provides org.dcm4assange.ElementDictionary with
            org.dcm4assange.elmdict.picker.PickerMR,
            org.dcm4assange.elmdict.picker.PickerNM;
}
