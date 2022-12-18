/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Dec 2022
 */
module org.dcm4assange.elmdict.elscint {
    exports org.dcm4assange.elmdict.gems;

    requires org.dcm4assange.core;

    provides org.dcm4assange.ElementDictionary with
            org.dcm4assange.elmdict.gems.GEMnetArchiveManager,
            org.dcm4assange.elmdict.gems.AMIAnnotations01,
            org.dcm4assange.elmdict.gems.AMIAnnotations02,
            org.dcm4assange.elmdict.gems.AMIImageContext,
            org.dcm4assange.elmdict.gems.AMIImageContextExt,
            org.dcm4assange.elmdict.gems.AMIImageTransform,
            org.dcm4assange.elmdict.gems.AMISequenceAnnotations01,
            org.dcm4assange.elmdict.gems.AMISequenceAnnotations02,
            org.dcm4assange.elmdict.gems.AMISequenceAnnotElements,
            org.dcm4assange.elmdict.gems.AMIStudyExtensions,
            org.dcm4assange.elmdict.gems.Apex,
            org.dcm4assange.elmdict.gems.ApplicareCentricityRadiologyWeb10,
            org.dcm4assange.elmdict.gems.ApplicareCentricityRadiologyWeb20,
            org.dcm4assange.elmdict.gems.ApplicarePrint,
            org.dcm4assange.elmdict.gems.ApplicareRadStore,
            org.dcm4assange.elmdict.gems.ApplicareRadWorks50,
            org.dcm4assange.elmdict.gems.ApplicareRadWorks60,
            org.dcm4assange.elmdict.gems.ApplicareRadWorks60Summary,
            org.dcm4assange.elmdict.gems.ApplicareWorkflow,
            org.dcm4assange.elmdict.gems.DLInternalUse,
            org.dcm4assange.elmdict.gems.DLXAnnot,
            org.dcm4assange.elmdict.gems.DLXExams,
            org.dcm4assange.elmdict.gems.DLXLkup,
            org.dcm4assange.elmdict.gems.DLXPatnt,
            org.dcm4assange.elmdict.gems.DLXSerie,
            org.dcm4assange.elmdict.gems.GEAdantageReviewCS,
            org.dcm4assange.elmdict.gems.GEGenericData,
            org.dcm4assange.elmdict.gems.GEGenesis,
            org.dcm4assange.elmdict.gems.GEGroup,
            org.dcm4assange.elmdict.gems.GEHCAdvApp,
            org.dcm4assange.elmdict.gems.GEIIS,
            org.dcm4assange.elmdict.gems.GEIIS_IW,
            org.dcm4assange.elmdict.gems.GEIIS_PACS,
            org.dcm4assange.elmdict.gems.GEIIS_RA,
            org.dcm4assange.elmdict.gems.GEInformaticsData,
            org.dcm4assange.elmdict.gems.GELUTAsymmetryParameter,
            org.dcm4assange.elmdict.gems.GEMSReportingTool,
            org.dcm4assange.elmdict.gems.GEMS3DIntvl,
            org.dcm4assange.elmdict.gems.GEMS3DState,
            org.dcm4assange.elmdict.gems.GEMSAcqu,
            org.dcm4assange.elmdict.gems.GEMSAcrqa10Block1,
            org.dcm4assange.elmdict.gems.GEMSAcrqa10Block2,
            org.dcm4assange.elmdict.gems.GEMSAcrqa10Block3,
            org.dcm4assange.elmdict.gems.GEMSAcrqa20Block1,
            org.dcm4assange.elmdict.gems.GEMSAcrqa20Block2,
            org.dcm4assange.elmdict.gems.GEMSAcrqa20Block3,
            org.dcm4assange.elmdict.gems.GEMSADWSoft3D,
            org.dcm4assange.elmdict.gems.GEMSADWSoftDPO,
            org.dcm4assange.elmdict.gems.GEMSADWSoftDPO1,
            org.dcm4assange.elmdict.gems.GEMSADWSoftCD,
            org.dcm4assange.elmdict.gems.GEMSCTCardiac,
            org.dcm4assange.elmdict.gems.GEMSCTHD,
            org.dcm4assange.elmdict.gems.GEMSCTHino,
            org.dcm4assange.elmdict.gems.GEMSCTVes,
            org.dcm4assange.elmdict.gems.GEMSDLFrame,
            org.dcm4assange.elmdict.gems.GEMSDLImg,
            org.dcm4assange.elmdict.gems.GEMSDLPatnt,
            org.dcm4assange.elmdict.gems.GEMSDLSeries,
            org.dcm4assange.elmdict.gems.GEMSDLStudy,
            org.dcm4assange.elmdict.gems.GEMSDRS,
            org.dcm4assange.elmdict.gems.GEMSFalcon,
            org.dcm4assange.elmdict.gems.GEMSFuncTool,
            org.dcm4assange.elmdict.gems.GEMSGDXEAthena,
            org.dcm4assange.elmdict.gems.GEMSGDXEFalcon,
            org.dcm4assange.elmdict.gems.GEMSGenie,
            org.dcm4assange.elmdict.gems.GEMSGNHD,
            org.dcm4assange.elmdict.gems.GEMSHelios,
            org.dcm4assange.elmdict.gems.GEMSIden,
            org.dcm4assange.elmdict.gems.GEMSIDI,
            org.dcm4assange.elmdict.gems.GEMSImag,
            org.dcm4assange.elmdict.gems.GEMSIMPS,
            org.dcm4assange.elmdict.gems.GEMSIQTBIden,
            org.dcm4assange.elmdict.gems.GEMSITCentricityRA600,
            org.dcm4assange.elmdict.gems.GEMSITUSReport,
            org.dcm4assange.elmdict.gems.GEMSLunarRaw,
            org.dcm4assange.elmdict.gems.GEMSParm,
            org.dcm4assange.elmdict.gems.GEMSPati,
            org.dcm4assange.elmdict.gems.GEMSPetd,
            org.dcm4assange.elmdict.gems.GEMSRela,
            org.dcm4assange.elmdict.gems.GEMSSend,
            org.dcm4assange.elmdict.gems.GEMSSeno,
            org.dcm4assange.elmdict.gems.GEMSSenoCrystal,
            org.dcm4assange.elmdict.gems.GEMSSers,
            org.dcm4assange.elmdict.gems.GEMSStdy,
            org.dcm4assange.elmdict.gems.GEMSUltrasoundExamGroup,
            org.dcm4assange.elmdict.gems.GEMSUltrasoundImageGroup,
            org.dcm4assange.elmdict.gems.GEMSUltrasoundMovieGroup,
            org.dcm4assange.elmdict.gems.GEMSVXTLUserData,
            org.dcm4assange.elmdict.gems.GEMSXELPRV,
            org.dcm4assange.elmdict.gems.GEMSXR3DCal,
            org.dcm4assange.elmdict.gems.GEMSYMHD,
            org.dcm4assange.elmdict.gems.GEMSITSBamwallThickness,
            org.dcm4assange.elmdict.gems.GEMSITSOrthoView,
            org.dcm4assange.elmdict.gems.GEMSITSRadPacs,
            org.dcm4assange.elmdict.gems.KretzUS,
            org.dcm4assange.elmdict.gems.MayoIBMArchiveProject,
            org.dcm4assange.elmdict.gems.QuasarInteralUse,
            org.dcm4assange.elmdict.gems.RadWorksTBR,
            org.dcm4assange.elmdict.gems.ReportFromApp;
}
