cd "`dirname "$0"`"
DICOM3TOOLS=dicom3tools_1.00.snapshot.20230917093524
curl https://www.dclunie.com/dicom3tools/workinprogress/${DICOM3TOOLS}.tar.bz2 | \
tar xj --strip-components=4 \
${DICOM3TOOLS}/libsrc/standard/elmdict/acuson.tpl \
${DICOM3TOOLS}/libsrc/standard/elmdict/agfa.tpl \
${DICOM3TOOLS}/libsrc/standard/elmdict/camtron.tpl \
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
