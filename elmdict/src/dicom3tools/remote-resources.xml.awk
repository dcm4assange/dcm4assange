BEGIN {
    FS = "|"
    print "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    print "<remoteResourcesBundle xmlns=\"http://maven.apache.org/remote-resources/1.1.0\""
    print "                       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
    print "                       xsi:schemaLocation=\"http://maven.apache.org/remote-resources/1.1.0"
    print "                       https://maven.apache.org/xsd/remote-resources-1.1.0.xsd\">"
    print "  <remoteResources>"
}
{
    print "    <remoteResource>" $1 ".json</remoteResource>"
}
END {
    print "  </remoteResources>"
    print "  <sourceEncoding>UTF-8</sourceEncoding>"
    print "</remoteResourcesBundle>"
}
