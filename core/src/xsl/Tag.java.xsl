<?xml version="1.0" encoding="UTF-8"?>

<!--
  ~ Copyright 2021 the original author or authors.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text"></xsl:output>
  <xsl:template match="/elements">
    <xsl:text>/*
 * This file is generated from DICOM Standard Documents PS3.6 Data Dictionary and PS3.7	Message Exchange
 * in DocBook format available at http://dicom.nema.org/medical/dicom/current/source/docbook/
 */

package org.dcm4assange;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Jul 2021
 */
public class Tag {
</xsl:text>
    <xsl:apply-templates select="//el[@keyword!='']" mode="Tag"/>
    <xsl:text>
    /** (0008,0012) VR=DA VM=1 Instance Creation Date
     *  (0008,0013) VR=TM VM=1 Instance Creation Time */
    public static final long InstanceCreationDateAndTime = 0x0008001200080013L;

    /** (0008,0020) VR=DA VM=1 Study Date
     *  (0008,0030) VR=TM VM=1 Study Time */
    public static final long StudyDateAndTime = 0x0008002000080030L;

    /** (0008,0021) VR=DA VM=1 Series Date
     *  (0008,0031) VR=TM VM=1 Series Time */
    public static final long SeriesDateAndTime = 0x0008002100080031L;

    /** (0008,0022) VR=DA VM=1 Acquisition Date
     *  (0008,0032) VR=TM VM=1 Acquisition Time */
    public static final long AcquisitionDateAndTime = 0x0008002200080032L;

    /** (0008,0023) VR=DA VM=1 Content Date
     *  (0008,0033) VR=TM VM=1 Content Time */
    public static final long ContentDateAndTime = 0x0008002300080033L;

    /** (0008,0024) VR=DA VM=1 Overlay Date (retired)
     *  (0008,0034) VR=TM VM=1 Overlay Time (retired) */
    public static final long OverlayDateAndTime = 0x0008002400080034L;

    /** (0008,0025) VR=DA VM=1 Curve Date (retired)
     *  (0008,0035) VR=TM VM=1 Curve Time (retired) */
    public static final long CurveDateAndTime = 0x0008002500080035L;

    /** (0010,0030) VR=DA VM=1 Patient's Birth Date
     *  (0010,0032) VR=TM VM=1 Patient's Birth Time */
    public static final long PatientBirthDateAndTime = 0x0010003000100032L;

    /** (0018,1012) VR=DA VM=1 Date of Secondary Capture
     *  (0018,1014) VR=TM VM=1 Time of Secondary Capture */
    public static final long DateAndTimeOfSecondaryCapture = 0x0018101200181014L;

    /** (0018,1200) VR=DA VM=1-n Date of Last Calibration
     *  (0018,1201) VR=TM VM=1-n Time of Last Calibration */
    public static final long DateAndTimeOfLastCalibration = 0x0018120000181201L;

    /** (0018,700C) VR=DA VM=1 Date of Last Detector Calibration
     *  (0018,700E) VR=TM VM=1 Time of Last Detector Calibration */
    public static final long DateAndTimeOfLastDetectorCalibration = 0x0018700C0018700EL;

    /** (0020,3403) VR=DA VM=1 Modified Image Date (retired)
     *  (0020,3405) VR=TM VM=1 Modified Image Time (retired) */
    public static final long ModifiedImageDateAndTime = 0x0020340300203405L;

    /** (0032,0032) VR=DA VM=1 Study Verified Date (retired)
     *  (0032,0033) VR=TM VM=1 Study Verified Time (retired) */
    public static final long StudyVerifiedDateAndTime = 0x0032003200320033L;

    /** (0032,0034) VR=DA VM=1 Study Read Date (retired)
     *  (0032,0035) VR=TM VM=1 Study Read Time (retired) */
    public static final long StudyReadDateAndTime = 0x0032003400320035L;

    /** (0032,1000) VR=DA VM=1 Scheduled Study Start Date (retired)
     *  (0032,1001) VR=TM VM=1 Scheduled Study Start Time (retired) */
    public static final long ScheduledStudyStartDateAndTime = 0x0032100000321001L;

    /** (0032,1010) VR=DA VM=1 Scheduled Study Stop Date (retired)
     *  (0032,1011) VR=TM VM=1 Scheduled Study Stop Time (retired) */
    public static final long ScheduledStudyStopDateAndTime = 0x0032101000321011L;

    /** (0032,1040) VR=DA VM=1 Study Arrival Date (retired)
     *  (0032,1041) VR=TM VM=1 Study Arrival Time (retired) */
    public static final long StudyArrivalDateAndTime = 0x0032104000321041L;

    /** (0032,1050) VR=DA VM=1 Study Completion Date (retired)
     *  (0032,1051) VR=TM VM=1 Study Completion Time (retired) */
    public static final long StudyCompletionDateAndTime = 0x0032105000321051L;

    /** (0038,001A) VR=DA VM=1 Scheduled Admission Date (retired)
     *  (0038,001B) VR=TM VM=1 Scheduled Admission Time (retired) */
    public static final long ScheduledAdmissionDateAndTime = 0x0038001A0038001BL;

    /** (0038,001C) VR=DA VM=1 Scheduled Discharge Date (retired)
     *  (0038,001D) VR=TM VM=1 Scheduled Discharge Time (retired) */
    public static final long ScheduledDischargeDateAndTime = 0x0038001C0038001DL;

    /** (0038,0020) VR=DA VM=1 Admitting Date
     *  (0038,0021) VR=TM VM=1 Admitting Time */
    public static final long AdmittingDateAndTime = 0x0038002000380021L;

    /** (0038,0030) VR=DA VM=1 Discharge Date (retired)
     *  (0038,0032) VR=TM VM=1 Discharge Time (retired) */
    public static final long DischargeDateAndTime = 0x0038003000380032L;

    /** (0040,0002) VR=DA VM=1 Scheduled Procedure Step Start Date
     *  (0040,0003) VR=TM VM=1 Scheduled Procedure Step Start Time */
    public static final long ScheduledProcedureStepStartDateAndTime = 0x0040000200400003L;

    /** (0040,0004) VR=DA VM=1 Scheduled Procedure Step End Date
     *  (0040,0005) VR=TM VM=1 Scheduled Procedure Step End Time */
    public static final long ScheduledProcedureStepEndDateAndTime = 0x0040000400400005L;

    /** (0040,0244) VR=DA VM=1 Performed Procedure Step Start Date
     *  (0040,0245) VR=TM VM=1 Performed Procedure Step Start Time */
    public static final long PerformedProcedureStepStartDateAndTime = 0x0040024400400245L;

    /** (0040,0250) VR=DA VM=1 Performed Procedure Step End Date
     *  (0040,0251) VR=TM VM=1 Performed Procedure Step End Time */
    public static final long PerformedProcedureStepEndDateAndTime = 0x0040025000400251L;

    /** (0040,2004) VR=DA VM=1 Issue Date of Imaging Service Request
     *  (0040,2005) VR=TM VM=1 Issue Time of Imaging Service Request */
    public static final long IssueDateAndTimeOfImagingServiceRequest = 0x0040200400402005L;

    /** (0040,A121) VR=DA VM=1 Date
     *  (0040,A122) VR=TM VM=1 Time */
    public static final long DateAndTime = 0x0040A1210040A122L;

    /** (0070,0082) VR=DA VM=1 Presentation Creation Date
     *  (0070,0083) VR=TM VM=1 Presentation Creation Time */
    public static final long PresentationCreationDateAndTime = 0x0070008200700083L;

    /** (2100,0040) VR=DA VM=1 Creation Date
     *  (2100,0050) VR=TM VM=1 Creation Time */
    public static final long CreationDateAndTime = 0x2100004021000050L;

    /** (3006,0008) VR=DA VM=1 Structure Set Date
     *  (3006,0009) VR=TM VM=1 Structure Set Time */
    public static final long StructureSetDateAndTime = 0x3006000830060009L;

    /** (3008,0024) VR=DA VM=1 Treatment Control Point Date
     *  (3008,0025) VR=TM VM=1 Treatment Control Point Time */
    public static final long TreatmentControlPointDateAndTime = 0x3008002430080025L;

    /** (3008,0162) VR=DA VM=1 Safe Position Exit Date
     *  (3008,0164) VR=TM VM=1 Safe Position Exit Time */
    public static final long SafePositionExitDateAndTime = 0x3008016230080164L;

    /** (3008,0166) VR=DA VM=1 Safe Position Return Date
     *  (3008,0168) VR=TM VM=1 Safe Position Return Time */
    public static final long SafePositionReturnDateAndTime = 0x3008016630080168L;

    /** (3008,0250) VR=DA VM=1 Treatment Date
     *  (3008,0251) VR=TM VM=1 Treatment Time */
    public static final long TreatmentDateAndTime = 0x3008025030080251L;

    /** (300A,0006) VR=DA VM=1 RT Plan Date
     *  (300A,0007) VR=TM VM=1 RT Plan Time */
    public static final long RTPlanDateAndTime = 0x300A0006300A0007L;

    /** (300A,022C) VR=DA VM=1 Source Strength Reference Date
     *  (300A,022E) VR=TM VM=1 Source Strength Reference Time */
    public static final long SourceStrengthReferenceDateAndTime = 0x300A022C300A022EL;

    /** (300E,0004) VR=DA VM=1 Review Date
     *  (300E,0005) VR=TM VM=1 Review Time */
    public static final long ReviewDateAndTime = 0x300E0004300E0005L;

    /** (4008,0100) VR=DA VM=1 Interpretation Recorded Date (retired)
     *  (4008,0101) VR=TM VM=1 Interpretation Recorded Time (retired) */
    public static final long InterpretationRecordedDateAndTime = 0x4008010040080101L;

    /** (4008,0108) VR=DA VM=1 Interpretation Transcription Date (retired)
     *  (4008,0109) VR=TM VM=1 Interpretation Transcription Time (retired) */
    public static final long InterpretationTranscriptionDateAndTime = 0x4008010840080109L;

    /** (4008,0112) VR=DA VM=1 Interpretation Approval Date (retired)
     *  (4008,0113) VR=TM VM=1 Interpretation Approval Time (retired) */
    public static final long InterpretationApprovalDateAndTime = 0x4008011240080113L;

    public static int of(String keyword) {
        try {
            return Tag.class.getField(keyword).getInt(null);
        } catch (Exception ignore) { }
        return -1;
    }

    public static int tmTagOf(int daTag) {
        switch (daTag) {
            case Tag.InstanceCreationDate:
                return Tag.InstanceCreationTime;
            case Tag.StudyDate:
                return Tag.StudyTime;
            case Tag.SeriesDate:
                return Tag.SeriesTime;
            case Tag.AcquisitionDate:
                return Tag.AcquisitionTime;
            case Tag.ContentDate:
                return Tag.ContentTime;
            case Tag.OverlayDate:
                return Tag.OverlayTime;
            case Tag.CurveDate:
                return Tag.CurveTime;
            case Tag.PatientBirthDate:
                return Tag.PatientBirthTime;
            case Tag.DateOfSecondaryCapture:
                return Tag.TimeOfSecondaryCapture;
            case Tag.DateOfLastCalibration:
                return Tag.TimeOfLastCalibration;
            case Tag.DateOfLastDetectorCalibration:
                return Tag.TimeOfLastDetectorCalibration;
            case Tag.ModifiedImageDate:
                return Tag.ModifiedImageTime;
            case Tag.StudyVerifiedDate:
                return Tag.StudyVerifiedTime;
            case Tag.StudyReadDate:
                return Tag.StudyReadTime;
            case Tag.ScheduledStudyStartDate:
                return Tag.ScheduledStudyStartTime;
            case Tag.ScheduledStudyStopDate:
                return Tag.ScheduledStudyStopTime;
            case Tag.StudyArrivalDate:
                return Tag.StudyArrivalTime;
            case Tag.StudyCompletionDate:
                return Tag.StudyCompletionTime;
            case Tag.ScheduledAdmissionDate:
                return Tag.ScheduledAdmissionTime;
            case Tag.ScheduledDischargeDate:
                return Tag.ScheduledDischargeTime;
            case Tag.AdmittingDate:
                return Tag.AdmittingTime;
            case Tag.DischargeDate:
                return Tag.DischargeTime;
            case Tag.ScheduledProcedureStepStartDate:
                return Tag.ScheduledProcedureStepStartTime;
            case Tag.ScheduledProcedureStepEndDate:
                return Tag.ScheduledProcedureStepEndTime;
            case Tag.PerformedProcedureStepStartDate:
                return Tag.PerformedProcedureStepStartTime;
            case Tag.PerformedProcedureStepEndDate:
                return Tag.PerformedProcedureStepEndTime;
            case Tag.IssueDateOfImagingServiceRequest:
                return Tag.IssueTimeOfImagingServiceRequest;
            case Tag.Date:
                return Tag.Time;
            case Tag.PresentationCreationDate:
                return Tag.PresentationCreationTime;
            case Tag.CreationDate:
                return Tag.CreationTime;
            case Tag.StructureSetDate:
                return Tag.StructureSetTime;
            case Tag.TreatmentControlPointDate:
                return Tag.TreatmentControlPointTime;
            case Tag.SafePositionExitDate:
                return Tag.SafePositionExitTime;
            case Tag.SafePositionReturnDate:
                return Tag.SafePositionReturnTime;
            case Tag.TreatmentDate:
                return Tag.TreatmentTime;
            case Tag.RTPlanDate:
                return Tag.RTPlanTime;
            case Tag.SourceStrengthReferenceDate:
                return Tag.SourceStrengthReferenceTime;
            case Tag.ReviewDate:
                return Tag.ReviewTime;
            case Tag.InterpretationRecordedDate:
                return Tag.InterpretationRecordedTime;
            case Tag.InterpretationTranscriptionDate:
                return Tag.InterpretationTranscriptionTime;
            case Tag.InterpretationApprovalDate:
                return Tag.InterpretationApprovalTime;
        }
        return 0;
    }

    public static int daTagOf(int tmTag) {
        switch (tmTag) {
            case Tag.InstanceCreationTime:
                return Tag.InstanceCreationDate;
            case Tag.StudyTime:
                return Tag.StudyDate;
            case Tag.SeriesTime:
                return Tag.SeriesDate;
            case Tag.AcquisitionTime:
                return Tag.AcquisitionDate;
            case Tag.ContentTime:
                return Tag.ContentDate;
            case Tag.OverlayTime:
                return Tag.OverlayDate;
            case Tag.CurveTime:
                return Tag.CurveDate;
            case Tag.PatientBirthTime:
                return Tag.PatientBirthDate;
            case Tag.TimeOfSecondaryCapture:
                return Tag.DateOfSecondaryCapture;
            case Tag.TimeOfLastCalibration:
                return Tag.DateOfLastCalibration;
            case Tag.TimeOfLastDetectorCalibration:
                return Tag.DateOfLastDetectorCalibration;
            case Tag.ModifiedImageTime:
                return Tag.ModifiedImageDate;
            case Tag.StudyVerifiedTime:
                return Tag.StudyVerifiedDate;
            case Tag.StudyReadTime:
                return Tag.StudyReadDate;
            case Tag.ScheduledStudyStartTime:
                return Tag.ScheduledStudyStartDate;
            case Tag.ScheduledStudyStopTime:
                return Tag.ScheduledStudyStopDate;
            case Tag.StudyArrivalTime:
                return Tag.StudyArrivalDate;
            case Tag.StudyCompletionTime:
                return Tag.StudyCompletionDate;
            case Tag.ScheduledAdmissionTime:
                return Tag.ScheduledAdmissionDate;
            case Tag.ScheduledDischargeTime:
                return Tag.ScheduledDischargeDate;
            case Tag.AdmittingTime:
                return Tag.AdmittingDate;
            case Tag.DischargeTime:
                return Tag.DischargeDate;
            case Tag.ScheduledProcedureStepStartTime:
                return Tag.ScheduledProcedureStepStartDate;
            case Tag.ScheduledProcedureStepEndTime:
                return Tag.ScheduledProcedureStepEndDate;
            case Tag.PerformedProcedureStepStartTime:
                return Tag.PerformedProcedureStepStartDate;
            case Tag.PerformedProcedureStepEndTime:
                return Tag.PerformedProcedureStepEndDate;
            case Tag.IssueTimeOfImagingServiceRequest:
                return Tag.IssueDateOfImagingServiceRequest;
            case Tag.Time:
                return Tag.Date;
            case Tag.PresentationCreationTime:
                return Tag.PresentationCreationDate;
            case Tag.CreationTime:
                return Tag.CreationDate;
            case Tag.StructureSetTime:
                return Tag.StructureSetDate;
            case Tag.TreatmentControlPointTime:
                return Tag.TreatmentControlPointDate;
            case Tag.SafePositionExitTime:
                return Tag.SafePositionExitDate;
            case Tag.SafePositionReturnTime:
                return Tag.SafePositionReturnDate;
            case Tag.TreatmentTime:
                return Tag.TreatmentDate;
            case Tag.RTPlanTime:
                return Tag.RTPlanDate;
            case Tag.SourceStrengthReferenceTime:
                return Tag.SourceStrengthReferenceDate;
            case Tag.ReviewTime:
                return Tag.ReviewDate;
            case Tag.InterpretationRecordedTime:
                return Tag.InterpretationRecordedDate;
            case Tag.InterpretationTranscriptionTime:
                return Tag.InterpretationTranscriptionDate;
            case Tag.InterpretationApprovalTime:
                return Tag.InterpretationApprovalDate;
        }
        return 0;
    }

    public static String keywordOf(int tag) {
        if ((tag &amp; 0x0000FFFF) == 0)
            return tag == 0x00000000 ? "CommandGroupLength"
                    : tag == 0x00020000 ? "FileMetaInformationGroupLength"
                    : "GroupLength";
        if ((tag &amp; 0x00010000) != 0)
            return ((tag &amp; 0x0000FF00) == 0 &amp;&amp; (tag &amp; 0x000000F0) != 0)
                    ? "PrivateCreatorID"
                    : "";
        switch (tag) {
</xsl:text>
    <xsl:apply-templates select="//el[@keyword and not(contains(@tag,'x'))]" mode="keywordOf"/>
    <xsl:text>        }
        switch (tag &amp; 0xFF00FFFF) {
</xsl:text>
    <xsl:apply-templates select="//el[@keyword and substring(@tag,3,2)='xx']" mode="keywordOf"/>
    <xsl:text>        }
        switch (tag &amp; 0xFFFFFF0F) {
            case 0x00280400:
                return "RowsForNthOrderCoefficients";
            case 0x00280401:
                return "ColumnsForNthOrderCoefficients";
            case 0x00280402:
                return "CoefficientCoding";
            case 0x00280403:
                return "CoefficientCodingPointers";
            case 0x00280800:
                return "CodeLabel";
            case 0x00280802:
                return "NumberOfTables";
            case 0x00280803:
                return "CodeTableLocation";
            case 0x00280804:
                return "BitsForCodeWord";
            case 0x00280808:
                return "ImageDataLocation";
        }
        switch (tag &amp; 0xFFFF000F) {
            case 0x10000000:
                return "EscapeTriplet";
            case 0x10000001:
                return "RunLengthTriplet";
            case 0x10000002:
                return "HuffmanTableSize";
            case 0x10000003:
                return "HuffmanTableTriplet";
            case 0x10000004:
                return "ShiftTableSize";
            case 0x10000005:
                return "ShiftTableTriplet";
        }
        if ((tag &amp; 0xFFFF0000) == 0x10100000)
            return "ZonalMap";
        if ((tag &amp; 0xFFFFFF00) == 0x00203100)
            return "SourceImageIDs";
        return "";
    }

    public static VR vrOf(int tag) {
        if ((tag &amp; 0x0000FFFF) == 0)
            return VR.UL;
        if ((tag &amp; 0x00010000) != 0)
            return ((tag &amp; 0x0000FF00) == 0 &amp;&amp; (tag &amp; 0x000000F0) != 0)
                    ? VR.LO
                    : VR.UN;
        switch (tag) {</xsl:text>
    <xsl:apply-templates select="//el[@vr='AE']"/>
    <xsl:apply-templates select="//el[@vr='AS']"/>
    <xsl:apply-templates select="//el[@vr='AT' and not(contains(@tag,'x'))]"/>
    <xsl:apply-templates select="//el[@vr='CS' and not(contains(@tag,'x'))]"/>
    <xsl:apply-templates select="//el[@vr='DA']"/>
    <xsl:apply-templates select="//el[@vr='DS' and not(contains(@tag,'x'))]"/>
    <xsl:apply-templates select="//el[@vr='DT']"/>
    <xsl:apply-templates select="//el[@vr='FL']"/>
    <xsl:apply-templates select="//el[@vr='FD']"/>
    <xsl:apply-templates select="//el[@vr='IS' and not(contains(@tag,'x'))]"/>
    <xsl:apply-templates select="//el[@vr='LO' and not(contains(@tag,'x'))]"/>
    <xsl:apply-templates select="//el[@vr='LT' and not(contains(@tag,'x'))]"/>
    <xsl:apply-templates select="//el[@vr='OB']"/>
    <xsl:apply-templates select="//el[@vr='OD']"/>
    <xsl:apply-templates select="//el[@vr='OF']"/>
    <xsl:apply-templates select="//el[@vr='OL']"/>
    <xsl:apply-templates select="//el[@vr='OV']"/>
    <xsl:apply-templates select="//el[contains(@vr,'OW') and not(contains(@tag,'x'))]">
      <xsl:with-param name="vr">OW</xsl:with-param>
    </xsl:apply-templates>
    <xsl:apply-templates select="//el[@vr='PN' and not(contains(@tag,'x'))]"/>
    <xsl:apply-templates select="//el[@vr='SH' and not(contains(@tag,'x'))]"/>
    <xsl:apply-templates select="//el[@vr='SL']"/>
    <xsl:apply-templates select="//el[@vr='SQ' and not(contains(@tag,'x'))]"/>
    <xsl:apply-templates select="//el[contains(@vr,'SS') and not(contains(@vr,'OW')) and not(contains(@tag,'x'))]">
      <xsl:with-param name="vr">SS</xsl:with-param>
    </xsl:apply-templates>
    <xsl:apply-templates select="//el[@vr='ST']"/>
    <xsl:apply-templates select="//el[@vr='SV']"/>
    <xsl:apply-templates select="//el[@vr='TM']"/>
    <xsl:apply-templates select="//el[@vr='UC']"/>
    <xsl:apply-templates select="//el[@vr='UI']"/>
    <xsl:apply-templates select="//el[@vr='UL' and not(contains(@tag,'x'))]"/>
    <xsl:apply-templates select="//el[@vr='UR']"/>
    <xsl:apply-templates select="//el[@vr='US' and not(contains(@tag,'x'))]"/>
    <xsl:apply-templates select="//el[@vr='UT']"/>
    <xsl:apply-templates select="//el[@vr='UV']"/>
    <xsl:text>
        }
        switch (tag &amp; 0xFF01FFFF) {</xsl:text>
    <xsl:apply-templates select="//el[@vr='AT' and substring(@tag,3,2)='xx']"/>
    <xsl:apply-templates select="//el[@vr='CS' and substring(@tag,3,2)='xx']"/>
    <xsl:apply-templates select="//el[@vr='DS' and substring(@tag,3,2)='xx']"/>
    <xsl:apply-templates select="//el[@vr='IS' and substring(@tag,3,2)='xx']"/>
    <xsl:apply-templates select="//el[@vr='LO' and substring(@tag,3,2)='xx']"/>
    <xsl:apply-templates select="//el[@vr='LT' and substring(@tag,3,2)='xx']"/>
    <xsl:apply-templates select="//el[contains(@vr,'OW') and substring(@tag,3,2)='xx']">
      <xsl:with-param name="vr">OW</xsl:with-param>
    </xsl:apply-templates>
    <xsl:apply-templates select="//el[@vr='PN' and substring(@tag,3,2)='xx']"/>
    <xsl:apply-templates select="//el[@vr='SH' and substring(@tag,3,2)='xx']"/>
    <xsl:apply-templates select="//el[@vr='SQ' and substring(@tag,3,2)='xx']"/>
    <xsl:apply-templates select="//el[contains(@vr,'SS') and not(contains(@vr,'OW')) and substring(@tag,3,2)='xx']">
      <xsl:with-param name="vr">SS</xsl:with-param>
    </xsl:apply-templates>
    <xsl:apply-templates select="//el[@vr='UL' and substring(@tag,3,2)='xx']"/>
    <xsl:apply-templates select="//el[@vr='US' and substring(@tag,3,2)='xx']"/>
    <xsl:text>
        }
        switch (tag &amp; 0xFFFFFF0F) {
            case 0x00280403:
            case 0x00280803:
            case 0x00280808:
                return VR.AT;
            case 0x00280800:
                return VR.CS;
            case 0x00280402:
                return VR.LO;
            case 0x00280400:
            case 0x00280401:
            case 0x00280802:
            case 0x00280804:
                return VR.US;
        }
        switch (tag &amp; 0xFFFF000F) {
            case 0x10000000:
            case 0x10000001:
            case 0x10000002:
            case 0x10000003:
            case 0x10000004:
            case 0x10000005:
                return VR.US;
        }
        if ((tag &amp; 0xFFFF0000) == 0x10100000)
            return VR.US;
        if ((tag &amp; 0xFFFFFF00) == 0x00203100)
            return VR.CS;
        return VR.UN;
    }
}
</xsl:text>
  </xsl:template>

  <xsl:template match="el" mode="Tag">
    <xsl:text>
    /** (</xsl:text>
    <xsl:value-of select="substring(@tag,1,4)" />
    <xsl:text>,</xsl:text>
    <xsl:value-of select="substring(@tag,5,4)" />
    <xsl:text>) VR=</xsl:text>
    <xsl:value-of select="@vr" />
    <xsl:text> VM=</xsl:text>
    <xsl:value-of select="@vm" />
    <xsl:text> </xsl:text>
    <xsl:value-of select="text()" />
    <xsl:if test="@retired='true'">
      <xsl:text> (retired)</xsl:text>
    </xsl:if>
    <xsl:text> */
    public static final int </xsl:text>
    <xsl:value-of select="@keyword" />
    <xsl:text> = 0x</xsl:text>
    <xsl:value-of select="translate(@tag,'x','0')" />
    <xsl:text>;
</xsl:text>
  </xsl:template>

  <xsl:template match="el" mode="keywordOf">
    <xsl:text>        case 0x</xsl:text>
    <xsl:value-of select="translate(@tag,'x','0')"/>
    <xsl:text>:
            return "</xsl:text>
    <xsl:value-of select="@keyword" />
    <xsl:text>";
</xsl:text>
  </xsl:template>

  <xsl:template match="el">
    <xsl:param name="vr" select="@vr" />
    <xsl:text>
        case 0x</xsl:text>
    <xsl:value-of select="translate(@tag,'x','0')"/>
    <xsl:text>:</xsl:text>
    <xsl:if test="position()=last()">
      <xsl:text>
            return VR.</xsl:text>
      <xsl:value-of select="$vr"/>
      <xsl:text>;</xsl:text>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
