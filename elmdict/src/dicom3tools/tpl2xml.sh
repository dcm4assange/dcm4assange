cd "`dirname "$0"`"

function generate_sources
{
  awk -f tpl2xml.awk $1.tpl > $1.xml
  grep owner $1.xml | cut -d\"  -f4 | sort | uniq \
  | awk '{
  name=$0
  gsub("[\? \.,;:-/()]+","_",name)
  gsub("^1","One",name)
  gsub("^2","Two",name)
  gsub("^3","Three",name)
  print name "|" $0}' \
  | tee $1.tmp \
  | awk -v name=$1 'BEGIN { FS = "|" } { print "org.dcm4assange.elmdict." name "." $1 }' \
  | tee ../../$1/src/main/resources/META-INF/services/org.dcm4assange.ElementDictionary \
  | awk -v name=$1 -f module-info.java.awk > ../../$1/src/main/java/module-info.java
  awk -f remote-resources.xml.awk $1.tmp > ../../$1/src/main/resources/META-INF/maven/remote-resources.xml
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
