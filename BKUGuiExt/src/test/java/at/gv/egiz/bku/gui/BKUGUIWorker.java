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
package at.gv.egiz.bku.gui;

import at.gv.egiz.smcc.PinInfo;
import at.gv.egiz.stal.HashDataInput;
import at.gv.egiz.stal.impl.ByteArrayHashDataInput;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author clemens
 */
public class BKUGUIWorker implements Runnable {

  ActivationGUIFacade gui;

  public void init(ActivationGUIFacade gui) {
    this.gui = gui;
  }

  @Override
  public void run() {
        try {

    final PinInfo signPinSpec = new SimplePinInfo(6, 10, "[0-9]", "Signatur-PIN", (byte)0x00, null, PinInfo.UNKNOWN_RETRIES);


    final ActionListener cancelListener = new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        System.out.println("CANCEL EVENT OCCURED: " + e);
      }
    };
    ActionListener okListener = new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        System.out.println("OK EVENT OCCURED: " + e);
      }
    };
    final ActionListener signListener = new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        System.out.println("SIGN EVENT OCCURED: " + e);
      }
    };
    ActionListener hashdataListener = new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        System.out.println("HASHDATA EVENT OCCURED: " + e);
        ActionListener returnListener = new ActionListener() {

          @Override
          public void actionPerformed(ActionEvent e) {
            gui.showSignaturePINDialog(signPinSpec, -1, signListener, "sign", cancelListener, "cancel", null, "hashdata");
          }
        };
        HashDataInput signedRef1 = new ByteArrayHashDataInput(
                "Ich bin ein einfacher Text mit Umlauten: öäüßéç@€\n123\n456\n\tHello, world!\n\nlkjsd\nnksdjf".getBytes(), 
                "ref-id-0000000000000000000000001", 
                "text/plain", 
                "UTF-8",
                "filename.txt");
        
        HashDataInput signedRef2 = new ByteArrayHashDataInput(
                "<xml>HashDataInput_002</xml>".getBytes(), 
                "ref-id-000000002", 
                "application/xhtml+xml", 
                "UTF-8",
                "filename.xhtml");
        
        HashDataInput signedRef3 = new ByteArrayHashDataInput(
                "<xml>HashDataInput_003</xml>".getBytes(), 
                "ref-id-000000003", 
                "application/xhtml+xml", 
                "UTF-8",
                "filename.xhtml");

        HashDataInput signedRef4 = new ByteArrayHashDataInput(
                "<xml>HashDataInput_004</xml>".getBytes(), 
                "ref-id-000000004", 
                "text/xml", 
                "UTF-8",
                "filename.xml");

        //
        List<HashDataInput> signedRefs = new ArrayList();
        signedRefs.add(signedRef1);
                    signedRefs.add(signedRef2);
                    signedRefs.add(signedRef3);
                    signedRefs.add(signedRef4);
//                    signedRefs.add(signedRef4);
//                    signedRefs.add(signedRef4);
//                    signedRefs.add(signedRef4);
//                    signedRefs.add(signedRef4);
//                    signedRefs = Collections.singletonList(signedRef1);
        gui.showSecureViewer(signedRefs, returnListener, "return");
      }
    };



//        gui.showWelcomeDialog();
//
//        Thread.sleep(2000);
//        
//        gui.showWaitDialog(null);
//        
//        Thread.sleep(1000);
//        
//        gui.showWaitDialog("test");
//        
//        Thread.sleep(1000);
//          
//
//            gui.showInsertCardDialog(cancelListener, "cancel");
//
//            Thread.sleep(2000);
//            
//            gui.showCardNotSupportedDialog(cancelListener, "cancel");
//            
//            Thread.sleep(2000);
//
//            PINSpec cardPinSpec = new PINSpec(4, 4, "[0-9]", "Karten-PIN");
//
//            gui.showCardPINDialog(cardPinSpec, okListener, "ok", cancelListener, "cancel");
//            
//            Thread.sleep(2000);
//
//            gui.showSignaturePINDialog(signPinSpec, signListener, "sign", cancelListener, "cancel", hashdataListener, "hashdata");
//
//            Thread.sleep(4000);
//

//            gui.showErrorDialog(BKUGUIFacade.ERR_NO_PCSC, null, null, null);
    
//            gui.showSignaturePINRetryDialog(signPinSpec, 2, signListener, "sign", cancelListener, "cancel", hashdataListener, "hashdata");
//
//            Thread.sleep(2000);
//            
//            gui.showErrorDialog(BKUGUIFacade.ERR_UNKNOWN, new Object[] {"Testfehler"}, null, null);
//            
//            Thread.sleep(2000);
//              
//            gui.showErrorDialog("error.test", new Object[] {"Testfehler", "noch ein TestFehler"}); 
//
//            Thread.sleep(2000);
//            
//            gui.showErrorDialog("error.no.hashdata", null); 
//            
//            Thread.sleep(2000);
//          
//            gui.showErrorDialog(BKUGUIFacade.ERR_UNKNOWN, new Object[] {"Testfehler"}); 
//
//            Thread.sleep(2000);
//          
//            gui.showErrorDialog("error.unknown", null); 

    gui.showActivationProgressDialog(1, 3, null, null);

    gui.incrementProgress();

    Thread.sleep(1000);

    gui.incrementProgress();

    Thread.sleep(1000);

    gui.incrementProgress();


    Thread.sleep(1000);

    gui.showIdleDialog(null, null);

//            gui.showTextPlainHashDataInput("hallo,\n welt!", "12345", null, "cancel", null, "save");
//            gui.showTextPlainHashDataInput("hallo,\n welt!", "12345", null, "cancel", null, "save");
//            Thread.sleep(2000);

        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
  }
}