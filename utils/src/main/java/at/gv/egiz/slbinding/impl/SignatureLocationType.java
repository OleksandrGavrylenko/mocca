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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package at.gv.egiz.slbinding.impl;

import at.gv.egiz.slbinding.*;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.namespace.NamespaceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author clemens
 */
public class SignatureLocationType extends at.buergerkarte.namespaces.securitylayer._1.SignatureLocationType implements NamespaceContextCallback {

    @XmlTransient
    private final Logger log = LoggerFactory.getLogger(SignatureLocationType.class);
    @XmlTransient
    protected NamespaceContext namespaceContext;

    @Override
    public NamespaceContext getNamespaceContext() {
        return namespaceContext;
    }

    @Override
    public void preserveNamespaceContext(RedirectEventFilter filter) {
        log.trace("preserving namespace context for SignatureLocationType");
        namespaceContext = filter.getCurrentNamespaceContext();
    }
}