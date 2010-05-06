/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package at.gv.egiz.bku.slcommands.impl;

import at.gv.egiz.bku.binding.HttpUtil;
import at.gv.egiz.bku.slcommands.impl.xsect.DataObject;
import at.gv.egiz.stal.HashDataInput;
import java.io.InputStream;

/**
 * DataObject-backed HashDataInput
 * If <a href="XMLSignContext.html#Supported Properties">reference caching</a> is enabled,
 * the hashdata input stream can be obtained repeatedly.
 * @author clemens
 */
public class DataObjectHashDataInput implements HashDataInput {

  protected DataObject dataObject;

    public DataObjectHashDataInput(DataObject dataObject) {
      if (dataObject.getReference() == null) 
        throw new NullPointerException("DataObject reference must not be null");
      this.dataObject = dataObject;
    }
    
    @Override
    public String getReferenceId() {
      return dataObject.getReference().getId();
    }

    @Override
    public String getMimeType() {
      String contentType = dataObject.getMimeType();
      return contentType.split(";")[0].trim();
    }

    /**
     * may be called repeatedly
     * @return the pre-digested input stream if reference caching is enabled, null otherwise
     */
    @Override
    public InputStream getHashDataInput() {
        return dataObject.getReference().getDigestInputStream();
    }

    @Override
    public String getEncoding() {
      return HttpUtil.getCharset(dataObject.getMimeType(), false);
    }

    @Override
    public String getFilename() {
      //TODO obtain filename from dataObject, if not set return null or get filename (extension!) from mimetype
      return dataObject.getFilename();
    }

}