package com.cloudhopper.smpp.ssl;

import java.util.HashSet;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.cloudhopper.smpp.type.SmppProcessingException;

public class SSlSessionTest {
	private static final Logger logger = LoggerFactory.getLogger(DefaultSmppServerTest.class);

	public static final int PORT = 9784;
	public static final String SYSTEMID = "smppclient1";
	public static final String PASSWORD = "password";

	private TestSmppServerHandler serverHandler = new TestSmppServerHandler();

	public SmppServerConfiguration createServerConfigurationNoSSL() {
		SmppServerConfiguration configuration = new SmppServerConfiguration();
		configuration.setPort(PORT);
		configuration.setSystemId("cloudhopper");
		return configuration;
	}

	public SmppServerConfiguration createServerConfigurationWeakSSL() {
		SmppServerConfiguration configuration = createServerConfigurationNoSSL();
		// Add SSL handler to the server configuration.
		// In this example, we use a bogus certificate at the server side.
		configuration.setSslEngine(SslContextFactoryMinimal.getServerContext().createSSLEngine());
		return configuration;
	}

	public SmppServerConfiguration createServerConfigurationStrongSSL() {
		SmppServerConfiguration configuration = createServerConfigurationNoSSL();
		// Add SSL handler to the server configuration.
		// In this example, we use a bogus certificate at the server side.
		configuration.setSslEngine(SslContextFactoryTrusted.getServerContext().createSSLEngine());
		// activate clients authentication
		configuration.getSslEngine().setNeedClientAuth(true);
		return configuration;
	}

	public DefaultSmppServer createSmppServer(SmppServerConfiguration configuration) {
		DefaultSmppServer smppServer = new DefaultSmppServer(configuration, serverHandler);
		return smppServer;
	}

	public SmppSessionConfiguration createClientConfigurationNoSSL() {
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

	public SmppSessionConfiguration createClientConfigurationWeakSSL() {
		SmppSessionConfiguration configuration = createClientConfigurationNoSSL();
		// Add SSL handler to encrypt and decrypt everything.
		configuration.setSslEngine(SslContextFactoryMinimal.getClientContext().createSSLEngine());
		return configuration;
	}

	public SmppSessionConfiguration createClientConfigurationStrongSSL() {
		SmppSessionConfiguration configuration = createClientConfigurationNoSSL();
		// Add SSL handler to encrypt and validate the other side.
		configuration.setSslEngine(SslContextFactoryTrusted.getClientContext().createSSLEngine());
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

			//throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL, null);
		}

		@Override
		public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse) {
			sessions.add(session);
			// need to do something it now (flag we're ready)
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
	public void serverOverSSL() throws Exception {
		// server is SSL, client is not!
		DefaultSmppServer server0 = createSmppServer(createServerConfigurationWeakSSL());
		server0.start();

		DefaultSmppClient client0 = new DefaultSmppClient();
		SmppSessionConfiguration sessionConfig0 = createClientConfigurationNoSSL();

		try {
			// this should fail
			SmppSession session0 = client0.bind(sessionConfig0);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertNotNull(e);
		} finally {
			server0.destroy();
		}
	}

	@Test @Ignore("ignored because it hangs out at bind(), after UnboundSmppSession.fireExceptionThrown()")
	public void clientOverSSL() throws Exception {
		// server is not SSL, client is SSL
		DefaultSmppServer server0 = createSmppServer(createServerConfigurationNoSSL());
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

			Thread.sleep(100);

			Assert.assertEquals(1, serverHandler.sessions.size());
			Assert.assertEquals(1, server0.getChannels().size());

			SmppServerSession serverSession0 = serverHandler.sessions.iterator().next();
			Assert.assertEquals(true, serverSession0.isBound());
			Assert.assertEquals(SmppBindType.TRANSCEIVER, serverSession0.getBindType());
			Assert.assertEquals(SmppSession.Type.SERVER, serverSession0.getLocalType());
			Assert.assertEquals(SmppSession.Type.CLIENT, serverSession0.getRemoteType());

			serverSession0.close();
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
	public void threeConnectionsOverSSL() throws Exception {
		// both server and client are SSL
		DefaultSmppServer server0 = createSmppServer(createServerConfigurationWeakSSL());
		server0.start();

		DefaultSmppClient client0 = new DefaultSmppClient();
		SmppSessionConfiguration sessionConfig0 = createClientConfigurationWeakSSL();

		try {
			// three clients in parallel
			SmppSession session0 = client0.bind(sessionConfig0);
			SmppSession session1 = client0.bind(sessionConfig0);
			SmppSession session2 = client0.bind(sessionConfig0);

			Thread.sleep(300);

			Assert.assertEquals(3, serverHandler.sessions.size());
			Assert.assertEquals(3, server0.getChannels().size());
		} finally {
			server0.destroy();
		}
	}

	@Test
	public void TrustedClientSSL() throws Exception {
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
	public void UntrustedClientSSL() throws Exception {
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
