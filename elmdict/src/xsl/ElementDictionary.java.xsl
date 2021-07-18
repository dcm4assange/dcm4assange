<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text"></xsl:output>
  <xsl:param name="package"/>
  <xsl:param name="class"/>
  <xsl:param name="PrivateCreatorID"/>
  <xsl:template match="/elements">
    <xsl:text>/*
 * This file is generated from element dictionary template files included in
 * David Clunie's Dicom3tools Software (https://www.dclunie.com/dicom3tools.html)
 */

package </xsl:text><xsl:value-of select="$package"/><xsl:text>;

import org.dcm4assange.ElementDictionary;
import org.dcm4assange.VR;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Jul 2021
 */
public class </xsl:text><xsl:value-of select="$class"/><xsl:text> extends ElementDictionary {

    public </xsl:text><xsl:value-of select="$class"/><xsl:text>() {
        super("</xsl:text>
    <xsl:value-of select="$PrivateCreatorID"/>
    <xsl:text>", Tag::of, </xsl:text>
    <xsl:value-of select="$class"/>
    <xsl:text>::keywordOf, </xsl:text>
    <xsl:value-of select="$class"/>
    <xsl:text>::vrOf);
    }

    public static class Tag {

        public static int of(String keyword) {
            try {
                return Tag.class.getField(keyword).getInt(null);
            } catch (Exception ignore) {
            }
            return -1;
        }
</xsl:text>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @keyword]" mode="Tag"/>
    <xsl:text>
    }

    public static String keywordOf(int tag) {
        switch (tag &amp; 0xFFFF00FF) {
</xsl:text>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @keyword]" mode="keywordOf"/>
    <xsl:text>        }
        return "";
    }

    public static VR vrOf(int tag) {
        switch (tag &amp; 0xFFFF00FF) {</xsl:text>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='AE']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='AS']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='AT']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='CS']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='DA']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='DS']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='DT']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='FL']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='FD']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='IS']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='LO']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='LT']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='OB']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='OD']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='OF']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='OL']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='OV']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and contains(@vr,'OW')]">
      <xsl:with-param name="vr">OW</xsl:with-param>
    </xsl:apply-templates>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='PN']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='SH']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='SL']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='SQ']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and contains(@vr,'SS') and not(contains(@vr,'OW'))]">
      <xsl:with-param name="vr">SS</xsl:with-param>
    </xsl:apply-templates>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='ST']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='SV']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='TM']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='UC']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='UI']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='UL']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='UR']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='US']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='UT']"/>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID and @vr='UV']"/>
    <xsl:text>
        }
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
    <xsl:value-of select="translate(@tag,'xX','00')" />
    <xsl:text>;
</xsl:text>
  </xsl:template>

  <xsl:template match="el" mode="keywordOf">
    <xsl:text>            case Tag.</xsl:text>
    <xsl:value-of select="@keyword" />
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
