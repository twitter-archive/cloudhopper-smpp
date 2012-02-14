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
package com.cloudhopper.smpp.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

/**
 * SSLContext factory.
 * If you need to trust both sides, the server and the client must have appropriate certificate stored in java keystore (jks).
 * The jks files below are for testing only. Created are with java keytool. The resulting jks are a self-signed certificates that are good enough for testing or internal use. 
 * In a real world applications you will need to buy a third party trusted certificate from a CA and import it in your jks.
 * 
 * Command lines to create sample jks:
 * 1. generate private key and self-signed certificate for server side; 
 * 		keytool -genkey -alias server_full -keypass serverkeypass -keystore server.jks -storepass serverstorepass -dname "CN=Test Certificate-do not use in production" -validity 3650
 * 2. export public certificate from server.jks
 *		keytool -export -alias server_full -file server_pub.crt -keystore server.jks -storepass serverstorepass 
 * 3. generate private key and self-signed certificate for client side; 
 *		keytool -genkey -alias client_full -keypass clientkeypass -keystore client.jks -storepass clientstorepass -dname "CN=Test Certificate-do not use in production" -validity 3650
 * 4. export public certificate from client.jks
 *		keytool -export -alias client_full -file client_pub.crt -keystore client.jks -storepass clientstorepass
 * 5. import and merge public server certificate into client.jks. Answer yes to question: "Trust this certificate?"
 *		keytool -import -alias server_public -file server_pub.crt -keystore client.jks -storepass clientstorepass
 * 6. import and merge public client certificate into server.jks. Answer yes to question: "Trust this certificate?"
 *		keytool -import -alias client_public -file client_pub.crt -keystore server.jks -storepass serverstorepass
 * 7. list the content of both jkses
 *		keytool -list -keystore client.jks -storepass clientstorepass
 *		keytool -list -keystore server.jks -storepass serverstorepass
 * 
 * Server side must call {@link SSLEngine#setNeedClientAuth(boolean)} to activate the client validation.
 *
 * @author bb
 * 
 */
public class SslContextFactoryTrusted {
	private static final SSLContext SERVER_CONTEXT;
	private static final SSLContext CLIENT_CONTEXT;
	private static final String PROTOCOL = "SSL";
	private static final String ALGORITHM = "SunX509";
	private static final String KEYSTORETYPE = "JKS";
	
	private static final String SERVER_KEYSTORE_FILEPATH = System.getProperty("user.dir") + "/src/test/resources/server.jks";
	private static final String SERVERSTOREPASSWORD = "serverstorepass";
	private static final String SERVERKEYPASSWORD = "serverkeypass";

	private static final String CLIENT_KEYSTORE_FILEPATH = System.getProperty("user.dir") + "/src/test/resources/client.jks";
	private static final String CLIENTSTOREPASSWORD = "clientstorepass";
	private static final String CLIENTKEYPASSWORD = "clientkeypass";

	static {
		SSLContext serverContext = null;
		SSLContext clientContext = null;

		// create server context
		try {
			File keyStoreFile = new File(SERVER_KEYSTORE_FILEPATH);
			if (keyStoreFile == null || !keyStoreFile.exists()) {
				throw new FileNotFoundException(SERVER_KEYSTORE_FILEPATH);
			}
			FileInputStream instream = new FileInputStream(keyStoreFile);

			KeyStore ks = KeyStore.getInstance(KEYSTORETYPE);
			ks.load(instream, SERVERSTOREPASSWORD.toCharArray());

			// Set up key manager factory to use our key store
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(ALGORITHM);
			kmf.init(ks, SERVERKEYPASSWORD.toCharArray());

			// Set up default trust manager factory
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(ALGORITHM);
			tmf.init(ks);
			
			// Initialize the SSLContext to work with our key managers.
			serverContext = SSLContext.getInstance(PROTOCOL);
			serverContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			// Next line is an example for custom trust manager logic. You can define your rules about when to trust the clients.
			// Don't forget to call SSLEngine.setNeedClientAuth(true) to activate the validation.
			//serverContext.init(kmf.getKeyManagers(), SslTrustManagerFactoryTrusted.getTrustManagers(), null);
		} catch (Exception e) {
			throw new Error("Failed to initialize the server-side SSLContext", e);
		}
		
		// create client context
		try {
			File keyStoreFile = new File(CLIENT_KEYSTORE_FILEPATH);
			if (keyStoreFile == null || !keyStoreFile.exists()) {
				throw new FileNotFoundException(CLIENT_KEYSTORE_FILEPATH);
			}
			FileInputStream instream = new FileInputStream(keyStoreFile);

			KeyStore ks = KeyStore.getInstance(KEYSTORETYPE);
			ks.load(instream, CLIENTSTOREPASSWORD.toCharArray());

			// Set up key manager factory to use our key store
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(ALGORITHM);
			kmf.init(ks, CLIENTKEYPASSWORD.toCharArray());

			// Set up trust manager factory
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(ALGORITHM);
			tmf.init(ks);
			
			// Initialize the SSLContext to work with our key managers.
			clientContext = SSLContext.getInstance(PROTOCOL);
			clientContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			//clientContext.init(kmf.getKeyManagers(), SslTrustManagerFactoryTrusted.getTrustManagers(), null);
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
