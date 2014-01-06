package com.cloudhopper.smpp.impl;

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

// third party imports
import com.cloudhopper.smpp.util.DaemonExecutors;
import com.cloudhopper.commons.util.windowing.OfferTimeoutException;
import com.cloudhopper.commons.util.windowing.WindowFuture;
import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.pdu.BufferHelper;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.EnquireLinkResp;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.pdu.UnbindResp;
import com.cloudhopper.smpp.simulator.SmppSimulatorBindProcessor;
import com.cloudhopper.smpp.simulator.SmppSimulatorPduProcessor;
import com.cloudhopper.smpp.simulator.SmppSimulatorServer;
import com.cloudhopper.smpp.simulator.SmppSimulatorSessionHandler;
import com.cloudhopper.smpp.type.GenericNackException;
import com.cloudhopper.smpp.type.SmppBindException;
import com.cloudhopper.smpp.type.SmppChannelConnectException;
import com.cloudhopper.smpp.type.SmppChannelConnectTimeoutException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.TerminatingNullByteNotFoundException;
import com.cloudhopper.smpp.type.UnexpectedPduResponseException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.util.SmppSessionUtil;
import java.util.concurrent.TimeUnit;
import org.jboss.netty.channel.Channel;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// my imports

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class DefaultSmppSessionTest {
    private static final Logger logger = LoggerFactory.getLogger(DefaultSmppSessionTest.class);

    public static final int PORT = 9785;
    public static final String SYSTEMID = "smppclient1";
    public static final String PASSWORD = "password";

    static SmppSimulatorServer server;
    static DefaultSmppClient bootstrap;
    
    @BeforeClass
    public static void startSimulator() {
        server = new SmppSimulatorServer(DaemonExecutors.newCachedDaemonThreadPool());
        server.start(PORT);
        bootstrap = new DefaultSmppClient();
    }

    @AfterClass
    public static void stopSimulator() {
        logger.info("Stopping the server inside test class");
        server.stop();
        logger.info("Stopping the boostrap inside test class");
        bootstrap.destroy();
    }

    public SmppSessionConfiguration createDefaultConfiguration() {
        SmppSessionConfiguration configuration = new SmppSessionConfiguration();
        configuration.setWindowSize(1);
        configuration.setName("Tester.Session.0");
        configuration.setType(SmppBindType.TRANSCEIVER);
        configuration.setHost("localhost");
        configuration.setPort(PORT);
        configuration.setConnectTimeout(100);
        configuration.setBindTimeout(100);
        configuration.setSystemId(SYSTEMID);
        configuration.setPassword(PASSWORD);
        configuration.getLoggingOptions().setLogBytes(true);
        return configuration;
    }

    public void clearAllServerSessions() {
        server.getHandler().getSessionQueue().clear();
    }

    public void registerServerBindProcessor() {
        server.getHandler().setDefaultPduProcessor(new SmppSimulatorBindProcessor(SYSTEMID, PASSWORD));
    }

    public void unregisterServerBindProcessor() {
        server.getHandler().setDefaultPduProcessor(null);
    }

    @Test
    public void bindToBadPortThrowsSmppChannelConnectException() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        // change this to a port we know a server isn't running on
        configuration.setPort(PORT+1);
        
        DefaultSmppSession session = null;
        try {
            session = (DefaultSmppSession)bootstrap.bind(configuration);
            Assert.fail();
        } catch (SmppChannelConnectException e) {
            // correct behavior
        } finally {
            SmppSessionUtil.close(session);
        }
    }

    @Test
    public void bindToUnknownHostThrowsSmppChannelConnectException() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        // change to a host that doesn't exist
        configuration.setHost("jfjdjdjdjdjjdjd");

        DefaultSmppSession session = null;
        try {
            session = (DefaultSmppSession)bootstrap.bind(configuration);
            Assert.fail();
        } catch (SmppChannelConnectException e) {
            // correct behavior
            logger.info("Expected error message: {}", e.getMessage());
        } finally {
            SmppSessionUtil.close(session);
        }
    }

    @Test
    public void bindToFirewalledHostThrowsSmppChannelConnectTimeoutException() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        // change to a host and port that are definitely firewalled
        configuration.setHost("www.twitter.com");
        configuration.setPort(81);

        DefaultSmppSession session = null;
        try {
            session = (DefaultSmppSession)bootstrap.bind(configuration);
            Assert.fail();
        } catch (SmppChannelConnectTimeoutException e) {
            // correct behavior
            logger.info("Expected error message: {}", e.getMessage());
        } finally {
            SmppSessionUtil.close(session);
        }
    }

    @Test
    public void bindConnectsButNoResponseThrowsSmppTimeoutException() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        unregisterServerBindProcessor();

        DefaultSmppSession session = null;
        try {
            session = (DefaultSmppSession)bootstrap.bind(configuration);
            Assert.fail();
        } catch (SmppTimeoutException e) {
            // correct behavior (underlying cause MUST be a response timeout)
//            Assert.assertNotNull(e.getCause());
//            Assert.assertEquals(ResponseTimeoutException.class, e.getCause().getClass());
        } finally {
            SmppSessionUtil.close(session);
        }
    }

    @Test
    public void bindWithBadCredentialsThrowsSmppBindException() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();

        // set a bad system id
        configuration.setSystemId("BADID");

        DefaultSmppSession session = null;
        try {
            session = (DefaultSmppSession)bootstrap.bind(configuration);
            Assert.fail();
        } catch (SmppBindException e) {
            // correct behavior
            Assert.assertNotNull(e.getBindResponse());
            Assert.assertEquals(SmppConstants.STATUS_INVSYSID, e.getBindResponse().getCommandStatus());
        } finally {
            SmppSessionUtil.close(session);
        }
    }

    @Test
    public void bindOK() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();

        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration);

        // verify the session stuff...
        Assert.assertEquals(true, session.isBound());
        
        SmppSessionUtil.close(session);
    }

    @Test
    public void enquireLinkWithGenericNackResponse() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration);
        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        // register a generic nack will come next
        simulator0.setPduProcessor(new SmppSimulatorPduProcessor() {
            @Override
            public boolean process(SmppSimulatorSessionHandler session, Channel channel, Pdu pdu) throws Exception {
                session.addPduToWriteOnNextPduReceived(((PduRequest)pdu).createGenericNack(SmppConstants.STATUS_SYSERR));
                return true;
            }
        });

        try {
            try {
                session.enquireLink(new EnquireLink(), 1000);
                Assert.fail();
            } catch (GenericNackException e) {
                // correct behavior
            }
        } finally {
            SmppSessionUtil.close(session);
        }
    }

    @Test
    public void enquireLinkWithARequestWithSameSequenceNumber() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration);
        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        // create an enquire link response back -- we should skip it and wait for a response instead
        simulator0.setPduProcessor(new SmppSimulatorPduProcessor() {
            @Override
            public boolean process(SmppSimulatorSessionHandler session, Channel channel, Pdu pdu) throws Exception {
                EnquireLink enquireLink = new EnquireLink();
                enquireLink.setSequenceNumber(pdu.getSequenceNumber());
                session.addPduToWriteOnNextPduReceived(enquireLink);
                return true;
            }
        });

        try {
            try {
                session.enquireLink(new EnquireLink(), 100);
                Assert.fail();
            } catch (SmppTimeoutException e) {
                // correct behavior (underlying cause MUST be a response timeout)
//                Assert.assertNotNull(e.getCause());
//                Assert.assertEquals(ResponseTimeoutException.class, e.getCause().getClass());
            }
        } finally {
            SmppSessionUtil.close(session);
        }
    }

    @Test
    public void multipleEnquireLinks() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration);
        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        // create an enquire link response back -- we should skip it and wait for a response instead
        simulator0.setPduProcessor(new SmppSimulatorPduProcessor() {
            @Override
            public boolean process(SmppSimulatorSessionHandler session, Channel channel, Pdu pdu) throws Exception {
                session.addPduToWriteOnNextPduReceived(((PduRequest)pdu).createResponse());
                return true;
            }
        });

        try {
            session.enquireLink(new EnquireLink(), 100);
            session.enquireLink(new EnquireLink(), 100);
            session.enquireLink(new EnquireLink(), 100);
            session.enquireLink(new EnquireLink(), 100);
            session.enquireLink(new EnquireLink(), 100);
        } finally {
            SmppSessionUtil.close(session);
        }
    }

    @Test
    public void windowSizeBlocksAsyncRequest() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // change window size to 3
        configuration.setWindowSize(3);

        // bind and get the simulator session
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration);
        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        // make sure the processor is null
        simulator0.setPduProcessor(null);

        try {
//            try {
                // create the requests and response we plan on sending
                EnquireLink el0 = new EnquireLink();
                EnquireLink el1 = new EnquireLink();
                EnquireLink el2 = new EnquireLink();
                EnquireLink el3 = new EnquireLink();
                el0.setSequenceNumber(0x7000);
                el1.setSequenceNumber(0x4541);
                el2.setSequenceNumber(0x5414);
                el3.setSequenceNumber(0x2414);
                EnquireLinkResp el0Resp = el0.createResponse();
                EnquireLinkResp el1Resp = el1.createResponse();
                EnquireLinkResp el2Resp = el2.createResponse();
                EnquireLinkResp el3Resp = el3.createResponse();

                // this request should be permitted (with window size = 2)
                WindowFuture future0 = session.sendRequestPdu(el0, 3000, true);
                WindowFuture future1 = session.sendRequestPdu(el1, 3000, true);
                WindowFuture future2 = session.sendRequestPdu(el2, 3000, true);

                Assert.assertEquals(3, session.getSendWindow().getSize());

                try {
                    // window size of 3 is now filled up, this one should timeout
                    session.sendRequestPdu(el3, 100, true);
                    Assert.fail();
                } catch (SmppTimeoutException e) {
                    Assert.assertNotNull(e.getCause());
                    Assert.assertEquals(OfferTimeoutException.class, e.getCause().getClass());
                }

                Assert.assertEquals(3, session.getSendWindow().getSize());

                // now the smsc will send a response back to the second request
                simulator0.sendPdu(el1Resp);

                // wait for the response to make its way back in
                future1.await();

                // there should be 1 slot free now in the window
                Assert.assertEquals(2, session.getSendWindow().getSize());

                // this request should now succeed
                WindowFuture future3 = session.sendRequestPdu(el3, 3000, true);

                // send back responses for everything that's missing
                simulator0.sendPdu(el2Resp);
                simulator0.sendPdu(el0Resp);
                simulator0.sendPdu(el3Resp);

                // make sure they all finished
                future0.await();
                future1.await();
                future2.await();
                future3.await();

                Assert.assertEquals(0, session.getSendWindow().getSize());
        } finally {
            SmppSessionUtil.close(session);
        }
    }


    @Test
    public void cumulationOfMultipleByteBuffersToParsePdu() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        PollableSmppSessionHandler sessionHandler = new PollableSmppSessionHandler();
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration, sessionHandler);

        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        simulator0.setPduProcessor(null);

        try {
            Assert.assertEquals(0, sessionHandler.getReceivedPduRequests().size());

            // send 1 byte
            logger.debug("Sending 1 byte");
            simulator0.getChannel().write(BufferHelper.createBuffer("00")).await();

            // nothing received yet
            Assert.assertEquals(0, sessionHandler.getReceivedPduRequests().size());

            // send 14 more bytes
            logger.debug("Sending 14 more bytes");
            simulator0.getChannel().write(BufferHelper.createBuffer("00001000000015000000000a342e")).await();

            // send 1 more byte
            logger.debug("Sending 1 more bytes");
            simulator0.getChannel().write(BufferHelper.createBuffer("e7")).await();

            // we should have received a PDU request, poll for it
            PduRequest pdu0 = sessionHandler.getReceivedPduRequests().poll(2000, TimeUnit.MILLISECONDS);
            Assert.assertNotNull(pdu0);
            Assert.assertEquals(SmppConstants.CMD_ID_ENQUIRE_LINK, pdu0.getCommandId());
            Assert.assertEquals(0, pdu0.getCommandStatus());
            Assert.assertEquals(16, pdu0.getCommandLength());
            Assert.assertEquals(171192039, pdu0.getSequenceNumber());
        } finally {
            SmppSessionUtil.close(session);
        }
    }

    @Test
    public void routePduResponseToWaitingThread() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        PollableSmppSessionHandler sessionHandler = new PollableSmppSessionHandler();
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration, sessionHandler);

        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        simulator0.setPduProcessor(new SmppSimulatorPduProcessor() {
            @Override
            public boolean process(SmppSimulatorSessionHandler session, Channel channel, Pdu pdu) throws Exception {
                session.addPduToWriteOnNextPduReceived(((PduRequest)pdu).createResponse());
                return true;
            }
        });

        try {
            // this should entirely succeed
            EnquireLink el0 = new EnquireLink();
            el0.setSequenceNumber(0x1000);

            EnquireLinkResp el0Resp = session.enquireLink(el0, 200);

            // check everything is correct afterwards
            Assert.assertEquals(SmppConstants.CMD_ID_ENQUIRE_LINK_RESP, el0Resp.getCommandId());
            Assert.assertEquals(0, el0Resp.getCommandStatus());
            Assert.assertEquals(16, el0Resp.getCommandLength());
            Assert.assertEquals(0x1000, el0Resp.getSequenceNumber());
            Assert.assertEquals(0, sessionHandler.getReceivedPduRequests().size());
            Assert.assertEquals(0, sessionHandler.getReceivedExpectedPduResponses().size());
            Assert.assertEquals(0, sessionHandler.getReceivedUnexpectedPduResponses().size());
        } finally {
            SmppSessionUtil.close(session);
        }
    }


    @Test
    public void receiveExpectedPduResponseViaAnAsynchronousSend() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        PollableSmppSessionHandler sessionHandler = new PollableSmppSessionHandler();
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration, sessionHandler);

        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        simulator0.setPduProcessor(null);

        try {
            EnquireLink el0 = new EnquireLink();
            el0.setSequenceNumber(0x1000);
            EnquireLinkResp el0Resp = el0.createResponse();

            // send asynchronously (no response will be received yet)
            session.sendRequestPdu(el0, 2000, false);

            // send the response back -- this should be routed as n ExpectedPduResponse....
            simulator0.sendPdu(el0Resp);

            // we should have received a PDU response
            PduAsyncResponse asyncpdu0 = sessionHandler.getReceivedExpectedPduResponses().poll(1000, TimeUnit.MILLISECONDS);
            PduResponse pdu0 = asyncpdu0.getResponse();
            Assert.assertNotNull("Unable to receive expected PDU response -- perhaps it was routed incorrectly?", pdu0);
            Assert.assertEquals(SmppConstants.CMD_ID_ENQUIRE_LINK_RESP, pdu0.getCommandId());
            Assert.assertEquals(0, pdu0.getCommandStatus());
            Assert.assertEquals(16, pdu0.getCommandLength());
            Assert.assertEquals(0x1000, pdu0.getSequenceNumber());    
        } finally {
            SmppSessionUtil.close(session);
        }
    }


    @Test
    public void impossiblePDULengthCausesUnrecoverablePduException() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        PollableSmppSessionHandler sessionHandler = new PollableSmppSessionHandler();
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration, sessionHandler);

        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        simulator0.setPduProcessor(null);

        try {
            // send some bytes that should trigger a MAJOR issue during parsing
            simulator0.getChannel().write(BufferHelper.createBuffer("F000001000000015000000000a342ee7")).await();

            // we should have received an Unrecoverable exception, poll for it
            Throwable t = sessionHandler.getThrowables().poll(2000, TimeUnit.MILLISECONDS);
            logger.debug("polled for exception: {}", t.getMessage());

            Assert.assertNotNull(t);
            Assert.assertTrue(t.getMessage(), (t instanceof UnrecoverablePduException));
        } finally {
            SmppSessionUtil.close(session);
        }
    }

    /** THIS TEST NO LONGER APPLIES, WE SUPPORT ALL 32-BITS NOW
    @Test
    public void invalidSequenceNumberCausesUnrecoverablePduException() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        PollableSmppSessionHandler sessionHandler = new PollableSmppSessionHandler();
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration, sessionHandler);

        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        simulator0.setPduProcessor(null);

        try {
            // send some bytes that should trigger a MAJOR issue during parsing
            simulator0.getChannel().write(BufferHelper.createBuffer("00000010000000150000000080000000")).await();

            // we should have received an Unrecoverable exception, poll for it
            Throwable t = sessionHandler.getThrowables().poll(2000, TimeUnit.MILLISECONDS);
            logger.debug("polled for exception: {}", t.getMessage());

            Assert.assertNotNull(t);
            Assert.assertTrue(t.getMessage(), (t instanceof UnrecoverablePduException));
        } finally {
            SmppSessionUtil.close(session);
        }
    }
     */

    @Test
    public void noTerminatingZeroCausesRecoverablePduException() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        PollableSmppSessionHandler sessionHandler = new PollableSmppSessionHandler();
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration, sessionHandler);

        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        simulator0.setPduProcessor(null);

        try {
            // send some bytes that should trigger a MAJOR issue during parsing
            simulator0.getChannel().write(BufferHelper.createBuffer("0000001180000004000000000a342ee139")).await();

            // we should have received an Unrecoverable exception, poll for it
            Throwable t = sessionHandler.getThrowables().poll(2000, TimeUnit.MILLISECONDS);
            logger.debug("polled for exception: {}", t.getMessage());

            Assert.assertNotNull(t);
            Assert.assertTrue(t.getMessage(), (t instanceof TerminatingNullByteNotFoundException));

            TerminatingNullByteNotFoundException ex = (TerminatingNullByteNotFoundException)t;
            // unwrap it
            SubmitSmResp pdu0 = (SubmitSmResp)ex.getPartialPdu();
            Assert.assertEquals(17, pdu0.getCommandLength());
            Assert.assertEquals(SmppConstants.CMD_ID_SUBMIT_SM_RESP, pdu0.getCommandId());
            Assert.assertEquals(0, pdu0.getCommandStatus());
            Assert.assertEquals(171192033, pdu0.getSequenceNumber());
            Assert.assertEquals(true, pdu0.isResponse());
            Assert.assertEquals(null, pdu0.getMessageId());

        } finally {
            SmppSessionUtil.close(session);
        }
    }


    @Test
    public void closeDoesNotTriggerUnexpectedlyClosedEvent() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        PollableSmppSessionHandler sessionHandler = new PollableSmppSessionHandler();
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration, sessionHandler);

        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        simulator0.setPduProcessor(null);

        // close the session (we initiated it)
        session.close();

        Assert.assertEquals(0, sessionHandler.getClosedCount());
        Assert.assertEquals(false, session.isBound());
    }


    @Test
    public void unbindWithNoResponseDoesNotTriggerUnexpectedlyClosedEvent() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        PollableSmppSessionHandler sessionHandler = new PollableSmppSessionHandler();
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration, sessionHandler);

        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        simulator0.setPduProcessor(null);

        // close the session (we initiated it)
        session.unbind(100);

        Assert.assertEquals(0, sessionHandler.getClosedCount());
        Assert.assertEquals(false, session.isBound());
    }
    
    @Test
    public void unbindTriggeringRemoteToCloseDoesNotTriggerUnexpectedlyClosedEvent() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        PollableSmppSessionHandler sessionHandler = new PollableSmppSessionHandler();
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration, sessionHandler);

        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        simulator0.setPduProcessor(new SmppSimulatorPduProcessor() {
            public boolean process(SmppSimulatorSessionHandler session, Channel channel, Pdu pdu) throws Exception {
                // close the channel
                channel.close().await(1000);
                return true;
            }
        });

        // unbind the session now -- this should work okay even though the channel is closed
        session.unbind(100);

        Assert.assertEquals(0, sessionHandler.getClosedCount());
        Assert.assertEquals(false, session.isBound());
    }

    @Test
    public void multipleClosesWorkOK() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        PollableSmppSessionHandler sessionHandler = new PollableSmppSessionHandler();
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration, sessionHandler);

        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        simulator0.setPduProcessor(null);

        Assert.assertEquals(SmppSession.Type.CLIENT, session.getLocalType());
        Assert.assertEquals(SmppSession.Type.SERVER, session.getRemoteType());
        Assert.assertEquals(true, session.isBound());
        Assert.assertEquals(false, session.isBinding());
        Assert.assertEquals(false, session.isClosed());
        Assert.assertEquals(SmppSession.STATES[SmppSession.STATE_BOUND], session.getStateName());

        // close the session (we initiated it)
        session.close();

        Assert.assertEquals(0, sessionHandler.getClosedCount());
        Assert.assertEquals(false, session.isBound());
        Assert.assertEquals(true, session.isClosed());
        Assert.assertEquals(false, session.isOpen());

        session.close();
        session.close();
        session.close();
    }

    @Test
    public void remoteCloseDoesTriggerUnexpectedlyClosedEvent() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        PollableSmppSessionHandler sessionHandler = new PollableSmppSessionHandler();
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration, sessionHandler);

        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        simulator0.setPduProcessor(null);

        simulator0.getChannel().close().awaitUninterruptibly(1000);

        // NOTE: this test sometimes fails if the close hasn't been received yet
        Thread.sleep(500);

        Assert.assertEquals(1, sessionHandler.getClosedCount());
        // DEFAULT handling is that we don't do anything special with this...
        Assert.assertEquals(true, session.isBound());

        // unbind the session now -- this should work okay even though the channel is closed
        session.unbind(100);

        Assert.assertEquals(1, sessionHandler.getClosedCount());
        Assert.assertEquals(false, session.isBound());
        Assert.assertEquals(true, session.isClosed());
    }


    @Test
    public void sendRequestAndGetResponseOKWithResponseTypeNotMatchingOriginalRequest() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        PollableSmppSessionHandler sessionHandler = new PollableSmppSessionHandler();
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration, sessionHandler);

        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        simulator0.setPduProcessor(null);

        try {
            // NOTE: send enquireLink but receive an unbind resp (with same sequence #)
            EnquireLink el0 = new EnquireLink();
            el0.setSequenceNumber(0x1000);

            UnbindResp ubResp = new UnbindResp();
            ubResp.setSequenceNumber(el0.getSequenceNumber());

            // schedule this response on the simulator
            simulator0.addPduToWriteOnNextPduReceived(ubResp);

            // send asynchronously (no response will be received yet)
            PduResponse pdu0 = session.sendRequestAndGetResponse(el0, 2000);
            logger.debug("{}", pdu0);

            // NOTE: this internal method is OK to return an unexpected response
            Assert.assertEquals(16, pdu0.getCommandLength());
            Assert.assertEquals(SmppConstants.CMD_ID_UNBIND_RESP, pdu0.getCommandId());
            Assert.assertEquals(0, pdu0.getCommandStatus());
            Assert.assertEquals(0x1000, pdu0.getSequenceNumber());
            Assert.assertEquals(true, pdu0.isResponse());
        } finally {
            SmppSessionUtil.close(session);
        }
    }

    @Test
    public void enquireLinkFailsWithResponseTypeNotMatchingOriginalRequest() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        PollableSmppSessionHandler sessionHandler = new PollableSmppSessionHandler();
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration, sessionHandler);

        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        simulator0.setPduProcessor(null);

        try {
            // NOTE: send enquireLink but receive an unbind resp (with same sequence #)
            EnquireLink el0 = new EnquireLink();
            el0.setSequenceNumber(0x1000);

            UnbindResp ubResp = new UnbindResp();
            ubResp.setSequenceNumber(el0.getSequenceNumber());

            // schedule this response on the simulator
            simulator0.addPduToWriteOnNextPduReceived(ubResp);

            PduResponse pdu0 = null;
            try {
                EnquireLinkResp el0Resp = session.enquireLink(el0, 200);
                Assert.fail();
            } catch (UnexpectedPduResponseException e) {
                // correct behavior
                pdu0 = e.getResponsePdu();
            }

            logger.debug("unexpected pdu: {}", pdu0);

            Assert.assertEquals(16, pdu0.getCommandLength());
            Assert.assertEquals(SmppConstants.CMD_ID_UNBIND_RESP, pdu0.getCommandId());
            Assert.assertEquals(0, pdu0.getCommandStatus());
            Assert.assertEquals(0x1000, pdu0.getSequenceNumber());
            Assert.assertEquals(true, pdu0.isResponse());
        } finally {
            SmppSessionUtil.close(session);
        }
    }

    @Test
    public void asynchronousPduRequestWithResponseTypeNotMatchingOriginalRequest() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        PollableSmppSessionHandler sessionHandler = new PollableSmppSessionHandler();
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration, sessionHandler);

        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        simulator0.setPduProcessor(null);

        try {
            // NOTE: send enquireLink but receive an unbind resp (with same sequence #)
            EnquireLink el0 = new EnquireLink();
            el0.setSequenceNumber(0x1000);

            UnbindResp ubResp = new UnbindResp();
            ubResp.setSequenceNumber(el0.getSequenceNumber());

            // schedule this response on the simulator
            simulator0.addPduToWriteOnNextPduReceived(ubResp);

            // send this PDU asynchronously
            session.sendRequestPdu(el0, 200, false);

            // wait for an expected pdu response
            PduAsyncResponse asyncpdu0 = sessionHandler.getReceivedExpectedPduResponses().poll(1000, TimeUnit.MILLISECONDS);
            logger.debug("{}", asyncpdu0);

            PduResponse pdu0 = asyncpdu0.getResponse();


            /**
            PduResponse pdu0 = null;
            try {
                EnquireLinkResp el0Resp = session.enquireLink(el0, 200);
                Assert.fail();
            } catch (UnexpectedPduResponseException e) {
                // correct behavior
                pdu0 = e.getResponsePdu();
            }

            logger.debug("unexpected pdu: {}", pdu0);

            Assert.assertEquals(16, pdu0.getCommandLength());
            Assert.assertEquals(SmppConstants.CMD_ID_UNBIND_RESP, pdu0.getCommandId());
            Assert.assertEquals(0, pdu0.getCommandStatus());
            Assert.assertEquals(0x1000, pdu0.getSequenceNumber());
            Assert.assertEquals(true, pdu0.isResponse());
             */
        } finally {
            SmppSessionUtil.close(session);
        }
    }
    
    
    @Test
    public void receiveUnexpectedPduResponseAfterSenderThreadTimeoutWaiting() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        PollableSmppSessionHandler sessionHandler = new PollableSmppSessionHandler();
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration, sessionHandler);

        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        simulator0.setPduProcessor(null);

        try {
            EnquireLink el0 = new EnquireLink();
            el0.setSequenceNumber(0x1001);
            EnquireLinkResp el0Resp = el0.createResponse();

            // send a request and wait for a response that never shows up
            WindowFuture future = session.sendRequestPdu(el0, 50, true);
            Assert.assertFalse(future.await());
            // a call to cancel() is usually done in sendRequestPduAndGetResponse
            // but for this test we'll manually need to call it here
            future.cancel();

            Assert.assertEquals(WindowFuture.CALLER_WAITING_TIMEOUT, future.getCallerStateHint());
            
            // send a response now after the caller would have timed out
            simulator0.sendPdu(el0Resp);

            // we should have received an unexpected PDU response
            Assert.assertEquals(0, sessionHandler.getReceivedPduRequests().size());
            Assert.assertEquals(0, sessionHandler.getReceivedExpectedPduResponses().size());
            PduResponse pdu0 = sessionHandler.getReceivedUnexpectedPduResponses().poll(1000, TimeUnit.MILLISECONDS);
            Assert.assertNotNull("Unable to receive unexpected PDU response -- perhaps it was routed incorrectly?", pdu0);
            Assert.assertEquals(SmppConstants.CMD_ID_ENQUIRE_LINK_RESP, pdu0.getCommandId());
            Assert.assertEquals(0, pdu0.getCommandStatus());
            Assert.assertEquals(16, pdu0.getCommandLength());
            Assert.assertEquals(0x1001, pdu0.getSequenceNumber());

            Assert.assertEquals(0, sessionHandler.getReceivedUnexpectedPduResponses().size());
        } finally {
            SmppSessionUtil.close(session);
        }
    }
    
    @Test
    public void receiveUnexpectedPduResponse() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        PollableSmppSessionHandler sessionHandler = new PollableSmppSessionHandler();
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration, sessionHandler);

        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        simulator0.setPduProcessor(null);

        try {
            EnquireLink el0 = new EnquireLink();
            el0.setSequenceNumber(0x1000);
            EnquireLinkResp el0Resp = el0.createResponse();

            // send a response to a request that was NEVER sent
            simulator0.sendPdu(el0Resp);

            // we should have received a PDU response
            PduResponse pdu0 = sessionHandler.getReceivedUnexpectedPduResponses().poll(1000, TimeUnit.MILLISECONDS);
            Assert.assertNotNull("Unable to receive unexpected PDU response -- perhaps it was routed incorrectly?", pdu0);
            Assert.assertEquals(SmppConstants.CMD_ID_ENQUIRE_LINK_RESP, pdu0.getCommandId());
            Assert.assertEquals(0, pdu0.getCommandStatus());
            Assert.assertEquals(16, pdu0.getCommandLength());
            Assert.assertEquals(0x1000, pdu0.getSequenceNumber());

            Assert.assertEquals(0, sessionHandler.getReceivedPduRequests().size());
            Assert.assertEquals(0, sessionHandler.getReceivedExpectedPduResponses().size());
            Assert.assertEquals(0, sessionHandler.getReceivedUnexpectedPduResponses().size());
        } finally {
            SmppSessionUtil.close(session);
        }
    }
    
    
    @Test
    public void synchronousSendButNeverGetResponse() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        PollableSmppSessionHandler sessionHandler = new PollableSmppSessionHandler();
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration, sessionHandler);

        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        simulator0.setPduProcessor(null);

        try {
            try {
                session.enquireLink(new EnquireLink(), 100);
                // request should timeout
                Assert.fail();
            } catch (SmppTimeoutException e) {
                // correct behavior
            }
            
            // with a "synchronous" type of send, after a timeout, the request
            // should have been cancelled
            Assert.assertEquals(0, session.getSendWindow().getSize()); 
        } finally {
            SmppSessionUtil.close(session);
        }
    }
    
    @Test
    public void shutdown() throws Exception {
        SmppSessionConfiguration configuration = createDefaultConfiguration();
        registerServerBindProcessor();
        clearAllServerSessions();

        // bind and get the simulator session
        PollableSmppSessionHandler sessionHandler = new PollableSmppSessionHandler();
        DefaultSmppSession session = (DefaultSmppSession)bootstrap.bind(configuration, sessionHandler);

        SmppSimulatorSessionHandler simulator0 = server.pollNextSession(1000);
        simulator0.setPduProcessor(null);

        // load up the "window" with a request
        session.sendRequestPdu(new EnquireLink(), 5000, false);
        
        Assert.assertEquals(1, session.getSendWindow().getSize());
        
        // make sure that a shutdown request performs all expected cleanup
        session.destroy();
        
        Assert.assertEquals(0, session.getSendWindow().getSize());
    }

}
