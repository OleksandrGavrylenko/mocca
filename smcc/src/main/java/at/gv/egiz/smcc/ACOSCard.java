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
package at.gv.egiz.smcc;

import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import at.gv.egiz.smcc.util.ISO7816Utils;
import at.gv.egiz.smcc.util.SMCCHelper;
import at.gv.egiz.smcc.util.TransparentFileInputStream;

public class ACOSCard extends AbstractSignatureCard implements PINMgmtSignatureCard {

  private static Log log = LogFactory.getLog(ACOSCard.class);

  public static final byte[] AID_DEC = new byte[] { (byte) 0xA0, (byte) 0x00,
      (byte) 0x00, (byte) 0x01, (byte) 0x18, (byte) 0x45, (byte) 0x4E };

  public static final byte[] DF_DEC = new byte[] { (byte) 0xdf, (byte) 0x71 };

  public static final byte[] AID_SIG = new byte[] { (byte) 0xA0, (byte) 0x00,
      (byte) 0x00, (byte) 0x01, (byte) 0x18, (byte) 0x45, (byte) 0x43 };

  public static final byte[] DF_SIG = new byte[] { (byte) 0xdf, (byte) 0x70 };

  public static final byte[] EF_C_CH_EKEY = new byte[] { (byte) 0xc0,
      (byte) 0x01 };

  public static final int EF_C_CH_EKEY_MAX_SIZE = 2000;

  public static final byte[] EF_C_CH_DS = new byte[] { (byte) 0xc0, (byte) 0x02 };

  public static final int EF_C_CH_DS_MAX_SIZE = 2000;

  public static final byte[] EF_PK_CH_EKEY = new byte[] { (byte) 0xb0,
      (byte) 0x01 };

  public static final byte[] EF_INFOBOX = new byte[] { (byte) 0xc0, (byte) 0x02 };
  
  public static final byte[] EF_INFO = new byte[] { (byte) 0xd0, (byte) 0x02 };

  public static final int EF_INFOBOX_MAX_SIZE = 1500;

  public static final byte KID_PIN_SIG = (byte) 0x81;

  public static final byte KID_PIN_DEC = (byte) 0x81;

  public static final byte KID_PIN_INF = (byte) 0x83;

  public static final byte[] DST_SIG = new byte[] { (byte) 0x84, (byte) 0x01, // tag
      // ,
      // length
      // (
      // key
      // ID
      // )
      (byte) 0x88, // SK.CH.SIGN
      (byte) 0x80, (byte) 0x01, // tag, length (algorithm ID)
      (byte) 0x14 // ECDSA
  };

  public static final byte[] AT_DEC = new byte[] { (byte) 0x84, (byte) 0x01, // tag
      // ,
      // length
      // (
      // key
      // ID
      // )
      (byte) 0x88, // SK.CH.EKEY
      (byte) 0x80, (byte) 0x01, // tag, length (algorithm ID)
      (byte) 0x01 // RSA // TODO: Not verified yet
  };

  private static final PINSpec DEC_PIN_SPEC = new PINSpec(0, 8, "[0-9]",
      "at/gv/egiz/smcc/ACOSCard", "dec.pin.name", KID_PIN_DEC, AID_DEC);

  private static final PINSpec SIG_PIN_SPEC = new PINSpec(0, 8, "[0-9]",
      "at/gv/egiz/smcc/ACOSCard", "sig.pin.name", KID_PIN_SIG, AID_SIG);

  private static final PINSpec INF_PIN_SPEC = new PINSpec(0, 8, "[0-9]",
      "at/gv/egiz/smcc/ACOSCard", "inf.pin.name", KID_PIN_INF, AID_DEC);
  
  /**
   * The version of the card's digital signature application.
   */
  protected int appVersion = -1;

  public ACOSCard() {
    super("at/gv/egiz/smcc/ACOSCard");
  }

  @Override
  public void init(Card card, CardTerminal cardTerminal) {
    super.init(card, cardTerminal);

    // determine application version
    try {
      CardChannel channel = getCardChannel();
      // SELECT application
      execSELECT_AID(channel, AID_SIG);
      // SELECT file
      execSELECT_FID(channel, EF_INFO);
      // READ BINARY
      TransparentFileInputStream is = ISO7816Utils.openTransparentFileInputStream(channel, 8);
      appVersion = is.read();
      log.info("a-sign premium application version = " + appVersion);
    } catch (FileNotFoundException e) {
      appVersion = 1;
      log.info("a-sign premium application version = " + appVersion);
    } catch (SignatureCardException e) {
      log.warn(e);
      appVersion = 0;
    } catch (IOException e) {
      log.warn(e);
      appVersion = 0;
    } catch (CardException e) {
      log.warn(e);
      appVersion = 0;
    } 
    
    pinSpecs.add(DEC_PIN_SPEC);
    pinSpecs.add(SIG_PIN_SPEC);
    if (appVersion < 2) {
      pinSpecs.add(INF_PIN_SPEC);
    }

  }

  @Override
  @Exclusive
  public byte[] getCertificate(KeyboxName keyboxName)
      throws SignatureCardException, InterruptedException {
    
      byte[] aid;
      byte[] fid;
      if (keyboxName == KeyboxName.SECURE_SIGNATURE_KEYPAIR) {
        aid = AID_SIG;
        fid = EF_C_CH_DS;
      } else if (keyboxName == KeyboxName.CERITIFIED_KEYPAIR) {
        aid = AID_DEC;
        fid = EF_C_CH_EKEY;
      } else {
        throw new IllegalArgumentException("Keybox " + keyboxName
            + " not supported.");
      }

      try {
        CardChannel channel = getCardChannel();
        // SELECT application
        execSELECT_AID(channel, aid);
        // SELECT file
        byte[] fcx = execSELECT_FID(channel, fid);
        int maxSize = -1;
        if (getAppVersion() < 2) {
          maxSize = ISO7816Utils.getLengthFromFCx(fcx);
          log.debug("Size of selected file = " + maxSize);
        }
        // READ BINARY
        byte[] certificate = ISO7816Utils.readTransparentFileTLV(channel, maxSize, (byte) 0x30);
        if (certificate == null) {
          throw new NotActivatedException();
        }
        return certificate;
      } catch (FileNotFoundException e) {
        throw new NotActivatedException();
      } catch (CardException e) {
        log.info("Failed to get certificate.", e);
        throw new SignatureCardException(e);
      } 

      
  }

  @Override
  @Exclusive
  public byte[] getInfobox(String infobox, PINProvider provider, String domainId)
      throws SignatureCardException, InterruptedException {
    
    if ("IdentityLink".equals(infobox)) {
      if (getAppVersion() < 2) {
        return getIdentityLinkV1(provider, domainId);
      } else {
        return getIdentityLinkV2(provider, domainId);
      }
    } else {
      throw new IllegalArgumentException("Infobox '" + infobox
          + "' not supported.");
    }
  
  }

  protected byte[] getIdentityLinkV1(PINProvider provider, String domainId) 
      throws SignatureCardException, InterruptedException {
    
    try {
      CardChannel channel = getCardChannel();
      // SELECT application
      execSELECT_AID(channel, AID_DEC);
      // SELECT file
      byte[] fcx = execSELECT_FID(channel, EF_INFOBOX);
      int maxSize = ISO7816Utils.getLengthFromFCx(fcx);
      log.debug("Size of selected file = " + maxSize);
      // READ BINARY
      while(true) {
        try {
          return ISO7816Utils.readTransparentFileTLV(channel, maxSize, (byte) 0x30);
        } catch (SecurityStatusNotSatisfiedException e) {
          verifyPINLoop(channel, INF_PIN_SPEC, provider);
        }
      }
      
    } catch (FileNotFoundException e) {
      throw new NotActivatedException();
    } catch (CardException e) {
      log.info("Faild to get infobox.", e);
      throw new SignatureCardException(e);
    }
    
  }
  
  protected byte[] getIdentityLinkV2(PINProvider provider, String domainId)
      throws SignatureCardException, InterruptedException {
    
    try {
      CardChannel channel = getCardChannel();
      // SELECT application
      execSELECT_AID(channel, AID_DEC);
      // SELECT file
      execSELECT_FID(channel, EF_INFOBOX);
      
      // READ BINARY
      TransparentFileInputStream is = ISO7816Utils.openTransparentFileInputStream(channel, -1);
      
      int b = is.read();
      if (b == 0x00) {
        return null;
      }
      if (b != 0x41 || is.read() != 0x49 || is.read() != 0x4b) {
        String msg = "Infobox structure invalid.";
        log.info(msg);
        throw new SignatureCardException(msg);
      }
        
      b = is.read();
      if (b != 0x01) {
        String msg = "Infobox structure v" + b + " not supported.";
        log.info(msg);
        throw new SignatureCardException(msg);
      }
      
      while ((b = is.read()) != 0x01 && b != 00) {
        is.read(); // modifiers
        is.skip(is.read() + (is.read() << 8)); // length
      }
      
      if (b != 0x01) {
        return null; 
      }
      
      int modifiers = is.read();
      int length = is.read() + (is.read() << 8);

      byte[] bytes;
      byte[] key = null;
      
      switch (modifiers) {
      case 0x00:
        bytes = new byte[length];
        break;
      case 0x01:
        key = new byte[is.read() + (is.read() << 8)];
        is.read(key);
        bytes = new byte[length - key.length - 2];
        break;
      default:
        String msg = "Compressed infobox structure not yet supported.";
        log.info(msg);
        throw new SignatureCardException(msg);
      }
      
      is.read(bytes);
      
      if (key == null) {
        return bytes;
      }

      execMSE(channel, 0x41, 0xb8, new byte[] {
          (byte) 0x84, (byte) 0x01, (byte) 0x88, (byte) 0x80, (byte) 0x01,
          (byte) 0x02 });


      byte[] plainKey = null;

      while (true) {
        try {
          plainKey = execPSO_DECIPHER(channel, key);
          break;
        } catch(SecurityStatusNotSatisfiedException e) {
          verifyPINLoop(channel, DEC_PIN_SPEC, provider);
        }
      }
      
      try {
        Cipher cipher = Cipher
            .getInstance("DESede/CBC/PKCS5Padding");
        byte[] iv = new byte[8];
        Arrays.fill(iv, (byte) 0x00);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        AlgorithmParameters parameters = AlgorithmParameters
            .getInstance("DESede");
        parameters.init(ivParameterSpec);

        DESedeKeySpec keySpec = new DESedeKeySpec(plainKey);
        SecretKeyFactory keyFactory = SecretKeyFactory
            .getInstance("DESede");
        SecretKey secretKey = keyFactory.generateSecret(keySpec);

        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameters);

        return cipher.doFinal(bytes);

      } catch (GeneralSecurityException e) {
        String msg = "Failed to decrypt infobox.";
        log.info(msg, e);
        throw new SignatureCardException(msg, e);
      }
      
      
    } catch (FileNotFoundException e) {
      throw new NotActivatedException();
    } catch (CardException e) {
      log.info("Faild to get infobox.", e);
      throw new SignatureCardException(e);
    } catch (IOException e) {
      if (e.getCause() instanceof SignatureCardException) {
        throw (SignatureCardException) e.getCause();
      } else {
        throw new SignatureCardException(e);
      }
    }
    
  }
  
  @Override
  @Exclusive
  public byte[] createSignature(byte[] hash, KeyboxName keyboxName,
      PINProvider provider) throws SignatureCardException, InterruptedException {
  
    if (hash.length != 20) {
      throw new IllegalArgumentException("Hash value must be of length 20.");
    }
  
    try {
      
      CardChannel channel = getCardChannel();

      if (KeyboxName.SECURE_SIGNATURE_KEYPAIR.equals(keyboxName)) {

        PINSpec spec = SIG_PIN_SPEC;
        
        // SELECT application
        execSELECT_AID(channel, AID_SIG);
        // MANAGE SECURITY ENVIRONMENT : SET DST
        execMSE(channel, 0x41, 0xb6, DST_SIG);
        // VERIFY
        verifyPINLoop(channel, spec, provider);
        // PERFORM SECURITY OPERATION : HASH
        execPSO_HASH(channel, hash);
        // PERFORM SECURITY OPERATION : COMPUTE DIGITAL SIGNATRE
        return execPSO_COMPUTE_DIGITAL_SIGNATURE(channel);
    
      } else if (KeyboxName.CERITIFIED_KEYPAIR.equals(keyboxName)) {
        
        PINSpec spec = DEC_PIN_SPEC;

        // SELECT application
        execSELECT_AID(channel, AID_DEC);
        // MANAGE SECURITY ENVIRONMENT : SET AT
        execMSE(channel, 0x41, 0xa4, AT_DEC);
        
        while (true) {
          try {
            // INTERNAL AUTHENTICATE
            return execINTERNAL_AUTHENTICATE(channel, hash);
          } catch (SecurityStatusNotSatisfiedException e) {
            verifyPINLoop(channel, spec, provider);
          }
        }

      } else {
        throw new IllegalArgumentException("KeyboxName '" + keyboxName
            + "' not supported.");
      }
      
    } catch (CardException e) {
      log.warn(e);
      throw new SignatureCardException("Failed to access card.", e);
    } 
      
  }
  
  public int getAppVersion() {
    return appVersion;
  }

  /* (non-Javadoc)
   * @see at.gv.egiz.smcc.AbstractSignatureCard#verifyPIN(at.gv.egiz.smcc.PINSpec, at.gv.egiz.smcc.PINProvider)
   */
  @Override
  public void verifyPIN(PINSpec pinSpec, PINProvider pinProvider)
      throws LockedException, NotActivatedException, CancelledException,
      TimeoutException, SignatureCardException, InterruptedException {

    CardChannel channel = getCardChannel();
    
    try {
      // SELECT application
      execSELECT_AID(channel, pinSpec.getContextAID());
      // VERIFY
      verifyPINLoop(channel, pinSpec, pinProvider);
    } catch (CardException e) {
      log.info("Failed to verify PIN.", e);
      throw new SignatureCardException("Failed to verify PIN.", e);
    }

  }
  
  /* (non-Javadoc)
   * @see at.gv.egiz.smcc.AbstractSignatureCard#changePIN(at.gv.egiz.smcc.PINSpec, at.gv.egiz.smcc.ChangePINProvider)
   */
  @Override
  public void changePIN(PINSpec pinSpec, ChangePINProvider pinProvider)
      throws LockedException, NotActivatedException, CancelledException,
      TimeoutException, SignatureCardException, InterruptedException {

    CardChannel channel = getCardChannel();
    
    try {
      // SELECT application
      execSELECT_AID(channel, pinSpec.getContextAID());
      // CHANGE REFERENCE DATA
      changePINLoop(channel, pinSpec, pinProvider);
    } catch (CardException e) {
      log.info("Failed to change PIN.", e);
      throw new SignatureCardException("Failed to change PIN.", e);
    }

  }

  @Override
  public void activatePIN(PINSpec pinSpec, PINProvider pinProvider)
      throws CancelledException, SignatureCardException, CancelledException,
      TimeoutException, InterruptedException {
    log.error("ACTIVATE PIN not supported by ACOS");
    throw new SignatureCardException("PIN activation not supported by this card.");
  }

  @Override
  public void unblockPIN(PINSpec pinSpec, PINProvider pinProvider)
      throws CancelledException, SignatureCardException, InterruptedException {
    throw new SignatureCardException("Unblock PIN not supported.");
  }
  
  /* (non-Javadoc)
   * @see at.gv.egiz.smcc.PINMgmtSignatureCard#getPINSpecs()
   */
  @Override
  public List<PINSpec> getPINSpecs() {
    if (getAppVersion() < 2) {
      return Arrays.asList(new PINSpec[] {DEC_PIN_SPEC, SIG_PIN_SPEC, INF_PIN_SPEC});
    } else {
      return Arrays.asList(new PINSpec[] {DEC_PIN_SPEC, SIG_PIN_SPEC});
    }
  }

  /* (non-Javadoc)
   * @see at.gv.egiz.smcc.PINMgmtSignatureCard#getPINStatus(at.gv.egiz.smcc.PINSpec)
   */
  @Override
  public PIN_STATE getPINState(PINSpec pinSpec) throws SignatureCardException {
    return PIN_STATE.UNKNOWN;
  }

  @Override
  public String toString() {
    return "a-sign premium";
  }

  ////////////////////////////////////////////////////////////////////////
  // PROTECTED METHODS (assume exclusive card access)
  ////////////////////////////////////////////////////////////////////////

  protected void verifyPINLoop(CardChannel channel, PINSpec spec, PINProvider provider)
      throws InterruptedException, LockedException, NotActivatedException,
      TimeoutException, PINFormatException, PINOperationAbortedException,
      SignatureCardException, CardException {
    
    int retries = -1;
    do {
      retries = verifyPIN(channel, spec, provider, retries);
    } while (retries > 0);
  }

  protected void changePINLoop(CardChannel channel, PINSpec spec, ChangePINProvider provider)
      throws InterruptedException, LockedException, NotActivatedException,
      TimeoutException, PINFormatException, PINOperationAbortedException,
      SignatureCardException, CardException {

    int retries = -1;
    do {
      retries = changePIN(channel, spec, provider, retries);
    } while (retries > 0);
  }

  protected int verifyPIN(CardChannel channel, PINSpec pinSpec,
      PINProvider provider, int retries) throws InterruptedException, CardException, SignatureCardException {
    
    VerifyAPDUSpec apduSpec = new VerifyAPDUSpec(
        new byte[] {
            (byte) 0x00, (byte) 0x20, (byte) 0x00, pinSpec.getKID(), (byte) 0x08,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 }, 
        0, VerifyAPDUSpec.PIN_FORMAT_ASCII, 8);
    
    ResponseAPDU resp = reader.verify(channel, apduSpec, pinSpec, provider, retries);
    
    if (resp.getSW() == 0x9000) {
      return -1;
    }
    if (resp.getSW() >> 4 == 0x63c) {
      return 0x0f & resp.getSW();
    }

    switch (resp.getSW()) {
    case 0x6983:
      // authentication method blocked
      throw new LockedException();
  
    default:
      String msg = "VERIFY failed. SW=" + Integer.toHexString(resp.getSW()); 
      log.info(msg);
      throw new SignatureCardException(msg);
    }

  }

  protected int changePIN(CardChannel channel, PINSpec pinSpec,
      ChangePINProvider pinProvider, int retries) throws CancelledException, InterruptedException, CardException, SignatureCardException {

    ChangeReferenceDataAPDUSpec apduSpec = new ChangeReferenceDataAPDUSpec(
        new byte[] {
            (byte) 0x00, (byte) 0x24, (byte) 0x00, pinSpec.getKID(), (byte) 0x10,  
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,       
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,       
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,       
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00        
        }, 
        0, VerifyAPDUSpec.PIN_FORMAT_ASCII, 8);
    
    
    
    ResponseAPDU resp = reader.modify(channel, apduSpec, pinSpec, pinProvider, retries);
    
    if (resp.getSW() == 0x9000) {
      return -1;
    }
    if (resp.getSW() >> 4 == 0x63c) {
      return 0x0f & resp.getSW();
    }
    
    switch (resp.getSW()) {
    case 0x6983:
      // authentication method blocked
      throw new LockedException();
  
    default:
      String msg = "CHANGE REFERENCE DATA failed. SW=" + Integer.toHexString(resp.getSW()); 
      log.info(msg);
      throw new SignatureCardException(msg);
    }
    
  }

  protected byte[] execSELECT_AID(CardChannel channel, byte[] aid)
      throws SignatureCardException, CardException {

    ResponseAPDU resp = channel.transmit(
        new CommandAPDU(0x00, 0xA4, 0x04, 0x00, aid, 256));

    if (resp.getSW() == 0x6A82) {
      String msg = "File or application not found AID="
          + SMCCHelper.toString(aid) + " SW="
          + Integer.toHexString(resp.getSW()) + ".";
      log.info(msg);
      throw new FileNotFoundException(msg);
    } else if (resp.getSW() != 0x9000) {
      String msg = "Failed to select application AID="
          + SMCCHelper.toString(aid) + " SW="
          + Integer.toHexString(resp.getSW()) + ".";
      log.info(msg);
      throw new SignatureCardException(msg);
    } else {
      return resp.getBytes();
    }

  }
  
  protected byte[] execSELECT_FID(CardChannel channel, byte[] fid)
      throws SignatureCardException, CardException {
    
    ResponseAPDU resp = channel.transmit(
        new CommandAPDU(0x00, 0xA4, 0x00, 0x00, fid, 256));
    
    if (resp.getSW() == 0x6A82) {
      String msg = "File or application not found FID="
          + SMCCHelper.toString(fid) + " SW="
          + Integer.toHexString(resp.getSW()) + ".";
      log.info(msg);
      throw new FileNotFoundException(msg);
    } else if (resp.getSW() != 0x9000) {
      String msg = "Failed to select application FID="
          + SMCCHelper.toString(fid) + " SW="
          + Integer.toHexString(resp.getSW()) + ".";
      log.error(msg);
      throw new SignatureCardException(msg);
    } else {
      return resp.getBytes();
    }

    
  }
  
  protected void execMSE(CardChannel channel, int p1,
      int p2, byte[] data) throws SignatureCardException, CardException {

    ResponseAPDU resp = channel.transmit(
        new CommandAPDU(0x00, 0x22, p1, p2, data));

    if (resp.getSW() != 0x9000) {
      String msg = "MSE failed: SW="
          + Integer.toHexString(resp.getSW());
      log.error(msg);
      throw new SignatureCardException(msg);
    } 
    
  }
  
  protected byte[] execPSO_DECIPHER(CardChannel channel, byte [] cipher) throws CardException, SignatureCardException {
    
    byte[] data = new byte[cipher.length + 1];
    data[0] = 0x00;
    System.arraycopy(cipher, 0, data, 1, cipher.length);
    ResponseAPDU resp = channel.transmit(new CommandAPDU(0x00, 0x2A, 0x80, 0x86, data, 256));
    if (resp.getSW() == 0x6982) {
      throw new SecurityStatusNotSatisfiedException();
    } else if (resp.getSW() != 0x9000) {
      throw new SignatureCardException(
          "PSO - DECIPHER failed: SW="
          + Integer.toHexString(resp.getSW()));
    }
    
    return resp.getData();
    
  }
  
  protected void execPSO_HASH(CardChannel channel, byte[] hash) throws CardException, SignatureCardException {
    
    ResponseAPDU resp = channel.transmit(
        new CommandAPDU(0x00, 0x2A, 0x90, 0x81, hash));
    if (resp.getSW() != 0x9000) {
      throw new SignatureCardException("PSO - HASH failed: SW="
          + Integer.toHexString(resp.getSW()));
    }
    
  }
  
  protected byte[] execPSO_COMPUTE_DIGITAL_SIGNATURE(CardChannel channel) throws CardException,
      SignatureCardException {

    ResponseAPDU resp = channel.transmit(
        new CommandAPDU(0x00, 0x2A, 0x9E, 0x9A, 256));
    if (resp.getSW() != 0x9000) {
      throw new SignatureCardException(
          "PSO - COMPUTE DIGITAL SIGNATRE failed: SW="
              + Integer.toHexString(resp.getSW()));
    } else {
      return resp.getData();
    }

  }
  
  protected byte[] execINTERNAL_AUTHENTICATE(CardChannel channel, byte[] hash) throws CardException,
      SignatureCardException {

    byte[] digestInfo = new byte[] { (byte) 0x30, (byte) 0x21, (byte) 0x30,
        (byte) 0x09, (byte) 0x06, (byte) 0x05, (byte) 0x2B, (byte) 0x0E,
        (byte) 0x03, (byte) 0x02, (byte) 0x1A, (byte) 0x05, (byte) 0x00,
        (byte) 0x04 };
    
    byte[] data = new byte[digestInfo.length + hash.length + 1];
    
    System.arraycopy(digestInfo, 0, data, 0, digestInfo.length);
    data[digestInfo.length] = (byte) hash.length;
    System.arraycopy(hash, 0, data, digestInfo.length + 1, hash.length);
    
    ResponseAPDU resp = channel.transmit(new CommandAPDU(0x00, 0x88, 0x10, 0x00, data, 256));
    if (resp.getSW() == 0x6982) {
      throw new SecurityStatusNotSatisfiedException();
    } else if (resp.getSW() == 0x6983) {
      throw new LockedException();
    } else if (resp.getSW() != 0x9000) {
      throw new SignatureCardException("INTERNAL AUTHENTICATE failed: SW="
          + Integer.toHexString(resp.getSW()));
    } else {
      return resp.getData();
    }
  }
}
