/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Dec 2022
 */
module org.dcm4assange.elmdict.isg {
    exports org.dcm4assange.elmdict.isg;

    requires org.dcm4assange.core;

    provides org.dcm4assange.ElementDictionary with
            org.dcm4assange.elmdict.isg.ISGShadow,
            org.dcm4assange.elmdict.isg.SilhouetteAnnot,
            org.dcm4assange.elmdict.isg.SilhouetteGraphicsExport,
            org.dcm4assange.elmdict.isg.SilhouetteLine,
            org.dcm4assange.elmdict.isg.SilhouetteROI,
            org.dcm4assange.elmdict.isg.SilhouetteSequenceIds,
            org.dcm4assange.elmdict.isg.Silhouette;
}
