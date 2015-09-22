package com.cloudhopper.smpp.demo;

/*
 * #%L
 * ch-smpp
 * %%
 * Copyright (C) 2009 - 2015 Cloudhopper by Twitter
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.cloudhopper.commons.util.windowing.WindowFuture;
import com.cloudhopper.smpp.*;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.*;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.SmppProcessingException;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author David.Wilkie <dwilkie@gmail.com>
 */
public class ServerEchoMain {
    private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);

    static public void main(String[] args) throws Exception {
        //
        // setup 3 things required for a server
        //

        // create and assign the NioEventLoopGroup instances to handle event processing,
        // such as accepting new connections, receiving data, writing data, and so on.
        NioEventLoopGroup group = new NioEventLoopGroup();

        // to enable automatic expiration of requests, a second scheduled executor
        // is required which is what a monitor task will be executed with - this
        // is probably a thread pool that can be shared with between all client bootstraps
        ScheduledThreadPoolExecutor monitorExecutor = (ScheduledThreadPoolExecutor)Executors.newScheduledThreadPool(1, new ThreadFactory() {
            private AtomicInteger sequence = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("SmppServerSessionWindowMonitorPool-" + sequence.getAndIncrement());
                return t;
            }
        });

        // create a server configuration
        SmppServerConfiguration configuration = new SmppServerConfiguration();
        configuration.setPort(2776);
        configuration.setMaxConnectionSize(10);
        configuration.setNonBlockingSocketsEnabled(true);
        configuration.setDefaultRequestExpiryTimeout(30000);
        configuration.setDefaultWindowMonitorInterval(15000);
        configuration.setDefaultWindowSize(5);
        configuration.setDefaultWindowWaitTimeout(configuration.getDefaultRequestExpiryTimeout());
        configuration.setDefaultSessionCountersEnabled(true);
        configuration.setJmxEnabled(true);

        // create a server, start it up
        DefaultSmppServer smppServer = new DefaultSmppServer(configuration, new DefaultSmppServerHandler(), monitorExecutor, group, group);

        logger.info("Starting SMPP server...");
        smppServer.start();
        logger.info("SMPP server started");

        System.out.println("Press any key to stop server");
        System.in.read();

        logger.info("Stopping SMPP server...");
        smppServer.stop();
        logger.info("SMPP server stopped");

        logger.info("Server counters: {}", smppServer.getCounters());
    }

    public static class DefaultSmppServerHandler implements SmppServerHandler {

        @Override
        public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration, final BaseBind bindRequest) throws SmppProcessingException {
            // test name change of sessions
            // this name actually shows up as thread context....
            sessionConfiguration.setName("Application.SMPP." + sessionConfiguration.getSystemId());

            //throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL, null);
        }

        @Override
        public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse) throws SmppProcessingException {
            logger.info("Session created: {}", session);
            // need to do something it now (flag we're ready)
            session.serverReady(new TestSmppSessionHandler(session));
        }

        @Override
        public void sessionDestroyed(Long sessionId, SmppServerSession session) {
            logger.info("Session destroyed: {}", session);
            // print out final stats
            if (session.hasCounters()) {
                logger.info(" final session rx-submitSM: {}", session.getCounters().getRxSubmitSM());
            }

            // make sure it's really shutdown
            session.destroy();
        }

    }

    public static class TestSmppSessionHandler extends DefaultSmppSessionHandler {

        private WeakReference<SmppSession> sessionRef;

        public TestSmppSessionHandler(SmppSession session) {
            this.sessionRef = new WeakReference<SmppSession>(session);
        }

        @Override
        public PduResponse firePduRequestReceived(PduRequest pduRequest) {
            SmppSession session = sessionRef.get();

            PduResponse response = pduRequest.createResponse();

            if (pduRequest.getCommandId() == SmppConstants.CMD_ID_SUBMIT_SM) {
                SubmitSm mt = (SubmitSm) pduRequest;
                Address mtSourceAddress = mt.getSourceAddress();
                Address mtDestinationAddress = mt.getDestAddress();
                byte dataCoding = mt.getDataCoding();
                byte[] shortMessage = mt.getShortMessage();

                //sendDeliveryReceipt(session, mtDestinationAddress, mtSourceAddress, dataCoding);
                sendMoMessage(session, mtDestinationAddress, mtSourceAddress, shortMessage, dataCoding);
            }

            return response;
        }

        private void sendDeliveryReceipt(SmppSession session, Address mtDestinationAddress, Address mtSourceAddress, byte dataCoding) {

            DeliverSm deliver = new DeliverSm();
            deliver.setEsmClass(SmppConstants.ESM_CLASS_MT_SMSC_DELIVERY_RECEIPT);
            deliver.setSourceAddress(mtDestinationAddress);
            deliver.setDestAddress(mtSourceAddress);
            deliver.setDataCoding(dataCoding);
            sendRequestPdu(session, deliver);
        }

        private void sendMoMessage(SmppSession session, Address moSourceAddress, Address moDestinationAddress, byte [] textBytes, byte dataCoding) {

            DeliverSm deliver = new DeliverSm();

            deliver.setSourceAddress(moSourceAddress);
            deliver.setDestAddress(moDestinationAddress);
            deliver.setDataCoding(dataCoding);
            try {
              deliver.setShortMessage(textBytes);
            } catch (Exception e) {
              logger.error("Error!", e);
            }

            sendRequestPdu(session, deliver);
        }

        private void sendRequestPdu(SmppSession session, DeliverSm deliver) {
            try {
                WindowFuture<Integer,PduRequest,PduResponse> future = session.sendRequestPdu(deliver, 10000, false);
                if (!future.await()) {
                    logger.error("Failed to receive deliver_sm_resp within specified time");
                } else if (future.isSuccess()) {
                   DeliverSmResp deliverSmResp = (DeliverSmResp)future.getResponse();
                    logger.info("deliver_sm_resp: commandStatus [" + deliverSmResp.getCommandStatus() + "=" + deliverSmResp.getResultMessage() + "]");
                } else {
                    logger.error("Failed to properly receive deliver_sm_resp: " + future.getCause());
                }
            } catch (Exception e) {}
        }
    }
}
