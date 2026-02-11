<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:Saa="urn:swift:saa:xsd:saa.2.0"
  exclude-result-prefixes="Saa">

  <xsl:output method="xml" indent="yes" encoding="UTF-8"/>
  <xsl:strip-space elements="*"/>

  <!-- Point d'entrée -->
  <xsl:template match="/">
    <extracted>
      <!-- Saa:MessageIdentifier  (à récupérer) -->
      <messageIdentifier>
        <xsl:value-of select="/Saa:DataPDU/Saa:Header/Saa:Message/Saa:MessageIdentifier"/>
      </messageIdentifier>

      <!-- Saa:Sender/Saa:FullName/Saa:X1  (à récupérer) -->
      <senderBic>
        <xsl:value-of select="/Saa:DataPDU/Saa:Header/Saa:Message/Saa:Sender/Saa:FullName/Saa:X1"/>
      </senderBic>

      <!-- Saa:NetworkInfo/Saa:SWIFTNetNetworkInfo/Saa:FileInfo  (à récupérer) -->
      <fileInfo>
        <xsl:value-of select="/Saa:DataPDU/Saa:Header/Saa:Message/Saa:NetworkInfo/Saa:SWIFTNetNetworkInfo/Saa:FileInfo"/>
      </fileInfo>

      <!-- Saa:FileLogicalName  (à récupérer) -->
      <fileLogicalName>
        <xsl:value-of select="/Saa:DataPDU/Saa:Header/Saa:Message/Saa:FileLogicalName"/>
      </fileLogicalName>
    </extracted>
  </xsl:template>

</xsl:stylesheet>
