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
grep '"CARDIO-D.R. 1.0"' siemens.xml | grep DisplayedArea >> philips.xml
grep '"SPI"' siemens.xml >> spi.xml
grep '"SPI Release 1"' philips.xml | grep 00090008 >> spi.xml
sed -i '/"1.2.840.113708.794.1.1.2.0"/d' other.xml
sed -i '/"CARDIO-D.R. 1.0"/d' siemens.xml
sed -i '/"SPI"/d' siemens.xml
sed -i '/"SPI Release 1"/d' philips.xml
