/*
 * Copyright 2011 by Graz University of Technology, Austria
 * MOCCA has been developed by the E-Government Innovation Center EGIZ, a joint
 * initiative of the Federal Chancellery Austria and Graz University of Technology.
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * http://www.osor.eu/eupl/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * This product combines work with different licenses. See the "NOTICE" text
 * file for details on the various modules and licenses.
 * The "NOTICE" text file is part of the distribution. Any derivative works
 * that you distribute must include a readable copy of the "NOTICE" text file.
 */


package at.gv.egiz.stalx.service;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.3-b02-
 * Generated source version: 2.1
 * 
 */
@WebServiceClient(name = "STALService", targetNamespace = "http://www.egiz.gv.at/wsdl/stal")
//, wsdlLocation = "file:/home/clemens/workspace/mocca/BKUOnline/src/main/webapp/WEB-INF/wsdl/stal-service.wsdl")
public class STALService
    extends Service
{

//    private final static URL STALSERVICE_WSDL_LOCATION;

//    static {
//        URL url = null;
//        try {
//            URL baseUrl;
//            baseUrl = at.gv.egiz.stal.service.STALService.class.getResource(".");
//            url = new URL(baseUrl, "file:/home/clemens/workspace/mocca/BKUOnline/src/main/webapp/WEB-INF/wsdl/stal-service.wsdl");
//        } catch (MalformedURLException e) {
//            logger.warning("Failed to create URL for the wsdl Location: 'file:/home/clemens/workspace/mocca/BKUOnline/src/main/webapp/WEB-INF/wsdl/stal-service.wsdl', retrying as a local file");
//            logger.warning(e.getMessage());
//        }
//        STALSERVICE_WSDL_LOCATION = url;
//    }

    public STALService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

//    public STALService() {
//        super(STALSERVICE_WSDL_LOCATION, new QName("http://www.egiz.gv.at/wsdl/stal", "STALService"));
//    }

    /**
     * Do not export package protected STALXPortType interface
     * (this is a dummy interface to make JAXB include the STAL-X types)
     * @return
     *     returns STALPortType
     */
    @WebEndpoint(name = "STALPort")
    public STALPortType getSTALPort() {
        return super.getPort(new QName("http://www.egiz.gv.at/wsdl/stal", "STALPort"), STALPortType.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns STALPortType
     */
//    @WebEndpoint(name = "STALPort")
//    public STALPortType getSTALPort(WebServiceFeature... features) {
//        return super.getPort(new QName("http://www.egiz.gv.at/wsdl/stal", "STALPort"), STALPortType.class, features);
//    }

}
