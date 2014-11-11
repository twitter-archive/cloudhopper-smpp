package com.cloudhopper.smpp.demo;

/*
 * #%L
 * ch-smpp
 * %%
 * Copyright (C) 2009 - 2012 Cloudhopper by Twitter
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
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitMulti;
import com.cloudhopper.smpp.pdu.SubmitMultiResp;
import com.cloudhopper.smpp.test.SmppTestDataProvider;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.SubmitMultiDestinationAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author innar.made@ishisystems.com
 */
public class SubmitMultiMain {

    private static final Logger logger = LoggerFactory.getLogger(SubmitMultiMain.class);

    private static ThreadPoolExecutor executor;
    private static ScheduledThreadPoolExecutor monitorExecutor;
    private static DefaultSmppClient clientBootstrap;
    private static SmppSession session;

    public static void main(String[] args) throws Exception {
        createClient();
        promptSendSubmitMulti();
        sendRequest(getSubmitMulti());
        promptDisconnect();
        disconnect();
    }

    private static void createClient() throws Exception {
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

        monitorExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1, new ThreadFactory() {
            private AtomicInteger sequence = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable runnable) {
                Thread t = new Thread(runnable);
                t.setName("SmppClientSessionWindowMonitorPool-" + sequence.getAndIncrement());
                return t;
            }
        });

        clientBootstrap = new DefaultSmppClient(executor, 1, monitorExecutor);
        DefaultSmppSessionHandler sessionHandler = new ClientSmppSessionHandler();

        SmppSessionConfiguration configuration = new SmppSessionConfiguration();
        configuration.setWindowSize(1);
        configuration.setName("Tester.Session.0");
        configuration.setType(SmppBindType.TRANSCEIVER);
        configuration.setHost("127.0.0.1");
        configuration.setPort(2776);
        configuration.setConnectTimeout(10000);
        configuration.setSystemId("smppclient1");
        configuration.setPassword("password");
        configuration.getLoggingOptions().setLogBytes(true);
        configuration.setRequestExpiryTimeout(30000);
        configuration.setWindowMonitorInterval(15000);
        configuration.setCountersEnabled(true);

        session = clientBootstrap.bind(configuration, sessionHandler);
    }

    private static SubmitMulti getSubmitMulti() throws Exception {
        SubmitMulti submitMulti = SmppTestDataProvider.getDefaultSubmitMulti();
        List<SubmitMultiDestinationAddress> addressList = new ArrayList<SubmitMultiDestinationAddress>();
        addressList.add(new SubmitMultiDestinationAddress(new Address((byte) 4, (byte) 5, "987654321")));
        addressList.add(new SubmitMultiDestinationAddress("12345678901"));
        addressList.add(new SubmitMultiDestinationAddress(new Address((byte) 7, (byte) 8, "123454321")));
        submitMulti.setSubmitMultiDestinationAddressList(addressList);
        return submitMulti;
    }

    private static void sendRequest(SubmitMulti submitMulti) throws Exception {
        WindowFuture<Integer, PduRequest, PduResponse> future = session.sendRequestPdu(submitMulti, 10000, true);
        if (!future.await()) {
            logger.error("Failed to receive submit_multi_resp within specified time");
        } else if (future.isSuccess()) {
            SubmitMultiResp submitMultiResp = (SubmitMultiResp) future.getResponse();
            logger.info("submit_multi_resp: commandStatus [" + submitMultiResp.getCommandStatus() + "=" + submitMultiResp.getResultMessage() + "]");
        } else {
            logger.error("Failed to properly receive submit_multi_resp: " + future.getCause());
        }
    }

    private static void promptSendSubmitMulti() throws IOException {
        System.out.println("Press any key to send submit_multi");
        System.in.read();
    }

    private static void promptDisconnect() throws IOException {
        System.out.println("Press any key to unbind and close sessions");
        System.in.read();
    }

    private static void disconnect() {
        session.unbind(5000);
        clientBootstrap.destroy();
        executor.shutdownNow();
        monitorExecutor.shutdownNow();
    }

    public static class ClientSmppSessionHandler extends DefaultSmppSessionHandler {

        public ClientSmppSessionHandler() {
            super(logger);
        }

        @Override
        public PduResponse firePduRequestReceived(PduRequest pduRequest) {
            return pduRequest.createResponse();
        }

    }

}
