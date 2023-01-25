<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text"></xsl:output>

  <xsl:template match="/">
    <xsl:text>{</xsl:text>
    <xsl:apply-templates select="//el[@keyword!='']"/>
    <xsl:text>
}
</xsl:text>
  </xsl:template>  

  <xsl:template match="el">
    <xsl:if test="position() != 1">,</xsl:if>
    <xsl:text>
"</xsl:text>
    <xsl:value-of select="@tag" />
    <xsl:text>":"</xsl:text>
    <xsl:value-of select="text()"/>
    <xsl:text>"</xsl:text>
  </xsl:template>

</xsl:stylesheet>
