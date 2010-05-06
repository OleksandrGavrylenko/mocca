/*
* Copyright 2008 Federal Chancellery Austria and
* Graz University of Technology
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package at.gv.egiz.stal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for InfoboxReadResponseType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="InfoboxReadResponseType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.egiz.gv.at/stal}ResponseType">
 *       &lt;sequence>
 *         &lt;element name="InfoboxValue" type="{http://www.w3.org/2001/XMLSchema}base64Binary"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InfoboxReadResponseType", propOrder = {
    "infoboxValue"
})
public class InfoboxReadResponse
    extends STALResponse
{

    @XmlElement(name = "InfoboxValue", required = true)
    protected byte[] infoboxValue;

    /**
     * Gets the value of the infoboxValue property.
     * 
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getInfoboxValue() {
        return infoboxValue;
    }

    /**
     * Sets the value of the infoboxValue property.
     * 
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setInfoboxValue(byte[] value) {
        this.infoboxValue = ((byte[]) value);
    }

}