BEGIN {
    print "// This file is generated from element dictionary template files included in"
    print "// David Clunie's Dicom3tools Software (https://www.dclunie.com/dicom3tools.html)"
    print ""
    print "module " package " {"
    print "    exports " package ";"
    print ""
    print "    requires org.dcm4assange.core;"
    print ""
    print "    provides org.dcm4assange.ElementDictionary with"
}
{
    if (FNR > 1) print str ","
    str="            " $0
}
END {
    print str ";"
    print "}"
}
