package com.cloudhopper.smpp.ssl;

/*
 * #%L
 * ch-smpp
 * %%
 * Copyright (C) 2009 - 2013 Cloudhopper by Twitter
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

import java.util.HashSet;
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.impl.DefaultSmppServerTest;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.EnquireLinkResp;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.type.SmppChannelConnectException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppProcessingException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author bbanko, garth
 */
public class SslSessionTest {

    private static final Logger logger = LoggerFactory.getLogger(SslSessionTest.class);
    private static final int PORT = 9784;
    private static final String SYSTEMID = "smppclient1";
    private static final String PASSWORD = "password";

    private TestSmppServerHandler serverHandler;

    @Before
    public void setUp() throws Exception {
	serverHandler = new TestSmppServerHandler();
    }

    private SmppServerConfiguration createServerConfigurationNoSSL() {
        SmppServerConfiguration configuration = new SmppServerConfiguration();
        configuration.setPort(PORT);
        configuration.setSystemId("cloudhopper");
        return configuration;
    }

    private SmppServerConfiguration createServerConfigurationWeakSSL() {
        SmppServerConfiguration configuration = createServerConfigurationNoSSL();
	SslConfiguration sslConfig = new SslConfiguration();
	sslConfig.setKeyStorePath("src/test/resources/keystore");
	sslConfig.setKeyStorePassword("changeit");
	sslConfig.setKeyManagerPassword("changeit");
	sslConfig.setTrustStorePath("src/test/resources/keystore");
	sslConfig.setTrustStorePassword("changeit");
	configuration.setUseSsl(true);
	configuration.setSslConfiguration(sslConfig);
        return configuration;
    }

    private SmppServerConfiguration createServerConfigurationStrongSSL() {
        SmppServerConfiguration configuration = createServerConfigurationNoSSL();
	SslConfiguration sslConfig = new SslConfiguration();
	sslConfig.setKeyStorePath("src/test/resources/keystore");
	sslConfig.setKeyStorePassword("changeit");
	sslConfig.setKeyManagerPassword("changeit");
	sslConfig.setTrustStorePath("src/test/resources/keystore");
	sslConfig.setTrustStorePassword("changeit");
        // activate clients authentication
        sslConfig.setNeedClientAuth(true);
	configuration.setUseSsl(true);
	configuration.setSslConfiguration(sslConfig);
        return configuration;
    }

    private DefaultSmppServer createSmppServer(SmppServerConfiguration configuration) {
        DefaultSmppServer smppServer = new DefaultSmppServer(configuration, serverHandler);
        return smppServer;
    }

    private SmppSessionConfiguration createClientConfigurationNoSSL() {
        SmppSessionConfiguration configuration = new SmppSessionConfiguration();
        configuration.setWindowSize(1);
        configuration.setName("Tester.Session.0");
        configuration.setType(SmppBindType.TRANSCEIVER);
        configuration.setHost("localhost");
        configuration.setPort(PORT);
        configuration.setConnectTimeout(200);
        configuration.setBindTimeout(200);
        configuration.setSystemId(SYSTEMID);
        configuration.setPassword(PASSWORD);
        configuration.getLoggingOptions().setLogBytes(true);
        return configuration;
    }

    private SmppSessionConfiguration createClientConfigurationWeakSSL() {
        SmppSessionConfiguration configuration = createClientConfigurationNoSSL();
	SslConfiguration sslConfig = new SslConfiguration();
	configuration.setUseSsl(true);
	configuration.setSslConfiguration(sslConfig);
        return configuration;
    }

    private SmppSessionConfiguration createClientConfigurationStrongSSL() {
        SmppSessionConfiguration configuration = createClientConfigurationNoSSL();
	SslConfiguration sslConfig = new SslConfiguration();
	configuration.setUseSsl(true);
	configuration.setSslConfiguration(sslConfig);
        return configuration;
    }

    public static class TestSmppServerHandler implements SmppServerHandler {
        public HashSet<SmppServerSession> sessions = new HashSet<SmppServerSession>();
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
        }
        @Override
        public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse) {
            sessions.add(session);
            session.serverReady(new TestSmppSessionHandler());
        }
        @Override
        public void sessionDestroyed(Long sessionId, SmppServerSession session) {
            sessions.remove(session);
        }
    }

    public static class TestSmppSessionHandler extends DefaultSmppSessionHandler {
        @Override
        public PduResponse firePduRequestReceived(PduRequest pduRequest) {
            return pduRequest.createResponse();
        }
    }

    @Test
    public void serverOverSSLButClientIsNotSSL() throws Exception {
        // server is SSL, client is not!
        DefaultSmppServer server0 = createSmppServer(createServerConfigurationWeakSSL());
        server0.start();

        DefaultSmppClient client0 = new DefaultSmppClient();
        SmppSessionConfiguration sessionConfig0 = createClientConfigurationNoSSL();
        // verify that the bad connection is detected earlier than the 30 second timeouts
        sessionConfig0.setConnectTimeout(30000);
        sessionConfig0.setBindTimeout(30000);

        long start = System.currentTimeMillis();
        try {
            // this should fail
            SmppSession session0 = client0.bind(sessionConfig0);
            Assert.fail();
        } catch (SmppChannelException e) {
            long stop = System.currentTimeMillis();
            Assert.assertNotNull(e);
            logger.info("Expected exception: " + e.getMessage());
            Assert.assertTrue((stop-start) < 30000);
        } finally {
            server0.destroy();
        }
    }

    @Test
    public void clientOverSSLButServerIsNotSSL() throws Exception {
        // server is not SSL, client is SSL
        DefaultSmppServer server0 = createSmppServer(createServerConfigurationNoSSL());
        server0.start();

        DefaultSmppClient client0 = new DefaultSmppClient();
        SmppSessionConfiguration sessionConfig0 = createClientConfigurationWeakSSL();
        // the server immediately closed the connection and there is a workaround
        // enabled to detect this closure much faster than a 30 second connect timeout
        // we set these to be longer than normal to verify the bind fails in less
        // time than these settings are set for
        sessionConfig0.setConnectTimeout(30000);
        sessionConfig0.setBindTimeout(30000);
        long start = System.currentTimeMillis();
        try {
            // this should fail
            SmppSession session0 = client0.bind(sessionConfig0);
            Assert.fail();
        } catch (SmppChannelException e) {
            long stop = System.currentTimeMillis();
            Assert.assertNotNull(e);
            logger.info("Expected exception: " + e.getMessage());
            // workaround for this unit test is working correctly since the
            // connection close event should be caught earlier than the connectTimeout
            Assert.assertTrue((stop-start) < 30000);
        } finally {
            server0.destroy();
        }
    }

    @Test
    public void bindOverSSL() throws Exception {
        // both server and client are SSL
        DefaultSmppServer server0 = createSmppServer(createServerConfigurationWeakSSL());
        server0.start();

        DefaultSmppClient client0 = new DefaultSmppClient();
        SmppSessionConfiguration sessionConfig0 = createClientConfigurationWeakSSL();

        try {
            // this should actually work
            SmppSession session0 = client0.bind(sessionConfig0);
            Thread.sleep(200);

            Assert.assertEquals(1, serverHandler.sessions.size());
            Assert.assertEquals(1, server0.getChannels().size());

            SmppServerSession serverSession0 = serverHandler.sessions.iterator().next();
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
    public void enquireLinkOverSSL() throws Exception {
        // both server and client are SSL
        DefaultSmppServer server0 = createSmppServer(createServerConfigurationWeakSSL());
        server0.start();

        DefaultSmppClient client0 = new DefaultSmppClient();
        SmppSessionConfiguration sessionConfig0 = createClientConfigurationWeakSSL();

        try {
            SmppSession session0 = client0.bind(sessionConfig0);
            Thread.sleep(100);

            // send encrypted enquire link; receive encrypted response.
            EnquireLinkResp enquireLinkResp = session0.enquireLink(new EnquireLink(), 1000);

            Assert.assertEquals(0, enquireLinkResp.getCommandStatus());
            Assert.assertEquals("OK", enquireLinkResp.getResultMessage());
        } finally {
            server0.destroy();
        }
    }

    @Test
    @Ignore
    public void trustedClientSSL() throws Exception {
        // both server and client are SSL with compatible certificate.
        DefaultSmppServer server0 = createSmppServer(createServerConfigurationStrongSSL());
        server0.start();

        DefaultSmppClient client0 = new DefaultSmppClient();
        SmppSessionConfiguration sessionConfig0 = createClientConfigurationStrongSSL();

        try {
            // this should work
            SmppSession session0 = client0.bind(sessionConfig0);
            Assert.assertNotNull(session0);
        } catch (Exception e) {
            Assert.fail();
        } finally {
            server0.destroy();
        }
    }

    @Test
    @Ignore
    public void untrustedClientSSL() throws Exception {
        // both server and client are SSL. But the client is certificateless.
        // server has activated trust manager that refuses untrusted clients.
        DefaultSmppServer server0 = createSmppServer(createServerConfigurationStrongSSL());
        server0.start();

        DefaultSmppClient client0 = new DefaultSmppClient();
        SmppSessionConfiguration sessionConfig0 = createClientConfigurationWeakSSL();
        try {
            // this should fail
            SmppSession session0 = client0.bind(sessionConfig0);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
            logger.info("Expected exception: " + e.getMessage());
        } finally {
            server0.destroy();
        }
    }

}
