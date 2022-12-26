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

public class </xsl:text><xsl:value-of select="$class"/><xsl:text> extends ElementDictionary {

    public static final String PRIVATE_CREATOR = "</xsl:text><xsl:value-of select="$PrivateCreatorID"/><xsl:text>";

    private static class LazyHolder {
        private static PrivateElements elements =
                new PrivateElements(</xsl:text>
    <xsl:value-of select="$class"/>
    <xsl:text>.class.getResource("</xsl:text>
    <xsl:value-of select="$class"/>
    <xsl:text>.properties"));
    }

    public </xsl:text><xsl:value-of select="$class"/><xsl:text>() {
        super(PRIVATE_CREATOR, Tag::of);
    }

    protected Element elementOfTag(int tag) {
        return LazyHolder.elements.apply(tag);
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
    <xsl:variable name="keyword" select="@keyword"/>
    <xsl:if test="preceding-sibling::el[@keyword=$keyword]|following-sibling::el[@keyword=$keyword]">
      <xsl:text>_</xsl:text>
      <xsl:value-of select="translate(@tag,'xX','00')" />
    </xsl:if>
    <xsl:text> = 0x</xsl:text>
    <xsl:value-of select="translate(@tag,'xX','00')" />
    <xsl:text>;
</xsl:text>
  </xsl:template>
</xsl:stylesheet>
