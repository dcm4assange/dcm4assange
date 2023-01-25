<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text"></xsl:output>
  <xsl:param name="PrivateCreatorID"/>

  <xsl:template match="/">
    <xsl:text>{
"privateCreator":"</xsl:text><xsl:value-of select="$PrivateCreatorID"/><xsl:text>"</xsl:text>
    <xsl:apply-templates select="//el[@owner=$PrivateCreatorID][text()]"/>
    <xsl:text>
}
</xsl:text>
  </xsl:template>  

  <xsl:template match="el">
    <xsl:text>,
"</xsl:text>
    <xsl:value-of select="@tag" />
    <xsl:text>":"</xsl:text>
    <xsl:value-of select="text()"/>
    <xsl:text>"</xsl:text>
  </xsl:template>

</xsl:stylesheet>
