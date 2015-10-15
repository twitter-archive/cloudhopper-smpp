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

import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.util.SmppSessionUtil;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class RebindMain {
    private static final Logger logger = LoggerFactory.getLogger(RebindMain.class);

    static public void main(String[] args) throws Exception {

        // a bootstrap can be shared (which will reused threads)
        // THIS VERSION USES "DAEMON" threads by default
	// SmppSessionBootstrap bootstrap = new SmppSessionBootstrap();
        // THIS VERSION DOESN'T - WILL HANG JVM UNTIL CLOSED
        final DefaultSmppClient bootstrap = new DefaultSmppClient(Executors.newCachedThreadPool());

        final DefaultSmppSessionHandler sessionHandler = new ClientSmppSessionHandler();

        final SmppSessionConfiguration config0 = new SmppSessionConfiguration();
        config0.setWindowSize(1);
        config0.setName("Tester.Session.0");
        config0.setType(SmppBindType.TRANSCEIVER);
        config0.setHost("localhost");
        config0.setPort(2776);
        config0.setConnectTimeout(10000);
        config0.setSystemId("smppclient1");
        config0.setPassword("password");

        Runnable bindRunnable = new Runnable() {

            public void run() {
                SmppSession session0 = null;
                try {
                    while (true) {
                        // attempt to bind and create a session
                        session0 = bootstrap.bind(config0, sessionHandler);

                        Thread.sleep(10);

                        //
                        session0.enquireLink(new EnquireLink(), 10000);

                        Thread.sleep(10);

                        session0.close();

                        logger.info("Final pending Requests in Window: {}", session0.getRequestWindow().getSize());
                        logger.info("With windowSize=" + session0.getRequestWindow().getMaxSize());
                    }
                } catch (Throwable t) {
                    logger.error("{}", t);
                } finally {
                    SmppSessionUtil.close(session0);
                }
            }

        };


        ExecutorService bindExecutor = Executors.newSingleThreadExecutor();
        bindExecutor.submit(bindRunnable);

        //Thread bindThread = new Thread(bindRunnable);
        //bindThread.start();

        System.out.println("Press any key to shutdown the threads");
        System.in.read();

        logger.info("Shutting down the bind executor, waiting up to 10 seconds");
        bindExecutor.shutdownNow();
        bindExecutor.awaitTermination(10000, TimeUnit.MILLISECONDS);

        System.out.println("Press any key to shutdown the bootstrap");
        System.in.read();

        // this is required to not causing server to hang from non-daemon threads
        // this also makes sure all open Channels are closed to I *think*
        logger.info("trying to shutdown bootstrap...");
        bootstrap.destroy();


        System.out.println("Press any key to exit");
        System.in.read();

        logger.info("Done. Exiting");
    }

    public static class ClientSmppSessionHandler extends DefaultSmppSessionHandler {

        public ClientSmppSessionHandler() {
            super(logger);
        }

        @Override
        public PduResponse firePduRequestReceived(PduRequest pduRequest) {
            // ignore for now (already logged)
            return pduRequest.createResponse();
        }
        
    }
    
}
