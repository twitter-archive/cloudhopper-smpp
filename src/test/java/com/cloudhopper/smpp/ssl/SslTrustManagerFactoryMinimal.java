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

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;

/**
 * Example of {@link TrustManagerFactorySpi} which accepts any server or client side certificate
 * even if it is invalid.
 *
 * @author bb
 * 
 */
public class SslTrustManagerFactoryMinimal extends TrustManagerFactorySpi {

    private static final TrustManager TRUST_ALL_MANAGER = new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        	// Do nothing.
        	// Trust all the clients. This is acceptable. We still have some degree of security, like the encrypted communication channel.
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			// Do nothing.
			// Trust all the servers, which is ok for smpp servers. It wouldn't be ok for a bank server.
        }
    };

    public static TrustManager[] getTrustManagers() {
        return new TrustManager[] { TRUST_ALL_MANAGER };
    }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
        return getTrustManagers();
    }

    @Override
    protected void engineInit(KeyStore keystore) throws KeyStoreException {
        // Unused
    }

    @Override
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws InvalidAlgorithmParameterException {
        // Unused
    }
}
