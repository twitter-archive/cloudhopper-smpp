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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;

/**
 * Example of {@link TrustManagerFactorySpi} which accepts only valid certificates, both from the client and server side.
 *
 * @author bb
 * 
 */
public class SslTrustManagerFactoryTrusted extends TrustManagerFactorySpi {

    private static final TrustManager TRUST_MANAGER = new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        	// Server checks the client here.
      		// You will reach here only if you enabled client certificate auth at the server config:
        	// configuration.getSslEngine().setNeedClientAuth(true);
            System.out.println("Client certificate: " + chain[0].getSubjectDN());
            Validate(chain[0]);
        }
        
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        	// Client checks the server here.
            System.out.println("Server certificate: " + chain[0].getSubjectDN());
            Validate(chain[0]);
        }
        
        // Custom logic to check the validity of the client and/or server certificate.
        // You can add more custom logic here that decides whether to trust the other side.
        private void Validate(X509Certificate cert) throws CertificateException {
        	//Check if it has expired.
			try {
				cert.checkValidity();
			}
			catch(Exception e) {
				throw new CertificateException("Certificate not trusted.",e);
			}
        	
     		X509Certificate caCertificate = null;
			try {
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				// certificate.pem is the certificate file issued by your CA
				FileInputStream finStream = new FileInputStream("path/to/certificate.pem");
				caCertificate = (X509Certificate)cf.generateCertificate(finStream);
			} catch (FileNotFoundException e) {
				throw new CertificateException("Missing pem file.", e);
			} 
			
			//Check if certificate send is your CA's
			if (!cert.equals(caCertificate)) {
				try	{
					//Not your CA's. Check if it has been signed by your CA
					cert.verify(caCertificate.getPublicKey());
				}
				catch(Exception e) {
					throw new CertificateException("Certificate not trusted.", e);
				}
			}
        }
    };

    public static TrustManager[] getTrustManagers() {
        return new TrustManager[] { TRUST_MANAGER };
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
