cd "`dirname "$0"`"
DICOM3TOOLS=dicom3tools_1.00.snapshot.20220618093127
curl https://www.dclunie.com/dicom3tools/workinprogress/${DICOM3TOOLS}.tar.bz2 | \
tar xj --strip-components=4 --to-command='awk -f tpl2xml.awk > ${TAR_FILENAME%tpl}xml' \
${DICOM3TOOLS}/libsrc/standard/elmdict/acuson.tpl \
${DICOM3TOOLS}/libsrc/standard/elmdict/agfa.tpl \
${DICOM3TOOLS}/libsrc/standard/elmdict/camtron.tpl \
${DICOM3TOOLS}/libsrc/standard/elmdict/diconde.tpl \
${DICOM3TOOLS}/libsrc/standard/elmdict/dicondep.tpl \
${DICOM3TOOLS}/libsrc/standard/elmdict/dicos.tpl \
${DICOM3TOOLS}/libsrc/standard/elmdict/elscint.tpl \
${DICOM3TOOLS}/libsrc/standard/elmdict/gems.tpl \
${DICOM3TOOLS}/libsrc/standard/elmdict/hitachi.tpl \
${DICOM3TOOLS}/libsrc/standard/elmdict/isg.tpl \
${DICOM3TOOLS}/libsrc/standard/elmdict/other.tpl \
${DICOM3TOOLS}/libsrc/standard/elmdict/philips.tpl \
${DICOM3TOOLS}/libsrc/standard/elmdict/picker.tpl \
${DICOM3TOOLS}/libsrc/standard/elmdict/papyrus.tpl \
${DICOM3TOOLS}/libsrc/standard/elmdict/siemens.tpl \
${DICOM3TOOLS}/libsrc/standard/elmdict/spi.tpl \
${DICOM3TOOLS}/libsrc/standard/elmdict/toshiba.tpl
# remove duplicates
tag002900AC=$(grep 'tag="002900AC" owner="CARDIO-D.R. 1.0"' siemens.xml)
tag002900AD=$(grep 'tag="002900AD" owner="CARDIO-D.R. 1.0"' siemens.xml)
tag00290060=$(grep 'tag="00290060" owner="SPI"' siemens.xml)
tag00090008=$(grep 'tag="00090008" owner="SPI Release 1"' philips.xml)
sed -i '/tag="002900af" owner="SPI-P Release 1"/i'"${tag002900AC}\n${tag002900AD}" philips.xml
sed -i \
-e '/tag="00090010" owner="SPI"/i'"${tag00090008}" \
-e '/tag="00290060" owner="SPI RELEASE 1"/i'"${tag00290060}" \
spi.xml
sed -i '/"1.2.840.113708.794.1.1.2.0"/d' other.xml
sed -i '/"CARDIO-D.R. 1.0"/d' siemens.xml
sed -i '/"SPI"/d' siemens.xml
sed -i '/"SPI Release 1"/d' philips.xml

function generate_sources
{
  PACKAGE=org.dcm4assange.elmdict.$1
  grep owner $1.xml \
  | cut -d\"  -f4 \
  | sort \
  | uniq \
  | awk '{
  name=$0
  gsub("[\? \.,;:-/()]+","_",name)
  gsub("^1","One",name)
  gsub("^2","Two",name)
  gsub("^3","Three",name)
  print name "|" $0}' \
  | tee $1.tmp \
  | awk -v package=$PACKAGE 'BEGIN { FS = "|" } { print package "." $1 }' \
  | tee ../../$1/src/main/resources/META-INF/services/org.dcm4assange.ElementDictionary \
  | awk -v package=$PACKAGE -f module-info.java.awk > ../../$1/src/main/java/module-info.java
  awk -v name=$1 -f pom.xml.awk $1.tmp > ../../$1/pom.xml
  rm $1.tmp
}

generate_sources acuson
generate_sources agfa
generate_sources camtron
generate_sources elscint
generate_sources gems
generate_sources hitachi
generate_sources isg
generate_sources other
generate_sources philips
generate_sources picker
generate_sources papyrus
generate_sources siemens
generate_sources spi
generate_sources toshiba
