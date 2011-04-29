/**
 * Copyright (C) 2011 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.cloudhopper.smpp.demo;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.commons.gsm.GsmUtil;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author joelauer
 */
public class ClientMain {
    private static final Logger logger = LoggerFactory.getLogger(ClientMain.class);

    static public void main(String[] args) throws Exception {

        // a bootstrap can be shared (which will reused threads)
        // THIS VERSION USES "DAEMON" threads by default
	// SmppSessionBootstrap bootstrap = new SmppSessionBootstrap();
        // THIS VERSION DOESN'T - WILL HANG JVM UNTIL CLOSED
        DefaultSmppClient bootstrap = new DefaultSmppClient(Executors.newCachedThreadPool());

        DefaultSmppSessionHandler sessionHandler = new ClientSmppSessionHandler();

        SmppSessionConfiguration config0 = new SmppSessionConfiguration();
        config0.setWindowSize(1);
        config0.setName("Tester.Session.0");
        config0.setType(SmppBindType.TRANSCEIVER);
        config0.setHost("127.0.0.1");
        config0.setPort(2776);
        config0.setConnectTimeout(10000);
        config0.setSystemId("1234567890");
        config0.setPassword("password");
        config0.getLoggingOptions().setLogBytes(true);

        SmppSession session0 = null;

        try {
            // attempt to bind and create a session
            session0 = bootstrap.bind(config0, sessionHandler);

            System.out.println("Press any key to send enquireLink #1");
            System.in.read();

            session0.enquireLink(new EnquireLink(), 10000);

            //System.out.println("Press any key to send enquireLink #2");
            //System.in.read();

            //session0.enquireLink(new EnquireLink(), 10000);

            System.out.println("Press any key to send submit");
            System.in.read();

            int count = 10000;
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < count; i++) {
                logger.info("Pending Requests in Window: {}", session0.getRequestWindow().getPendingSize());

                String text160 = "\u20AC Lorem [ipsum] dolor sit amet, consectetur adipiscing elit. Proin feugiat, leo id commodo tincidunt, nibh diam ornare est, vitae accumsan risus lacus sed sem metus.";
                byte[] textBytes = CharsetUtil.encode(text160, CharsetUtil.CHARSET_GSM);
                byte[][] messageParts = GsmUtil.createConcatenatedBinaryShortMessages(textBytes, (byte)0);

                SubmitSm submit0 = new SubmitSm();

                // add delivery receipt
                //submit0.setRegisteredDelivery(SmppConstants.REGISTERED_DELIVERY_SMSC_RECEIPT_REQUESTED);

                submit0.setSourceAddress(new Address((byte)0x03, (byte)0x00, "40404"));
                submit0.setDestAddress(new Address((byte)0x01, (byte)0x01, "44555519205"));

                submit0.setShortMessage(CharsetUtil.encode("Hello World" + i, CharsetUtil.CHARSET_GSM));

                // SYNCHRONOUS TYPE...
                SubmitSmResp submitResp = session0.submit(submit0, 10000);

                // WINDOWED TYPE
                //session0.sendRequestPdu(submit0, 10000, false);
            }

            long stopTime = System.currentTimeMillis();

            Thread.sleep(1000);
            logger.info("Final pending Requests in Window: {}", session0.getRequestWindow().getPendingSize());
            logger.info("With windowSize=" + session0.getRequestWindow().getWindowSize() + " took " + (stopTime-startTime) + " ms to process " + count + " requests");

            System.out.println("Press any key to unbind and close sessions");
            System.in.read();

            session0.unbind(5000);
        } catch (Exception e) {
            logger.error("{}", e);
        }

        if (session0 != null) {
            logger.info("trying to close session...");
            session0.close();
        }

        // this is required to not causing server to hang from non-daemon threads
        // this also makes sure all open Channels are closed to I *think*
        logger.info("trying to shutdown bootstrap...");
        bootstrap.shutdown();

        logger.info("Done. Exiting");
    }

    public static class ClientSmppSessionHandler extends DefaultSmppSessionHandler {

        public ClientSmppSessionHandler() {
            super(logger);
        }

        @Override
        public void firePduRequestExpired(PduRequest pduRequest) {
            logger.warn("PDU request expired: {}", pduRequest);
        }

        @Override
        public PduResponse firePduRequestReceived(PduRequest pduRequest) {
            PduResponse response = pduRequest.createResponse();
            
            // error out just delivery reports
            if (pduRequest instanceof DeliverSm) {
                DeliverSm deliver = (DeliverSm)pduRequest;
            }

            return response;
        }
        
    }
    
}
