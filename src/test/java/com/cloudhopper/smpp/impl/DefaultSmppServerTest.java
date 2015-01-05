package com.cloudhopper.smpp.impl;

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

// third party imports
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.type.SmppBindException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppProcessingException;
import java.util.HashSet;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// my imports

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class DefaultSmppServerTest {
    private static final Logger logger = LoggerFactory.getLogger(DefaultSmppServerTest.class);

    public static final int PORT = 9784;
    public static final String SYSTEMID = "smppclient1";
    public static final String PASSWORD = "password";

    private TestSmppServerHandler serverHandler = new TestSmppServerHandler();

    public SmppServerConfiguration createSmppServerConfiguration() {
        SmppServerConfiguration configuration = new SmppServerConfiguration();
        configuration.setPort(PORT);
        configuration.setSystemId("cloudhopper");
        return configuration;
    }
    
    public DefaultSmppServer createSmppServer() {
        SmppServerConfiguration configuration = createSmppServerConfiguration();
        DefaultSmppServer smppServer = new DefaultSmppServer(configuration, serverHandler);
        return smppServer;
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

    public static class TestSmppServerHandler implements SmppServerHandler {
        public HashSet<SmppServerSession> sessions = new HashSet<SmppServerSession>();
        public PollableSmppSessionHandler sessionHandler = new PollableSmppSessionHandler();

        @Override
        public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration, final BaseBind bindRequest) throws SmppProcessingException {
            // test name change of sessions
            sessionConfiguration.setName("Test1");

            if (!SYSTEMID.equals(bindRequest.getSystemId())) {
                throw new SmppProcessingException(SmppConstants.STATUS_INVSYSID);
            }

            if (!PASSWORD.equals(bindRequest.getPassword())) {
                throw new SmppProcessingException(SmppConstants.STATUS_INVPASWD);
            }

            //throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL, null);
        }

        @Override
        public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse) {
            sessions.add(session);
            // need to do something it now (flag we're ready)
            session.serverReady(sessionHandler);
        }

        @Override
        public void sessionDestroyed(Long sessionId, SmppServerSession session) {
            sessions.remove(session);
        }
    }

    @Test
    public void serverSessionOK() throws Exception {
        DefaultSmppServer server0 = createSmppServer();
        server0.start();

        try {
            DefaultSmppClient client0 = new DefaultSmppClient();
            SmppSessionConfiguration sessionConfig0 = createDefaultConfiguration();
            // this should actually work
            SmppSession session0 = client0.bind(sessionConfig0);

            Thread.sleep(100);

            SmppServerSession serverSession0 = serverHandler.sessions.iterator().next();
            Assert.assertEquals(1, serverHandler.sessions.size());
            Assert.assertEquals(1, server0.getChannels().size());
            Assert.assertEquals(true, serverSession0.isBound());
            Assert.assertEquals(SmppBindType.TRANSCEIVER, serverSession0.getBindType());
            Assert.assertEquals(SmppSession.Type.SERVER, serverSession0.getLocalType());
            Assert.assertEquals(SmppSession.Type.CLIENT, serverSession0.getRemoteType());

            serverSession0.close();
            Thread.sleep(200);
            Assert.assertEquals(0, serverHandler.sessions.size());
            Assert.assertEquals(0, server0.getChannels().size());
            Assert.assertEquals(false, serverSession0.isBound());
        } finally {
            server0.destroy();
        }
    }

    @Test
    public void serverSessionBindRejectedWithInvalidSystemId() throws Exception {
        DefaultSmppServer server0 = createSmppServer();
        server0.start();

        try {
            DefaultSmppClient client0 = new DefaultSmppClient();
            SmppSessionConfiguration sessionConfig0 = createDefaultConfiguration();
            sessionConfig0.setSystemId("TESTID");

            // this should fail (invalid system id)
            try {
                SmppSession session0 = client0.bind(sessionConfig0);
                Assert.fail();
            } catch (SmppBindException e) {
                Assert.assertEquals(SmppConstants.STATUS_INVSYSID, e.getBindResponse().getCommandStatus());
            }

            // give this a little time to catch up
            Thread.sleep(100);

            Assert.assertEquals(0, serverHandler.sessions.size());
            Assert.assertEquals(0, server0.getChannels().size());
        } finally {
            server0.destroy();
        }
    }

    @Test
    public void serverSessionBindRejectedWithInvalidPassword() throws Exception {
        DefaultSmppServer server0 = createSmppServer();
        server0.start();

        try {
            DefaultSmppClient client0 = new DefaultSmppClient();
            SmppSessionConfiguration sessionConfig0 = createDefaultConfiguration();
            sessionConfig0.setSystemId(SYSTEMID);
            sessionConfig0.setPassword("BADPASS");

            // this should fail (invalid password)
            try {
                SmppSession session0 = client0.bind(sessionConfig0);
                Assert.fail();
            } catch (SmppBindException e) {
                Assert.assertEquals(SmppConstants.STATUS_INVPASWD, e.getBindResponse().getCommandStatus());
            }

            Assert.assertEquals(0, serverHandler.sessions.size());
            Assert.assertEquals(0, server0.getChannels().size());
        } finally {
            server0.destroy();
        }
    }

    @Test
    public void serverSessionBindVersion33() throws Exception {
        DefaultSmppServer server0 = createSmppServer();
        server0.start();

        try {
            DefaultSmppClient client0 = new DefaultSmppClient();
            SmppSessionConfiguration sessionConfig0 = createDefaultConfiguration();

            // set back to version 3.3
            sessionConfig0.setInterfaceVersion(SmppConstants.VERSION_3_3);

            // we will not use the proper method of binding since we need to 
            // access the bind response to verify it's correct
            DefaultSmppSession session0 = client0.doOpen(sessionConfig0, new DefaultSmppSessionHandler());

            // create a bind request based on this config
            BaseBind bindRequest = client0.createBindRequest(sessionConfig0);

            // execute a bind request and wait for a bind response
            BaseBindResp bindResponse = session0.bind(bindRequest, 200);

            Thread.sleep(100);

            SmppServerSession serverSession0 = serverHandler.sessions.iterator().next();
            Assert.assertEquals(1, serverHandler.sessions.size());
            Assert.assertEquals(1, server0.getChannels().size());
            Assert.assertEquals(true, serverSession0.isBound());
            Assert.assertEquals(SmppBindType.TRANSCEIVER, serverSession0.getBindType());
            Assert.assertEquals(SmppSession.Type.SERVER, serverSession0.getLocalType());
            Assert.assertEquals(SmppSession.Type.CLIENT, serverSession0.getRemoteType());

            // verify "requested" version is still 3.3
            Assert.assertEquals(SmppConstants.VERSION_3_3, serverSession0.getConfiguration().getInterfaceVersion());
            // verify the session interface version is normalized to 3.3
            Assert.assertEquals(SmppConstants.VERSION_3_3, serverSession0.getInterfaceVersion());
            Assert.assertEquals(false, serverSession0.areOptionalParametersSupported());

            // verify client session version settings are correct
            Assert.assertEquals((byte)0x33, session0.getConfiguration().getInterfaceVersion());
            Assert.assertEquals((byte)0x33, session0.getInterfaceVersion());
            Assert.assertEquals(false, session0.areOptionalParametersSupported());

            // verify NO optional parameters were included in bind response
            Assert.assertEquals(0, bindResponse.getOptionalParameterCount());
            Assert.assertEquals("cloudhopper", bindResponse.getSystemId());

            serverSession0.close();
            Thread.sleep(200);
            Assert.assertEquals(0, serverHandler.sessions.size());
            Assert.assertEquals(0, server0.getChannels().size());
            Assert.assertEquals(false, serverSession0.isBound());
        } finally {
            server0.destroy();
        }
    }

    @Test
    public void serverSessionBindVersion31NormalizedTo33() throws Exception {
        DefaultSmppServer server0 = createSmppServer();
        server0.start();

        try {
            DefaultSmppClient client0 = new DefaultSmppClient();
            SmppSessionConfiguration sessionConfig0 = createDefaultConfiguration();

            // set to version 3.1
            sessionConfig0.setInterfaceVersion((byte)0x31);

            // we will not use the proper method of binding since we need to
            // access the bind response to verify it's correct
            DefaultSmppSession session0 = client0.doOpen(sessionConfig0, new DefaultSmppSessionHandler());

            // create a bind request based on this config
            BaseBind bindRequest = client0.createBindRequest(sessionConfig0);

            // execute a bind request and wait for a bind response
            BaseBindResp bindResponse = session0.bind(bindRequest, 200);

            Thread.sleep(100);

            SmppServerSession serverSession0 = serverHandler.sessions.iterator().next();
            Assert.assertEquals(1, serverHandler.sessions.size());
            Assert.assertEquals(1, server0.getChannels().size());
            Assert.assertEquals(true, serverSession0.isBound());
            Assert.assertEquals(SmppBindType.TRANSCEIVER, serverSession0.getBindType());
            Assert.assertEquals(SmppSession.Type.SERVER, serverSession0.getLocalType());
            Assert.assertEquals(SmppSession.Type.CLIENT, serverSession0.getRemoteType());

            // verify "requested" version is 3.1
            Assert.assertEquals((byte)0x31, serverSession0.getConfiguration().getInterfaceVersion());
            // verify the session interface version is normalized to 3.3
            Assert.assertEquals(SmppConstants.VERSION_3_3, serverSession0.getInterfaceVersion());
            Assert.assertEquals(false, serverSession0.areOptionalParametersSupported());

            // verify client session version settings are correct
            Assert.assertEquals((byte)0x31, session0.getConfiguration().getInterfaceVersion());
            Assert.assertEquals((byte)0x33, session0.getInterfaceVersion());
            Assert.assertEquals(false, session0.areOptionalParametersSupported());

            // verify NO optional parameters were included in bind response
            Assert.assertEquals(0, bindResponse.getOptionalParameterCount());
            Assert.assertEquals("cloudhopper", bindResponse.getSystemId());

            serverSession0.close();
            Thread.sleep(200);
            Assert.assertEquals(0, serverHandler.sessions.size());
            Assert.assertEquals(0, server0.getChannels().size());
            Assert.assertEquals(false, serverSession0.isBound());
        } finally {
            server0.destroy();
        }
    }

    @Test
    public void serverSessionBindVersion34() throws Exception {
        DefaultSmppServer server0 = createSmppServer();
        server0.start();

        try {
            DefaultSmppClient client0 = new DefaultSmppClient();
            SmppSessionConfiguration sessionConfig0 = createDefaultConfiguration();

            // set to version 3.4
            sessionConfig0.setInterfaceVersion((byte)0x34);

            // we will not use the proper method of binding since we need to
            // access the bind response to verify it's correct
            DefaultSmppSession session0 = client0.doOpen(sessionConfig0, new DefaultSmppSessionHandler());

            // create a bind request based on this config
            BaseBind bindRequest = client0.createBindRequest(sessionConfig0);

            // execute a bind request and wait for a bind response
            BaseBindResp bindResponse = session0.bind(bindRequest, 200);

            Thread.sleep(100);

            SmppServerSession serverSession0 = serverHandler.sessions.iterator().next();
            Assert.assertEquals(1, serverHandler.sessions.size());
            Assert.assertEquals(1, server0.getChannels().size());
            Assert.assertEquals(true, serverSession0.isBound());
            Assert.assertEquals(SmppBindType.TRANSCEIVER, serverSession0.getBindType());
            Assert.assertEquals(SmppSession.Type.SERVER, serverSession0.getLocalType());
            Assert.assertEquals(SmppSession.Type.CLIENT, serverSession0.getRemoteType());

            // verify "requested" version is 3.4
            Assert.assertEquals((byte)0x34, serverSession0.getConfiguration().getInterfaceVersion());
            // verify the session interface version is normalized to 3.4
            Assert.assertEquals(SmppConstants.VERSION_3_4, serverSession0.getInterfaceVersion());
            Assert.assertEquals(true, serverSession0.areOptionalParametersSupported());

            // verify client session version settings are correct
            Assert.assertEquals((byte)0x34, session0.getConfiguration().getInterfaceVersion());
            Assert.assertEquals((byte)0x34, session0.getInterfaceVersion());
            Assert.assertEquals(true, session0.areOptionalParametersSupported());

            // verify 1 optional parameter was included in bind response
            Assert.assertEquals(1, bindResponse.getOptionalParameterCount());
            Assert.assertEquals("cloudhopper", bindResponse.getSystemId());

            Tlv scInterfaceVersion = bindResponse.getOptionalParameter(SmppConstants.TAG_SC_INTERFACE_VERSION);
            Assert.assertNotNull(scInterfaceVersion);
            Assert.assertEquals(SmppConstants.VERSION_3_4, scInterfaceVersion.getValueAsByte());

            serverSession0.close();
            Thread.sleep(200);
            Assert.assertEquals(0, serverHandler.sessions.size());
            Assert.assertEquals(0, server0.getChannels().size());
            Assert.assertEquals(false, serverSession0.isBound());
        } finally {
            server0.destroy();
        }
    }

    @Test
    public void serverSessionBindVersion35NormalizesTo34() throws Exception {
        DefaultSmppServer server0 = createSmppServer();
        server0.start();

        try {
            DefaultSmppClient client0 = new DefaultSmppClient();
            SmppSessionConfiguration sessionConfig0 = createDefaultConfiguration();

            // set to version 3.5
            sessionConfig0.setInterfaceVersion((byte)0x35);

            // we will not use the proper method of binding since we need to
            // access the bind response to verify it's correct
            DefaultSmppSession session0 = client0.doOpen(sessionConfig0, new DefaultSmppSessionHandler());

            // create a bind request based on this config
            BaseBind bindRequest = client0.createBindRequest(sessionConfig0);

            // execute a bind request and wait for a bind response
            BaseBindResp bindResponse = session0.bind(bindRequest, 200);

            Thread.sleep(100);

            SmppServerSession serverSession0 = serverHandler.sessions.iterator().next();
            Assert.assertEquals(1, serverHandler.sessions.size());
            Assert.assertEquals(1, server0.getChannels().size());
            Assert.assertEquals(true, serverSession0.isBound());
            Assert.assertEquals(SmppBindType.TRANSCEIVER, serverSession0.getBindType());
            Assert.assertEquals(SmppSession.Type.SERVER, serverSession0.getLocalType());
            Assert.assertEquals(SmppSession.Type.CLIENT, serverSession0.getRemoteType());

            // verify "requested" version is 3.5
            Assert.assertEquals((byte)0x35, serverSession0.getConfiguration().getInterfaceVersion());
            // verify the session interface version is normalized to 3.4
            Assert.assertEquals(SmppConstants.VERSION_3_4, serverSession0.getInterfaceVersion());
            Assert.assertEquals(true, serverSession0.areOptionalParametersSupported());

            // verify client session version settings are correct
            Assert.assertEquals((byte)0x35, session0.getConfiguration().getInterfaceVersion());
            Assert.assertEquals((byte)0x34, session0.getInterfaceVersion());
            Assert.assertEquals(true, session0.areOptionalParametersSupported());

            // verify 1 optional parameter was included in bind response
            Assert.assertEquals(1, bindResponse.getOptionalParameterCount());
            Assert.assertEquals("cloudhopper", bindResponse.getSystemId());

            Tlv scInterfaceVersion = bindResponse.getOptionalParameter(SmppConstants.TAG_SC_INTERFACE_VERSION);
            Assert.assertNotNull(scInterfaceVersion);
            Assert.assertEquals(SmppConstants.VERSION_3_4, scInterfaceVersion.getValueAsByte());

            serverSession0.close();
            Thread.sleep(200);
            Assert.assertEquals(0, serverHandler.sessions.size());
            Assert.assertEquals(0, server0.getChannels().size());
            Assert.assertEquals(false, serverSession0.isBound());
        } finally {
            server0.destroy();
        }
    }

    @Test
    public void serverSessionTimesOutWithNoBindRequest() throws Exception {
        DefaultSmppServer server0 = createSmppServer();
        server0.getConfiguration().setBindTimeout(50);
        server0.start();

        try {
            DefaultSmppClient client0 = new DefaultSmppClient();
            SmppSessionConfiguration sessionConfig0 = createDefaultConfiguration();

            // we will not use the proper method of binding since we need to
            DefaultSmppSession session0 = client0.doOpen(sessionConfig0, new DefaultSmppSessionHandler());

            // there is a bind timeout of 50 ms and we'll wait 100 ms
            Thread.sleep(100);

            // try to bind and execute a bind request and wait for a bind response
            BaseBind bindRequest = client0.createBindRequest(sessionConfig0);

            try {
                BaseBindResp bindResponse = session0.bind(bindRequest, 200);
                Assert.fail();
            } catch (SmppChannelException e) {
                // correct behavior
            }

            // verify everything after the session timed out
            Assert.assertEquals(0, serverHandler.sessions.size());
            Assert.assertEquals(0, server0.getChannels().size());
            Assert.assertEquals(false, session0.isBound());
            Assert.assertEquals(true, session0.isClosed());
            
        } finally {
            server0.destroy();
        }
    }
    

    public static class BlockThreadSmppServerHandler implements SmppServerHandler {
        @Override
        public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration, final BaseBind bindRequest) throws SmppProcessingException {
            // we want to block processing for a period of time
            try {
                Thread.sleep(10000);
            } catch (Exception e) {
                // do nothing
            }
        }

        @Override
        public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse) {
            // do nothing
        }

        @Override
        public void sessionDestroyed(Long sessionId, SmppServerSession session) {
            // do nothing
        }
    }
    
    @Test
    public void serverNotEnoughWorkerThreadsCausesBindTimerToCloseChannel() throws Exception {
        BlockThreadSmppServerHandler serverHandler0 = new BlockThreadSmppServerHandler();
        SmppServerConfiguration configuration = createSmppServerConfiguration();
        // permit up to 0.5 seconds to bind
        configuration.setBindTimeout(500);
        DefaultSmppServer server0 = new DefaultSmppServer(configuration, serverHandler0);
        server0.start();
        
        try {
            // there is an issue without telling the server how many worker threads
            // to create beforehand with starvation only Runtime.getRuntime().availableProcessors()
            // worker threads are created by default!!! (yikes)
            int workersToStarveWith = Runtime.getRuntime().availableProcessors();
            
            // initiate bind requests on all sessions we care about -- this should
            // technicaly "starve" the server of worker threads since they'll all
            // be blocked in a Thread.sleep
            for (int i = 0; i < workersToStarveWith; i++) {
                DefaultSmppClient client0 = new DefaultSmppClient();
                SmppSessionConfiguration sessionConfig0 = createDefaultConfiguration();
                sessionConfig0.setName("WorkerTest.Session." + i);
                // don't use default method of binding, connect the socket first
                DefaultSmppSession session0 = client0.doOpen(sessionConfig0, new DefaultSmppSessionHandler());
                // try to bind and execute a bind request and wait for a bind response
                BaseBind bindRequest = client0.createBindRequest(sessionConfig0);
                try {
                    // just send the request without caring if it succeeds
                    session0.sendRequestPdu(bindRequest, 2000, false);
                } catch (SmppChannelException e) {
                    // correct behavior
                }
            }
            
            // now try to bind normally -- since all previous workers are "starved"
            // this should fail to bind and the socket closed by the "BindTimer"
            DefaultSmppClient client0 = new DefaultSmppClient();
            SmppSessionConfiguration sessionConfig0 = createDefaultConfiguration();
            sessionConfig0.setName("WorkerTestChannelClosed.Session");
            sessionConfig0.setBindTimeout(750);
            
            try {
                client0.bind(sessionConfig0);
                Assert.fail();
            } catch (SmppChannelException e) {
                // the BindTimer should end up closing the connection since the
                // worker thread were "starved"
                logger.debug("Correctly received SmppChannelException during bind");
            }
            
        } finally {
            server0.destroy();
        }
    }
    
    @Test
    public void serverBindToUnavailablePortThrowsException() throws Exception {
        DefaultSmppServer server0 = createSmppServer();
        DefaultSmppServer server1 = createSmppServer();
        
        server0.start();
        try {
            server1.getConfiguration().setPort(server0.getConfiguration().getPort());
            try {
                // this should fail since we can't bind twice to same port
                server1.start();
                Assert.fail();
            } catch (SmppChannelException e) {
                // correct behavior
            }
        } finally {
            server0.destroy();
            if (server1 != null) {
                server1.destroy();
            }
        }
    }
}
