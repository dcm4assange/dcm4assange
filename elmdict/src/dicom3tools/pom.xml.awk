BEGIN {
    FS = "|"
    print "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    print "<!--"
    print "  ~ This file is generated from element dictionary template files included in"
    print "  ~ David Clunie's Dicom3tools Software (https://www.dclunie.com/dicom3tools.html)"
    print "-->"
    print ""
    print "<project xmlns=\"http://maven.apache.org/POM/4.0.0\""
    print "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
    print "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">"
    print "  <parent>"
    print "    <artifactId>dcm4assange-elmdict</artifactId>"
    print "    <groupId>org.dcm4assange.elmdict</groupId>"
    print "    <version>7.0.0</version>"
    print "  </parent>"
    print "  <modelVersion>4.0.0</modelVersion>"
    print ""
    print "  <artifactId>dcm4assange-elmdict-" name "</artifactId>"
    print ""
    print "  <dependencies>"
    print "    <dependency>"
    print "      <groupId>org.dcm4assange</groupId>"
    print "      <artifactId>dcm4assange-core</artifactId>"
    print "      <version>${project.version}</version>"
    print "    </dependency>"
    print "  </dependencies>"
    print ""
    print "  <build>"
    print "    <plugins>"
    print "      <plugin>"
    print "        <groupId>org.codehaus.mojo</groupId>"
    print "        <artifactId>xml-maven-plugin</artifactId>"
    print "        <executions>"
    print "          <execution>"
    print "            <goals>"
    print "              <goal>transform</goal>"
    print "            </goals>"
    print "          </execution>"
    print "        </executions>"
    print "        <configuration>"
    print "          <transformationSets>"
}
{
    print "            <transformationSet>"
    print "              <dir>../src/dicom3tools</dir>"
    print "              <includes>"
    print "                <include>" name ".xml</include>"
    print "              </includes>"
    print "              <stylesheet>../src/xsl/ElementDictionary.java.xsl</stylesheet>"
    print "              <parameters>"
    print "                <parameter>"
    print "                  <name>package</name>"
    print "                  <value>org.dcm4assange.elmdict." name "</value>"
    print "                </parameter>"
    print "                <parameter>"
    print "                  <name>class</name>"
    print "                  <value>" $1 "</value>"
    print "                </parameter>"
    print "                <parameter>"
    print "                  <name>PrivateCreatorID</name>"
    print "                  <value>" $2 "</value>"
    print "                </parameter>"
    print "              </parameters>"
    print "              <outputDir>target/generated-sources/org/dcm4assange/elmdict/" name "</outputDir>"
    print "              <fileMappers>"
    print "                <fileMapper implementation=\"org.codehaus.plexus.components.io.filemappers.MergeFileMapper\">"
    print "                  <targetName>" $1 ".java</targetName>"
    print "                </fileMapper>"
    print "              </fileMappers>"
    print "            </transformationSet>"
    print "            <transformationSet>"
    print "              <dir>../src/dicom3tools</dir>"
    print "              <includes>"
    print "                <include>" name ".xml</include>"
    print "              </includes>"
    print "              <stylesheet>../src/xsl/ElementDictionary.properties.xsl</stylesheet>"
    print "              <parameters>"
    print "                <parameter>"
    print "                  <name>PrivateCreatorID</name>"
    print "                  <value>" $2 "</value>"
    print "                </parameter>"
    print "              </parameters>"
    print "              <outputDir>target/generated-resources/org/dcm4assange/elmdict/" name "</outputDir>"
    print "              <fileMappers>"
    print "                <fileMapper implementation=\"org.codehaus.plexus.components.io.filemappers.MergeFileMapper\">"
    print "                  <targetName>" $1 ".properties</targetName>"
    print "                </fileMapper>"
    print "              </fileMappers>"
    print "            </transformationSet>"
    print "            <transformationSet>"
    print "              <dir>../src/dicom3tools</dir>"
    print "              <includes>"
    print "                <include>" name ".xml</include>"
    print "              </includes>"
    print "              <stylesheet>../src/xsl/ElementDictionary.json.xsl</stylesheet>"
    print "              <parameters>"
    print "                <parameter>"
    print "                  <name>PrivateCreatorID</name>"
    print "                  <value>" $2 "</value>"
    print "                </parameter>"
    print "              </parameters>"
    print "              <outputDir>target/generated-resources</outputDir>"
    print "              <fileMappers>"
    print "                <fileMapper implementation=\"org.codehaus.plexus.components.io.filemappers.MergeFileMapper\">"
    print "                  <targetName>" $1 ".json</targetName>"
    print "                </fileMapper>"
    print "              </fileMappers>"
    print "            </transformationSet>"
}
END {
    print "          </transformationSets>"
    print "        </configuration>"
    print "      </plugin>"
    print "      <plugin>"
    print "        <groupId>org.codehaus.mojo</groupId>"
    print "        <artifactId>build-helper-maven-plugin</artifactId>"
    print "        <executions>"
    print "          <execution>"
    print "            <id>add-source</id>"
    print "            <goals>"
    print "              <goal>add-source</goal>"
    print "            </goals>"
    print "            <configuration>"
    print "              <sources>"
    print "                <source>target/generated-sources</source>"
    print "              </sources>"
    print "            </configuration>"
    print "          </execution>"
    print "          <execution>"
    print "            <id>add-resource</id>"
    print "            <goals>"
    print "              <goal>add-resource</goal>"
    print "            </goals>"
    print "            <configuration>"
    print "              <resources>"
    print "                <resource>"
    print "                  <directory>target/generated-resources</directory>"
    print "                </resource>"
    print "              </resources>"
    print "            </configuration>"
    print "          </execution>"
    print "        </executions>"
    print "      </plugin>"
    print "      <plugin>"
    print "        <artifactId>maven-jar-plugin</artifactId>"
    print "        <executions>"
    print "          <execution>"
    print "            <id>default-jar</id>"
    print "            <phase>package</phase>"
    print "            <goals>"
    print "              <goal>jar</goal>"
    print "            </goals>"
    print "            <configuration>"
    print "              <excludes>"
    print "                <exclude>META-INF/maven/remote-resources.xml</exclude>"
    print "                <exclude>*.json</exclude>"
    print "              </excludes>"
    print "            </configuration>"
    print "          </execution>"
    print "          <execution>"
    print "            <id>json-jar</id>"
    print "            <phase>package</phase>"
    print "            <goals>"
    print "              <goal>jar</goal>"
    print "            </goals>"
    print "            <configuration>"
    print "              <classifier>json</classifier>"
    print "              <includes>"
    print "                <include>META-INF/maven/remote-resources.xml</include>"
    print "                <include>*.json</include>"
    print "              </includes>"
    print "            </configuration>"
    print "          </execution>"
    print "        </executions>"
    print "      </plugin>"
    print "    </plugins>"
    print "  </build>"
    print ""
    print "</project>"
}
