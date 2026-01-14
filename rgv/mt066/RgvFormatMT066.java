<?xml version="1.0" encoding="UTF-8"?>
<BWSharedResource xmlns:xsd="http://www.w3.org/2001/XMLSchema">
    <name>RgvFormat-MT066</name>
    <resourceType>ae.shared.ParseSharedResource</resourceType>
    <config xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <FormatType>Fixed format</FormatType>
        <ColumnSeparator>,</ColumnSeparator>
        <ColSeparatorRule>Treat all characters as entered as a single column separator string</ColSeparatorRule>
        <LineSeparator>&#xD;</LineSeparator>
        <FillCharacter>Space</FillCharacter>
        <LineLength>143</LineLength>
        <OffsetString>O_R1_Identification_emetteur(0,16)
O_R2_Reference_operation(16,32)
O_R3_Identification_recepteur(32,48)
F_R4_Type_emetteur(48,49)
O_R5_Code_operation(49,54)
O_Horodatage(54,80)
O_Reference_operation_demande_codification(80,96)
F_Zone_reservee(96,98)
O_Code_etablissement_domiciliataire(98,110)
O_Code_adherent_domiciliataire(110,115)
F_Zone_reservee_1(115,117)
O_Code_valeur(117,129)
O_Date_effet_referentiel(129,137)
O_Heure_effet_referentiel(137,143)</OffsetString>
        <DataFormat>
            <xsd:element name="MT066">
                <xsd:complexType>
                    <xsd:sequence>
                        <xsd:element name="O_R1_Identification_emetteur" type="xsd:string"/>
                        <xsd:element name="O_R2_Reference_operation" type="xsd:string"/>
                        <xsd:element name="O_R3_Identification_recepteur" type="xsd:string"/>
                        <xsd:element name="F_R4_Type_emetteur" type="xsd:string" minOccurs="0"/>
                        <xsd:element name="O_R5_Code_operation" type="xsd:string"/>
                        <xsd:element name="O_Horodatage" type="xsd:string"/>
                        <xsd:element name="O_Reference_operation_demande_codification" type="xsd:string"/>
                        <xsd:element name="F_Zone_reservee" type="xsd:string" minOccurs="0"/>
                        <xsd:element name="O_Code_etablissement_domiciliataire" type="xsd:string"/>
                        <xsd:element name="O_Code_adherent_domiciliataire" type="xsd:string"/>
                        <xsd:element name="F_Zone_reservee_1" type="xsd:string" minOccurs="0"/>
                        <xsd:element name="O_Code_valeur" type="xsd:string"/>
                        <xsd:element name="O_Date_effet_referentiel" type="xsd:string"/>
                        <xsd:element name="O_Heure_effet_referentiel" type="xsd:string"/>
                    </xsd:sequence>
                </xsd:complexType>
            </xsd:element>
        </DataFormat>
    </config>
</BWSharedResource>
