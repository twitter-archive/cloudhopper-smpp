package com.cloudhopper.smpp.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import org.jboss.netty.handler.ssl.SslHandler;

/**
 * To enable client certificate authentication:
 * <ul>
 * <li>Enable client authentication on the server side by calling
 *     {@link SSLEngine#setNeedClientAuth(boolean)} before creating
 *     {@link SslHandler}.</li>
 * <li>When initializing an {@link SSLContext} on the client side,
 *     specify the {@link KeyManager} that contains the client certificate as
 *     the first argument of {@link SSLContext#init(KeyManager[], javax.net.ssl.TrustManager[], java.security.SecureRandom)}.</li>
 * <li>When initializing an {@link SSLContext} on the server side,
 *     specify the proper {@link TrustManager} as the second argument of
 *     {@link SSLContext#init(KeyManager[], javax.net.ssl.TrustManager[], java.security.SecureRandom)}
 *     to validate the client certificate.</li>
 * </ul>
 */
public class SslContextFactory {
	private static final SSLContext SERVER_CONTEXT;
	private static final SSLContext CLIENT_CONTEXT;
	private static final String PROTOCOL = "SSL";
	private static final String ALGORITHM = "SunX509";
	private static final String KEYSTORETYPE = "JKS";
	private static final String KEYSTORE_FILEPATH = System.getProperty("user.dir") + "/src/test/resources/DummyCert.jks";
	private static final String KEYSTOREPASSWORD = "secret";
	private static final String CERTIFICATEPASSWORD = "secret";

	static {
		SSLContext serverContext = null;
		SSLContext clientContext = null;

		// create server context
		try {
			File keyStoreFile = new File(KEYSTORE_FILEPATH);
			if (keyStoreFile == null || !keyStoreFile.exists()) {
				throw new FileNotFoundException(KEYSTORE_FILEPATH);
			}
			FileInputStream instream = new FileInputStream(keyStoreFile);

			KeyStore ks = KeyStore.getInstance(KEYSTORETYPE);
			ks.load(instream, KEYSTOREPASSWORD.toCharArray());

			// Set up key manager factory to use our key store
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(ALGORITHM);
			kmf.init(ks, CERTIFICATEPASSWORD.toCharArray());

			// Initialize the SSLContext to work with our key managers.
			serverContext = SSLContext.getInstance(PROTOCOL);
			serverContext.init(kmf.getKeyManagers(), null, null);
		} catch (Exception e) {
			throw new Error("Failed to initialize the server-side SSLContext", e);
		}

		// create client context
		try {
			clientContext = SSLContext.getInstance(PROTOCOL);
			clientContext.init(null, SslTrustManagerFactory.getTrustManagers(), null);
		} catch (Exception e) {
			throw new Error("Failed to initialize the client-side SSLContext", e);
		}

		SERVER_CONTEXT = serverContext;
		CLIENT_CONTEXT = clientContext;
	}

	public static SSLContext getServerContext() {
		return SERVER_CONTEXT;
	}

	public static SSLContext getClientContext() {
		return CLIENT_CONTEXT;
	}

}
