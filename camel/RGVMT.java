
package fr.lbp.rgv.soap.ws;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour anonymous complex type.</p>
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.</p>
 * 
 * <pre>{@code
 * <complexType>
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element ref="{http://www.lbp.fr/xml}MTformat"/>
 *         <element ref="{http://www.lbp.fr/xml}RGVstring"/>
 *         <element ref="{http://www.lbp.fr/xml}RGVWarning" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "mTformat",
    "rgVstring",
    "rgvWarning"
})
@XmlRootElement(name = "RGVMT", namespace = "http://www.lbp.fr/xml")
public class RGVMT {

    @XmlElement(name = "MTformat", namespace = "http://www.lbp.fr/xml", required = true)
    protected String mTformat;
    @XmlElement(name = "RGVstring", namespace = "http://www.lbp.fr/xml", required = true)
    protected String rgVstring;
    @XmlElement(name = "RGVWarning", namespace = "http://www.lbp.fr/xml")
    protected String rgvWarning;

    /**
     * Obtient la valeur de la propriété mTformat.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMTformat() {
        return mTformat;
    }

    /**
     * Définit la valeur de la propriété mTformat.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMTformat(String value) {
        this.mTformat = value;
    }

    /**
     * Obtient la valeur de la propriété rgVstring.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRGVstring() {
        return rgVstring;
    }

    /**
     * Définit la valeur de la propriété rgVstring.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRGVstring(String value) {
        this.rgVstring = value;
    }

    /**
     * Obtient la valeur de la propriété rgvWarning.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRGVWarning() {
        return rgvWarning;
    }

    /**
     * Définit la valeur de la propriété rgvWarning.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRGVWarning(String value) {
        this.rgvWarning = value;
    }

}
