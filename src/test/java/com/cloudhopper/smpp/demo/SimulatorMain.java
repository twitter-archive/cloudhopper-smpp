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

import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.simulator.SmppSimulatorServer;
import com.cloudhopper.smpp.simulator.SmppSimulatorSessionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class SimulatorMain {
    private static final Logger logger = LoggerFactory.getLogger(SimulatorMain.class);

    static public void main(String[] args) throws Exception {
        SmppSimulatorServer server = new SmppSimulatorServer();
        server.start(2776);
        logger.info("SMPP simulator server started");

        // wait for a session
        logger.info("Waiting for the first smsc session within 30 seconds");
        SmppSimulatorSessionHandler smscSession = server.pollNextSession(30000);

        logger.info("Successfully got an new session!");

        System.out.println("Press any key to shutdown simulator server");
        System.in.read();

        server.stop();
    }

    public static class SimulatorSmppSessionHandler extends DefaultSmppSessionHandler {

        public SimulatorSmppSessionHandler() {
            super(logger);
        }

        @Override
        public PduResponse firePduRequestReceived(PduRequest pduRequest) {
            // ignore for now (already logged)
            return pduRequest.createResponse();
        }

    }
    
}
