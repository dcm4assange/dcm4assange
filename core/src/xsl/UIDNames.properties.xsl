<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text"/>
  <xsl:template match="/uids">
    <xsl:text>
# This file is generated from DICOM Standard Documents PS3.6 Data Dictionary in DocBook format
# available at http://dicom.nema.org/medical/dicom/current/source/docbook/
</xsl:text>
    <xsl:apply-templates select="uid" />
  </xsl:template>
  <xsl:template match="uid">
    <xsl:value-of select="@value" />
    <xsl:text>=</xsl:text>
    <xsl:value-of select="text()" />
    <xsl:text>
</xsl:text>
  </xsl:template>
</xsl:stylesheet>