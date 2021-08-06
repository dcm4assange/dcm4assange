<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text"/>
  <xsl:param name="PrivateCreatorID"/>
  <xsl:template match="/elements">
    <xsl:text># This file is generated from element dictionary template files included in
# David Clunie's Dicom3tools Software (https://www.dclunie.com/dicom3tools.html)
</xsl:text>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID]" />
  </xsl:template>
  <xsl:template match="el">
    <xsl:value-of select="@tag" />
    <xsl:text>:</xsl:text>
    <xsl:choose>
      <xsl:when test="contains(@vr,'OW')">OW</xsl:when>
      <xsl:when test="contains(@vr,'SS')">SS</xsl:when>
      <xsl:otherwise><xsl:value-of select="@vr" /></xsl:otherwise>
    </xsl:choose>
    <xsl:text>:</xsl:text>
    <xsl:value-of select="@keyword" />
    <xsl:text>
</xsl:text>
  </xsl:template>
</xsl:stylesheet>
