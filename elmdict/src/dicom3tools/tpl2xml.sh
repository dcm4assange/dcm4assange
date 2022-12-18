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
sed -i \
-e 's/tag="00210018" owner="GEMS_RELA_01" keyword="GenesisVersionNow" vr="SH" vm="1">Genesis Version Now/tag="00210018" owner="GEMS_RELA_01" keyword="GenesisVersionNow2" vr="SH" vm="1">Genesis Version Now 2/' \
-e 's/tag="00210035" owner="GEMS_RELA_01" keyword="SeriesFromWhichPrescribed" vr="SS" vm="1">Series From Which Prescribed/tag="00210035" owner="GEMS_RELA_01" keyword="SeriesFromWhichPrescribed2" vr="SS" vm="1">Series From Which Prescribed 2/' \
-e 's/tag="000900a4" owner="GEMS_PETD_01" keyword="compatible_version" vr="SH" vm="1">compatible_version/tag="000900a4" owner="GEMS_PETD_01" keyword="compatible_version9" vr="SH" vm="1">compatible_version 9/' \
-e 's/tag="000900a5" owner="GEMS_PETD_01" keyword="software_version" vr="SH" vm="1">software_version/tag="000900a5" owner="GEMS_PETD_01" keyword="software_version9" vr="SH" vm="1">software_version 9/' \
-e 's/tag="000900d4" owner="GEMS_PETD_01" keyword="ctac_conv_scale" vr="LO" vm="1">ctac_conv_scale/tag="000900d4" owner="GEMS_PETD_01" keyword="ctac_conv_scale2" vr="LO" vm="1">ctac_conv_scale 2/' \
-e 's/tag="00170002" owner="GEMS_PETD_01" keyword="compatible_version" vr="SH" vm="1">compatible_version/tag="00170002" owner="GEMS_PETD_01" keyword="compatible_version17" vr="SH" vm="1">compatible_version 17/' \
-e 's/tag="00170003" owner="GEMS_PETD_01" keyword="software_version" vr="SH" vm="1">software_version/tag="00170003" owner="GEMS_PETD_01" keyword="software_version17" vr="SH" vm="1">software_version 17/' \
-e 's/tag="00170005" owner="GEMS_PETD_01" keyword="cal_description" vr="LO" vm="1">cal_datetime/tag="00170005" owner="GEMS_PETD_01" keyword="cal_description" vr="LO" vm="1">cal_description/' \
-e 's/tag="00170009" owner="GEMS_PETD_01" keyword="scan_id" vr="LO" vm="1">scan_id/tag="00170009" owner="GEMS_PETD_01" keyword="scan_id17" vr="LO" vm="1">scan_id 17/' \
-e 's/tag="00190002" owner="GEMS_PETD_01" keyword="compatible_version" vr="SH" vm="1">compatible_version/tag="00190002" owner="GEMS_PETD_01" keyword="compatible_version19" vr="SH" vm="1">compatible_version 19/' \
-e 's/tag="00190003" owner="GEMS_PETD_01" keyword="software_version" vr="SH" vm="1">software_version/tag="00190003" owner="GEMS_PETD_01" keyword="software_version19" vr="SH" vm="1">software_version 19/' \
-e 's/tag="00190004" owner="GEMS_PETD_01" keyword="cal_datetime" vr="DT" vm="1">cal_datetime/tag="00190004" owner="GEMS_PETD_01" keyword="cal_datetime19" vr="DT" vm="1">cal_datetime 19/' \
-e 's/tag="00190005" owner="GEMS_PETD_01" keyword="cal_type" vr="SL" vm="1">cal_type/tag="00190005" owner="GEMS_PETD_01" keyword="cal_type19" vr="SL" vm="1">cal_type 19/' \
-e 's/tag="00190006" owner="GEMS_PETD_01" keyword="cal_description" vr="LO" vm="1">cal_description/tag="00190006" owner="GEMS_PETD_01" keyword="cal_description19" vr="LO" vm="1">cal_description 19/' \
-e 's/tag="0019000c" owner="GEMS_PETD_01" keyword="scan_id" vr="LO" vm="1">scan_id/tag="0019000c" owner="GEMS_PETD_01" keyword="scan_id19" vr="LO" vm="1">scan_id 19/' \
-e 's/tag="0019000d" owner="GEMS_PETD_01" keyword="scan_datetime" vr="DT" vm="1">scan_datetime/tag="0019000d" owner="GEMS_PETD_01" keyword="scan_datetime19" vr="DT" vm="1">scan_datetime 19/' \
-e 's/tag="0019000e" owner="GEMS_PETD_01" keyword="hosp_identifier" vr="SH" vm="1">hosp_identifier/tag="0019000e" owner="GEMS_PETD_01" keyword="hosp_identifier19" vr="SH" vm="1">hosp_identifier 19/' \
-e 's/tag="000900aa" owner="GEMS_PETD_01" keyword="archived" vr="SL" vm="1">archived/tag="000900aa" owner="GEMS_PETD_01" keyword="archived9" vr="SL" vm="1">archived 9/' \
-e 's/tag="0017000d" owner="GEMS_PETD_01" keyword="archived" vr="SL" vm="1">archived/tag="0017000d" owner="GEMS_PETD_01" keyword="archived17" vr="SL" vm="1">archived 17/' \
-e 's/tag="00190014" owner="GEMS_PETD_01" keyword="archived" vr="SL" vm="1">archived/tag="00190014" owner="GEMS_PETD_01" keyword="archived19" vr="SL" vm="1">archived 19/' \
-e 's/tag="50010007" owner="GEMS_PETD_01" keyword="MultiPatient" vr="SL" vm="1">Multi Patient/tag="50010007" owner="GEMS_PETD_01" keyword="MultiPatient5001" vr="SL" vm="1">Multi Patient 5001/' \
-e 's/tag="50030026" owner="GEMS_PETD_01" keyword="MultiPatient" vr="SL" vm="1">Multi Patient/tag="50030026" owner="GEMS_PETD_01" keyword="MultiPatient5003" vr="SL" vm="1">Multi Patient 5003/' \
-e 's/tag="50050003" owner="GEMS_PETD_01" keyword="GraphID" vr="UI" vm="1">Graph ID/tag="50050003" owner="GEMS_PETD_01" keyword="GraphID5005" vr="UI" vm="1">Graph ID 5005/' \
-e 's/tag="50050004" owner="GEMS_PETD_01" keyword="CurveID" vr="UI" vm="1">Curve ID/tag="50050004" owner="GEMS_PETD_01" keyword="CurveID5005" vr="UI" vm="1">Curve ID 5005/' \
-e 's/tag="50010001" owner="GEMS_GENIE_1" keyword="Modified" vr="SL" vm="1">Modified/tag="50010001" owner="GEMS_GENIE_1" keyword="Modified2" vr="SL" vm="1">Modified 2/' \
-e 's/tag="50010038" owner="GEMS_GENIE_1" keyword="Modified" vr="LO" vm="1">Modified/tag="50010038" owner="GEMS_GENIE_1" keyword="Modified3" vr="LO" vm="1">Modified 3/' \
-e 's/tag="50010041" owner="GEMS_GENIE_1" keyword="Modified" vr="SL" vm="1">Modified/tag="50010041" owner="GEMS_GENIE_1" keyword="Modified4" vr="SL" vm="1">Modified 4/' \
-e 's/tag="50010002" owner="GEMS_GENIE_1" keyword="Name" vr="LO" vm="1">Name/tag="50010002" owner="GEMS_GENIE_1" keyword="Name2" vr="LO" vm="1">Name 2/' \
-e 's/tag="50010039" owner="GEMS_GENIE_1" keyword="Name" vr="LO" vm="1">Name/tag="50010039" owner="GEMS_GENIE_1" keyword="Name3" vr="LO" vm="1">Name 3/' \
-e 's/tag="50010042" owner="GEMS_GENIE_1" keyword="Name" vr="LO" vm="1">Name/tag="50010042" owner="GEMS_GENIE_1" keyword="Name4" vr="LO" vm="1">Name 4/' \
-e 's/tag="50010043" owner="GEMS_GENIE_1" keyword="Name" vr="SL" vm="1">Name/tag="50010043" owner="GEMS_GENIE_1" keyword="Name5" vr="SL" vm="1">Name 5/' \
-e 's/tag="50010044" owner="GEMS_GENIE_1" keyword="Name" vr="SL" vm="1">Name/tag="50010044" owner="GEMS_GENIE_1" keyword="Name6" vr="SL" vm="1">Name 6/' \
-e 's/tag="50010035" owner="GEMS_GENIE_1" keyword="DatasetName" vr="LO" vm="1">Dataset Name/tag="50010035" owner="GEMS_GENIE_1" keyword="DatasetName2" vr="LO" vm="1">Dataset Name 2/' \
-e 's/tag="50010045" owner="GEMS_GENIE_1" keyword="SOPClassUID" vr="LO" vm="1">SOP Class UID/tag="50010045" owner="GEMS_GENIE_1" keyword="SOPClassUID2" vr="LO" vm="1">SOP Class UID 2/' \
-e 's/tag="50010046" owner="GEMS_GENIE_1" keyword="SOPInstanceUID" vr="LO" vm="1">SOP Instance UID/tag="50010046" owner="GEMS_GENIE_1" keyword="SOPInstanceUID2" vr="LO" vm="1">SOP Instance UID 2/' \
-e 's/tag="00430087" owner="GEMS_PARM_01" keyword="Reserved" vr="UT" vm="1">Reserved/tag="00430087" owner="GEMS_PARM_01" keyword="Reserved2" vr="UT" vm="1">Reserved 2/' \
gems.xml
