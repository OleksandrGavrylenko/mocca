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
package at.gv.egiz.bku.slxhtml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Locale;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import at.gv.egiz.bku.slxhtml.css.CSSValidatorSLXHTML;
import at.gv.egiz.bku.viewer.ValidationException;

public class SLXHTMLValidator implements at.gv.egiz.bku.viewer.Validator {

  /**
   * The schema file for the SLXHTML schema.
   */
  private static final String SLXHTML_SCHEMA_FILE = "at/gv/egiz/bku/slxhtml/slxhtml.xsd";
  
  /**
   * Logging facility.
   */
  private static Log log = LogFactory.getLog(SLXHTMLValidator.class);

  private static Schema slSchema;
  
  /**
   * Initialize the security layer schema.
   */
  private synchronized static void ensureSchema() {
      if (slSchema == null) {
          try {
              SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
              ClassLoader cl = SLXHTMLValidator.class.getClassLoader();
              URL schemaURL = cl.getResource(SLXHTML_SCHEMA_FILE);
              log.debug("Trying to create SLXHTML schema from URL '" + schemaURL + "'.");
              long t0 = System.currentTimeMillis();
              slSchema = schemaFactory.newSchema(schemaURL);
              long t1 = System.currentTimeMillis();
              log.debug("SLXHTML schema successfully created in " + (t1 - t0) + "ms.");
          } catch (SAXException e) {
              log.error("Failed to load security layer XHTML schema.", e);
              throw new RuntimeException("Failed to load security layer XHTML schema.", e);
          }

      }
  }
  
  public SLXHTMLValidator() {
    ensureSchema();
  }
  
  public void validate(InputStream is, String charset)
      throws ValidationException {
    if (charset == null) {
      validate(is, (Charset) null);
    } else {
      try {
        validate(is, Charset.forName(charset));
      } catch (IllegalCharsetNameException e) {
        throw new ValidationException(e);
      } catch (UnsupportedCharsetException e) {
        throw new ValidationException(e);
      }
    }
  }
  
  public void validate(InputStream is, Charset charset) throws ValidationException {
    
    SAXParserFactory spf = SAXParserFactory.newInstance();
    spf.setNamespaceAware(true);
    spf.setSchema(slSchema);
    spf.setValidating(true);
    spf.setXIncludeAware(false);
    
    SAXParser parser;
    try {
      parser = spf.newSAXParser();
    } catch (ParserConfigurationException e) {
      log.error("Failed to create SLXHTML parser.", e);
      throw new RuntimeException("Failed to create SLXHTML parser.", e);
    } catch (SAXException e) {
      log.error("Failed to create SLXHTML parser.", e);
      throw new RuntimeException("Failed to create SLXHTML parser.", e);
    }
    
    InputSource source;
    if (charset != null) {
      source = new InputSource(new InputStreamReader(is, charset));
    } else {
      source = new InputSource(is);
    }
    

    ValidatorHandler validatorHandler = slSchema.newValidatorHandler();

    DefaultHandler defaultHandler = new ValidationHandler(validatorHandler);
    try {
      parser.parse(source, defaultHandler);
    } catch (SAXException e) {
      if (e.getException() instanceof ValidationException) {
        throw (ValidationException) e.getException();
      } else {
        throw new ValidationException(e);
      }
    } catch (IOException e) {
      throw new ValidationException(e);
    }
       
  }
  
  private void validateCss(InputStream is) throws ValidationException {
    CSSValidatorSLXHTML cssValidator = new CSSValidatorSLXHTML();
    // TODO: use the right locale
    cssValidator.validate(is, Locale.getDefault(), "SLXHTML", 0);
  }
  
  private class ValidationHandler extends DefaultHandler implements ContentHandler {
    
    private ValidatorHandler validatorHandler;
    
    private boolean insideStyle = false;
    
    private StringBuffer style = new StringBuffer();
    
    private ValidationHandler(ValidatorHandler contentHandler) {
      this.validatorHandler = contentHandler;
    }
    
    @Override
    public void endDocument() throws SAXException {
      validatorHandler.endDocument();
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
      validatorHandler.endPrefixMapping(prefix);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length)
        throws SAXException {
      validatorHandler.ignorableWhitespace(ch, start, length);
    }

    @Override
    public void processingInstruction(String target, String data)
        throws SAXException {
      validatorHandler.processingInstruction(target, data);
    }

    @Override
    public void setDocumentLocator(Locator locator) {
      validatorHandler.setDocumentLocator(locator);
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
      validatorHandler.skippedEntity(name);
    }

    @Override
    public void startDocument() throws SAXException {
      validatorHandler.startDocument();
    }

    @Override
    public void startPrefixMapping(String prefix, String uri)
        throws SAXException {
      validatorHandler.startPrefixMapping(prefix, uri);
    }

    @Override
    public void startElement(String uri, String localName, String name,
        Attributes attributes) throws SAXException {
      validatorHandler.startElement(uri, localName, name, attributes);
      
      System.out.println(uri + ":" + localName);
      
      if ("http://www.w3.org/1999/xhtml".equals(uri) &&
          "style".equals(localName)) {
        insideStyle = true;
      }
    }

    @Override
    public void characters(char[] ch, int start, int length)
        throws SAXException {
      validatorHandler.characters(ch, start, length);
      
      if (insideStyle) {
        style.append(ch, start, length);
      }
      
    }

    @Override
    public void endElement(String uri, String localName, String name)
        throws SAXException {
      validatorHandler.endElement(uri, localName, name);
      
      if (insideStyle) {
        insideStyle = false;
        try {
          validateCss(new ByteArrayInputStream(style.toString().getBytes(Charset.forName("UTF-8"))));
        } catch (ValidationException e) {
          throw new SAXException(e);
        }
      }
    }

  }
  
  
}