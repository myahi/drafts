<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:head003="urn:iso:std:iso:20022:tech:xsd:head.003.001.01"
    xmlns:auth030="urn:iso:std:iso:20022:tech:xsd:auth.030.001.03"
    exclude-result-prefixes="head003 auth030">

    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>

    <!-- Identity transform -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- RptgTmStmp : enlever les millisecondes (ex: ...:03.652Z -> ...:03Z) -->
    <xsl:template match="auth030:RptgTmStmp/text()">
        <xsl:choose>
            <!-- Cas standard: présence d'un point et suffixe Z -->
            <xsl:when test="contains(., '.') and contains(., 'Z')">
                <xsl:value-of select="concat(substring-before(., '.'), 'Z')"/>
            </xsl:when>

            <!-- Si millisecondes sans Z (au cas où) -->
            <xsl:when test="contains(., '.')">
                <xsl:value-of select="substring-before(., '.')"/>
            </xsl:when>

            <!-- Sinon on laisse tel quel -->
            <xsl:otherwise>
                <xsl:value-of select="."/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
