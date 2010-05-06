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
package at.gv.egiz.bku.utils.binding;

public enum Protocol {
  HTTP("http"), HTTPS("https"), SAML("saml");
  
  private String name;
  
  Protocol(String s) {
    name = s;
  }
  
  public String toString() {
    return name;
  }
  
  public static Protocol fromString(String protocol) {
    if (HTTP.toString().equalsIgnoreCase(protocol)) {
      return HTTP;
    }
    if (HTTPS.toString().equalsIgnoreCase(protocol)) {
      return HTTPS;
    }
    if (SAML.toString().equalsIgnoreCase(protocol)) {
      return SAML;
    }
    return null;
  }
}