/**
 * Copyright (C) 2011 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.cloudhopper.smpp.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/**
 * Minimal security SSLContext factory. The server must have a cerificate (at
 * least a dummy one) to initiate SSL communication. Client doesn't need any
 * certificate. The benefit of such SSLContext is the simplicity. The client
 * doesn't need to register any certificate, and we still have encrypted
 * communication channel.
 *
 * @author bb
 *
 */
public class SslContextFactoryMinimal {

    private static final SSLContext SERVER_CONTEXT;
    private static final SSLContext CLIENT_CONTEXT;
    private static final String PROTOCOL = "SSL";
    private static final String ALGORITHM = "SunX509";
    private static final String KEYSTORETYPE = "JKS";
    private static final String SERVER_KEYSTORE_FILEPATH = System.getProperty("user.dir") + "/src/test/resources/server.jks";
    private static final String SERVERSTOREPASSWORD = "serverstorepass";
    private static final String SERVERKEYPASSWORD = "serverkeypass";

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

            // Initialize the SSLContext to work with our key managers
            serverContext = SSLContext.getInstance(PROTOCOL);
            serverContext.init(kmf.getKeyManagers(), null, null);
            //serverContext.init(kmf.getKeyManagers(), SslTrustManagerFactoryMinimal.getTrustManagers(), null);
        } catch (Exception e) {
            throw new Error("Failed to initialize the server-side SSLContext", e);
        }

        // create client context
        try {
            clientContext = SSLContext.getInstance(PROTOCOL);
            clientContext.init(null, SslTrustManagerFactoryMinimal.getTrustManagers(), null);
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
