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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.security.InvalidParameterException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CRL;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for SSLContext. This is used to create an SSLContext that is used by 
 * SMPP clients and servers that are configured to use SSL. This class is modeled 
 * after the netty SecureChatSslContextFactory example, the jetty SslContextFactory
 * utility, and the contribution of bbanko to cloudhopper-smpp.
 * 
 * @author garth, bbanko, Jetty7
 */
public class SslContextFactory {
    private static final Logger logger = LoggerFactory.getLogger(SslContextFactory.class);
    
    private SSLContext sslContext;
    private InputStream keyStoreInputStream;
    private InputStream trustStoreInputStream;

    private final SslConfiguration sslConfig;

    public SslContextFactory() throws Exception {
        this(new SslConfiguration());
    }

    public SslContextFactory(SslConfiguration sslConfig) throws Exception {
	this.sslConfig = sslConfig;
	init();
    }

    /**
     * Create the SSLContext
     */
    private void init() throws Exception {
        if (sslContext == null) {
            if (keyStoreInputStream == null && sslConfig.getKeyStorePath() == null &&
		trustStoreInputStream == null && sslConfig.getTrustStorePath() == null) {
                TrustManager[] trust_managers = null;
                if (sslConfig.isTrustAll()) {
                    logger.debug("No keystore or trust store configured.  ACCEPTING UNTRUSTED CERTIFICATES!!!!!");
                    // Create a trust manager that does not validate certificate chains
                    TrustManager trustAllCerts = new X509TrustManager() {
			    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			    }
			    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			    }

			    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			    }
			};
                    trust_managers = new TrustManager[] { trustAllCerts };
                }
                
                SecureRandom secureRandom = (sslConfig.getSecureRandomAlgorithm() == null)?null:
		    SecureRandom.getInstance(sslConfig.getSecureRandomAlgorithm());
                sslContext = SSLContext.getInstance(sslConfig.getProtocol());
                sslContext.init(null, trust_managers, secureRandom);
            } else {
                // verify that keystore and truststore
                // parameters are set up correctly               
                checkKeyStore();

                KeyStore keyStore = loadKeyStore();
                KeyStore trustStore = loadTrustStore();

                Collection<? extends CRL> crls = loadCRL(sslConfig.getCrlPath());

                if (sslConfig.isValidateCerts() && keyStore != null) {
                    if (sslConfig.getCertAlias() == null) {
                        List<String> aliases = Collections.list(keyStore.aliases());
			sslConfig.setCertAlias(aliases.size() == 1 ? aliases.get(0) : null);
                    }

                    Certificate cert = sslConfig.getCertAlias() == null?null:
			keyStore.getCertificate(sslConfig.getCertAlias());
                    if (cert == null) {
                        throw new Exception("No certificate found in the keystore" + (sslConfig.getCertAlias() == null ? "":" for alias " + sslConfig.getCertAlias()));
                    }

                    CertificateValidator validator = new CertificateValidator(trustStore, crls);
                    validator.setMaxCertPathLength(sslConfig.getMaxCertPathLength());
                    validator.setEnableCRLDP(sslConfig.isEnableCRLDP());
                    validator.setEnableOCSP(sslConfig.isEnableOCSP());
                    validator.setOcspResponderURL(sslConfig.getOcspResponderURL());
                    validator.validate(keyStore, cert);
                }

                KeyManager[] keyManagers = getKeyManagers(keyStore);
                TrustManager[] trustManagers = getTrustManagers(trustStore, crls);

                SecureRandom secureRandom = (sslConfig.getSecureRandomAlgorithm() == null)?null:
		    SecureRandom.getInstance(sslConfig.getSecureRandomAlgorithm());
                sslContext = (sslConfig.getProvider() == null)?
		    SSLContext.getInstance(sslConfig.getProtocol()):
		    SSLContext.getInstance(sslConfig.getProtocol(), sslConfig.getProvider());
                sslContext.init(keyManagers, trustManagers, secureRandom);

                SSLEngine engine = newSslEngine();
                
                logger.info("Enabled Protocols {} of {}",
			    Arrays.asList(engine.getEnabledProtocols()),
			    Arrays.asList(engine.getSupportedProtocols()));
		logger.debug("Enabled Ciphers {} of {}",
			     Arrays.asList(engine.getEnabledCipherSuites()),
			     Arrays.asList(engine.getSupportedCipherSuites()));
            }
        }
    }

    /**
     * Get the underlying SSLContext.
     */
    public SSLContext getSslContext() {
	return sslContext;
    }
    
    /**
     * Override this method to provide alternate way to load a keystore.
     *
     * @return the key store instance
     * @throws Exception
     */
    protected KeyStore loadKeyStore() throws Exception {
        return getKeyStore(keyStoreInputStream, sslConfig.getKeyStorePath(),
			   sslConfig.getKeyStoreType(), sslConfig.getKeyStoreProvider(),
			   sslConfig.getKeyStorePassword());
    }

    /**
     * Override this method to provide alternate way to load a truststore.
     *
     * @return the key store instance
     * @throws Exception
     */
    protected KeyStore loadTrustStore() throws Exception {
        return getKeyStore(trustStoreInputStream, sslConfig.getTrustStorePath(),
			   sslConfig.getTrustStoreType(), sslConfig.getTrustStoreProvider(),
			   sslConfig.getTrustStorePassword());
    }

    /**
     * Loads certificate revocation list (CRL) from a file.
     *
     * Required for integrations to be able to override the mechanism used to
     * load CRL in order to provide their own implementation.
     *
     * @param crlPath path of certificate revocation list file
     * @return Collection of CRL's
     * @throws Exception
     */
    protected Collection<? extends CRL> loadCRL(String crlPath) throws Exception {
        Collection<? extends CRL> crlList = null;
        if (crlPath != null) {
            InputStream in = null;
            try {
		in = new FileInputStream(crlPath); //assume it's a file
                crlList = CertificateFactory.getInstance("X.509").generateCRLs(in);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }
        return crlList;
    }

    /**
     * Loads keystore using an input stream or a file path in the same
     * order of precedence.
     *
     * Required for integrations to be able to override the mechanism
     * used to load a keystore in order to provide their own implementation.
     *
     * @param storeStream keystore input stream
     * @param storePath path of keystore file
     * @param storeType keystore type
     * @param storeProvider keystore provider
     * @param storePassword keystore password
     * @return created keystore
     * @throws Exception if the keystore cannot be obtained
     *
     */
    protected KeyStore getKeyStore(InputStream storeStream, String storePath, String storeType, String storeProvider, String storePassword) throws Exception {
        KeyStore keystore = null;
        if (storeStream != null || storePath != null) {
            InputStream inStream = storeStream;
            try {
                if (inStream == null) {
                    inStream = new FileInputStream(storePath); //assume it's a file
                }
                if (storeProvider != null) {
                    keystore = KeyStore.getInstance(storeType, storeProvider);
                } else {
                    keystore = KeyStore.getInstance(storeType);
                }
                keystore.load(inStream, storePassword == null ? null : storePassword.toCharArray());
            } finally {
                if (inStream != null) {
                    inStream.close();
                }
            }
        }
        return keystore;
    }

    protected KeyManager[] getKeyManagers(KeyStore keyStore) throws Exception {
        KeyManager[] managers = null;
        if (keyStore != null) {
            KeyManagerFactory keyManagerFactory =
		KeyManagerFactory.getInstance(sslConfig.getKeyManagerFactoryAlgorithm());
            keyManagerFactory.init(keyStore, sslConfig.getKeyManagerPassword() == null?
				   (sslConfig.getKeyStorePassword() == null?null:
				    sslConfig.getKeyStorePassword().toCharArray()):
				   sslConfig.getKeyManagerPassword().toCharArray());
            managers = keyManagerFactory.getKeyManagers();

            if (sslConfig.getCertAlias() != null) {
                for (int idx = 0; idx < managers.length; idx++) {
                    if (managers[idx] instanceof X509KeyManager) {
                        managers[idx] = new AliasedX509ExtendedKeyManager(sslConfig.getCertAlias(),
									  (X509KeyManager)managers[idx]);
                    }
                }
            }
        }
        return managers;
    }

    protected TrustManager[] getTrustManagers(KeyStore trustStore, Collection<? extends CRL> crls) throws Exception {   
        TrustManager[] managers = null;
        if (trustStore != null) {
            // Revocation checking is only supported for PKIX algorithm
            if (sslConfig.isValidatePeerCerts() &&
		sslConfig.getTrustManagerFactoryAlgorithm().equalsIgnoreCase("PKIX")) {
                PKIXBuilderParameters pbParams = new PKIXBuilderParameters(trustStore, new X509CertSelector());
                // Set maximum certification path length
                pbParams.setMaxPathLength(sslConfig.getMaxCertPathLength());
                // Make sure revocation checking is enabled
                pbParams.setRevocationEnabled(true);

                if (crls != null && !crls.isEmpty()) {
                    pbParams.addCertStore(CertStore.getInstance("Collection",
								new CollectionCertStoreParameters(crls)));
                }

                if (sslConfig.isEnableCRLDP()) {
                    // Enable Certificate Revocation List Distribution Points (CRLDP) support
                    System.setProperty("com.sun.security.enableCRLDP","true");
                }

                if (sslConfig.isEnableOCSP()) {
                    // Enable On-Line Certificate Status Protocol (OCSP) support
                    Security.setProperty("ocsp.enable","true");

                    if (sslConfig.getOcspResponderURL() != null) {
                        // Override location of OCSP Responder
                        Security.setProperty("ocsp.responderURL", sslConfig.getOcspResponderURL());
                    }
                }

                TrustManagerFactory trustManagerFactory =
		    TrustManagerFactory.getInstance(sslConfig.getTrustManagerFactoryAlgorithm());
                trustManagerFactory.init(new CertPathTrustManagerParameters(pbParams));
                managers = trustManagerFactory.getTrustManagers();
            } else {
                TrustManagerFactory trustManagerFactory =
		    TrustManagerFactory.getInstance(sslConfig.getTrustManagerFactoryAlgorithm());
                trustManagerFactory.init(trustStore);
                managers = trustManagerFactory.getTrustManagers();
            }
        }
        return managers;
    }

    /**
     * Check KeyStore Configuration. Ensures that if keystore has been
     * configured but there's no truststore, that keystore is
     * used as truststore.
     * @throws IllegalStateException if SslContextFactory configuration can't be used.
     */
    public void checkKeyStore() {
        if (sslContext != null)
            return; //nothing to check if using preconfigured context
        
        if (keyStoreInputStream == null &&
	    sslConfig.getKeyStorePath() == null) {
            throw new IllegalStateException("SSL doesn't have a valid keystore");
        }
        // if the keystore has been configured but there is no
        // truststore configured, use the keystore as the truststore
        if (trustStoreInputStream == null && sslConfig.getTrustStorePath() == null) {
            trustStoreInputStream = keyStoreInputStream;
            sslConfig.setTrustStorePath(sslConfig.getKeyStorePath());
            sslConfig.setTrustStoreType(sslConfig.getKeyStoreType());
            sslConfig.setTrustStoreProvider(sslConfig.getKeyStoreProvider());
            sslConfig.setTrustStorePassword(sslConfig.getKeyStorePassword());
            sslConfig.setTrustManagerFactoryAlgorithm(sslConfig.getKeyManagerFactoryAlgorithm());
        }

        // It's the same stream we cannot read it twice, so read it once in memory
        if (keyStoreInputStream != null && keyStoreInputStream == trustStoreInputStream) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
		streamCopy(keyStoreInputStream, baos, null, false);
                keyStoreInputStream.close();
                keyStoreInputStream = new ByteArrayInputStream(baos.toByteArray());
                trustStoreInputStream = new ByteArrayInputStream(baos.toByteArray());
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    /**
     * Copy the contents of is to os.
     * @param is
     * @param os
     * @param buf Can be null
     * @param close If true, is is closed after the copy.
     * @throws IOException
     */
    private static void streamCopy(InputStream is, OutputStream os, byte[] buf, boolean close) throws IOException {
        int len;
        if (buf == null) {
            buf = new byte[4096];
        }
        while ((len = is.read(buf)) > 0) {
            os.write(buf, 0, len);
        }
        os.flush();
        if (close) {
            is.close();
        }
    }

    /**
     * Does an object array include an object.
     * @param arr The array
     * @param obj The object
     */
    private static boolean contains(Object[] arr, Object obj) {
	for (Object o : arr) {
	    if (o.equals(obj)) return true;
	}
	return false;
    }

    /**
     * Select cipher suites to be used by the connector
     * based on configured inclusion and exclusion lists
     * as well as enabled and supported cipher suite lists.
     * @param enabledCipherSuites Array of enabled cipher suites
     * @param supportedCipherSuites Array of supported cipher suites
     * @return Array of cipher suites to enable
     */
    public String[] selectProtocols(String[] enabledProtocols, String[] supportedProtocols) {
        Set<String> selected_protocols = new HashSet<String>();
        
        // Set the starting protocols - either from the included or enabled list
        if (sslConfig.getIncludeProtocols() != null) {
            // Use only the supported included protocols
            for (String protocol : supportedProtocols)
                if (contains(sslConfig.getIncludeProtocols(), protocol))
                    selected_protocols.add(protocol);
        } else {
            selected_protocols.addAll(Arrays.asList(enabledProtocols));
        }
        
        // Remove any excluded protocols
        if (sslConfig.getExcludeProtocols() != null) {
            selected_protocols.removeAll(Arrays.asList(sslConfig.getExcludeProtocols()));
        }

        return selected_protocols.toArray(new String[selected_protocols.size()]);
    }
    
    /**
     * Select cipher suites to be used by the connector
     * based on configured inclusion and exclusion lists
     * as well as enabled and supported cipher suite lists.
     * @param enabledCipherSuites Array of enabled cipher suites
     * @param supportedCipherSuites Array of supported cipher suites
     * @return Array of cipher suites to enable
     */
    public String[] selectCipherSuites(String[] enabledCipherSuites, String[] supportedCipherSuites) {
        Set<String> selected_ciphers = new HashSet<String>();
        
        // Set the starting ciphers - either from the included or enabled list
        if (sslConfig.getIncludeCipherSuites() != null) {
            // Use only the supported included ciphers
            for (String cipherSuite : supportedCipherSuites)
                if (contains(sslConfig.getIncludeCipherSuites(), cipherSuite))
                    selected_ciphers.add(cipherSuite);
        } else {
            selected_ciphers.addAll(Arrays.asList(enabledCipherSuites));
        }
        
        // Remove any excluded ciphers
        if (sslConfig.getExcludeCipherSuites() != null) {
            selected_ciphers.removeAll(Arrays.asList(sslConfig.getExcludeCipherSuites()));
	}

        return selected_ciphers.toArray(new String[selected_ciphers.size()]);
    }

    public SSLServerSocket newSslServerSocket(String host,int port,int backlog) throws IOException {
        SSLServerSocketFactory factory = sslContext.getServerSocketFactory();

        SSLServerSocket socket = 
            (SSLServerSocket) (host==null ?
			       factory.createServerSocket(port, backlog):
			       factory.createServerSocket(port, backlog, InetAddress.getByName(host)));

        if (sslConfig.getWantClientAuth())
            socket.setWantClientAuth(sslConfig.getWantClientAuth());
        if (sslConfig.getNeedClientAuth())
            socket.setNeedClientAuth(sslConfig.getNeedClientAuth());

        socket.setEnabledCipherSuites(selectCipherSuites(socket.getEnabledCipherSuites(),
							 socket.getSupportedCipherSuites()));
        socket.setEnabledProtocols(selectProtocols(socket.getEnabledProtocols(),socket.getSupportedProtocols()));
	
        return socket;
    }
    
    /**
     * Get an SSLSocket from this context.
     * {@link SSLContext#getSocketFactory()}
     */
    public SSLSocket newSslSocket() throws IOException {
        SSLSocketFactory factory = sslContext.getSocketFactory();
        
        SSLSocket socket = (SSLSocket)factory.createSocket();
        
        if (sslConfig.getWantClientAuth())
            socket.setWantClientAuth(sslConfig.getWantClientAuth());
        if (sslConfig.getNeedClientAuth())
            socket.setNeedClientAuth(sslConfig.getNeedClientAuth());

        socket.setEnabledCipherSuites(selectCipherSuites(socket.getEnabledCipherSuites(),
							 socket.getSupportedCipherSuites()));   
        socket.setEnabledProtocols(selectProtocols(socket.getEnabledProtocols(),socket.getSupportedProtocols()));

        return socket;
    }
    
    /**
     * Get an SSLEngine from this context. Use this method to hint instead of the 
     * no-op for an internal session reuse strategy. Also, some cipher suites require
     * remote hostname information.
     * {@link SSLContext#createSSLEngine(String,int)}
     * @param host The non-authoritative name of the host
     * @param port The non-authoritative port 
     */
    public SSLEngine newSslEngine(String host,int port) {
        SSLEngine sslEngine = sslConfig.isSessionCachingEnabled()
            ?sslContext.createSSLEngine(host, port)
            :sslContext.createSSLEngine();
	
        customize(sslEngine);
        return sslEngine;
    }
    
    /**
     * Get an SSLEngine from this context.
     * {@link SSLContext#createSSLEngine()}
     */
    public SSLEngine newSslEngine() {
        SSLEngine sslEngine = sslContext.createSSLEngine();
        customize(sslEngine);
        return sslEngine;
    }

    private void customize(SSLEngine sslEngine) {
        if (sslConfig.getWantClientAuth())
            sslEngine.setWantClientAuth(sslConfig.getWantClientAuth());
        if (sslConfig.getNeedClientAuth())
            sslEngine.setNeedClientAuth(sslConfig.getNeedClientAuth());

        sslEngine.setEnabledCipherSuites(selectCipherSuites(sslEngine.getEnabledCipherSuites(),
							    sslEngine.getSupportedCipherSuites()));
	
        sslEngine.setEnabledProtocols(selectProtocols(sslEngine.getEnabledProtocols(),
						      sslEngine.getSupportedProtocols()));
    }
    
}
